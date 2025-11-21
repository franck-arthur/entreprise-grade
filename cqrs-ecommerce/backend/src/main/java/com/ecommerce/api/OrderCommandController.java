package com.ecommerce.api;

import com.ecommerce.command.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * COMMAND CONTROLLER - Expose les endpoints d'écriture (WRITE SIDE).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderCommandController {

    private final CommandGateway commandGateway;

    // ==================== DTOs ====================

    public record CreateOrderRequest(String customerId, String customerEmail) {}

    public record AddItemRequest(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public record ConfirmOrderRequest(String shippingAddress, String paymentMethod) {}

    public record CommandResponse(String id, String message) {}

    // ==================== ENDPOINTS ====================

    /**
     * POST /api/orders - Créer une nouvelle commande
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<CommandResponse>> createOrder(
            @RequestBody CreateOrderRequest request) {

        String orderId = UUID.randomUUID().toString();
        log.info("Creating order with ID: {} for customer: {}", orderId, request.customerId());

        CreateOrderCommand command = CreateOrderCommand.builder()
                .orderId(orderId)
                .customerId(request.customerId())
                .customerEmail(request.customerEmail())
                .build();

        return commandGateway.send(command)
                .thenApply(result -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(new CommandResponse(orderId, "Order created successfully")))
                .exceptionally(ex -> {
                    log.error("Failed to create order", ex);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(new CommandResponse(null, ex.getMessage()));
                });
    }

    /**
     * POST /api/orders/{orderId}/items - Ajouter un article
     */
    @PostMapping("/{orderId}/items")
    public CompletableFuture<ResponseEntity<CommandResponse>> addItem(
            @PathVariable String orderId,
            @RequestBody AddItemRequest request) {

        log.info("Adding item {} to order {}", request.productId(), orderId);

        AddItemToOrderCommand command = AddItemToOrderCommand.builder()
                .orderId(orderId)
                .productId(request.productId())
                .productName(request.productName())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .build();

        return commandGateway.send(command)
                .thenApply(result -> ResponseEntity.ok(
                        new CommandResponse(orderId, "Item added successfully")))
                .exceptionally(ex -> {
                    log.error("Failed to add item", ex);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(new CommandResponse(orderId, ex.getMessage()));
                });
    }

    /**
     * DELETE /api/orders/{orderId}/items/{productId} - Supprimer un article
     */
    @DeleteMapping("/{orderId}/items/{productId}")
    public CompletableFuture<ResponseEntity<CommandResponse>> removeItem(
            @PathVariable String orderId,
            @PathVariable String productId) {

        log.info("Removing item {} from order {}", productId, orderId);

        RemoveItemFromOrderCommand command = RemoveItemFromOrderCommand.builder()
                .orderId(orderId)
                .productId(productId)
                .build();

        return commandGateway.send(command)
                .thenApply(result -> ResponseEntity.ok(
                        new CommandResponse(orderId, "Item removed successfully")))
                .exceptionally(ex -> {
                    log.error("Failed to remove item", ex);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(new CommandResponse(orderId, ex.getMessage()));
                });
    }

    /**
     * POST /api/orders/{orderId}/confirm - Confirmer la commande
     */
    @PostMapping("/{orderId}/confirm")
    public CompletableFuture<ResponseEntity<CommandResponse>> confirmOrder(
            @PathVariable String orderId,
            @RequestBody ConfirmOrderRequest request) {

        log.info("Confirming order {}", orderId);

        ConfirmOrderCommand command = ConfirmOrderCommand.builder()
                .orderId(orderId)
                .shippingAddress(request.shippingAddress())
                .paymentMethod(request.paymentMethod())
                .build();

        return commandGateway.send(command)
                .thenApply(result -> ResponseEntity.ok(
                        new CommandResponse(orderId, "Order confirmed successfully")))
                .exceptionally(ex -> {
                    log.error("Failed to confirm order", ex);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(new CommandResponse(orderId, ex.getMessage()));
                });
    }
}
