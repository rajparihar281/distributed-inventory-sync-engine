# Distributed Multi-Channel Inventory Synchronization Engine

A high-throughput, concurrency-safe inventory synchronization and dynamic order fulfillment backend built with Java 21, Spring Boot 4, Spring Data JPA, MySQL 8, and Redis.

Designed to handle flash-sale scenarios with zero overselling, strict mathematical invariant enforcement, pluggable warehouse allocation strategies, an immutable append-only audit ledger, and a distributed Cache-Aside layer.

## Architecture Highlights

```
┌──────────────────────────┐
│   HTTP / REST Clients    │
└─────────────┬────────────┘
              │
              ▼
┌──────────────────────────┐
│    Spring Boot Engine    │
└──────┬────────────┬──────┘
       │            │
 Cache-Aside Reads  │  Pessimistic Writes
 (Sub-millisecond)  │  (SELECT ... FOR UPDATE)
       ▼            ▼
┌───────────────┐   ┌─────────────────┐
│ Redis Cache   │   │  MySQL 8 Engine │
│ (TTL + Evict) │   │ (ACID / InnoDB) │
└───────────────┘   └─────────────────┘
```

## Key Engineering Capabilities

### 1. Mathematical Domain Invariants — Zero Overselling Guarantee

The core domain model strictly enforces physical balance integrity. Any state transition violating this rule immediately throws a runtime exception and rolls back the transaction:

```
Physical Stock = Available Stock + Reserved Stock + Damaged Stock
```

### 2. High-Concurrency Pessimistic Row Locking

- Eliminates race conditions and phantom reads during concurrent checkout spikes.
- Uses explicit `SELECT ... FOR UPDATE` row locks on `(product_id, warehouse_id)`.
- Combined with Hibernate optimistic `@Version` checks for dual-layer safety.

### 3. Distributed Cache-Aside — Write-Around / Evict — with Redis

- **Reads:** Intercepted via `StringRedisTemplate` and Jackson 3 serialization using the key pattern `inventory:{productId}:{warehouseId}`. Serves read requests with ultra-low latency (< 2 ms).
- **Mutations:** Immediate, deterministic cache invalidation upon any reservation, inward procurement, or dispatch to eliminate stale-read overselling.

### 4. Pluggable Warehouse Allocation Engine

Dynamic order routing using the Strategy Pattern and Factory Pattern:

- **SingleWarehouseStrategy** — Prioritizes fulfillment from a single warehouse to minimize split-shipment overhead.
- **GreedySplitStrategy** — Iteratively aggregates stock across multiple regional warehouses when order quantity exceeds single-warehouse capacity.

### 5. Immutable Append-Only Audit Ledger

Every inventory mutation logs directional deltas:

```
±ΔP, ±ΔA, ±ΔR, ±ΔD
```

These changes are stored in `inventory_audit_log` with external reference IDs such as `PO`, `ORDER`, and `DMG`, enabling reconciliation and forensic auditing.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.1.1 |
| Persistence | Spring Data JPA / Hibernate ORM 7 |
| Relational Database | MySQL 8.0 (InnoDB Engine) |
| Distributed Caching | Redis 7 (Alpine) / Lettuce Client |
| Connection Pooling | HikariCP |
| Testing | JUnit 5, AssertJ, Spring Boot Test |
| Build & Tooling | Maven, Docker Desktop |

## Database Schema Design

```sql
-- Inventory Master Table
CREATE TABLE inventory
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT NOT NULL,
    warehouse_id  BIGINT NOT NULL,
    physical_qty  INT    NOT NULL DEFAULT 0,
    available_qty INT    NOT NULL DEFAULT 0,
    reserved_qty  INT    NOT NULL DEFAULT 0,
    damaged_qty   INT    NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)     DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_product_warehouse UNIQUE (product_id, warehouse_id)
);

-- Directional Immutable Audit Ledger
CREATE TABLE inventory_audit_log
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id    BIGINT      NOT NULL,
    change_type     VARCHAR(50) NOT NULL,
    delta_physical  INT         NOT NULL,
    delta_available INT         NOT NULL,
    delta_reserved  INT         NOT NULL,
    delta_damaged   INT         NOT NULL,
    reference_id    VARCHAR(100),
    reason          VARCHAR(255),
    created_at      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (inventory_id) REFERENCES inventory (id)
);
```

## Quick Start Guide

### Prerequisites

- JDK 21+
- Maven 3.9+
- MySQL 8.0
- Docker Desktop — for Redis

### Step 1: Clone Repository

```bash
git clone https://github.com/<YOUR-USERNAME>/distributed-inventory-sync-engine.git
cd distributed-inventory-sync-engine
```

### Step 2: Spin Up Redis

```bash
docker run -d --name redis-sync -p 6379:6379 redis:alpine
```

### Step 3: Configure Database

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/inventory_sync_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### Step 4: Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

## Core API Reference

### 1. Inward Stock Procurement

Increases physical and available stock simultaneously.

**Endpoint:** `POST /api/v1/inventory/inward`

```json
{
  "productId": 1,
  "warehouseId": 1,
  "quantity": 50,
  "purchaseOrderRef": "PO-2026-AUG-001"
}
```

### 2. Mark Damaged — Quarantine

Deducts from available stock and places units in quarantine without altering the physical count.

**Endpoint:** `POST /api/v1/inventory/mark-damaged`

```json
{
  "productId": 1,
  "warehouseId": 1,
  "quantity": 2,
  "reason": "Water leak damage in aisle 4B"
}
```

### 3. Fetch Inventory Balance — Cached

Queries Redis first. Falls back to MySQL on cache miss and populates Redis with a 300-second TTL.

**Endpoint:** `GET /api/v1/inventory?productId=1&warehouseId=1`

**Sample Response:**

```json
{
  "inventoryId": 1,
  "productId": 1,
  "productSku": "SSD-1TB-NVME",
  "productName": "Samsung 980 Pro 1TB NVMe SSD",
  "warehouseId": 1,
  "warehouseCode": "WH-MUM-01",
  "physicalQty": 60,
  "availableQty": 51,
  "reservedQty": 7,
  "damagedQty": 2,
  "version": 6,
  "updatedAt": "2026-08-25T19:05:00"
}
```

### 4. Create & Route Order

Reserves stock dynamically across warehouses according to the configured routing policy.

**Endpoint:** `POST /api/v1/orders`

```json
{
  "orderNumber": "ORD-2026-9901",
  "customerEmail": "alex@example.com",
  "items": [
    {
      "productId": 1,
      "quantity": 5
    }
  ]
}
```

## Integration & Concurrency Testing

The test suite includes multithreaded concurrency simulations using `ExecutorService` and `CountDownLatch`.

These tests verify that concurrent threads attempting to reserve inventory beyond available quantities fail gracefully with zero over-allocation.

Run the test suite:

```bash
mvn test
```

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Push README to GitHub

Run these commands in your IntelliJ terminal:

```bash
git add README.md
git commit -m "docs: add comprehensive production-grade README"
git push origin main
```