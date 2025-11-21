package com.ecommerce.query.handler;

import com.ecommerce.query.model.OrderView;
import com.ecommerce.query.repository.OrderViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QUERY HANDLER - Gère les requêtes de lecture.
 *
 * Utilise des classes de requête typées pour une meilleure maintenabilité.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {

    private final OrderViewRepository orderViewRepository;

    // ==================== QUERY CLASSES ====================

    public record FindOrderByIdQuery(String orderId) {}
    public record FindOrdersByCustomerQuery(String customerId) {}
    public record FindOrdersByStatusQuery(OrderView.OrderStatus status) {}
    public record FindAllOrdersQuery() {}

    // ==================== QUERY HANDLERS ====================

    @QueryHandler
    public OrderView handle(FindOrderByIdQuery query) {
        log.debug("Handling FindOrderByIdQuery for orderId: {}", query.orderId());
        return orderViewRepository.findByIdWithItems(query.orderId());
    }

    @QueryHandler
    public List<OrderView> handle(FindOrdersByCustomerQuery query) {
        log.debug("Handling FindOrdersByCustomerQuery for customerId: {}", query.customerId());
        return orderViewRepository.findByCustomerId(query.customerId());
    }

    @QueryHandler
    public List<OrderView> handle(FindOrdersByStatusQuery query) {
        log.debug("Handling FindOrdersByStatusQuery for status: {}", query.status());
        return orderViewRepository.findByStatus(query.status());
    }

    @QueryHandler
    public List<OrderView> handle(FindAllOrdersQuery query) {
        log.debug("Handling FindAllOrdersQuery");
        return orderViewRepository.findAll();
    }
}
