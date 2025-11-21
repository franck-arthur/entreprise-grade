package com.ecommerce.api;

import com.ecommerce.query.handler.OrderQueryHandler.*;
import com.ecommerce.query.model.OrderView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * QUERY CONTROLLER - Expose les endpoints de lecture (READ SIDE).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderQueryController {

    private final QueryGateway queryGateway;

    /**
     * GET /api/orders - Récupérer toutes les commandes
     */
    @GetMapping
    public CompletableFuture<List<OrderView>> getAllOrders() {
        log.info("Fetching all orders");
        return queryGateway.query(
                new FindAllOrdersQuery(),
                ResponseTypes.multipleInstancesOf(OrderView.class)
        );
    }

    /**
     * GET /api/orders/{orderId} - Récupérer une commande par ID
     */
    @GetMapping("/{orderId}")
    public CompletableFuture<ResponseEntity<OrderView>> getOrderById(
            @PathVariable String orderId) {

        log.info("Fetching order: {}", orderId);
        return queryGateway.query(
                        new FindOrderByIdQuery(orderId),
                        ResponseTypes.instanceOf(OrderView.class))
                .thenApply(order -> order != null
                        ? ResponseEntity.ok(order)
                        : ResponseEntity.notFound().build());
    }

    /**
     * GET /api/orders/customer/{customerId} - Commandes par client
     */
    @GetMapping("/customer/{customerId}")
    public CompletableFuture<List<OrderView>> getOrdersByCustomer(
            @PathVariable String customerId) {

        log.info("Fetching orders for customer: {}", customerId);
        return queryGateway.query(
                new FindOrdersByCustomerQuery(customerId),
                ResponseTypes.multipleInstancesOf(OrderView.class)
        );
    }

    /**
     * GET /api/orders/status/{status} - Commandes par statut
     */
    @GetMapping("/status/{status}")
    public CompletableFuture<List<OrderView>> getOrdersByStatus(
            @PathVariable OrderView.OrderStatus status) {

        log.info("Fetching orders with status: {}", status);
        return queryGateway.query(
                new FindOrdersByStatusQuery(status),
                ResponseTypes.multipleInstancesOf(OrderView.class)
        );
    }
}
