package com.ecommerce.command.aggregate;

import com.ecommerce.command.model.*;
import com.ecommerce.event.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ORDER AGGREGATE - Le coeur du domain model pour les commandes.
 *
 * Responsabilités:
 * - Valider les commandes (commands)
 * - Appliquer les règles métier
 * - Émettre les événements (events)
 * - Reconstruire l'état à partir des événements (event sourcing)
 */
@Aggregate
@NoArgsConstructor
@Slf4j
public class OrderAggregate {

    @AggregateIdentifier
    private String orderId;

    private String customerId;
    private String customerEmail;
    private OrderStatus status;
    private Map<String, OrderItem> items;
    private BigDecimal totalAmount;
    private Instant createdAt;

    // ==================== COMMAND HANDLERS ====================

    /**
     * Handler pour créer une nouvelle commande.
     * C'est un constructeur annoté qui crée l'agrégat.
     */
    @CommandHandler
    public OrderAggregate(CreateOrderCommand command) {
        log.info("Handling CreateOrderCommand for orderId: {}", command.getOrderId());

        // Validation
        if (command.getCustomerId() == null || command.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        // Émettre l'événement - l'état sera mis à jour par l'EventSourcingHandler
        AggregateLifecycle.apply(OrderCreatedEvent.builder()
                .orderId(command.getOrderId())
                .customerId(command.getCustomerId())
                .customerEmail(command.getCustomerEmail())
                .createdAt(Instant.now())
                .build());
    }

    /**
     * Handler pour ajouter un article à la commande.
     */
    @CommandHandler
    public void handle(AddItemToOrderCommand command) {
        log.info("Handling AddItemToOrderCommand for orderId: {}, productId: {}",
                command.getOrderId(), command.getProductId());

        // Validation des règles métier
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot add items to a confirmed order");
        }
        if (command.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }

        BigDecimal lineTotal = command.getUnitPrice()
                .multiply(BigDecimal.valueOf(command.getQuantity()));

        AggregateLifecycle.apply(ItemAddedToOrderEvent.builder()
                .orderId(command.getOrderId())
                .productId(command.getProductId())
                .productName(command.getProductName())
                .quantity(command.getQuantity())
                .unitPrice(command.getUnitPrice())
                .lineTotal(lineTotal)
                .build());
    }

    /**
     * Handler pour supprimer un article de la commande.
     */
    @CommandHandler
    public void handle(RemoveItemFromOrderCommand command) {
        log.info("Handling RemoveItemFromOrderCommand for orderId: {}, productId: {}",
                command.getOrderId(), command.getProductId());

        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot remove items from a confirmed order");
        }
        if (!items.containsKey(command.getProductId())) {
            throw new IllegalArgumentException("Product not found in order");
        }

        OrderItem item = items.get(command.getProductId());
        BigDecimal removedAmount = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        AggregateLifecycle.apply(ItemRemovedFromOrderEvent.builder()
                .orderId(command.getOrderId())
                .productId(command.getProductId())
                .removedAmount(removedAmount)
                .build());
    }

    /**
     * Handler pour confirmer la commande.
     */
    @CommandHandler
    public void handle(ConfirmOrderCommand command) {
        log.info("Handling ConfirmOrderCommand for orderId: {}", command.getOrderId());

        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order is already confirmed");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty order");
        }
        if (command.getShippingAddress() == null || command.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        AggregateLifecycle.apply(OrderConfirmedEvent.builder()
                .orderId(command.getOrderId())
                .shippingAddress(command.getShippingAddress())
                .paymentMethod(command.getPaymentMethod())
                .totalAmount(totalAmount)
                .confirmedAt(Instant.now())
                .build());
    }

    // ==================== EVENT SOURCING HANDLERS ====================
    // Ces handlers reconstruisent l'état de l'agrégat à partir des événements

    @EventSourcingHandler
    public void on(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.customerEmail = event.getCustomerEmail();
        this.status = OrderStatus.CREATED;
        this.items = new HashMap<>();
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = event.getCreatedAt();

        log.debug("Order {} created with status {}", orderId, status);
    }

    @EventSourcingHandler
    public void on(ItemAddedToOrderEvent event) {
        OrderItem item = new OrderItem(
                event.getProductId(),
                event.getProductName(),
                event.getQuantity(),
                event.getUnitPrice()
        );

        // Si l'article existe déjà, on cumule les quantités
        if (items.containsKey(event.getProductId())) {
            OrderItem existing = items.get(event.getProductId());
            item = new OrderItem(
                    event.getProductId(),
                    event.getProductName(),
                    existing.getQuantity() + event.getQuantity(),
                    event.getUnitPrice()
            );
        }

        items.put(event.getProductId(), item);
        totalAmount = totalAmount.add(event.getLineTotal());

        log.debug("Item {} added to order {}. New total: {}",
                event.getProductId(), orderId, totalAmount);
    }

    @EventSourcingHandler
    public void on(ItemRemovedFromOrderEvent event) {
        items.remove(event.getProductId());
        totalAmount = totalAmount.subtract(event.getRemovedAmount());

        log.debug("Item {} removed from order {}. New total: {}",
                event.getProductId(), orderId, totalAmount);
    }

    @EventSourcingHandler
    public void on(OrderConfirmedEvent event) {
        this.status = OrderStatus.CONFIRMED;

        log.debug("Order {} confirmed", orderId);
    }

    // ==================== INNER CLASSES ====================

    public enum OrderStatus {
        CREATED,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    public record OrderItem(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
