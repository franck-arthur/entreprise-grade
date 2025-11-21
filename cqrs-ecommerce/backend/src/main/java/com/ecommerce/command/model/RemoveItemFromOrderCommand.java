package com.ecommerce.command.model;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveItemFromOrderCommand {

    @TargetAggregateIdentifier
    private String orderId;
    private String productId;
}
