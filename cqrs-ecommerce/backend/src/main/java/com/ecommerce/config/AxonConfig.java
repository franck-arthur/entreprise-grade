package com.ecommerce.config;

import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.modelling.command.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.command.aggregate.OrderAggregate;

/**
 * Configuration Axon Framework.
 *
 * Par défaut, Axon utilise:
 * - InMemoryEventStorageEngine pour le développement
 * - JpaEventStorageEngine avec une DB configurée
 * - Ou Axon Server (recommandé en production)
 */
@Configuration
public class AxonConfig {

    /**
     * Repository Event-Sourced pour l'agrégat Order.
     * Reconstruit l'état de l'agrégat en rejouant les événements.
     */
    @Bean
    public Repository<OrderAggregate> orderAggregateRepository(EventStore eventStore) {
        return EventSourcingRepository.builder(OrderAggregate.class)
                .eventStore(eventStore)
                .build();
    }
}
