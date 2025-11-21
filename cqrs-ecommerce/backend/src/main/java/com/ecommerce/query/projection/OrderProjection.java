package com.ecommerce.query.projection;

import com.ecommerce.event.*;
import com.ecommerce.query.model.OrderItemView;
import com.ecommerce.query.model.OrderView;
import com.ecommerce.query.repository.OrderViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * PROJECTION - Met à jour la base de lecture (Read Model).
 *
 * Les projections écoutent les événements et mettent à jour
 * les vues matérialisées pour optimiser les requêtes de lecture.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProjection {

    private final OrderViewRepository orderViewRepository;

    @EventHandler
    public void on(OrderCreatedEvent event) {
        log.info("Projecting OrderCreatedEvent for order: {}", event.getOrderId());

        OrderView orderView = OrderView.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .customerEmail(event.getCustomerEmail())
                .status(OrderView.OrderStatus.CREATED)
                .items(new ArrayList<>())
                .totalAmount(java.math.BigDecimal.ZERO)
                .createdAt(event.getCreatedAt())
                .build();

        orderViewRepository.save(orderView);
    }

    @EventHandler
    public void on(ItemAddedToOrderEvent event) {
        log.info("Projecting ItemAddedToOrderEvent for order: {}, product: {}",
                event.getOrderId(), event.getProductId());

        OrderView order = orderViewRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + event.getOrderId()));

        // Vérifier si l'article existe déjà
        OrderItemView existingItem = order.getItems().stream()
                .filter(item -> item.getProductId().equals(event.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Mettre à jour la quantité existante
            existingItem.setQuantity(existingItem.getQuantity() + event.getQuantity());
            existingItem.setLineTotal(existingItem.getLineTotal().add(event.getLineTotal()));
        } else {
            // Ajouter un nouvel article
            OrderItemView newItem = OrderItemView.builder()
                    .productId(event.getProductId())
                    .productName(event.getProductName())
                    .quantity(event.getQuantity())
                    .unitPrice(event.getUnitPrice())
                    .lineTotal(event.getLineTotal())
                    .build();
            order.addItem(newItem);
        }

        order.setTotalAmount(order.getTotalAmount().add(event.getLineTotal()));
        orderViewRepository.save(order);
    }

    @EventHandler
    public void on(ItemRemovedFromOrderEvent event) {
        log.info("Projecting ItemRemovedFromOrderEvent for order: {}, product: {}",
                event.getOrderId(), event.getProductId());

        OrderView order = orderViewRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + event.getOrderId()));

        order.removeItem(event.getProductId());
        order.setTotalAmount(order.getTotalAmount().subtract(event.getRemovedAmount()));
        orderViewRepository.save(order);
    }

    @EventHandler
    public void on(OrderConfirmedEvent event) {
        log.info("Projecting OrderConfirmedEvent for order: {}", event.getOrderId());

        OrderView order = orderViewRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + event.getOrderId()));

        order.setStatus(OrderView.OrderStatus.CONFIRMED);
        order.setShippingAddress(event.getShippingAddress());
        order.setPaymentMethod(event.getPaymentMethod());
        order.setConfirmedAt(event.getConfirmedAt());
        orderViewRepository.save(order);
    }

    /**
     * Handler appelé lors d'un reset de la projection.
     * Permet de reconstruire la vue depuis zéro.
     */
    @ResetHandler
    public void reset() {
        log.warn("Resetting OrderProjection - clearing all order views");
        orderViewRepository.deleteAll();
    }
}
