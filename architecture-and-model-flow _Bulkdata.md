# Dynamic Pricing Engine — Architecture & Model Flow

## 1. High-Level Architecture

The system follows a classic **Spring Boot layered architecture**, with a Strategy Pattern layer injected for pricing rule evaluation and a caching layer sitting between the service and repository tiers to keep hot-path reads fast.

```
       ┌─────────────────────────────┐
       │        Client / Admin        │
       │   (REST consumers, UI, jobs) │
       └───────────────┬──────────────┘
                       │ HTTP
       ┌───────────────▼──────────────┐
       │        Controller Layer        │
       │  ProductController              │
       │  PricingRuleController (Admin)   │
       │  DynamicPriceController           │
       │  - Request validation (JSR-380)    │
       │  - DTO <-> mapping                  │
       └───────────────┬──────────────┘
                       │
       ┌───────────────▼──────────────┐
       │          Service Layer          │
       │  PricingEngineService            │
       │  ProductService                    │
       │  PricingRuleService (Admin CRUD)     │
       │  - @Transactional boundaries           │
       │  - Orchestrates strategy execution        │
       └───────┬───────────────┬────────┘
               │               │
┌──────────────▼───┐   ┌───────▼───────────────┐
│  Strategy Layer   │   │   Cache Layer (Spring  │
│  PricingStrategy  │   │   Cache / Redis)         │
│  interface        │   │  - Product base price      │
│   ├─ SurgePricingStrategy                          │
│   ├─ TimeBasedPricingStrategy                       │
│   └─ InventoryBasedPricingStrategy                    │
│  PricingStrategyFactory (resolves by RuleType)          │
└──────────────┬───┘   └───────────────────────┘
               │
       ┌───────▼──────────────┐
       │     Repository Layer   │
       │  Spring Data JPA          │
       │  ProductRepository          │
       │  PricingRuleRepository         │
       │  DynamicPriceRepository            │
       │  - Fetch joins / EntityGraph to avoid N+1 │
       └───────────────┬──────────────┘
                       │
       ┌───────────────▼──────────────┐
       │           Database              │
       │  Product | PricingRule | DynamicPrice │
       └─────────────────────────────────┘
```

### Layer Responsibilities

| Layer          | Responsibility                                                                                                                                                                                     |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Controller** | Exposes REST endpoints, validates input via `@Valid` + Hibernate Validator, translates exceptions to HTTP responses.                                                                               |
| **Service**    | Business orchestration, transaction boundaries (`@Transactional`), coordinates strategy execution and cache reads/writes.                                                                          |
| **Strategy**   | Each pricing rule type (Surge, Time-based, Inventory-based) implements a common `PricingStrategy` interface; a `PricingStrategyFactory` resolves the correct strategy(ies) per `PricingRule.type`. |
| **Cache**      | Spring Cache (`@Cacheable` / `@CacheEvict`) in front of `Product` base price and active `PricingRule` lookups, since these change far less often than price is requested.                          |
| **Repository** | Spring Data JPA repositories; uses `JOIN FETCH` / `@EntityGraph` to load `Product` + associated `PricingRule`s in a single query.                                                                  |
| **Database**   | Relational store for `Product`, `PricingRule`, `DynamicPrice`.                                                                                                                                     |

---

## 2. Domain Model Flow

### Entities & Relationship

```
Product (1) ────< PricingRule (many)      [rules scoped to a product, or global if product_id is null]
Product (1) ────< DynamicPrice (many)     [audit/history of computed prices]
```

- **Product**: `id, name, base_price` — the anchor entity; every price calculation starts here.
- **PricingRule**: `id, type, value, condition, product_id (nullable)` — configurable, admin-managed rule; `type` maps to a `PricingStrategy` (SURGE, TIME_BASED, INVENTORY_BASED); `condition` holds rule-specific criteria (e.g. demand threshold, time window, stock level) as JSON/text.
- **DynamicPrice**: `id, product_id, final_price, timestamp` — immutable snapshot written every time a price is (re)calculated; acts as both cache-of-record and audit trail.

### Request Flow: Calculating a Dynamic Price

```
1. Client calls GET /api/products/{id}/price
        │
        ▼
2. Controller validates path/query params
        │
        ▼
3. Service: PricingEngineService.getCurrentPrice(productId)
        │
        ├─ 3a. Cache lookup: "product::{id}" → base price (cache hit skips DB)
        │
        ├─ 3b. Cache lookup: "activeRules::{id}" → PricingRule list
        │        (fetched via single query with JOIN FETCH, avoiding N+1)
        │
        ▼
4. PricingStrategyFactory resolves ordered list of strategies
   matching the fetched PricingRules
        │
        ▼
5. Strategies applied sequentially/aggregated on base_price:
        base_price
          → SurgePricingStrategy.apply()        (demand multiplier)
          → TimeBasedPricingStrategy.apply()    (time-of-day/day-of-week factor)
          → InventoryBasedPricingStrategy.apply()(low-stock markup)
        │
        ▼
6. Conflict resolution: rules are ordered by priority/precedence;
   overlapping rules are combined per a defined strategy
   (e.g. multiplicative stacking, or "highest priority wins" — configurable)
        │
        ▼
7. Service persists result as new DynamicPrice row (transactional write)
        │
        ▼
8. Response DTO (final_price, breakdown, timestamp) returned to client
```

### Admin Rule-Update Flow

```
1. Admin calls POST/PUT /api/admin/pricing-rules
        │
        ▼
2. Controller validates payload (type, value, condition) via Hibernate Validator
        │
        ▼
3. PricingRuleService (transactional):
       - persists/updates PricingRule
       - evicts affected cache entries: @CacheEvict("activeRules::{productId}")
        │
        ▼
4. Subsequent price requests recompute using the updated rule set
   (no stale cache served after eviction)
```

---

## 3. Handling Edge Cases

| Edge Case                          | Handling Approach                                                                                                                                                                                        |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Conflicting rules**              | Each `PricingRule` carries a precedence/priority; `PricingStrategyFactory` applies a defined combination policy (e.g. multiplicative stacking vs. max-priority override) so results are deterministic.   |
| **Demand spikes**                  | `SurgePricingStrategy` reads a demand signal (e.g. request rate / order count in a rolling window) rather than static config, recalculated per request rather than cached long-term.                     |
| **Rule updates during processing** | Rule reads happen inside the same transaction as price calculation; cache eviction on rule update ensures in-flight requests either use the pre-update or post-update rule set consistently, never a mixed state. |
| **High concurrency**               | Optimistic locking (`@Version`) on `Product`/`PricingRule` to prevent lost updates; DB-level row locks avoided in the read path via caching; writes to `DynamicPrice` are append-only, so no contention on price history. |

---

## 4. Performance Notes

- **N+1 avoidance**: `PricingRuleRepository.findActiveRulesByProduct(productId)` uses `@EntityGraph` or `JOIN FETCH` to load `Product` and its `PricingRule`s in one round trip.
- **Caching**: `Product` base price and active `PricingRule`s are cached (`@Cacheable`); cache is evicted only on admin writes (`@CacheEvict`), keeping the read-heavy pricing path fast.
- **Low latency**: Strategy chain executes in-memory once rules/base price are fetched — no additional DB calls during calculation itself.
- **Write path**: `DynamicPrice` inserts are lightweight, append-only, and decoupled from the read/cache path so they don't block subsequent price lookups.
