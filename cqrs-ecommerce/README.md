# Architecture CQRS / Event Sourcing - E-Commerce

## Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ANGULAR FRONTEND                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   Order Module  │  │  Product Module │  │   Cart Module   │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                    │                        │
│  ┌────────▼────────────────────▼────────────────────▼────────┐              │
│  │                    API Services                            │              │
│  │  CommandService (POST)          QueryService (GET)         │              │
│  └────────┬─────────────────────────────────┬────────────────┘              │
└───────────┼─────────────────────────────────┼───────────────────────────────┘
            │                                 │
            ▼                                 ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                           SPRING BOOT BACKEND                                  │
│                                                                                │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐          │
│  │      WRITE SIDE (Commands)  │    │      READ SIDE (Queries)    │          │
│  │                             │    │                             │          │
│  │  ┌───────────────────────┐  │    │  ┌───────────────────────┐  │          │
│  │  │   Command Controller  │  │    │  │   Query Controller    │  │          │
│  │  └───────────┬───────────┘  │    │  └───────────┬───────────┘  │          │
│  │              │              │    │              │              │          │
│  │  ┌───────────▼───────────┐  │    │  ┌───────────▼───────────┐  │          │
│  │  │   Command Gateway     │  │    │  │   Query Gateway       │  │          │
│  │  └───────────┬───────────┘  │    │  └───────────┬───────────┘  │          │
│  │              │              │    │              │              │          │
│  │  ┌───────────▼───────────┐  │    │  ┌───────────▼───────────┐  │          │
│  │  │   Command Handler     │  │    │  │   Query Handler       │  │          │
│  │  │   (in Aggregate)      │  │    │  └───────────┬───────────┘  │          │
│  │  └───────────┬───────────┘  │    │              │              │          │
│  │              │              │    │  ┌───────────▼───────────┐  │          │
│  │  ┌───────────▼───────────┐  │    │  │   Projection DB       │  │          │
│  │  │   ORDER AGGREGATE     │  │    │  │   (Read Models)       │  │          │
│  │  │   - orderId           │  │    │  └───────────────────────┘  │          │
│  │  │   - items[]           │  │    │                             │          │
│  │  │   - status            │  │    └─────────────────────────────┘          │
│  │  │   - totalAmount       │  │                                             │
│  │  └───────────┬───────────┘  │                                             │
│  │              │              │                                             │
│  │  ┌───────────▼───────────┐  │                                             │
│  │  │   Event Store         │◄─┼─────────────────────────────────┐           │
│  │  │   (Axon Server)       │  │                                 │           │
│  │  └───────────┬───────────┘  │                                 │           │
│  │              │              │    ┌─────────────────────────────┴─────┐    │
│  └──────────────┼──────────────┘    │       EVENT HANDLERS              │    │
│                 │                   │  ┌─────────────────────────────┐  │    │
│                 └───────────────────┼─►│  OrderProjection Handler    │  │    │
│                                     │  │  (Updates Read DB)          │  │    │
│                                     │  └─────────────────────────────┘  │    │
│                                     └───────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Diagramme de Séquence - Flux Complet

```
┌────────┐     ┌──────────┐     ┌─────────┐     ┌───────────┐     ┌────────────┐     ┌──────────┐
│ Angular│     │Controller│     │ Gateway │     │ Aggregate │     │ EventStore │     │Projection│
└───┬────┘     └────┬─────┘     └────┬────┘     └─────┬─────┘     └──────┬─────┘     └────┬─────┘
    │               │                │               │                   │                │
    │ POST /orders  │                │               │                   │                │
    │──────────────►│                │               │                   │                │
    │               │                │               │                   │                │
    │               │ send(command)  │               │                   │                │
    │               │───────────────►│               │                   │                │
    │               │                │               │                   │                │
    │               │                │ @CommandHandler                   │                │
    │               │                │──────────────►│                   │                │
    │               │                │               │                   │                │
    │               │                │               │ apply(event)      │                │
    │               │                │               │──────────────────►│                │
    │               │                │               │                   │                │
    │               │                │               │                   │ @EventHandler  │
    │               │                │               │                   │───────────────►│
    │               │                │               │                   │                │
    │               │                │               │                   │   Update       │
    │               │                │               │                   │   Read DB      │
    │               │                │               │                   │                │
    │  202 Accepted │                │               │                   │                │
    │◄──────────────│                │               │                   │                │
    │               │                │               │                   │                │
    │ GET /orders   │                │               │                   │                │
    │──────────────►│                │               │                   │                │
    │               │                │               │                   │  Query         │
    │               │───────────────────────────────────────────────────────────────────►│
    │               │                │               │                   │                │
    │  Order View   │                │               │                   │                │
    │◄──────────────│                │               │                   │                │
    │               │                │               │                   │                │
```

## Structure du Projet

```
cqrs-ecommerce/
├── backend/
│   └── src/main/java/com/ecommerce/
│       ├── command/
│       │   ├── aggregate/
│       │   │   └── OrderAggregate.java
│       │   ├── handler/
│       │   │   └── (handlers externes si nécessaire)
│       │   └── model/
│       │       ├── CreateOrderCommand.java
│       │       ├── AddItemToOrderCommand.java
│       │       ├── RemoveItemFromOrderCommand.java
│       │       └── ConfirmOrderCommand.java
│       ├── query/
│       │   ├── projection/
│       │   │   └── OrderProjection.java
│       │   ├── handler/
│       │   │   └── OrderQueryHandler.java
│       │   ├── model/
│       │   │   ├── OrderView.java
│       │   │   └── OrderItemView.java
│       │   └── repository/
│       │       └── OrderViewRepository.java
│       ├── event/
│       │   ├── OrderCreatedEvent.java
│       │   ├── ItemAddedToOrderEvent.java
│       │   ├── ItemRemovedFromOrderEvent.java
│       │   └── OrderConfirmedEvent.java
│       ├── api/
│       │   ├── OrderCommandController.java
│       │   └── OrderQueryController.java
│       └── config/
│           └── AxonConfig.java
├── frontend/
│   └── src/app/
│       ├── core/
│       │   ├── services/
│       │   │   ├── order-command.service.ts
│       │   │   └── order-query.service.ts
│       │   └── models/
│       │       ├── order.model.ts
│       │       └── commands.model.ts
│       └── features/
│           └── orders/
│               ├── order-list/
│               └── order-detail/
└── README.md
```
