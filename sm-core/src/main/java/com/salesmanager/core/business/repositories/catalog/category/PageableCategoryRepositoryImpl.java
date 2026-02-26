package com.salesmanager.core.business.repositories.catalog.category;

import com.salesmanager.core.model.catalog.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;


public class PageableCategoryRepositoryImpl implements PageableCategoryRepositoryCustom {

	@PersistenceContext
	private EntityManager em;
	@SuppressWarnings("unchecked")
	@Override
	public Page<Category> listByStore(Integer storeId, Integer languageId, String name, Pageable pageable) {
	  String filterName = name == null ? "" : name;
	  String dataJpql =
	      "select distinct c from Category c "
	          + "left join fetch c.descriptions cd "
	          + "join fetch cd.language cdl "
	          + "join fetch c.merchantStore cm "
	          + "where cm.id = :storeId and cdl.id = :languageId "
	          + "and (:name = '' or cd.name like concat('%', :name, '%')) "
	          + "order by c.lineage, c.sortOrder asc";
	  String countJpql =
	      "select count(distinct c) from Category c "
	          + "join c.descriptions cd "
	          + "join cd.language cdl "
	          + "join c.merchantStore cm "
	          + "where cm.id = :storeId and cdl.id = :languageId "
	          + "and (:name = '' or cd.name like concat('%', :name, '%'))";

	  TypedQuery<Category> query = em.createQuery(dataJpql, Category.class);
	  TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);
	  for (Query q : new Query[] {query, countQuery}) {
	    q.setParameter("storeId", storeId);
	    q.setParameter("languageId", languageId);
	    q.setParameter("name", filterName);
	  }
	  query.setMaxResults(pageable.getPageSize());
	  query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());

		return new PageImpl<Category>(query.getResultList(), pageable, countQuery.getSingleResult());
	}

}
