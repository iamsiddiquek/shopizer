#!/usr/bin/env python3
"""API-only data seeder for a subset of Shopizer tables.

This script intentionally avoids direct DB writes. It seeds only through existing API endpoints,
tracks duplicate-safe reuse where possible, and records blockers for tables that do not expose
usable create/list APIs in the current runtime.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import traceback
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from datetime import date
from typing import Any, Dict, Iterable, List, Optional, Tuple


ALL_TABLES = [
    "CATALOG",
    "CATALOG_ENTRY",
    "CATEGORY",
    "CATEGORY_DESCRIPTION",
    "CONTENT",
    "CONTENT_DESCRIPTION",
    "COUNTRY",
    "COUNTRY_DESCRIPTION",
    "CURRENCY",
    "CUSTOMER",
    "CUSTOMER_ATTRIBUTE",
    "CUSTOMER_GROUP",
    "CUSTOMER_OPTIN",
    "CUSTOMER_OPTION",
    "CUSTOMER_OPTION_DESC",
    "CUSTOMER_OPTION_SET",
    "CUSTOMER_OPTION_VALUE",
    "CUSTOMER_OPT_VAL_DESCRIPTION",
    "CUSTOMER_REVIEW",
    "CUSTOMER_REVIEW_DESCRIPTION",
    "FILE_HISTORY",
    "GEOZONE",
    "GEOZONE_DESCRIPTION",
    "LANGUAGE",
    "MANUFACTURER",
    "MANUFACTURER_DESCRIPTION",
    "MERCHANT_CONFIGURATION",
    "MERCHANT_LANGUAGE",
    "MERCHANT_LOG",
    "MERCHANT_STORE",
    "MODULE_CONFIGURATION",
    "OPTIN",
]


@dataclass
class TableStat:
    status: str = "pending"  # seeded | partial | blocked | unsupported | pending
    target: int = 10
    inserted: int = 0         # API-verified inserts
    reused: int = 0           # API-verified reuses
    inferred_inserted: int = 0  # success inferred when API has no read-back/count capability
    verification: str = "api_verified"
    notes: List[str] = field(default_factory=list)
    dependencies: List[str] = field(default_factory=list)
    api_order: List[str] = field(default_factory=list)

    def note(self, msg: str) -> None:
        if msg and msg not in self.notes:
            self.notes.append(msg)


class ApiError(Exception):
    def __init__(self, status: int, body: str, path: str):
        super().__init__(f"HTTP {status} for {path}: {body[:500]}")
        self.status = status
        self.body = body
        self.path = path

    def body_json(self) -> Optional[Dict[str, Any]]:
        try:
            return json.loads(self.body)
        except Exception:
            return None


class ApiClient:
    def __init__(self, base_url: str, store: str, lang: str):
        self.base_url = base_url.rstrip("/")
        self.store = store
        self.lang = lang
        self.token: Optional[str] = None

    def set_token(self, token: str) -> None:
        self.token = token

    def _request(
        self,
        method: str,
        path: str,
        body: Optional[Any] = None,
        auth: bool = True,
        expected: Iterable[int] = (200, 201),
        extra_headers: Optional[Dict[str, str]] = None,
        timeout: int = 30,
    ) -> Tuple[int, str]:
        url = f"{self.base_url}{path}"
        headers: Dict[str, str] = {}
        if extra_headers:
            headers.update(extra_headers)
        data = None
        if body is not None:
            if "Content-Type" not in headers:
                headers["Content-Type"] = "application/json"
            if headers.get("Content-Type") == "application/json":
                data = json.dumps(body).encode("utf-8")
            else:
                data = body if isinstance(body, (bytes, bytearray)) else str(body).encode("utf-8")
        if auth:
            if not self.token:
                raise RuntimeError("No token set for authenticated request")
            headers["Authorization"] = f"Bearer {self.token}"
        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                text = resp.read().decode("utf-8")
                if resp.status not in set(expected):
                    raise ApiError(resp.status, text, path)
                return resp.status, text
        except urllib.error.HTTPError as e:
            text = e.read().decode("utf-8")
            if e.code in set(expected):
                return e.code, text
            raise ApiError(e.code, text, path)

    def get_json(self, path: str, auth: bool = True) -> Any:
        _, text = self._request("GET", path, auth=auth, expected=(200,))
        return json.loads(text) if text else None

    def post_json(self, path: str, body: Any, auth: bool = True, expected: Iterable[int] = (200, 201)) -> Any:
        _, text = self._request("POST", path, body=body, auth=auth, expected=expected)
        return json.loads(text) if text else None

    def put_json(self, path: str, body: Any, auth: bool = True, expected: Iterable[int] = (200, 201)) -> Any:
        _, text = self._request("PUT", path, body=body, auth=auth, expected=expected)
        return json.loads(text) if text else None

    def patch_json(self, path: str, body: Any, auth: bool = True, expected: Iterable[int] = (200, 201)) -> Any:
        _, text = self._request("PATCH", path, body=body, auth=auth, expected=expected)
        return json.loads(text) if text else None

    def raw_request(self, method: str, path: str, body: Optional[Any] = None, auth: bool = True,
                    expected: Iterable[int] = (200, 201), extra_headers: Optional[Dict[str, str]] = None) -> Tuple[int, str]:
        return self._request(method, path, body=body, auth=auth, expected=expected, extra_headers=extra_headers)


class ShopizerSeeder:
    def __init__(self, base_url: str, store: str, lang: str, target_count: int, prefix: str, dry_run: bool = False):
        self.base_url = base_url.rstrip("/")
        self.store = store
        self.lang = lang
        self.target_count = target_count
        self.prefix = prefix.lower()
        self.prefix_upper = prefix.upper()
        self.today = str(date.today())
        self.dry_run = dry_run

        self.public_client = ApiClient(self.base_url, store, lang)
        self.bootstrap_client = ApiClient(self.base_url, store, lang)
        self.retail_client = ApiClient(self.base_url, store, lang)     # ADMIN_RETAIL
        self.retailer_client = ApiClient(self.base_url, store, lang)   # ADMIN_RETAILER

        self.stats: Dict[str, TableStat] = {t: TableStat() for t in ALL_TABLES}
        self.execution_order: List[str] = []
        self.api_call_samples: Dict[str, Dict[str, Any]] = {}
        self.blockers: List[Dict[str, Any]] = []
        self.runtime_errors: List[str] = []

        self._customer_cache: Dict[str, Dict[str, Any]] = {}
        self._catalog_cache: Dict[str, Dict[str, Any]] = {}
        self._product_sku_cache: Optional[str] = None

        self._prime_static_table_metadata()

    def _prime_static_table_metadata(self) -> None:
        def mark(table: str, *, deps: List[str], apis: List[str], verification: str = "api_verified") -> None:
            self.stats[table].dependencies = deps
            self.stats[table].api_order = apis
            self.stats[table].verification = verification

        mark("CATEGORY", deps=["MERCHANT_STORE (DEFAULT reused)", "LANGUAGE(en reused)"], apis=["GET /api/v1/private/category/unique", "POST /api/v1/private/category"])
        mark("CATEGORY_DESCRIPTION", deps=["CATEGORY"], apis=["POST /api/v1/private/category"], verification="api_inferred")
        mark("MANUFACTURER", deps=["MERCHANT_STORE (DEFAULT reused)", "LANGUAGE(en reused)"], apis=["GET /api/v1/private/manufacturer/unique", "POST /api/v1/private/manufacturer"])
        mark("MANUFACTURER_DESCRIPTION", deps=["MANUFACTURER"], apis=["POST /api/v1/private/manufacturer"], verification="api_inferred")
        mark("CONTENT", deps=["MERCHANT_STORE (DEFAULT reused)", "LANGUAGE(en reused)"], apis=["GET /api/v1/private/content/page/{code}/exists", "POST /api/v1/private/content/page"])
        mark("CONTENT_DESCRIPTION", deps=["CONTENT"], apis=["POST /api/v1/private/content/page"], verification="api_inferred")
        mark("CATALOG", deps=["MERCHANT_STORE (DEFAULT reused)", "LANGUAGE(en reused)"], apis=["GET /api/v1/private/catalog/unique", "POST /api/v1/private/catalog", "GET /api/v1/private/catalogs"])
        mark("CATALOG_ENTRY", deps=["CATALOG", "CATEGORY", "PRODUCT (reused via /api/v1/products)", "MERCHANT_STORE (DEFAULT reused)"], apis=["GET /api/v1/products", "POST /api/v1/private/catalog/{id}", "GET /api/v1/private/catalog/{id}/entry"])
        mark("CUSTOMER", deps=["MERCHANT_STORE (DEFAULT reused)", "COUNTRY(US reused)"], apis=["GET /api/v1/private/customers", "POST /api/v1/private/customer"])
        mark("CUSTOMER_GROUP", deps=["CUSTOMER", "default CUSTOMER group (server-side)"], apis=["POST /api/v1/private/customer"], verification="api_inferred")
        mark("CUSTOMER_REVIEW", deps=["CUSTOMER"], apis=["GET /api/v1/customers/{id}/reviews", "POST /api/v1/private/customers/{id}/reviews"])
        mark("CUSTOMER_REVIEW_DESCRIPTION", deps=["CUSTOMER_REVIEW"], apis=["POST /api/v1/private/customers/{id}/reviews"], verification="api_inferred")
        mark("OPTIN", deps=["MERCHANT_STORE (DEFAULT reused)"], apis=["POST /api/v1/private/optin"], verification="api_inferred")
        mark("CUSTOMER_OPTIN", deps=["OPTIN (newsletter type)", "CUSTOMER (email reused)"], apis=["POST /api/v1/newsletter"], verification="not_queryable_via_api")
        mark("MERCHANT_CONFIGURATION", deps=["MERCHANT_STORE (DEFAULT reused)"], apis=["POST /api/v1/private/modules/payment", "POST /api/v1/private/shipping/origin", "POST /api/v1/private/shipping/expedition", "POST /api/v1/private/store/{code}/marketing"], verification="partially_queryable")
        mark("MERCHANT_STORE", deps=["COUNTRY", "CURRENCY", "LANGUAGE"], apis=["POST /api/v1/private/store", "GET /api/v1/private/store/{code}"])
        mark("MERCHANT_LANGUAGE", deps=["MERCHANT_STORE", "LANGUAGE"], apis=["POST /api/v1/private/store"], verification="api_inferred")
        for t in ["COUNTRY", "COUNTRY_DESCRIPTION", "CURRENCY", "LANGUAGE", "CUSTOMER_ATTRIBUTE", "CUSTOMER_OPTION", "CUSTOMER_OPTION_DESC", "CUSTOMER_OPTION_SET", "CUSTOMER_OPTION_VALUE", "CUSTOMER_OPT_VAL_DESCRIPTION", "FILE_HISTORY", "GEOZONE", "GEOZONE_DESCRIPTION", "MERCHANT_LOG", "MODULE_CONFIGURATION"]:
            if not self.stats[t].dependencies:
                self.stats[t].verification = "no_create_api_found"

    def _set_blocked(self, table: str, reason: str, status: str = "blocked") -> None:
        st = self.stats[table]
        st.status = status
        st.note(reason)

    def _set_unsupported(self, table: str, reason: str) -> None:
        self._set_blocked(table, reason, status="unsupported")

    def _sanitize_sample_payload(self, payload: Any) -> Any:
        sensitive_keys = {
            "password",
            "repeatPassword",
            "token",
            "authorization",
            "accessToken",
            "secret",
            "apiKey",
        }
        if isinstance(payload, dict):
            sanitized = {}
            for key, value in payload.items():
                if key in sensitive_keys:
                    sanitized[key] = "<redacted>"
                else:
                    sanitized[key] = self._sanitize_sample_payload(value)
            return sanitized
        if isinstance(payload, list):
            return [self._sanitize_sample_payload(item) for item in payload]
        return payload

    def _record_api_sample(self, name: str, method: str, path: str, payload: Any) -> None:
        if name not in self.api_call_samples:
            self.api_call_samples[name] = {
                "method": method,
                "path": path,
                "payload": self._sanitize_sample_payload(payload),
            }

    def _record_blocker(self, table: str, step: str, error: str) -> None:
        self.blockers.append({"table": table, "step": step, "error": error})

    def _note_exec(self, step: str) -> None:
        if step not in self.execution_order:
            self.execution_order.append(step)

    def _login(self, client: ApiClient, username: str, password: str) -> Dict[str, Any]:
        body = {"username": username, "password": password}
        self._record_api_sample("login", "POST", "/api/v1/private/login", body)
        resp = client.post_json("/api/v1/private/login", body, auth=False, expected=(200,))
        if not isinstance(resp, dict) or not resp.get("token"):
            raise RuntimeError(f"Login failed for {username}: missing token")
        client.set_token(resp["token"])
        return resp

    def _paginate(self, client: ApiClient, path_builder, items_key: str, page_size: int = 100, max_pages: int = 25) -> List[Dict[str, Any]]:
        items: List[Dict[str, Any]] = []
        seen_ids = set()
        for page in range(max_pages):
            path = path_builder(page, page_size)
            data = client.get_json(path)
            batch = data.get(items_key) if isinstance(data, dict) else None
            if batch is None and isinstance(data, dict):
                batch = data.get("items")
            if not batch:
                break
            new_count = 0
            for obj in batch:
                oid = obj.get("id") if isinstance(obj, dict) else None
                if oid is not None and oid in seen_ids:
                    continue
                if oid is not None:
                    seen_ids.add(oid)
                items.append(obj)
                new_count += 1
            if new_count == 0 or len(batch) < page_size:
                break
        return items

    def _with_ctx(self, path: str) -> str:
        sep = "&" if "?" in path else "?"
        return f"{path}{sep}store={urllib.parse.quote(self.store)}&lang={urllib.parse.quote(self.lang)}"

    def _bootstrap_seed_users(self) -> None:
        bootstrap_user = os.getenv("SHOPIZER_BOOTSTRAP_USERNAME", "iamskk1@gmail.com")
        bootstrap_pass = os.getenv("SHOPIZER_BOOTSTRAP_PASSWORD", "password")
        self._login(self.bootstrap_client, bootstrap_user, bootstrap_pass)

        retail_user = os.getenv("SHOPIZER_SEED_ADMIN_RETAIL_USERNAME", "seed.retail.admin")
        retail_pass = os.getenv("SHOPIZER_SEED_ADMIN_RETAIL_PASSWORD", "Password123!")
        retailer_user = os.getenv("SHOPIZER_SEED_ADMIN_RETAILER_USERNAME", "seed.retailer.admin")
        retailer_pass = os.getenv("SHOPIZER_SEED_ADMIN_RETAILER_PASSWORD", "Password123!")

        self._ensure_admin_user(retail_user, f"{retail_user}@example.com", retail_pass, "ADMIN_RETAIL")
        self._ensure_admin_user(retailer_user, f"{retailer_user}@example.com", retailer_pass, "ADMIN_RETAILER")

        self._login(self.retail_client, retail_user, retail_pass)
        self._login(self.retailer_client, retailer_user, retailer_pass)

    def _ensure_admin_user(self, username: str, email: str, password: str, group_name: str) -> None:
        # Prefer list-by-email for duplicate-safe reuse (user/unique endpoint requires merchant payload and returns only exists flag).
        list_path = self._with_ctx(f"/api/v1/private/users?page=0&count=200&emailAddress={urllib.parse.quote(email)}")
        users = self.bootstrap_client.get_json(list_path)
        existing = []
        if isinstance(users, dict):
            existing = users.get("users") or users.get("data") or []
        if any((u.get("userName") == username or u.get("emailAddress") == email) for u in existing if isinstance(u, dict)):
            return

        payload = {
            "userName": username,
            "emailAddress": email,
            "firstName": "Seed",
            "lastName": group_name,
            "password": password,
            "repeatPassword": password,
            "defaultLanguage": self.lang,
            "active": True,
            "store": self.store,
            "groups": [{"name": group_name, "type": "ADMIN"}],
        }
        self._record_api_sample("create_admin_user", "POST", self._with_ctx("/api/v1/private/user/"), payload)
        if self.dry_run:
            return
        try:
            self.bootstrap_client.post_json(self._with_ctx("/api/v1/private/user/"), payload, expected=(200,))
        except ApiError as e:
            # Duplicate or existing role assignment should not fail the whole seed run.
            if e.status in (400, 409):
                return
            raise

    def seed(self) -> Dict[str, Any]:
        self._note_exec("Step 1: Identify dependency chain and initialize authenticated API clients")
        self._bootstrap_seed_users()

        self._mark_unreachable_tables()

        self._note_exec("Step 2: Create/reuse base tables and directly seedable master entities")
        self.seed_categories(self.target_count)
        self.seed_manufacturers(self.target_count)
        self.seed_content_pages(self.target_count)
        self.seed_catalogs(self.target_count)
        self.seed_customers(self.target_count)
        self.seed_optins(self.target_count)

        self._note_exec("Step 3: Create dependent entities using IDs returned from APIs")
        self.seed_customer_reviews(self.target_count)
        self.seed_customer_optins(self.target_count)
        self.seed_catalog_entries(self.target_count)  # expected blocked in current runtime
        self.seed_merchant_configuration_partial()
        self.seed_store_attempt()  # expected blocked in current runtime

        self._note_exec("Step 4: Repeat until target counts are met for supported tables; mark blocked/unsupported tables explicitly")
        self._finalize_statuses()

        self._note_exec("Step 5: Stop on runtime blockers, preserve idempotency, and rely on per-request API transactions")

        return self.build_report()

    def _mark_unreachable_tables(self) -> None:
        unsupported_reasons = {
            "COUNTRY": "No create API is exposed in REST references endpoints; store-create indirect path is blocked in current runtime.",
            "COUNTRY_DESCRIPTION": "No country description create API is exposed; reference endpoints are read-only.",
            "CURRENCY": "No create API is exposed in REST references endpoints; indirect creation requires store create and valid missing currency codes.",
            "LANGUAGE": "No dedicated create API is exposed; indirect creation is not reliably observable via /api/v1/languages in current runtime.",
            "CUSTOMER_ATTRIBUTE": "No customer-attribute create/list API found in public/admin REST layer.",
            "CUSTOMER_OPTION": "No customer option create API found in public/admin REST layer.",
            "CUSTOMER_OPTION_DESC": "No customer option description create API found in public/admin REST layer.",
            "CUSTOMER_OPTION_SET": "No customer option set create API found in public/admin REST layer.",
            "CUSTOMER_OPTION_VALUE": "No customer option value create API found in public/admin REST layer.",
            "CUSTOMER_OPT_VAL_DESCRIPTION": "No customer option value description create API found in public/admin REST layer.",
            "FILE_HISTORY": "No deterministic file-history API for direct create/list verification was identified.",
            "GEOZONE": "No geozone create API found in the REST layer.",
            "GEOZONE_DESCRIPTION": "No geozone description create API found in the REST layer.",
            "MERCHANT_LOG": "Merchant log rows are internal/audit-generated and no create/list API is exposed.",
            "MODULE_CONFIGURATION": "System module-definition API is legacy/global and unsafe for seeding; no safe idempotent admin create/list flow for this task.",
        }
        for table, reason in unsupported_reasons.items():
            self._set_unsupported(table, reason)

    def _exists_flag(self, client: ApiClient, path: str) -> bool:
        data = client.get_json(path)
        return bool(isinstance(data, dict) and data.get("exists"))

    def seed_categories(self, target: int) -> None:
        st = self.stats["CATEGORY"]
        st_desc = self.stats["CATEGORY_DESCRIPTION"]
        self._note_exec("Create/reuse CATEGORY (+ CATEGORY_DESCRIPTION) via /api/v1/private/category")
        for i in range(1, target + 1):
            code = f"{self.prefix}_cat_{i:02d}"
            exists_path = self._with_ctx(f"/api/v1/private/category/unique?code={urllib.parse.quote(code)}")
            if self._exists_flag(self.retail_client, exists_path):
                st.reused += 1
                st.status = "partial" if st.status == "pending" else st.status
                st_desc.reused += 1
                continue
            payload = {
                "code": code,
                "visible": True,
                "featured": False,
                "description": {
                    "language": self.lang,
                    "name": f"{self.prefix} category {i:02d}",
                    "title": f"{self.prefix} category {i:02d}",
                    "friendlyUrl": f"{self.prefix}-category-{i:02d}",
                },
            }
            self._record_api_sample("create_category", "POST", self._with_ctx("/api/v1/private/category"), payload)
            if self.dry_run:
                st.inserted += 1
                st_desc.inferred_inserted += 1
                continue
            try:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/category"), payload, expected=(201,))
                st.inserted += 1
                st_desc.inferred_inserted += 1
            except ApiError as e:
                st.status = "blocked"
                st.note(f"Create failed for code {code}: {e.body[:300]}")
                st_desc.status = "blocked"
                st_desc.note("Blocked because CATEGORY create failed")
                self._record_blocker("CATEGORY", "create_category", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"
        if st_desc.status == "pending":
            st_desc.status = "seeded" if (st_desc.inferred_inserted + st_desc.reused) >= target else "partial"
            st_desc.note("One description row per category create is inferred from category payload with description object")

    def seed_manufacturers(self, target: int) -> None:
        st = self.stats["MANUFACTURER"]
        st_desc = self.stats["MANUFACTURER_DESCRIPTION"]
        self._note_exec("Create/reuse MANUFACTURER (+ MANUFACTURER_DESCRIPTION) via /api/v1/private/manufacturer")
        for i in range(1, target + 1):
            code = f"{self.prefix}_man_{i:02d}"
            exists_path = self._with_ctx(f"/api/v1/private/manufacturer/unique?code={urllib.parse.quote(code)}")
            if self._exists_flag(self.retail_client, exists_path):
                st.reused += 1
                st_desc.reused += 1
                continue
            payload = {
                "code": code,
                "descriptions": [{"language": self.lang, "name": f"{self.prefix} manufacturer {i:02d}"}],
            }
            self._record_api_sample("create_manufacturer", "POST", self._with_ctx("/api/v1/private/manufacturer"), payload)
            if self.dry_run:
                st.inserted += 1
                st_desc.inferred_inserted += 1
                continue
            try:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/manufacturer"), payload, expected=(201,))
                st.inserted += 1
                st_desc.inferred_inserted += 1
            except ApiError as e:
                st.status = "blocked"
                st_desc.status = "blocked"
                st.note(f"Create failed for code {code}: {e.body[:300]}")
                st_desc.note("Blocked because manufacturer create failed")
                self._record_blocker("MANUFACTURER", "create_manufacturer", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"
        if st_desc.status == "pending":
            st_desc.status = "seeded" if st_desc.inferred_inserted + st_desc.reused >= target else "partial"
            st_desc.note("Manufacturer description rows are inferred from create payload descriptions[]")

    def seed_content_pages(self, target: int) -> None:
        st = self.stats["CONTENT"]
        st_desc = self.stats["CONTENT_DESCRIPTION"]
        self._note_exec("Create/reuse CONTENT (+ CONTENT_DESCRIPTION) via /api/v1/private/content/page")
        for i in range(1, target + 1):
            code = f"{self.prefix}_page_{i:02d}"
            exists_path = self._with_ctx(f"/api/v1/private/content/page/{urllib.parse.quote(code)}/exists")
            if self._exists_flag(self.retail_client, exists_path):
                st.reused += 1
                st_desc.reused += 1
                continue
            payload = {
                "id": None,
                "code": code,
                "visible": True,
                "contentType": "PAGE",
                "linkToMenu": False,
                "descriptions": [
                    {
                        "language": self.lang,
                        "name": f"{self.prefix} page {i:02d}",
                        "title": f"{self.prefix} page {i:02d}",
                        "description": f"{self.prefix} content page {i:02d}",
                        "friendlyUrl": f"{self.prefix}-page-{i:02d}",
                    }
                ],
            }
            self._record_api_sample("create_content_page", "POST", self._with_ctx("/api/v1/private/content/page"), payload)
            if self.dry_run:
                st.inserted += 1
                st_desc.inferred_inserted += 1
                continue
            try:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/content/page"), payload, expected=(201,))
                st.inserted += 1
                st_desc.inferred_inserted += 1
            except ApiError as e:
                st.status = "blocked"
                st_desc.status = "blocked"
                st.note(f"Create failed for code {code}: {e.body[:300]}")
                st_desc.note("Blocked because content page create failed")
                self._record_blocker("CONTENT", "create_content_page", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"
        if st_desc.status == "pending":
            st_desc.status = "seeded" if st_desc.inferred_inserted + st_desc.reused >= target else "partial"
            st_desc.note("Content description rows are inferred from page create payload descriptions[]")

    def _load_catalogs(self) -> Dict[str, Dict[str, Any]]:
        items = self._paginate(
            self.retail_client,
            lambda page, count: self._with_ctx(f"/api/v1/private/catalogs?page={page}&count={count}"),
            "items",
            page_size=200,
        )
        self._catalog_cache = {it.get("code"): it for it in items if isinstance(it, dict) and it.get("code")}
        return self._catalog_cache

    def seed_catalogs(self, target: int) -> None:
        st = self.stats["CATALOG"]
        self._note_exec("Create/reuse CATALOG via /api/v1/private/catalog and retrieve IDs via /api/v1/private/catalogs")
        self._load_catalogs()
        for i in range(1, target + 1):
            code = f"{self.prefix}_catalog_{i:02d}"
            if code in self._catalog_cache:
                st.reused += 1
                continue
            exists_path = self._with_ctx(f"/api/v1/private/catalog/unique?code={urllib.parse.quote(code)}")
            if self._exists_flag(self.retail_client, exists_path):
                # unique endpoint says exists but cache is stale; refresh on demand
                self._load_catalogs()
                if code in self._catalog_cache:
                    st.reused += 1
                    continue
            payload = {"id": None, "code": code, "visible": True, "defaultCatalog": False}
            self._record_api_sample("create_catalog", "POST", self._with_ctx("/api/v1/private/catalog"), payload)
            if self.dry_run:
                st.inserted += 1
                self._catalog_cache[code] = {"id": None, "code": code}
                continue
            try:
                resp = self.retail_client.post_json(self._with_ctx("/api/v1/private/catalog"), payload, expected=(200,))
                if isinstance(resp, dict):
                    self._catalog_cache[code] = resp
                else:
                    self._load_catalogs()
                st.inserted += 1
            except ApiError as e:
                st.status = "blocked"
                st.note(f"Create failed for code {code}: {e.body[:300]}")
                self._record_blocker("CATALOG", "create_catalog", str(e))
                break
        # Refresh IDs for reused rows (used later by catalog-entry attempt)
        if not self.dry_run:
            self._load_catalogs()
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"

    def _load_customers(self) -> Dict[str, Dict[str, Any]]:
        customers = self._paginate(
            self.retail_client,
            lambda page, count: self._with_ctx(f"/api/v1/private/customers?page={page}&count={count}"),
            "customers",
            page_size=200,
            max_pages=50,
        )
        self._customer_cache = {
            c.get("emailAddress"): c for c in customers if isinstance(c, dict) and c.get("emailAddress")
        }
        return self._customer_cache

    def seed_customers(self, target: int) -> None:
        st = self.stats["CUSTOMER"]
        st_group = self.stats["CUSTOMER_GROUP"]
        self._note_exec("Create/reuse CUSTOMER (+ inferred CUSTOMER_GROUP join rows) via /api/v1/private/customer")
        self._load_customers()
        for i in range(1, target + 1):
            email = f"{self.prefix}.customer.{i:02d}@example.com"
            if email in self._customer_cache:
                st.reused += 1
                st_group.reused += 1
                continue
            payload = {
                "emailAddress": email,
                "userName": email,
                "firstName": f"Seed{i:02d}",
                "lastName": "Customer",
                "password": "Password123!",
                "repeatPassword": "Password123!",
                "billing": {
                    "firstName": f"Seed{i:02d}",
                    "lastName": "Customer",
                    "address": f"{i} Seed Street",
                    "city": "Austin",
                    "postalCode": f"73{i:03d}",
                    "country": "US",
                    "stateProvince": "TX",
                    "phone": f"555000{i:04d}"[-10:],
                },
            }
            self._record_api_sample("create_customer", "POST", self._with_ctx("/api/v1/private/customer"), payload)
            if self.dry_run:
                st.inserted += 1
                st_group.inferred_inserted += 1
                self._customer_cache[email] = {"id": None, "emailAddress": email}
                continue
            try:
                resp = self.retail_client.post_json(self._with_ctx("/api/v1/private/customer"), payload, expected=(200, 201))
                if isinstance(resp, dict):
                    self._customer_cache[email] = resp
                st.inserted += 1
                groups = resp.get("groups") if isinstance(resp, dict) else None
                if groups:
                    st_group.inferred_inserted += 1
                else:
                    st_group.note("Customer create response did not expose groups for some rows; join-row count is inferred")
            except ApiError as e:
                # Duplicate path can happen on rerun if customer exists but pagination metadata hid it.
                if e.status in (400, 409):
                    self._load_customers()
                    if email in self._customer_cache:
                        st.reused += 1
                        st_group.reused += 1
                        continue
                st.status = "blocked"
                st_group.status = "blocked"
                st.note(f"Create failed for {email}: {e.body[:300]}")
                st_group.note("Blocked because customer create failed")
                self._record_blocker("CUSTOMER", "create_customer", str(e))
                break
        if not self.dry_run:
            self._load_customers()
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"
        if st_group.status == "pending":
            st_group.status = "seeded" if (st_group.inferred_inserted + st_group.reused) >= target else "partial"
            st_group.note("CUSTOMER_GROUP is a join table populated indirectly by customer create/default group assignment")

    def seed_customer_reviews(self, target: int) -> None:
        st = self.stats["CUSTOMER_REVIEW"]
        st_desc = self.stats["CUSTOMER_REVIEW_DESCRIPTION"]
        self._note_exec("Create/reuse CUSTOMER_REVIEW (+ CUSTOMER_REVIEW_DESCRIPTION) via customer review API using returned customer IDs")
        if not self._customer_cache:
            self._load_customers()
        seeded_customers = []
        for i in range(1, target + 1):
            email = f"{self.prefix}.customer.{i:02d}@example.com"
            c = self._customer_cache.get(email)
            if c:
                seeded_customers.append((i, c))
        if len(seeded_customers) < target:
            st.status = "blocked"
            st_desc.status = "blocked"
            st.note("Not enough seeded customers available to create customer reviews")
            st_desc.note("Blocked because CUSTOMER seed did not reach target")
            self._record_blocker("CUSTOMER_REVIEW", "precheck_customers", "Missing customer IDs for review creation")
            return

        for i, c in seeded_customers:
            cid = c.get("id")
            if not cid:
                st.status = "blocked"
                st_desc.status = "blocked"
                msg = f"Customer {c.get('emailAddress')} missing API-exposed ID"
                st.note(msg)
                st_desc.note(msg)
                self._record_blocker("CUSTOMER_REVIEW", "customer_id_lookup", msg)
                break
            desc = f"{self.prefix} customer review {i:02d}"
            try:
                reviews = self.retail_client.get_json(self._with_ctx(f"/api/v1/customers/{cid}/reviews"), auth=True)
                if isinstance(reviews, list) and any(r.get("description") == desc for r in reviews if isinstance(r, dict)):
                    st.reused += 1
                    st_desc.reused += 1
                    continue
            except ApiError as e:
                if e.status != 404:
                    st.note(f"Review list failed for customer {cid}: {e.body[:200]}")
            payload = {
                "description": desc,
                "rating": float((i % 5) + 1),
                "reviewedCustomer": cid,
                "customerId": cid,
                "date": self.today,
            }
            self._record_api_sample("create_customer_review", "POST", self._with_ctx("/api/v1/private/customers/{id}/reviews"), payload)
            if self.dry_run:
                st.inserted += 1
                st_desc.inferred_inserted += 1
                continue
            try:
                self.retail_client.post_json(self._with_ctx(f"/api/v1/private/customers/{cid}/reviews"), payload, expected=(201,))
                st.inserted += 1
                st_desc.inferred_inserted += 1
            except ApiError as e:
                st.status = "blocked"
                st_desc.status = "blocked"
                st.note(f"Create failed for customer {cid}: {e.body[:300]}")
                st_desc.note("Blocked because customer review create failed")
                self._record_blocker("CUSTOMER_REVIEW", "create_customer_review", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"
        if st_desc.status == "pending":
            st_desc.status = "seeded" if st_desc.inferred_inserted + st_desc.reused >= target else "partial"
            st_desc.note("Customer review description rows are inferred from review create payload description field")

    def seed_optins(self, target: int) -> None:
        st = self.stats["OPTIN"]
        self._note_exec("Create/reuse OPTIN definitions via /api/v1/private/optin (409 duplicate => reuse)")
        for i in range(1, target + 1):
            code = f"{self.prefix_upper}_OPTIN_{i:02d}"
            payload = {
                "code": code,
                "optinType": "NEWSLETTER",
                "description": f"{self.prefix} optin {i:02d}",
                "store": self.store,
            }
            self._record_api_sample("create_optin", "POST", self._with_ctx("/api/v1/private/optin"), payload)
            if self.dry_run:
                st.inferred_inserted += 1
                continue
            try:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/optin"), payload, expected=(200,))
                st.inferred_inserted += 1
            except ApiError as e:
                if e.status == 409:
                    st.reused += 1
                    continue
                st.status = "blocked"
                st.note(f"Create failed for code {code}: {e.body[:300]}")
                self._record_blocker("OPTIN", "create_optin", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inferred_inserted + st.reused >= target else "partial"
            st.note("OPTIN create endpoint does not expose a list API for ID lookup; duplicate prevention uses API unique-key conflict (409)")
            st.verification = "api_inferred"

    def seed_customer_optins(self, target: int) -> None:
        st = self.stats["CUSTOMER_OPTIN"]
        self._note_exec("Create/reuse CUSTOMER_OPTIN via /api/v1/newsletter using seeded customer emails (idempotent 200 responses)")
        # Use the same deterministic customer emails. Newsletter endpoint is idempotent and returns no body.
        for i in range(1, target + 1):
            email = f"{self.prefix}.customer.{i:02d}@example.com"
            payload = {"email": email, "firstName": f"Seed{i:02d}", "lastName": "Customer"}
            self._record_api_sample("create_customer_optin_newsletter", "POST", self._with_ctx("/api/v1/newsletter"), payload)
            if self.dry_run:
                st.inferred_inserted += 1
                continue
            try:
                # Public endpoint; no auth token required.
                self.public_client.post_json(self._with_ctx("/api/v1/newsletter"), payload, auth=False, expected=(200,))
                st.inferred_inserted += 1
            except ApiError as e:
                st.status = "blocked"
                st.note(f"Newsletter optin failed for {email}: {e.body[:300]}")
                self._record_blocker("CUSTOMER_OPTIN", "newsletter_create", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inferred_inserted >= target else "partial"
            st.verification = "not_queryable_via_api"
            st.note("Newsletter endpoint returns 200 with no body and no list endpoint exists for CUSTOMER_OPTIN; count is inferred from successful submissions")

    def _get_seed_product_sku(self) -> Optional[str]:
        if self._product_sku_cache:
            return self._product_sku_cache
        data = self.public_client.get_json(self._with_ctx("/api/v1/products?page=0&count=20"), auth=False)
        products = data.get("products") if isinstance(data, dict) else None
        if not products:
            return None
        for p in products:
            sku = p.get("sku") or p.get("productSku") or p.get("productCode")
            if sku:
                self._product_sku_cache = sku
                return sku
        return None

    def seed_catalog_entries(self, target: int) -> None:
        st = self.stats["CATALOG_ENTRY"]
        self._note_exec("Attempt CATALOG_ENTRY seeding using catalog IDs, category codes, and product SKU (blocked in current runtime)")
        if st.status in ("unsupported", "blocked"):
            return
        sku = self._get_seed_product_sku()
        if not sku:
            st.status = "blocked"
            st.note("No product SKU available from /api/v1/products")
            self._record_blocker("CATALOG_ENTRY", "product_lookup", "No product SKU returned by /api/v1/products")
            return
        if not self._catalog_cache:
            self._load_catalogs()
        if not self._catalog_cache:
            st.status = "blocked"
            st.note("No catalogs available for entry creation")
            self._record_blocker("CATALOG_ENTRY", "catalog_lookup", "No catalogs available")
            return

        for i in range(1, target + 1):
            catalog_code = f"{self.prefix}_catalog_{i:02d}"
            category_code = f"{self.prefix}_cat_{i:02d}"
            catalog = self._catalog_cache.get(catalog_code)
            if not catalog or not catalog.get("id"):
                st.note(f"Skipping {catalog_code}: missing catalog ID in list response")
                continue
            catalog_id = catalog["id"]
            # duplicate check via entry list when endpoint is working
            try:
                existing = self.retail_client.get_json(self._with_ctx(f"/api/v1/private/catalog/{catalog_id}/entry?page=0&count=200"))
                items = existing.get("items") if isinstance(existing, dict) else None
                if items:
                    if any((e.get("productCode") == sku and e.get("categoryCode") == category_code) for e in items if isinstance(e, dict)):
                        st.reused += 1
                        continue
            except ApiError:
                pass

            payload = {"id": None, "productCode": sku, "categoryCode": category_code, "visible": True}
            self._record_api_sample("create_catalog_entry", "POST", self._with_ctx("/api/v1/private/catalog/{id}"), payload)
            if self.dry_run:
                st.inserted += 1
                continue
            try:
                self.retail_client.post_json(self._with_ctx(f"/api/v1/private/catalog/{catalog_id}"), payload, expected=(200, 201))
                st.inserted += 1
            except ApiError as e:
                # Current runtime consistently fails with ConversionRuntimeException.
                st.status = "blocked"
                st.note(f"Runtime API error on catalog entry create: {e.body[:400]}")
                self._record_blocker("CATALOG_ENTRY", "create_catalog_entry", str(e))
                break
        if st.status == "pending":
            st.status = "seeded" if st.inserted + st.reused >= target else "partial"

    def seed_merchant_configuration_partial(self) -> None:
        st = self.stats["MERCHANT_CONFIGURATION"]
        self._note_exec("Configure merchant settings via existing APIs to seed/update MERCHANT_CONFIGURATION rows where possible")
        st.verification = "partially_queryable"
        st.status = "partial"
        st.note("MERCHANT_CONFIGURATION has no list/count API; successful writes are tracked, row inserts are inferred")

        # 1) Payment module configuration (creates/updates PAYMENT_MODULES config row)
        payment_payload = {
            "code": "moneyorder",
            "active": True,
            "defaultSelected": False,
            "integrationKeys": {"address": f"{self.prefix} payment address"},
            "integrationOptions": {},
        }
        self._record_api_sample("configure_payment_module", "POST", self._with_ctx("/api/v1/private/modules/payment"), payment_payload)
        try:
            if not self.dry_run:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/modules/payment"), payment_payload, expected=(200,))
            st.inferred_inserted += 1
            st.note("Payment module configuration submitted successfully (PAYMENT_MODULES merchant configuration key updated)")
        except ApiError as e:
            st.note(f"Payment module configuration failed: {e.body[:300]}")
            self._record_blocker("MERCHANT_CONFIGURATION", "configure_payment_module", str(e))

        # 2) Shipping origin (ShippingConfiguration merchant config)
        origin_payload = {
            "address": f"{self.prefix} shipping origin",
            "city": "Austin",
            "postalCode": "73301",
            "country": "US",
            "stateProvince": "TX",
        }
        self._record_api_sample("save_shipping_origin", "POST", self._with_ctx("/api/v1/private/shipping/origin"), origin_payload)
        try:
            if not self.dry_run:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/shipping/origin"), origin_payload, expected=(200,))
            st.inferred_inserted += 1
            st.note("Shipping origin saved successfully (shipping configuration stored server-side)")
        except ApiError as e:
            st.note(f"Shipping origin save failed: {e.body[:300]}")
            self._record_blocker("MERCHANT_CONFIGURATION", "save_shipping_origin", str(e))

        # 3) Shipping expedition config (ship-to countries / tax on shipping)
        expedition_payload = {"iternationalShipping": False, "taxOnShipping": False, "shipToCountry": []}
        self._record_api_sample("save_shipping_expedition", "POST", self._with_ctx("/api/v1/private/shipping/expedition"), expedition_payload)
        try:
            if not self.dry_run:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/shipping/expedition"), expedition_payload, expected=(200,))
            st.inferred_inserted += 1
            st.note("Shipping expedition configuration submitted successfully (supported-countries config likely updated)")
        except ApiError as e:
            st.note(f"Shipping expedition save failed: {e.body[:300]}")
            self._record_blocker("MERCHANT_CONFIGURATION", "save_shipping_expedition", str(e))

        # 4) Store marketing config (brand/socials) on existing DEFAULT store
        marketing_payload = {"socialNetworks": []}
        self._record_api_sample("save_store_marketing", "POST", self._with_ctx(f"/api/v1/private/store/{self.store}/marketing"), marketing_payload)
        try:
            if not self.dry_run:
                current_brand = self.retail_client.get_json(self._with_ctx(f"/api/v1/private/store/{self.store}/marketing"))
                if isinstance(current_brand, dict):
                    current_brand.setdefault("socialNetworks", [])
                    marketing_payload = current_brand
                self.retail_client.post_json(self._with_ctx(f"/api/v1/private/store/{self.store}/marketing"), marketing_payload, expected=(201, 200))
            st.inferred_inserted += 1
            st.note("Store marketing saved successfully (merchant CONFIG row likely updated)")
        except ApiError as e:
            st.note(f"Store marketing save failed: {e.body[:300]}")
            self._record_blocker("MERCHANT_CONFIGURATION", "save_store_marketing", str(e))

        # Attempt shipping module config to document runtime bug that prevents more MERCHANT_CONFIGURATION rows.
        shipping_module_payload = {
            "code": "storePickUp",
            "active": True,
            "defaultSelected": False,
            "integrationKeys": {"price": "0.00", "note": "Pickup at store"},
            "integrationOptions": {},
        }
        self._record_api_sample("configure_shipping_module", "POST", self._with_ctx("/api/v1/private/modules/shipping"), shipping_module_payload)
        try:
            if not self.dry_run:
                self.retail_client.post_json(self._with_ctx("/api/v1/private/modules/shipping"), shipping_module_payload, expected=(200,))
            st.inferred_inserted += 1
        except ApiError as e:
            st.note("Shipping module configuration API is runtime-broken in current server (returns 500 wrapper on valid payload)")
            st.note(f"Observed error: {e.body[:200]}")
            self._record_blocker("MERCHANT_CONFIGURATION", "configure_shipping_module", str(e))

        # Cannot reach target 10 without creating additional stores or working shipping-module create/update flow.
        if st.inferred_inserted + st.inserted + st.reused < self.target_count:
            st.note("Target 10 MERCHANT_CONFIGURATION rows cannot be reached safely via currently working APIs in this runtime")

    def seed_store_attempt(self) -> None:
        st = self.stats["MERCHANT_STORE"]
        st_lang = self.stats["MERCHANT_LANGUAGE"]
        self._note_exec("Attempt MERCHANT_STORE creation with ADMIN_RETAILER to validate current runtime support")
        payload = {
            "code": f"{self.prefix_upper}_STORE_01",
            "name": f"{self.prefix} Store 01",
            "email": f"{self.prefix}.store.01@example.com",
            "phone": "1112223333",
            "supportedLanguages": [self.lang],
            "defaultLanguage": self.lang,
            "currency": "USD",
            "inBusinessSince": self.today,
            "weight": "LB",
            "dimension": "IN",
            "useCache": False,
            "address": {
                "address": "1 Seed Store Street",
                "city": "Austin",
                "postalCode": "73301",
                "country": "US",
                "stateProvince": "TX",
            },
        }
        self._record_api_sample("create_store", "POST", self._with_ctx("/api/v1/private/store"), payload)
        if self.dry_run:
            st.status = "partial"
            st.note("Dry-run only; store create not executed")
            st_lang.status = "partial"
            st_lang.note("Dry-run only; merchant-language join rows not executed")
            return
        try:
            # Try unique first for duplicate-safe behavior.
            exists = self.retailer_client.get_json(self._with_ctx(f"/api/v1/private/store/unique?code={urllib.parse.quote(payload['code'])}"))
            if isinstance(exists, dict) and exists.get("exists"):
                st.reused += 1
                st_lang.reused += 1
                st.status = "partial"
                st_lang.status = "partial"
                st.note("Store already exists; full 10-store seed not attempted because runtime is currently unstable for store create")
                return
            self.retailer_client.post_json(self._with_ctx("/api/v1/private/store"), payload, expected=(200,))
            # If create succeeds unexpectedly, mark partial only (script intentionally does not create all 10 stores in current version).
            st.inserted += 1
            st_lang.inferred_inserted += 1
            st.status = "partial"
            st_lang.status = "partial"
            st.note("Single store create probe succeeded; extend script to full 10-store loop if runtime is stable")
        except ApiError as e:
            st.status = "blocked"
            st_lang.status = "blocked"
            st.note("Store create is blocked in current runtime")
            st_lang.note("Blocked because MERCHANT_STORE create failed")
            # Common observed issue after dynamic dependency resolver changes: Country HibernateProxy unknown entity type.
            st.note(f"Observed error: {e.body[:350]}")
            self._record_blocker("MERCHANT_STORE", "create_store", str(e))

    def _finalize_statuses(self) -> None:
        for table, stat in self.stats.items():
            total_effective = stat.inserted + stat.reused + stat.inferred_inserted
            if stat.status == "pending":
                if total_effective == 0:
                    stat.status = "unsupported" if stat.verification == "no_create_api_found" else "partial"
                elif total_effective >= stat.target:
                    stat.status = "seeded"
                else:
                    stat.status = "partial"

        # Specific unsupported tables not reached by runtime attempts.
        for table in [
            "COUNTRY", "COUNTRY_DESCRIPTION", "CURRENCY", "LANGUAGE",
            "CUSTOMER_ATTRIBUTE", "CUSTOMER_OPTION", "CUSTOMER_OPTION_DESC", "CUSTOMER_OPTION_SET",
            "CUSTOMER_OPTION_VALUE", "CUSTOMER_OPT_VAL_DESCRIPTION", "FILE_HISTORY",
            "GEOZONE", "GEOZONE_DESCRIPTION", "MERCHANT_LOG", "MODULE_CONFIGURATION",
        ]:
            if self.stats[table].status == "pending":
                self._set_unsupported(table, "No safe/create-capable API path was confirmed for this table")

    def build_report(self) -> Dict[str, Any]:
        return {
            "baseUrl": self.base_url,
            "store": self.store,
            "lang": self.lang,
            "targetPerTable": self.target_count,
            "prefix": self.prefix,
            "dryRun": self.dry_run,
            "executionOrder": self.execution_order,
            "apiCallSamples": self.api_call_samples,
            "tables": {k: {
                "status": v.status,
                "target": v.target,
                "inserted": v.inserted,
                "reused": v.reused,
                "inferredInserted": v.inferred_inserted,
                "verification": v.verification,
                "dependencies": v.dependencies,
                "apiOrder": v.api_order,
                "notes": v.notes,
            } for k, v in self.stats.items()},
            "blockers": self.blockers,
            "runtimeErrors": self.runtime_errors,
        }


def main() -> int:
    parser = argparse.ArgumentParser(description="API-only Shopizer table seeder (supported subset + blocker report)")
    parser.add_argument("--base-url", default=os.getenv("SHOPIZER_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--store", default=os.getenv("SHOPIZER_STORE", "DEFAULT"))
    parser.add_argument("--lang", default=os.getenv("SHOPIZER_LANG", "en"))
    parser.add_argument("--target", type=int, default=int(os.getenv("SHOPIZER_SEED_TARGET", "10")))
    parser.add_argument("--prefix", default=os.getenv("SHOPIZER_SEED_PREFIX", "api_seed"))
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--report", default="seed_reports/api_seed_report.json")
    args = parser.parse_args()

    seeder = ShopizerSeeder(
        base_url=args.base_url,
        store=args.store,
        lang=args.lang,
        target_count=args.target,
        prefix=args.prefix,
        dry_run=args.dry_run,
    )

    try:
        report = seeder.seed()
    except Exception as e:
        report = seeder.build_report()
        report.setdefault("runtimeErrors", []).append(str(e))
        report["fatalError"] = traceback.format_exc()
        print(f"FATAL: {e}", file=sys.stderr)

    report_path = args.report
    report_dir = os.path.dirname(report_path)
    if report_dir:
        os.makedirs(report_dir, exist_ok=True)
    with open(report_path, "w", encoding="utf-8") as fh:
        json.dump(report, fh, indent=2, sort_keys=True)

    # Human-readable summary.
    print(f"Report written to {report_path}")
    print(f"Base URL: {report.get('baseUrl')}  Store: {report.get('store')}  Lang: {report.get('lang')}")
    print(f"Target per table: {report.get('targetPerTable')}")
    print()
    print("Table Summary:")
    tables = report.get("tables", {})
    for name in ALL_TABLES:
        t = tables.get(name, {})
        status = t.get("status", "?")
        ins = t.get("inserted", 0)
        reu = t.get("reused", 0)
        inf = t.get("inferredInserted", 0)
        ver = t.get("verification", "")
        print(f"- {name}: status={status} inserted={ins} reused={reu} inferred={inf} verification={ver}")
    blockers = report.get("blockers") or []
    if blockers:
        print() 
        print("Blockers:")
        for b in blockers:
            print(f"- {b.get('table')}: {b.get('step')} -> {b.get('error')}")

    return 0 if not report.get("fatalError") else 1


if __name__ == "__main__":
    raise SystemExit(main())
