package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.DeliveryProtocol;
import br.com.estoqueti.repository.DeliveryProtocolRepository;
import jakarta.persistence.EntityManager;

public class JpaDeliveryProtocolRepository implements DeliveryProtocolRepository {

    private final EntityManager entityManager;

    public JpaDeliveryProtocolRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public DeliveryProtocol save(DeliveryProtocol deliveryProtocol) {
        if (deliveryProtocol.getId() == null) {
            entityManager.persist(deliveryProtocol);
            return deliveryProtocol;
        }

        return entityManager.merge(deliveryProtocol);
    }
}