# saga-orchestration-demo

Benchmarks two saga-orchestration implementations against the same
order-fulfillment scenario to show, with real numbers, what a purpose-built
orchestration engine (Temporal) buys you over a hand-rolled orchestrator:
crash-recovery time, compensation correctness under concurrent failures, and
code/complexity delta.

Same pattern as `racing-conditions-demo`: implement N interchangeable
strategies against an identical workload, benchmark them, produce a
comparison table backed by real data.

## Stack

- **Language**: Java 21, Spring Boot 3
- **Build**: Maven (multi-module)
- **Database**: PostgreSQL 16 — one instance, one schema/DB per service
- **Messaging** (Tier 1): Spring Kafka or Spring AMQP for orchestrator ↔
  service command/reply
- **Orchestration engine** (Tier 2): Temporal Java SDK +
  `temporal-spring-boot-starter`
- **Load generation**: virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`)
- **Runtime**: Docker Compose; benchmark/chaos services gated behind
  `--profile bench`

## Scenario: order fulfillment saga

Four downstream services, each its own Spring Boot app with its own Postgres
database:

1. **order-service** — creates order (`PENDING` → `CONFIRMED`/`CANCELLED`)
2. **inventory-service** — reserves stock (`RESERVED` → `RELEASED`)
3. **payment-service** — charges customer (`CHARGED` → `REFUNDED`)
4. **shipping-service** — books carrier (`CONFIRMED` → `CANCELLED`)

Happy path: `CreateOrder → ReserveInventory → ChargePayment → ConfirmShipment
→ CompleteOrder`

Compensations run in reverse from the point of failure:
`RefundPayment → ReleaseInventory → CancelOrder`.

Every service exposes a "do" and "undo" REST endpoint, both idempotent via a
client-supplied `idempotencyKey` — a retried call returns the existing
record instead of creating a duplicate.

## Tiers

- **Tier 1 — `orchestrator-custom`**: a central orchestrator with an explicit
  state machine, a Postgres event log as the crash-recovery source of truth,
  and async command/reply to the services over a message broker.
- **Tier 2 — `orchestrator-temporal`**: the same saga expressed as a Temporal
  workflow, with each step as an activity and compensations registered as
  the workflow executes. Temporal's durable execution model owns crash
  recovery — no hand-written event log.

Both tiers drive the same four downstream services where possible.

## Repo structure

```
saga-orchestration-demo/
├── docker-compose.yml          # Postgres + the 4 services
├── docker/postgres-init/       # creates order_db/inventory_db/payment_db/shipping_db
├── order-service/
├── inventory-service/
├── payment-service/
├── shipping-service/
├── orchestrator-custom/        # Tier 1 ✅
├── orchestrator-temporal/      # Tier 2 ✅
├── load-generator/             # virtual-thread saga driver, --profile bench ✅
├── chaos/                      # failure/crash injection, --profile bench (not yet built)
└── results/                    # benchmark CSV output
```

## Status

Both orchestrator tiers are built and verified end-to-end (happy path +
compensation, confirmed against each downstream service's Postgres state).
The load generator drives either tier through the identical `/sagas` REST
contract and reports latency percentiles + outcome counts. Remaining work
is the chaos injector and the final benchmark run.

## Load generator

```bash
docker compose --profile bench run --rm \
  -e LOADGEN_TIER=custom \
  -e LOADGEN_BASE_URL=http://orchestrator-custom:8090 \
  -e LOADGEN_REQUESTS=200 \
  -e LOADGEN_CONCURRENCY=20 \
  -e LOADGEN_FAILURE_RATE=0.2 \
  -e LOADGEN_RUN_ID=run1 \
  load-generator
```

Swap `LOADGEN_TIER=temporal` / `LOADGEN_BASE_URL=http://orchestrator-temporal:8091`
to drive Tier 2 instead. Results land in `results/<tier>-<run-id>.csv`; failure
injection is randomly assigned per saga (at the given rate) to one of
CREATE_ORDER/RESERVE_INVENTORY/CHARGE_PAYMENT/CONFIRM_SHIPMENT, the only
steps that accept a `simulateFailure` flag downstream.

## Build sequence

1. ~~Scaffold the four downstream services~~ ✅
2. ~~Build Tier 1 custom orchestrator~~ ✅
3. ~~Verify Tier 1 end-to-end~~ ✅
4. ~~Build Tier 2 Temporal workflow + activities~~ ✅
5. ~~Verify Tier 2 end-to-end~~ ✅
6. ~~Build load generator (virtual threads)~~ ✅ — chaos injector still open
7. Run identical benchmark suite against both tiers, produce comparison
   table/CSV.

## Running locally

```bash
docker compose up -d postgres
# each service also runs standalone via its Spring Boot main class,
# pointed at localhost:5432/<service>_db (user/pass: saga/saga)
```
