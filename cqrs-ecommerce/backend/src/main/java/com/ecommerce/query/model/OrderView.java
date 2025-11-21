package com.ecommerce.query.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Vue matérialisée d'une commande (READ MODEL).
 * Cette entité est optimisée pour la lecture et dénormalisée.
 */
@Entity
@Table(name = "order_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderView {

    @Id
    private String orderId;

    private String customerId;
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemView> items = new ArrayList<>();

    private BigDecimal totalAmount;

    private String shippingAddress;
    private String paymentMethod;

    private Instant createdAt;
    private Instant confirmedAt;

    @Version
    private Long version;

    public enum OrderStatus {
        CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    public void addItem(OrderItemView item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(String productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }
}
