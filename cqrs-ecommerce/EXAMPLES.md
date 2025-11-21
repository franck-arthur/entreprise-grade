# Exemples et Scénarios CQRS/Event Sourcing

## 1. Scénario Complet : Création et Confirmation d'une Commande

### Étape 1 : Créer une commande

```bash
# COMMAND: Création d'une commande
POST /api/orders
Content-Type: application/json

{
  "customerId": "CUST-001",
  "customerEmail": "jean.dupont@email.com"
}

# RESPONSE
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Order created successfully"
}
```

**Événement généré:**
```json
{
  "eventType": "OrderCreatedEvent",
  "aggregateId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "CUST-001",
    "customerEmail": "jean.dupont@email.com",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### Étape 2 : Ajouter des articles

```bash
# COMMAND: Ajouter un article
POST /api/orders/550e8400-e29b-41d4-a716-446655440000/items
Content-Type: application/json

{
  "productId": "PROD-001",
  "productName": "MacBook Pro 14\"",
  "quantity": 1,
  "unitPrice": 2499.00
}

# Ajouter un deuxième article
POST /api/orders/550e8400-e29b-41d4-a716-446655440000/items
{
  "productId": "PROD-002",
  "productName": "Magic Mouse",
  "quantity": 2,
  "unitPrice": 99.00
}
```

**Événements générés:**
```json
[
  {
    "eventType": "ItemAddedToOrderEvent",
    "sequenceNumber": 2,
    "payload": {
      "orderId": "550e8400-e29b-41d4-a716-446655440000",
      "productId": "PROD-001",
      "productName": "MacBook Pro 14\"",
      "quantity": 1,
      "unitPrice": 2499.00,
      "lineTotal": 2499.00
    }
  },
  {
    "eventType": "ItemAddedToOrderEvent",
    "sequenceNumber": 3,
    "payload": {
      "orderId": "550e8400-e29b-41d4-a716-446655440000",
      "productId": "PROD-002",
      "productName": "Magic Mouse",
      "quantity": 2,
      "unitPrice": 99.00,
      "lineTotal": 198.00
    }
  }
]
```

### Étape 3 : Lire la projection (Read Model)

```bash
# QUERY: Récupérer la commande
GET /api/orders/550e8400-e29b-41d4-a716-446655440000

# RESPONSE (Vue matérialisée)
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "customerEmail": "jean.dupont@email.com",
  "status": "CREATED",
  "items": [
    {
      "id": 1,
      "productId": "PROD-001",
      "productName": "MacBook Pro 14\"",
      "quantity": 1,
      "unitPrice": 2499.00,
      "lineTotal": 2499.00
    },
    {
      "id": 2,
      "productId": "PROD-002",
      "productName": "Magic Mouse",
      "quantity": 2,
      "unitPrice": 99.00,
      "lineTotal": 198.00
    }
  ],
  "totalAmount": 2697.00,
  "createdAt": "2024-01-15T10:30:00Z",
  "confirmedAt": null
}
```

### Étape 4 : Confirmer la commande

```bash
# COMMAND: Confirmer la commande
POST /api/orders/550e8400-e29b-41d4-a716-446655440000/confirm
Content-Type: application/json

{
  "shippingAddress": "123 Rue de Paris, 75001 Paris, France",
  "paymentMethod": "CARD"
}
```

**Événement généré:**
```json
{
  "eventType": "OrderConfirmedEvent",
  "sequenceNumber": 4,
  "payload": {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "shippingAddress": "123 Rue de Paris, 75001 Paris, France",
    "paymentMethod": "CARD",
    "totalAmount": 2697.00,
    "confirmedAt": "2024-01-15T10:45:00Z"
  }
}
```

---

## 2. Event Store - Exemple de Données

L'Event Store contient l'historique complet de tous les événements:

```sql
-- Table: domain_event_entry (Axon Framework)
SELECT aggregate_identifier, sequence_number, type, payload
FROM domain_event_entry
WHERE aggregate_identifier = '550e8400-e29b-41d4-a716-446655440000'
ORDER BY sequence_number;
```

| sequence | type | payload (JSON) |
|----------|------|----------------|
| 0 | OrderCreatedEvent | `{"orderId":"550e...","customerId":"CUST-001"...}` |
| 1 | ItemAddedToOrderEvent | `{"productId":"PROD-001","quantity":1...}` |
| 2 | ItemAddedToOrderEvent | `{"productId":"PROD-002","quantity":2...}` |
| 3 | OrderConfirmedEvent | `{"shippingAddress":"123 Rue..."...}` |

---

## 3. Jeu de Données - Projections (Read Model)

### Table: order_view

| order_id | customer_id | status | total_amount | created_at |
|----------|-------------|--------|--------------|------------|
| 550e8400-... | CUST-001 | CONFIRMED | 2697.00 | 2024-01-15 10:30 |
| 660f9500-... | CUST-002 | CREATED | 149.99 | 2024-01-15 11:00 |
| 770a0600-... | CUST-001 | CONFIRMED | 599.00 | 2024-01-14 09:15 |

### Table: order_item_view

| id | order_id | product_id | product_name | quantity | unit_price | line_total |
|----|----------|------------|--------------|----------|------------|------------|
| 1 | 550e8400-... | PROD-001 | MacBook Pro 14" | 1 | 2499.00 | 2499.00 |
| 2 | 550e8400-... | PROD-002 | Magic Mouse | 2 | 99.00 | 198.00 |
| 3 | 660f9500-... | PROD-003 | AirPods Pro | 1 | 149.99 | 149.99 |

---

## 4. Reconstruction de l'Aggregate (Event Sourcing)

Quand l'Aggregate est chargé, Axon rejoue tous les événements:

```java
// Chargement de l'Aggregate "550e8400-..."

// Event 0: OrderCreatedEvent
aggregate.orderId = "550e8400-..."
aggregate.customerId = "CUST-001"
aggregate.status = CREATED
aggregate.items = {}
aggregate.totalAmount = 0

// Event 1: ItemAddedToOrderEvent (PROD-001)
aggregate.items["PROD-001"] = OrderItem("PROD-001", "MacBook", 1, 2499)
aggregate.totalAmount = 2499.00

// Event 2: ItemAddedToOrderEvent (PROD-002)
aggregate.items["PROD-002"] = OrderItem("PROD-002", "Mouse", 2, 99)
aggregate.totalAmount = 2697.00

// Event 3: OrderConfirmedEvent
aggregate.status = CONFIRMED

// État final reconstruit, prêt à recevoir de nouvelles commandes
```

---

## 5. Flux Angular Complet

```typescript
// 1. L'utilisateur crée une commande
this.orderCommandService.createOrder({
  customerId: 'CUST-001',
  customerEmail: 'jean@email.com'
}).subscribe(response => {
  console.log('Order created:', response.id);
  this.orderId = response.id;
});

// 2. L'utilisateur ajoute un article
this.orderCommandService.addItem(this.orderId, {
  productId: 'PROD-001',
  productName: 'MacBook Pro',
  quantity: 1,
  unitPrice: 2499
}).subscribe(() => {
  // 3. Rafraîchir la vue (lecture de la projection)
  this.refreshOrder();
});

// 4. Lecture de la projection mise à jour
refreshOrder(): void {
  this.orderQueryService.getOrderById(this.orderId)
    .subscribe(order => {
      this.order = order; // Vue matérialisée à jour
    });
}
```

---

## 6. Avantages de cette Architecture

| Aspect | Bénéfice |
|--------|----------|
| **Audit complet** | Tous les changements sont tracés via les événements |
| **Scalabilité** | Read et Write peuvent être scalés indépendamment |
| **Performance** | Lectures optimisées via les projections dénormalisées |
| **Évolutivité** | Nouvelles projections sans modifier le domain model |
| **Debugging** | Possibilité de "rejouer" l'historique pour comprendre les bugs |
| **Event replay** | Reconstruction de l'état à n'importe quel moment |
