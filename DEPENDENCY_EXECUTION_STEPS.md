# Dependency Execution Steps

---------------------------------------------------------------------------------------------
## Scenario 1: Create User (`POST /api/v1/private/user/`)
---------------------------------------------------------------------------------------------

### Dependency Chain
- Root entity: `User`
- Direct dependencies detected/resolved dynamically:
  - `User.merchantStore` (`@ManyToOne`) -> existing store only (validated before resolver)
  - `User.defaultLanguage` (`@ManyToOne`) -> reused or created
  - `User.groups` (`@ManyToMany`) -> each group reused or created
- Recursive support (if present in entity graph):
  - Example: `User -> Group -> Permission` (`@ManyToMany`) and deeper nested relations

### Step-by-Step Execution
1. API receives user create request and delegates to `UserFacadeImpl.create(...)`.
2. Existing duplicate user check runs first using user name + store.
3. `PersistableUserPopulator` builds a `User` entity and attaches dependency stubs (language/group entities with natural keys) instead of hardcoding DB creation.
4. `EntityDependencyResolver.resolveDependencies(userModel)` scans JPA relationship annotations dynamically.
5. Resolver walks dependencies recursively (unlimited depth), handling both singular and collection relationships.
6. For each dependency, resolver checks whether an ID already exists.
7. If no ID exists, resolver queries DB using auto-detected lookup fields (unique columns first, then natural-key heuristics such as `code`, `groupName`).
8. If dependency row exists, resolver reuses it and attaches the managed entity.
9. If dependency row does not exist, resolver persists the dependency, flushes, and verifies generated ID.
10. Resolved dependencies are reattached to the `User` entity and duplicate collection entries are de-duplicated.
11. Main user entity is saved using existing `userService.saveOrUpdate(userModel)` logic.

### DB Queries Used
- User duplicate prevention (existing logic): `userService.getByUserName(userName, storeCode)`
- Explicit user store validation (existing-only dependency): `merchantStoreService.getByCode(storeCode)`
- Dynamic dependency reuse (resolver):
  - `EntityManager.find(EntityClass, id)` when a dependency already has an ID
  - Dynamic JPQL lookup by detected key, examples:
    - `select e from Language e where e.code = :value`
    - `select e from Group e where e.groupName = :value`
    - Recursive nested lookups for additional dependency entities using the same pattern
- Dependency creation (resolver): `EntityManager.persist(...)` + `EntityManager.flush()`

### Files Modified (Scenario 1)
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/populator/user/PersistableUserPopulator.java`
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/facade/user/UserFacadeImpl.java`
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/support/EntityDependencyResolver.java`

### Duplicate Prevention Logic
- Prevents duplicate user rows using existing user lookup before save.
- Resolver prevents duplicate dependency rows by querying DB before any dependency persist.
- Resolver de-duplicates relation collections (e.g., duplicate groups in payload) after resolution using entity ID (or natural-key fallback).
- Group names are normalized to uppercase in the populator to avoid duplicates like `admin` vs `ADMIN`.

### Transaction Handling
- `UserFacadeImpl.create(...)` is transactional.
- `EntityDependencyResolver.resolveDependencies(...)` requires an active transaction (`MANDATORY`).
- Dependency creation + main user save occur in the same transaction.
- Runtime errors trigger rollback for both dependency rows and the main user create operation.

---------------------------------------------------------------------------------------------
## Scenario 2: Create Store (`POST /api/v1/private/store`)
---------------------------------------------------------------------------------------------


### Dependency Chain
- Root entity: `MerchantStore`
- Direct dependencies detected/resolved dynamically:
  - `MerchantStore.defaultLanguage` (`@ManyToOne`) -> reused or created
  - `MerchantStore.languages` (`@ManyToMany`) -> each language reused or created
  - `MerchantStore.currency` (`@ManyToOne`) -> reused or created
  - `MerchantStore.country` (`@ManyToOne`) -> reused or created
- Direct dependencies validated/reused by existing explicit logic (preserved behavior):
  - `MerchantStore.parent` (`@ManyToOne`) via `retailerStore` -> existing only
  - `MerchantStore.zone` (`@ManyToOne`) -> reused when found; otherwise state/province remains text (`storestateprovince`)
- Recursive support (if nested dependencies are present on attached dependency objects):
  - Resolver continues traversal automatically based on JPA relationship annotations

### Step-by-Step Execution
1. API receives store create request and delegates to `StoreFacadeImpl.create(...)`.
2. Existing duplicate store-code check runs first.
3. `PersistableMerchantStorePopulator` builds a `MerchantStore` entity and attaches dependency stubs:
   - Default language (payload value or system default fallback)
   - Supported languages list
   - Currency stub from currency code
   - Country stub from address country code
4. Existing parent-store and zone behaviors are preserved:
   - Parent store is validated and attached only if already present
   - Zone is reused if found; otherwise state/province text is kept
5. `EntityDependencyResolver.resolveDependencies(mStore)` scans relationship annotations dynamically.
6. Resolver recursively traverses dependencies (unlimited depth) and resolves each dependency entity.
7. Resolver checks for dependency IDs, then DB lookup by detected natural/unique fields before creating anything.
8. Missing dependencies are created with `persist + flush`; generated IDs are validated.
9. Resolved dependencies are reattached to the store entity and duplicate collection entries are removed.
10. Main store entity is saved with existing `merchantStoreService.saveOrUpdate(mStore)` logic.

### DB Queries Used
- Store duplicate prevention (existing logic): `merchantStoreService.getByCode(storeCode)`
- Parent store validation (existing-only dependency): `merchantStoreService.getByCode(retailerStoreCode)`
- Zone reuse lookup (existing behavior preserved): `zoneService.getByCode(stateProvinceCode)`
- Dynamic dependency reuse (resolver):
  - `EntityManager.find(EntityClass, id)` when dependency already has an ID
  - Dynamic JPQL lookup by detected key, examples:
    - `select e from Language e where e.code = :value`
    - `select e from Currency e where e.code = :value`
    - `select e from Country e where e.isoCode = :value`
- Dependency creation (resolver): `EntityManager.persist(...)` + `EntityManager.flush()`

### Files Modified (Scenario 2)
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/populator/store/PersistableMerchantStorePopulator.java`
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/controller/store/facade/StoreFacadeImpl.java`
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/support/EntityDependencyResolver.java`

### Duplicate Prevention Logic
- Prevents duplicate store rows using existing store-code lookup before save.
- Resolver prevents duplicate dependency records by checking DB before dependency `persist`.
- Supported languages are normalized and de-duplicated by code before attach in the populator.
- Resolver also de-duplicates relationship collections after resolution using persistent IDs.
- Existing zone fallback behavior avoids creating incorrect zone rows when a zone code is not found.

### Transaction Handling
- `StoreFacadeImpl.create(...)` is transactional.
- `EntityDependencyResolver.resolveDependencies(...)` requires an active transaction (`MANDATORY`).
- Dependency creation + main store save execute in one transaction.
- Runtime failures roll back both dependency changes and the main store create operation.

---

## Scenario 3: Generic Recursive Dependency Resolution Utility (Reusable / Unlimited Depth)

### Dependency Chain
- Generic pattern supported by resolver:
  - `RootEntity` -> relationship field (`@ManyToOne`, `@OneToOne`, `@ManyToMany`, `@OneToMany`)
  - Nested dependency -> nested dependency -> ... (no fixed depth limit)
- Cycles are tolerated using identity-based traversal tracking (prevents infinite recursion)

### Step-by-Step Execution
1. Resolver receives a root entity inside an active transaction.
2. Resolver scans the entity class and superclasses for JPA relationship annotations.
3. For each relationship field:
   - Singular dependency: resolve one entity reference
   - Collection dependency: resolve each element and de-duplicate the collection
4. Resolver recursively processes the dependency's own relationships before persisting/reusing it.
5. Resolver checks if dependency already has a DB ID.
6. If no ID, resolver auto-detects lookup fields:
   - Priority 1: fields annotated `@Column(unique = true)`
   - Priority 2: natural-key field names (`code`, `isoCode`, `groupName`, `permissionName`, `adminName`, `name`)
7. Resolver queries DB by detected lookup fields and reuses existing rows when found.
8. If no row is found, resolver persists the dependency and flushes to obtain the generated ID.
9. Resolver validates ID presence after persistence and writes managed/reused references back into the graph.
10. Resolver continues until all reachable dependencies are resolved.

### DB Queries Used
- `EntityManager.find(...)` for ID-based reuse
- Dynamic JPQL generated per entity + detected field:
  - `select e from <EntityName> e where e.<field> = :value`
- `EntityManager.persist(...)` for new dependencies
- `EntityManager.flush()` to force ID generation before main save

### Files Modified (Scenario 3)
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/support/EntityDependencyResolver.java`
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/facade/user/UserFacadeImpl.java`
- `/Users/imsiddique/Desktop/Development/Projects/Shopizer/shopizer/sm-shop/src/main/java/com/salesmanager/shop/store/controller/store/facade/StoreFacadeImpl.java`

### Duplicate Prevention Logic
- DB lookup happens before every dependency persist attempt.
- Collection dependencies are de-duplicated after resolution.
- Root entity duplicate checks remain in existing API-specific logic (user/store duplicate validations are not removed).

### Transaction Handling
- Resolver method is transaction-bound (`MANDATORY`) and is designed to run under the API create transaction.
- Ensures atomic behavior for recursive dependency creation and main entity creation.
- Any runtime exception aborts the current transaction and prevents partial nested dependency insertion.
