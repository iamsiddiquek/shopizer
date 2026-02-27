# DATA_SEED_EXECUTION_STEPS

- Run Date: `2026-02-26`
- Base URL: `http://localhost:8080`
- Store Context: `DEFAULT`
- Language Context: `en`
- Target Per Table: `10`
- Seed Prefix: `api_seed`

## Scope and Constraints (Observed During Execution)

- All create operations were executed through existing HTTP API endpoints only (no direct DB inserts).
- Duplicate prevention was applied using API uniqueness checks, list lookups, deterministic keys, or endpoint idempotency (depending on endpoint support).
- Several requested tables are not directly seedable through the current API surface (no create/list endpoints), and a few exposed endpoints are runtime-broken in the running server; these are documented explicitly below.
- Cross-API rollback is not possible because calls are independent HTTP requests; the script stops on blockers and relies on per-request server-side transactions.

## Files Modified / Added

- `scripts/api_seed_tables.py` (new API-only seeding automation script)
- `seed_reports/api_seed_report.json` (generated execution report from the script run)
- `DATA_SEED_EXECUTION_STEPS.md` (this documentation file)

## Execution Order List (Actual Run)

1. Step 1: Identify dependency chain and initialize authenticated API clients
2. Step 2: Create/reuse base tables and directly seedable master entities
3. Create/reuse CATEGORY (+ CATEGORY_DESCRIPTION) via /api/v1/private/category
4. Create/reuse MANUFACTURER (+ MANUFACTURER_DESCRIPTION) via /api/v1/private/manufacturer
5. Create/reuse CONTENT (+ CONTENT_DESCRIPTION) via /api/v1/private/content/page
6. Create/reuse CATALOG via /api/v1/private/catalog and retrieve IDs via /api/v1/private/catalogs
7. Create/reuse CUSTOMER (+ inferred CUSTOMER_GROUP join rows) via /api/v1/private/customer
8. Create/reuse OPTIN definitions via /api/v1/private/optin (409 duplicate => reuse)
9. Step 3: Create dependent entities using IDs returned from APIs
10. Create/reuse CUSTOMER_REVIEW (+ CUSTOMER_REVIEW_DESCRIPTION) via customer review API using returned customer IDs
11. Create/reuse CUSTOMER_OPTIN via /api/v1/newsletter using seeded customer emails (idempotent 200 responses)
12. Attempt CATALOG_ENTRY seeding using catalog IDs, category codes, and product SKU (blocked in current runtime)
13. Configure merchant settings via existing APIs to seed/update MERCHANT_CONFIGURATION rows where possible
14. Attempt MERCHANT_STORE creation with ADMIN_RETAILER to validate current runtime support
15. Step 4: Repeat until target counts are met for supported tables; mark blocked/unsupported tables explicitly
16. Step 5: Stop on runtime blockers, preserve idempotency, and rely on per-request API transactions

## Scenario-by-Scenario Execution

### Scenario 1: Bootstrap Authentication and Role-Specific Clients

Tables covered:
- `(enabler only: no requested table directly seeded)`

Dependency chain:
- Bootstrap user (`iamskk1@gmail.com`) -> create/reuse `seed.retail.admin` (ADMIN_RETAIL)
- Bootstrap user (`iamskk1@gmail.com`) -> create/reuse `seed.retailer.admin` (ADMIN_RETAILER)
- Role split is required because some endpoints authorize `ADMIN_RETAIL` while store create requires `ADMIN_RETAILER`

Step-by-step execution:
1. Login via `POST /api/v1/private/login` using bootstrap credentials.
2. Lookup existing admin users via `GET /api/v1/private/users?page=0&count=200&emailAddress=...`.
3. Create missing seed users via `POST /api/v1/private/user/` with group `ADMIN_RETAIL` / `ADMIN_RETAILER`.
4. Login both seed users and reuse those tokens for subsequent API groups.

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/private/users?page=0&count=200&emailAddress=...`
- `POST /api/v1/private/login`
- `POST /api/v1/private/user/`

How IDs were retrieved and reused:
- User IDs come from `ReadableUser` create response (`id`) when user is newly created.
- On reuse path, the script reuses the existing user record discovered via `/private/users` email filter.

Duplicate prevention logic:

Transaction handling:
- Each user create/login request is isolated to API request scope. The script does not attempt cross-request transactions; it stops on fatal errors and is idempotent on rerun.

Sample payloads:

`login` -> `POST /api/v1/private/login`

```json
{
  "password": "<redacted>",
  "username": "iamskk1@gmail.com"
}
```

### Scenario 2: CATEGORY and CATEGORY_DESCRIPTION

Tables covered:
- `CATEGORY`
- `CATEGORY_DESCRIPTION`

Dependency chain:
- MERCHANT_STORE `DEFAULT` (reused by request context) -> CATEGORY -> CATEGORY_DESCRIPTION (from embedded `description`)

Step-by-step execution:
1. Check category code uniqueness using `GET /api/v1/private/category/unique?code=...`.
2. Create missing categories via `POST /api/v1/private/category` with an embedded `description` object.
3. Track create/reuse counts using deterministic codes (`api_seed_cat_01` ... `api_seed_cat_10`).

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/private/category/unique?code=...`
- `POST /api/v1/private/category`

How IDs were retrieved and reused:
- CATEGORY ID is read from the `PersistableCategory` response (`id`).
- CATEGORY_DESCRIPTION row is inferred from the same create payload/response because description is nested under category create and no direct description API exists.

Duplicate prevention logic:
- `CATEGORY`: Pre-check `/private/category/unique?code=...`; create only when `exists=false`.
- `CATEGORY_DESCRIPTION`: Inherited from CATEGORY create; no direct create API used.

Transaction handling:
- Server handles category creation transactionally per request. Script pre-checks uniqueness and only posts missing codes to avoid duplicate inserts.

Sample payloads:

`create_category` -> `POST /api/v1/private/category?store=DEFAULT&lang=en`

```json
{
  "code": "api_seed_cat_01",
  "description": {
    "friendlyUrl": "api_seed-category-01",
    "language": "en",
    "name": "api_seed category 01",
    "title": "api_seed category 01"
  },
  "featured": false,
  "visible": true
}
```

### Scenario 3: MANUFACTURER and MANUFACTURER_DESCRIPTION

Tables covered:
- `MANUFACTURER`
- `MANUFACTURER_DESCRIPTION`

Dependency chain:
- MERCHANT_STORE `DEFAULT` (reused) -> MANUFACTURER -> MANUFACTURER_DESCRIPTION (from `descriptions[]`)

Step-by-step execution:
1. Check manufacturer code via `GET /api/v1/private/manufacturer/unique?code=...`.
2. Create missing manufacturers via `POST /api/v1/private/manufacturer` with one language description.
3. Use deterministic codes to support reruns without duplicate rows.

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/private/manufacturer/unique?code=...`
- `POST /api/v1/private/manufacturer`

How IDs were retrieved and reused:
- MANUFACTURER ID is returned by the create response (`id`).
- MANUFACTURER_DESCRIPTION row count is inferred from `descriptions[]` because there is no separate create endpoint.

Duplicate prevention logic:
- `MANUFACTURER`: Pre-check `/private/manufacturer/unique?code=...`; create only when `exists=false`.
- `MANUFACTURER_DESCRIPTION`: Inherited from MANUFACTURER create `descriptions[]` payload.

Transaction handling:
- Each manufacturer create request is isolated and server-transactional. The script uses uniqueness checks first to prevent duplicate inserts.

Sample payloads:

`create_manufacturer` -> `POST /api/v1/private/manufacturer?store=DEFAULT&lang=en`

```json
{
  "code": "api_seed_man_01",
  "descriptions": [
    {
      "language": "en",
      "name": "api_seed manufacturer 01"
    }
  ]
}
```

### Scenario 4: CONTENT and CONTENT_DESCRIPTION (Pages)

Tables covered:
- `CONTENT`
- `CONTENT_DESCRIPTION`

Dependency chain:
- MERCHANT_STORE `DEFAULT` + LANGUAGE `en` -> CONTENT (PAGE type) -> CONTENT_DESCRIPTION (from nested `descriptions[]`)

Step-by-step execution:
1. Check page code via `GET /api/v1/private/content/page/{code}/exists`.
2. Create missing content pages via `POST /api/v1/private/content/page` with explicit `"id": null` (required to avoid Hibernate id=0 update-path issue).
3. Use deterministic codes and friendly URLs for repeatable idempotent runs.

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/private/content/page/{code}/exists`
- `POST /api/v1/private/content/page`

How IDs were retrieved and reused:
- CONTENT ID is returned by the page create response (`{"id": <contentId>}`).
- CONTENT_DESCRIPTION is inferred from the page payload `descriptions[]` and server-side page creation logic.

Duplicate prevention logic:
- `CONTENT`: Pre-check `/private/content/page/{code}/exists`; create only when `exists=false`.
- `CONTENT_DESCRIPTION`: Inherited from CONTENT page `descriptions[]` payload.

Transaction handling:
- Each page create runs in one API request transaction. Script pre-checks `exists` and posts only when missing.

Sample payloads:

`create_content_page` -> `POST /api/v1/private/content/page?store=DEFAULT&lang=en`

```json
{
  "code": "api_seed_page_01",
  "contentType": "PAGE",
  "descriptions": [
    {
      "description": "api_seed content page 01",
      "friendlyUrl": "api_seed-page-01",
      "language": "en",
      "name": "api_seed page 01",
      "title": "api_seed page 01"
    }
  ],
  "id": null,
  "linkToMenu": false,
  "visible": true
}
```

### Scenario 5: CATALOG

Tables covered:
- `CATALOG`

Dependency chain:
- MERCHANT_STORE `DEFAULT` -> CATALOG

Step-by-step execution:
1. Check code via `GET /api/v1/private/catalog/unique?code=...`.
2. Create missing catalogs using `POST /api/v1/private/catalog` with `"id": null`.
3. Refresh `GET /api/v1/private/catalogs` cache to retrieve/reuse catalog IDs for downstream dependencies (`CATALOG_ENTRY`).

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/private/catalog/unique?code=...`
- `POST /api/v1/private/catalog`
- `GET /api/v1/private/catalogs?page=...&count=...`

How IDs were retrieved and reused:
- CATALOG ID is read from create response when created.
- On rerun/reuse, CATALOG ID is loaded from `/api/v1/private/catalogs` and matched by `code`.

Duplicate prevention logic:
- `CATALOG`: Pre-check `/private/catalog/unique?code=...`; refresh `/private/catalogs` cache to reuse IDs.

Transaction handling:
- Each catalog create request is per-request transactional. The script uses unique check + list cache to avoid duplicate catalog rows and to preserve IDs for dependent calls.

Sample payloads:

`create_catalog` -> `POST /api/v1/private/catalog?store=DEFAULT&lang=en`

```json
{
  "code": "api_seed_catalog_01",
  "defaultCatalog": false,
  "id": null,
  "visible": true
}
```

### Scenario 6: CUSTOMER and CUSTOMER_GROUP (Join Table)

Tables covered:
- `CUSTOMER`
- `CUSTOMER_GROUP`

Dependency chain:
- MERCHANT_STORE `DEFAULT` + COUNTRY `US` (reused) -> CUSTOMER
- CUSTOMER -> CUSTOMER_GROUP (server-side default group assignment / join row)

Step-by-step execution:
1. Page through `GET /api/v1/private/customers` and build an email->customer map.
2. Create missing customers via `POST /api/v1/private/customer` using deterministic emails.
3. Reuse existing customers on rerun by matching email instead of hardcoded IDs.
4. Treat `CUSTOMER_GROUP` as an inferred join row created by the customer create flow (default CUSTOMER group).

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/private/customers?page=...&count=...`
- `POST /api/v1/private/customer`

How IDs were retrieved and reused:
- CUSTOMER ID is read from the `ReadableCustomer` create response (`id`).
- On reuse, CUSTOMER ID is looked up from the `/private/customers` list by deterministic email.
- CUSTOMER_GROUP join table IDs are not exposed directly; row creation is inferred from customer create + returned groups.

Duplicate prevention logic:
- `CUSTOMER`: List `/private/customers` and reuse by deterministic email; create only if missing.
- `CUSTOMER_GROUP`: Inferred join row from CUSTOMER create (default customer group assignment).

Transaction handling:
- Each customer create is transactional per request. Duplicate prevention uses list lookup by email before POST.

Sample payloads:

`create_customer` -> `POST /api/v1/private/customer?store=DEFAULT&lang=en`

```json
{
  "billing": {
    "address": "1 Seed Street",
    "city": "Austin",
    "country": "US",
    "firstName": "Seed01",
    "lastName": "Customer",
    "phone": "5550000001",
    "postalCode": "73001",
    "stateProvince": "TX"
  },
  "emailAddress": "api_seed.customer.01@example.com",
  "firstName": "Seed01",
  "lastName": "Customer",
  "password": "<redacted>",
  "repeatPassword": "<redacted>",
  "userName": "api_seed.customer.01@example.com"
}
```

### Scenario 7: CUSTOMER_REVIEW and CUSTOMER_REVIEW_DESCRIPTION

Tables covered:
- `CUSTOMER_REVIEW`
- `CUSTOMER_REVIEW_DESCRIPTION`

Dependency chain:
- CUSTOMER (seeded/reused) -> CUSTOMER_REVIEW -> CUSTOMER_REVIEW_DESCRIPTION (from `description` field)

Step-by-step execution:
1. Load/reuse seeded customer IDs (from Scenario 6).
2. List reviews via `GET /api/v1/customers/{id}/reviews` and reuse by deterministic description text.
3. Create missing reviews via `POST /api/v1/private/customers/{id}/reviews` using the returned customer ID in both path and payload references.

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/customers/{id}/reviews`
- `POST /api/v1/private/customers/{id}/reviews`

How IDs were retrieved and reused:
- Customer IDs are reused from Scenario 6 (`ReadableCustomer.id`).
- Review IDs are returned by the review create response (`PersistableCustomerReview.id`).
- Review description row is inferred from the same review create payload because there is no separate review-description API.

Duplicate prevention logic:
- `CUSTOMER_REVIEW`: List `/customers/{id}/reviews` and reuse by deterministic review description before POST.
- `CUSTOMER_REVIEW_DESCRIPTION`: Inherited from CUSTOMER_REVIEW `description` field.

Transaction handling:
- Each review create request is server-transactional. Script prevents duplicates by reading current customer reviews and matching the deterministic review description before POST.

Sample payloads:

`create_customer_review` -> `POST /api/v1/private/customers/{id}/reviews?store=DEFAULT&lang=en`

```json
{
  "customerId": 53,
  "date": "2026-02-26",
  "description": "api_seed customer review 01",
  "rating": 2.0,
  "reviewedCustomer": 53
}
```

### Scenario 8: OPTIN and CUSTOMER_OPTIN (Newsletter)

Tables covered:
- `OPTIN`
- `CUSTOMER_OPTIN`

Dependency chain:
- MERCHANT_STORE `DEFAULT` -> OPTIN (NEWSLETTER type)
- OPTIN (newsletter type) + CUSTOMER email -> CUSTOMER_OPTIN via `/api/v1/newsletter`

Step-by-step execution:
1. Create/reuse OPTIN definitions using `POST /api/v1/private/optin` and deterministic codes (`API_SEED_OPTIN_01..10`).
2. Treat HTTP `409` duplicate key response as reuse (no duplicate insert).
3. Submit newsletter optin requests via `POST /api/v1/newsletter` using seeded customer emails.
4. Newsletter endpoint returns `200` and no body on both first and repeated calls; counts are inferred from successful submissions.

DB queries used (via APIs / service-backed API lookups):
- `POST /api/v1/private/optin`
- `POST /api/v1/newsletter`

How IDs were retrieved and reused:
- OPTIN create response returns an object but does not expose a reliable persisted ID (observed `id=0`), so the script reuses deterministic `code` as the natural key.
- CUSTOMER_OPTIN endpoint returns no body/ID; the script reuses the customer email and relies on endpoint idempotency for duplicate prevention.

Duplicate prevention logic:
- `OPTIN`: POST idempotency via DB unique key; duplicate returns HTTP 409 and is counted as reuse.
- `CUSTOMER_OPTIN`: Newsletter endpoint is idempotent (HTTP 200 on repeats); duplicate rows are not directly count-queryable via API.

Transaction handling:
- Each optin/newsletter submission runs in its own request transaction. Duplicate prevention uses unique-key conflict handling for `OPTIN` and idempotent newsletter behavior for `CUSTOMER_OPTIN`.

Sample payloads:

`create_optin` -> `POST /api/v1/private/optin?store=DEFAULT&lang=en`

```json
{
  "code": "API_SEED_OPTIN_01",
  "description": "api_seed optin 01",
  "optinType": "NEWSLETTER",
  "store": "DEFAULT"
}
```

`create_customer_optin_newsletter` -> `POST /api/v1/newsletter?store=DEFAULT&lang=en`

```json
{
  "email": "api_seed.customer.01@example.com",
  "firstName": "Seed01",
  "lastName": "Customer"
}
```

### Scenario 9: MERCHANT_CONFIGURATION (Partial, API-Limited)

Tables covered:
- `MERCHANT_CONFIGURATION`

Dependency chain:
- MERCHANT_STORE `DEFAULT` (reused) -> payment module config (`PAYMENT_MODULES` key)
- MERCHANT_STORE `DEFAULT` (reused) -> shipping origin config
- MERCHANT_STORE `DEFAULT` (reused) -> shipping expedition config
- MERCHANT_STORE `DEFAULT` (reused) -> store marketing/brand config (`CONFIG` key)
- Attempted additional shipping-module config -> blocked by runtime API error

Step-by-step execution:
1. Configure `moneyorder` payment module via `POST /api/v1/private/modules/payment` using required `integrationKeys.address`.
2. Save shipping origin via `POST /api/v1/private/shipping/origin`.
3. Save shipping expedition settings via `POST /api/v1/private/shipping/expedition`.
4. Round-trip brand settings via `GET/POST /api/v1/private/store/DEFAULT/marketing`.
5. Attempt `POST /api/v1/private/modules/shipping` (storePickUp) for additional config rows; runtime returns a wrapped 500/400 error (`Error saving shipping module`).

DB queries used (via APIs / service-backed API lookups):
- `POST /api/v1/private/modules/payment`
- `POST /api/v1/private/shipping/origin`
- `POST /api/v1/private/shipping/expedition`
- `GET /api/v1/private/store/{code}/marketing`
- `POST /api/v1/private/store/{code}/marketing`
- `POST /api/v1/private/modules/shipping` (blocked in current runtime)

How IDs were retrieved and reused:
- No merchant-configuration list/count API is exposed in v1, so row IDs cannot be retrieved directly via API.
- Script records successful configuration writes and maps them to known config keys (inferred rows), while noting row-count verification is API-limited.

Duplicate prevention logic:
- `MERCHANT_CONFIGURATION`: Config writes target fixed keys; repeated calls update same config keys rather than creating duplicates.

Transaction handling:
- Each config write is transactional per API request. Script uses fixed config keys and round-trip GET/POST where available to update existing rows instead of creating duplicates.

Sample payloads:

`configure_payment_module` -> `POST /api/v1/private/modules/payment?store=DEFAULT&lang=en`

```json
{
  "active": true,
  "code": "moneyorder",
  "defaultSelected": false,
  "integrationKeys": {
    "address": "api_seed payment address"
  },
  "integrationOptions": {}
}
```

`save_shipping_origin` -> `POST /api/v1/private/shipping/origin?store=DEFAULT&lang=en`

```json
{
  "address": "api_seed shipping origin",
  "city": "Austin",
  "country": "US",
  "postalCode": "73301",
  "stateProvince": "TX"
}
```

`save_shipping_expedition` -> `POST /api/v1/private/shipping/expedition?store=DEFAULT&lang=en`

```json
{
  "iternationalShipping": false,
  "shipToCountry": [],
  "taxOnShipping": false
}
```

`save_store_marketing` -> `POST /api/v1/private/store/DEFAULT/marketing?store=DEFAULT&lang=en`

```json
{
  "socialNetworks": []
}
```

`configure_shipping_module` -> `POST /api/v1/private/modules/shipping?store=DEFAULT&lang=en`

```json
{
  "active": true,
  "code": "storePickUp",
  "defaultSelected": false,
  "integrationKeys": {
    "note": "Pickup at store",
    "price": "0.00"
  },
  "integrationOptions": {}
}
```

### Scenario 10: Blocked APIs (CATALOG_ENTRY, MERCHANT_STORE, MERCHANT_LANGUAGE)

Tables covered:
- `CATALOG_ENTRY`
- `MERCHANT_STORE`
- `MERCHANT_LANGUAGE`

Dependency chain:
- CATALOG_ENTRY chain (CATALOG + CATEGORY + PRODUCT SKU + DEFAULT store) exists, but API fails during conversion at runtime.
- MERCHANT_STORE -> MERCHANT_LANGUAGE chain exists conceptually, but store create is blocked by current runtime proxy/entity-type error.

Step-by-step execution:
1. CATALOG_ENTRY attempt used seeded catalog IDs, seeded category codes, and an existing product SKU from `/api/v1/products`.
2. `POST /api/v1/private/catalog/{id}` returned a conversion runtime error on every tested payload combination.
3. MERCHANT_STORE probe used `ADMIN_RETAILER` token and full required payload; `POST /api/v1/private/store` failed with `Unknown entity type Country$HibernateProxy` in the running server.
4. Because MERCHANT_STORE creation failed, `MERCHANT_LANGUAGE` join rows could not be created via store supportedLanguages.

DB queries used (via APIs / service-backed API lookups):
- `GET /api/v1/products`
- `GET /api/v1/private/catalogs`
- `POST /api/v1/private/catalog/{id}` (blocked)
- `GET /api/v1/private/store/unique`
- `POST /api/v1/private/store` (blocked)

How IDs were retrieved and reused:
- CATALOG IDs were retrieved from `/api/v1/private/catalogs` and product SKU from `/api/v1/products`.
- MERCHANT_STORE ID retrieval was not possible because create failed before persistence.

Duplicate prevention logic:
- `CATALOG_ENTRY`: Planned: list `/private/catalog/{id}/entry` + match (`productCode`,`categoryCode`) before POST. Blocked by runtime conversion error.
- `MERCHANT_STORE`: Planned: `/private/store/unique?code=...` before POST; blocked by runtime proxy error during create.
- `MERCHANT_LANGUAGE`: Would be created indirectly from MERCHANT_STORE supportedLanguages; store create blocked.

Transaction handling:
- Server-side request transactions roll back on these runtime exceptions, so partial rows were not committed for the failed calls.

Sample payloads:

`create_catalog_entry` -> `POST /api/v1/private/catalog/{id}?store=DEFAULT&lang=en`

```json
{
  "categoryCode": "api_seed_cat_01",
  "id": null,
  "productCode": "QA_SKU_001",
  "visible": true
}
```

`create_store` -> `POST /api/v1/private/store?store=DEFAULT&lang=en`

```json
{
  "address": {
    "address": "1 Seed Store Street",
    "city": "Austin",
    "country": "US",
    "postalCode": "73301",
    "stateProvince": "TX"
  },
  "code": "API_SEED_STORE_01",
  "currency": "USD",
  "defaultLanguage": "en",
  "dimension": "IN",
  "email": "api_seed.store.01@example.com",
  "inBusinessSince": "2026-02-26",
  "name": "api_seed Store 01",
  "phone": "1112223333",
  "supportedLanguages": [
    "en"
  ],
  "useCache": false,
  "weight": "LB"
}
```

## Table-by-Table Results Matrix

| Table | Status | Dependency Chain | API Call Order | Duplicate Prevention | Total Inserted (Verified) | Reused (Verified) | Inferred Inserts | Verification | Notes |
|---|---|---|---|---|---:|---:|---:|---|---|
| `CATALOG` | seeded | MERCHANT_STORE (DEFAULT reused)<br>LANGUAGE(en reused) | GET /api/v1/private/catalog/unique<br>POST /api/v1/private/catalog<br>GET /api/v1/private/catalogs | Pre-check `/private/catalog/unique?code=...`; refresh `/private/catalogs` cache to reuse IDs. | 10 | 0 | 0 | api_verified | — |
| `CATALOG_ENTRY` | blocked | CATALOG<br>CATEGORY<br>PRODUCT (reused via /api/v1/products)<br>MERCHANT_STORE (DEFAULT reused) | GET /api/v1/products<br>POST /api/v1/private/catalog/{id}<br>GET /api/v1/private/catalog/{id}/entry | Planned: list `/private/catalog/{id}/entry` + match (`productCode`,`categoryCode`) before POST. Blocked by runtime conversion error. | 0 | 0 | 0 | api_verified | Runtime API error on catalog entry create: {"errorCode":null,"message":"Error while converting CatalogEntry, com.salesmanager.shop.store.api.exception.ConversionRuntimeException"} |
| `CATEGORY` | seeded | MERCHANT_STORE (DEFAULT reused)<br>LANGUAGE(en reused) | GET /api/v1/private/category/unique<br>POST /api/v1/private/category | Pre-check `/private/category/unique?code=...`; create only when `exists=false`. | 10 | 0 | 0 | api_verified | — |
| `CATEGORY_DESCRIPTION` | seeded | CATEGORY | POST /api/v1/private/category | Inherited from CATEGORY create; no direct create API used. | 0 | 0 | 10 | api_inferred | One description row per category create is inferred from category payload with description object |
| `CONTENT` | seeded | MERCHANT_STORE (DEFAULT reused)<br>LANGUAGE(en reused) | GET /api/v1/private/content/page/{code}/exists<br>POST /api/v1/private/content/page | Pre-check `/private/content/page/{code}/exists`; create only when `exists=false`. | 10 | 0 | 0 | api_verified | — |
| `CONTENT_DESCRIPTION` | seeded | CONTENT | POST /api/v1/private/content/page | Inherited from CONTENT page `descriptions[]` payload. | 0 | 0 | 10 | api_inferred | Content description rows are inferred from page create payload descriptions[] |
| `COUNTRY` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No create API is exposed in REST references endpoints; store-create indirect path is blocked in current runtime. |
| `COUNTRY_DESCRIPTION` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No country description create API is exposed; reference endpoints are read-only. |
| `CURRENCY` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No create API is exposed in REST references endpoints; indirect creation requires store create and valid missing currency codes. |
| `CUSTOMER` | seeded | MERCHANT_STORE (DEFAULT reused)<br>COUNTRY(US reused) | GET /api/v1/private/customers<br>POST /api/v1/private/customer | List `/private/customers` and reuse by deterministic email; create only if missing. | 10 | 0 | 0 | api_verified | — |
| `CUSTOMER_ATTRIBUTE` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No customer-attribute create/list API found in public/admin REST layer. |
| `CUSTOMER_GROUP` | seeded | CUSTOMER<br>default CUSTOMER group (server-side) | POST /api/v1/private/customer | Inferred join row from CUSTOMER create (default customer group assignment). | 0 | 0 | 10 | api_inferred | CUSTOMER_GROUP is a join table populated indirectly by customer create/default group assignment |
| `CUSTOMER_OPTIN` | seeded | OPTIN (newsletter type)<br>CUSTOMER (email reused) | POST /api/v1/newsletter | Newsletter endpoint is idempotent (HTTP 200 on repeats); duplicate rows are not directly count-queryable via API. | 0 | 0 | 10 | not_queryable_via_api | Newsletter endpoint returns 200 with no body and no list endpoint exists for CUSTOMER_OPTIN; count is inferred from successful submissions |
| `CUSTOMER_OPTION` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No customer option create API found in public/admin REST layer. |
| `CUSTOMER_OPTION_DESC` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No customer option description create API found in public/admin REST layer. |
| `CUSTOMER_OPTION_SET` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No customer option set create API found in public/admin REST layer. |
| `CUSTOMER_OPTION_VALUE` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No customer option value create API found in public/admin REST layer. |
| `CUSTOMER_OPT_VAL_DESCRIPTION` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No customer option value description create API found in public/admin REST layer. |
| `CUSTOMER_REVIEW` | seeded | CUSTOMER | GET /api/v1/customers/{id}/reviews<br>POST /api/v1/private/customers/{id}/reviews | List `/customers/{id}/reviews` and reuse by deterministic review description before POST. | 10 | 0 | 0 | api_verified | — |
| `CUSTOMER_REVIEW_DESCRIPTION` | seeded | CUSTOMER_REVIEW | POST /api/v1/private/customers/{id}/reviews | Inherited from CUSTOMER_REVIEW `description` field. | 0 | 0 | 10 | api_inferred | Customer review description rows are inferred from review create payload description field |
| `FILE_HISTORY` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No deterministic file-history API for direct create/list verification was identified. |
| `GEOZONE` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No geozone create API found in the REST layer. |
| `GEOZONE_DESCRIPTION` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No geozone description create API found in the REST layer. |
| `LANGUAGE` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | No dedicated create API is exposed; indirect creation is not reliably observable via /api/v1/languages in current runtime. |
| `MANUFACTURER` | seeded | MERCHANT_STORE (DEFAULT reused)<br>LANGUAGE(en reused) | GET /api/v1/private/manufacturer/unique<br>POST /api/v1/private/manufacturer | Pre-check `/private/manufacturer/unique?code=...`; create only when `exists=false`. | 10 | 0 | 0 | api_verified | — |
| `MANUFACTURER_DESCRIPTION` | seeded | MANUFACTURER | POST /api/v1/private/manufacturer | Inherited from MANUFACTURER create `descriptions[]` payload. | 0 | 0 | 10 | api_inferred | Manufacturer description rows are inferred from create payload descriptions[] |
| `MERCHANT_CONFIGURATION` | partial | MERCHANT_STORE (DEFAULT reused) | POST /api/v1/private/modules/payment<br>POST /api/v1/private/shipping/origin<br>POST /api/v1/private/shipping/expedition<br>POST /api/v1/private/store/{code}/marketing | Config writes target fixed keys; repeated calls update same config keys rather than creating duplicates. | 0 | 0 | 4 | partially_queryable | MERCHANT_CONFIGURATION has no list/count API; successful writes are tracked, row inserts are inferred<br>Payment module configuration submitted successfully (PAYMENT_MODULES merchant configuration key updated)<br>Shipping origin saved successfully (shipping configuration stored server-side)<br>Shipping expedition configuration submitted successfully (supported-countries config likely updated)<br>Store marketing saved successfully (merchant CONFIG row likely updated)<br>Shipping module configuration API is runtime-broken in current server (returns 500 wrapper on valid payload)<br>Observed error: {"errorCode":"500","message":"Error saving shipping module"}<br>Target 10 MERCHANT_CONFIGURATION rows cannot be reached safely via currently working APIs in this runtime |
| `MERCHANT_LANGUAGE` | blocked | MERCHANT_STORE<br>LANGUAGE | POST /api/v1/private/store | Would be created indirectly from MERCHANT_STORE supportedLanguages; store create blocked. | 0 | 0 | 0 | api_inferred | Blocked because MERCHANT_STORE create failed |
| `MERCHANT_LOG` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | Merchant log rows are internal/audit-generated and no create/list API is exposed. |
| `MERCHANT_STORE` | blocked | COUNTRY<br>CURRENCY<br>LANGUAGE | POST /api/v1/private/store<br>GET /api/v1/private/store/{code} | Planned: `/private/store/unique?code=...` before POST; blocked by runtime proxy error during create. | 0 | 0 | 0 | api_verified | Store create is blocked in current runtime<br>Observed error: {"errorCode":"500","message":"Unknown entity type 'com.salesmanager.core.model.reference.country.Country$HibernateProxy', Unknown entity type 'com.salesmanager.core.model.reference.country.Country$HibernateProxy'"} |
| `MODULE_CONFIGURATION` | unsupported | N/A | N/A | No safe duplicate-prevention API path available because no supported create/list API was found for this table. | 0 | 0 | 0 | no_create_api_found | System module-definition API is legacy/global and unsafe for seeding; no safe idempotent admin create/list flow for this task. |
| `OPTIN` | seeded | MERCHANT_STORE (DEFAULT reused) | POST /api/v1/private/optin | POST idempotency via DB unique key; duplicate returns HTTP 409 and is counted as reuse. | 0 | 0 | 10 | api_inferred | OPTIN create endpoint does not expose a list API for ID lookup; duplicate prevention uses API unique-key conflict (409) |

## Summary of What Was Seeded vs Blocked

Seeded to target 10 (fully or with API-inferred secondary-table counts):
- `CATALOG`: inserted=10, reused=0, inferred=0 (api_verified)
- `CATEGORY`: inserted=10, reused=0, inferred=0 (api_verified)
- `CATEGORY_DESCRIPTION`: inserted=0, reused=0, inferred=10 (api_inferred)
- `CONTENT`: inserted=10, reused=0, inferred=0 (api_verified)
- `CONTENT_DESCRIPTION`: inserted=0, reused=0, inferred=10 (api_inferred)
- `CUSTOMER`: inserted=10, reused=0, inferred=0 (api_verified)
- `CUSTOMER_GROUP`: inserted=0, reused=0, inferred=10 (api_inferred)
- `CUSTOMER_OPTIN`: inserted=0, reused=0, inferred=10 (not_queryable_via_api)
- `CUSTOMER_REVIEW`: inserted=10, reused=0, inferred=0 (api_verified)
- `CUSTOMER_REVIEW_DESCRIPTION`: inserted=0, reused=0, inferred=10 (api_inferred)
- `MANUFACTURER`: inserted=10, reused=0, inferred=0 (api_verified)
- `MANUFACTURER_DESCRIPTION`: inserted=0, reused=0, inferred=10 (api_inferred)
- `OPTIN`: inserted=0, reused=0, inferred=10 (api_inferred)

Partial / blocked / unsupported:
- `CATALOG_ENTRY`: status=blocked, inserted=0, reused=0, inferred=0 (api_verified)
  - Runtime API error on catalog entry create: {"errorCode":null,"message":"Error while converting CatalogEntry, com.salesmanager.shop.store.api.exception.ConversionRuntimeException"}
- `COUNTRY`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No create API is exposed in REST references endpoints; store-create indirect path is blocked in current runtime.
- `COUNTRY_DESCRIPTION`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No country description create API is exposed; reference endpoints are read-only.
- `CURRENCY`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No create API is exposed in REST references endpoints; indirect creation requires store create and valid missing currency codes.
- `CUSTOMER_ATTRIBUTE`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No customer-attribute create/list API found in public/admin REST layer.
- `CUSTOMER_OPTION`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No customer option create API found in public/admin REST layer.
- `CUSTOMER_OPTION_DESC`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No customer option description create API found in public/admin REST layer.
- `CUSTOMER_OPTION_SET`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No customer option set create API found in public/admin REST layer.
- `CUSTOMER_OPTION_VALUE`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No customer option value create API found in public/admin REST layer.
- `CUSTOMER_OPT_VAL_DESCRIPTION`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No customer option value description create API found in public/admin REST layer.
- `FILE_HISTORY`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No deterministic file-history API for direct create/list verification was identified.
- `GEOZONE`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No geozone create API found in the REST layer.
- `GEOZONE_DESCRIPTION`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No geozone description create API found in the REST layer.
- `LANGUAGE`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - No dedicated create API is exposed; indirect creation is not reliably observable via /api/v1/languages in current runtime.
- `MERCHANT_CONFIGURATION`: status=partial, inserted=0, reused=0, inferred=4 (partially_queryable)
  - MERCHANT_CONFIGURATION has no list/count API; successful writes are tracked, row inserts are inferred
  - Payment module configuration submitted successfully (PAYMENT_MODULES merchant configuration key updated)
  - Shipping origin saved successfully (shipping configuration stored server-side)
  - Shipping expedition configuration submitted successfully (supported-countries config likely updated)
  - Store marketing saved successfully (merchant CONFIG row likely updated)
  - Shipping module configuration API is runtime-broken in current server (returns 500 wrapper on valid payload)
  - Observed error: {"errorCode":"500","message":"Error saving shipping module"}
  - Target 10 MERCHANT_CONFIGURATION rows cannot be reached safely via currently working APIs in this runtime
- `MERCHANT_LANGUAGE`: status=blocked, inserted=0, reused=0, inferred=0 (api_inferred)
  - Blocked because MERCHANT_STORE create failed
- `MERCHANT_LOG`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - Merchant log rows are internal/audit-generated and no create/list API is exposed.
- `MERCHANT_STORE`: status=blocked, inserted=0, reused=0, inferred=0 (api_verified)
  - Store create is blocked in current runtime
  - Observed error: {"errorCode":"500","message":"Unknown entity type 'com.salesmanager.core.model.reference.country.Country$HibernateProxy', Unknown entity type 'com.salesmanager.core.model.reference.country.Country$HibernateProxy'"}
- `MODULE_CONFIGURATION`: status=unsupported, inserted=0, reused=0, inferred=0 (no_create_api_found)
  - System module-definition API is legacy/global and unsafe for seeding; no safe idempotent admin create/list flow for this task.

## Runtime Blockers Captured (Actual Errors)

- `CATALOG_ENTRY` / `create_catalog_entry`: `HTTP 400 for /api/v1/private/catalog/2006?store=DEFAULT&lang=en: {"errorCode":null,"message":"Error while converting CatalogEntry, com.salesmanager.shop.store.api.exception.ConversionRuntimeException"}`
- `MERCHANT_CONFIGURATION` / `configure_shipping_module`: `HTTP 400 for /api/v1/private/modules/shipping?store=DEFAULT&lang=en: {"errorCode":"500","message":"Error saving shipping module"}`
- `MERCHANT_STORE` / `create_store`: `HTTP 500 for /api/v1/private/store?store=DEFAULT&lang=en: {"errorCode":"500","message":"Unknown entity type 'com.salesmanager.core.model.reference.country.Country$HibernateProxy', Unknown entity type 'com.salesmanager.core.model.reference.country.Country$HibernateProxy'"}`

## Script Usage

```bash
python3 scripts/api_seed_tables.py --target 10
```

Optional environment variables:

- `SHOPIZER_BASE_URL` (default `http://localhost:8080`)
- `SHOPIZER_STORE` (default `DEFAULT`)
- `SHOPIZER_LANG` (default `en`)
- `SHOPIZER_BOOTSTRAP_USERNAME` / `SHOPIZER_BOOTSTRAP_PASSWORD` (defaults from local Postman config)
- `SHOPIZER_SEED_ADMIN_RETAIL_USERNAME` / `SHOPIZER_SEED_ADMIN_RETAIL_PASSWORD`
- `SHOPIZER_SEED_ADMIN_RETAILER_USERNAME` / `SHOPIZER_SEED_ADMIN_RETAILER_PASSWORD`
- `SHOPIZER_SEED_PREFIX` (default `api_seed`)
- `SHOPIZER_SEED_TARGET` (default `10`)

Report output (generated by the script): `seed_reports/api_seed_report.json`
