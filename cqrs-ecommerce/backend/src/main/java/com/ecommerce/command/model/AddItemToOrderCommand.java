package com.ecommerce.command.model;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddItemToOrderCommand {

    @TargetAggregateIdentifier
    private String orderId;
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
}
