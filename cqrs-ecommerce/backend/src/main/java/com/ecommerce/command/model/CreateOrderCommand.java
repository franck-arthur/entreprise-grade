package com.ecommerce.command.model;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {

    @TargetAggregateIdentifier
    private String orderId;
    private String customerId;
    private String customerEmail;

    public static CreateOrderCommand create(String customerId, String customerEmail) {
        return CreateOrderCommand.builder()
                .orderId(UUID.randomUUID().toString())
                .customerId(customerId)
                .customerEmail(customerEmail)
                .build();
    }
}
