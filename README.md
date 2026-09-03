# phase0-ecommerce

Phase 0 scaffold: two entities, plain CRUD, one foreign-key relation. No auth, no
capabilities, no search — deliberately.

## Stack

| | |
|---|---|
| Java | 21 (toolchain target) |
| Spring Boot | 3.5.6 |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 in-memory (`MODE=PostgreSQL`) |
| Build | Maven |

## Run

```bash
mvn spring-boot:run          # http://localhost:8080
mvn test
mvn clean package            # executable jar in target/
```

## Domain

```
Product 1 ──── * Order
```

**Product** — `id`, `name`, `sku` (unique), `price` (non-negative, 2 dp),
`stock` (non-negative), `status` (`ACTIVE` | `INACTIVE`)

**Order** — `id`, `customerName`, `productId` (FK → Product, required),
`quantity` (>= 1), `unitPrice` (non-negative, 2 dp), `status` (`PENDING` | `COMPLETED` | `CANCELLED`)

The FK is mapped as a lazy `@ManyToOne` on `Order`; the wire format exposes it as a
flat `productId` so responses never leak the entity graph.

## API

Both resources expose the same five operations.

| Method | Path | Success |
|---|---|---|
| `POST` | `/api/{products,orders}` | `201` + `Location` |
| `GET` | `/api/{products,orders}/{id}` | `200` |
| `GET` | `/api/{products,orders}` | `200` (unpaged list) |
| `PUT` | `/api/{products,orders}/{id}` | `200` (full replace) |
| `DELETE` | `/api/{products,orders}/{id}` | `204` |

### Example

```bash
PRODUCT=$(curl -s -X POST localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Wireless Mouse","sku":"SKU-0001","price":29.99,"stock":100,"status":"ACTIVE"}')

ID=$(echo "$PRODUCT" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

curl -s -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d "{\"customerName\":\"Ada Lovelace\",\"productId\":\"$ID\",\"quantity\":2,\"unitPrice\":29.99,\"status\":\"PENDING\"}"
```

## Errors

Every handled failure returns the same body, so clients parse one shape:

```json
{
  "timestamp": "2026-09-03T07:39:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "sku": "sku is required" }
}
```

| Status | Cause |
|---|---|
| `400` | bean-validation failure (`fieldErrors` populated), or malformed JSON / unknown enum value |
| `404` | unknown id — including an `Order` pointing at a non-existent `Product` |
| `409` | duplicate `sku`, or deleting a `Product` that still has orders |

That last case is guarded in the service rather than left to the DB constraint, so it
surfaces as a `409` instead of a `500`.

## Layout

```
com.example.ecommerce
├── EcommerceApplication.java
├── common/          ApiError, GlobalExceptionHandler, 3 exception types
├── product/         entity, enum, repository, service, controller, dto/
└── order/           entity, enum, repository, service, controller, dto/
```

Each slice is self-contained; `OrderService` resolves the FK through
`ProductService.findOrThrow`, which is the single place the `Product` 404 is raised.

## Tests

- `EcommerceApplicationTests` — context loads with both slices wired
- `ProductServiceTest`, `OrderServiceTest` — unit tests over mocked repositories
- `ProductControllerIntegrationTest`, `OrderControllerIntegrationTest` — `MockMvc`
  against real H2: full CRUD lifecycle per resource, plus the 400/404/409 paths
