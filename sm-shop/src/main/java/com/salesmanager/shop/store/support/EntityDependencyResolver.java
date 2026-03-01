package com.salesmanager.shop.store.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Transient;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.salesmanager.shop.store.api.exception.ServiceRuntimeException;

/**
 * Resolves JPA entity dependencies recursively by inspecting relationship annotations.
 *
 * It attempts to reuse existing rows by querying on unique/natural-key-like fields before persisting
 * transient dependency entities, preventing duplicate reference records.
 */
@Component
public class EntityDependencyResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityDependencyResolver.class);

	private static final List<String> LOOKUP_FIELD_PRIORITY = List.of(
			"code",
			"isoCode",
			"groupName",
			"permissionName",
			"adminName",
			"name");

	private final EntityManager entityManager;

	public EntityDependencyResolver(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void resolveDependencies(Object rootEntity) {
		if (rootEntity == null) {
			return;
		}
		ResolutionContext context = new ResolutionContext(rootEntity);
		resolveEntityRelations(rootEntity, context);
	}

	private void resolveEntityRelations(Object entity, ResolutionContext context) {
		if (entity == null) {
			return;
		}
		if (context.processed.contains(entity)) {
			return;
		}
		if (!context.visiting.add(entity)) {
			// Cycle detected in object graph; leave current reference as-is and unwind.
			return;
		}

		for (Field field : getAllFields(entityClassOf(entity))) {
			if (!isRelationshipField(field)) {
				continue;
			}

			Object value = readField(field, entity);
			if (value == null) {
				continue;
			}

			if (value instanceof Collection<?> collectionValue) {
				resolveCollectionDependencies(field, entity, collectionValue, context);
			} else {
				Object resolved = resolveReference(value, context);
				if (resolved != null && resolved != value) {
					writeField(field, entity, resolved);
				}
			}
		}

		context.visiting.remove(entity);
		context.processed.add(entity);
	}

	private void resolveCollectionDependencies(Field field, Object owner, Collection<?> values, ResolutionContext context) {
		if (values.isEmpty()) {
			return;
		}

		List<Object> resolvedItems = new ArrayList<Object>();
		Set<String> dedupeKeys = new LinkedHashSet<String>();

		for (Object raw : values) {
			Object resolved = resolveReference(raw, context);
			if (resolved == null) {
				continue;
			}
			String dedupeKey = dependencyKey(resolved);
			if (dedupeKeys.add(dedupeKey)) {
				resolvedItems.add(resolved);
			}
		}

		@SuppressWarnings("unchecked")
		Collection<Object> target = (Collection<Object>) readField(field, owner);
		target.clear();
		for (Object item : resolvedItems) {
			target.add(item);
		}
	}

	private Object resolveReference(Object reference, ResolutionContext context) {
		if (reference == null) {
			return null;
		}
		if (reference == context.rootEntity) {
			return reference;
		}

		resolveEntityRelations(reference, context);

		Object identifier = getIdentifier(reference);
		if (identifier != null) {
			Class<?> entityClass = entityClassOf(reference);
			Object managed = entityManager.find(entityClass, identifier);
			return managed != null ? managed : reference;
		}

		Optional<Object> existing = findExisting(reference);
		if (existing.isPresent()) {
			return existing.get();
		}

		try {
			entityManager.persist(reference);
			entityManager.flush();
		} catch (Exception e) {
			throw new ServiceRuntimeException("Cannot persist dependency [" + reference.getClass().getSimpleName() + "]", e);
		}

		Object persistedId = getIdentifier(reference);
		if (persistedId == null) {
			throw new ServiceRuntimeException("Dependency persisted without id [" + reference.getClass().getSimpleName() + "]");
		}

		return reference;
	}

	private Optional<Object> findExisting(Object reference) {
		Class<?> entityClass = entityClassOf(reference);
		if (!isEntityClass(entityClass)) {
			return Optional.empty();
		}

		List<Field> lookupFields = collectLookupFields(reference);
		for (Field lookupField : lookupFields) {
			Object value = readField(lookupField, reference);
			if (value == null) {
				continue;
			}

			Object found = querySingleByField(entityClass, lookupField.getName(), value);
			if (found != null) {
				LOGGER.debug("Reused dependency {} by {}={}", entityClass.getSimpleName(), lookupField.getName(), value);
				return Optional.of(found);
			}
		}

		return Optional.empty();
	}

	private Object querySingleByField(Class<?> entityClass, String fieldName, Object value) {
		try {
			String entityName = entityManager.getMetamodel().entity(entityClass).getName();
			String jpql = "select e from " + entityName + " e where e." + fieldName + " = :value";
			List<?> results = entityManager.createQuery(jpql, entityClass)
					.setParameter("value", value)
					.setMaxResults(1)
					.getResultList();
			return results.isEmpty() ? null : results.get(0);
		} catch (Exception e) {
			throw new ServiceRuntimeException(
					"Cannot query dependency [" + entityClass.getSimpleName() + "] by [" + fieldName + "]", e);
		}
	}

	private List<Field> collectLookupFields(Object entity) {
		Map<String, Field> selected = new LinkedHashMap<String, Field>();
		List<Field> fields = getAllFields(entity.getClass());

		for (String preferredName : LOOKUP_FIELD_PRIORITY) {
			for (Field field : fields) {
				if (!preferredName.equals(field.getName())) {
					continue;
				}
				if (isLookupCandidate(field, entity, true)) {
					selected.put(field.getName(), field);
				}
			}
		}

		for (Field field : fields) {
			if (isLookupCandidate(field, entity, true)) {
				selected.put(field.getName(), field);
			}
		}

		for (String preferredName : LOOKUP_FIELD_PRIORITY) {
			for (Field field : fields) {
				if (!preferredName.equals(field.getName())) {
					continue;
				}
				if (isLookupCandidate(field, entity, false)) {
					selected.putIfAbsent(field.getName(), field);
				}
			}
		}

		return new ArrayList<Field>(selected.values());
	}

	private boolean isLookupCandidate(Field field, Object entity, boolean uniqueOnly) {
		if (Modifier.isStatic(field.getModifiers())) {
			return false;
		}
		if (field.getAnnotation(Transient.class) != null) {
			return false;
		}
		if (field.getAnnotation(ManyToOne.class) != null || field.getAnnotation(OneToOne.class) != null
				|| field.getAnnotation(ManyToMany.class) != null || field.getAnnotation(OneToMany.class) != null) {
			return false;
		}
		if ("id".equals(field.getName())) {
			return false;
		}

		Column column = field.getAnnotation(Column.class);
		if (uniqueOnly && (column == null || !column.unique())) {
			return false;
		}

		Class<?> type = field.getType();
		if (!isLookupValueType(type)) {
			return false;
		}

		Object value = readField(field, entity);
		return value != null;
	}

	private boolean isLookupValueType(Class<?> type) {
		if (type.isPrimitive() || type.isEnum()) {
			return true;
		}
		if (String.class.equals(type) || Boolean.class.equals(type) || Character.class.equals(type)) {
			return true;
		}
		if (Number.class.isAssignableFrom(type)) {
			return true;
		}
		if (Date.class.isAssignableFrom(type)) {
			return true;
		}
		if (Temporal.class.isAssignableFrom(type)) {
			return true;
		}
		return java.util.Currency.class.equals(type);
	}

	private boolean isRelationshipField(Field field) {
		if (Modifier.isStatic(field.getModifiers())) {
			return false;
		}
		if (field.getAnnotation(Transient.class) != null) {
			return false;
		}
		return field.getAnnotation(ManyToOne.class) != null
				|| field.getAnnotation(OneToOne.class) != null
				|| field.getAnnotation(ManyToMany.class) != null
				|| field.getAnnotation(OneToMany.class) != null;
	}

	private boolean isEntityClass(Class<?> type) {
		return type.getAnnotation(Entity.class) != null;
	}

	private Class<?> entityClassOf(Object entity) {
		return Hibernate.getClass(entity);
	}

	private Object getIdentifier(Object entity) {
		try {
			PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
			return unitUtil.getIdentifier(entity);
		} catch (Exception e) {
			return null;
		}
	}

	private String dependencyKey(Object entity) {
		Class<?> entityClass = entityClassOf(entity);
		Object id = getIdentifier(entity);
		if (id != null) {
			return entityClass.getName() + "#" + id;
		}

		List<Field> fields = collectLookupFields(entity);
		if (!fields.isEmpty()) {
			Field field = fields.get(0);
			Object value = readField(field, entity);
			return entityClass.getName() + ":" + field.getName() + "=" + String.valueOf(value);
		}

		return entityClass.getName() + "@" + System.identityHashCode(entity);
	}

	private List<Field> getAllFields(Class<?> type) {
		if (type == null || Object.class.equals(type)) {
			return Collections.emptyList();
		}

		List<Field> fields = new ArrayList<Field>();
		Class<?> current = type;
		while (current != null && !Object.class.equals(current)) {
			Field[] declared = current.getDeclaredFields();
			for (Field field : declared) {
				fields.add(field);
			}
			current = current.getSuperclass();
		}
		return fields;
	}

	private Object readField(Field field, Object target) {
		try {
			field.setAccessible(true);
			return field.get(target);
		} catch (IllegalAccessException e) {
			throw new ServiceRuntimeException("Cannot read field [" + field.getName() + "]", e);
		}
	}

	private void writeField(Field field, Object target, Object value) {
		try {
			field.setAccessible(true);
			field.set(target, value);
		} catch (IllegalAccessException e) {
			throw new ServiceRuntimeException("Cannot write field [" + field.getName() + "]", e);
		}
	}

	private static final class ResolutionContext {
		private final Object rootEntity;
		private final Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
		private final Set<Object> processed = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

		private ResolutionContext(Object rootEntity) {
			this.rootEntity = rootEntity;
		}
	}
}
