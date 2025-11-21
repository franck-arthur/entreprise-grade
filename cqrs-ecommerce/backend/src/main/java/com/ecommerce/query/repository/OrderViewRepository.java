package com.ecommerce.query.repository;

import com.ecommerce.query.model.OrderView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderViewRepository extends JpaRepository<OrderView, String> {

    List<OrderView> findByCustomerId(String customerId);

    List<OrderView> findByStatus(OrderView.OrderStatus status);

    @Query("SELECT o FROM OrderView o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<OrderView> findRecentOrdersByCustomer(String customerId);

    @Query("SELECT o FROM OrderView o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    OrderView findByIdWithItems(String orderId);
}
