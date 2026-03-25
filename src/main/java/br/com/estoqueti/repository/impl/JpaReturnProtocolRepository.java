package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.ReturnProtocol;
import br.com.estoqueti.repository.ReturnProtocolRepository;
import jakarta.persistence.EntityManager;

public class JpaReturnProtocolRepository implements ReturnProtocolRepository {

    private final EntityManager entityManager;

    public JpaReturnProtocolRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ReturnProtocol save(ReturnProtocol returnProtocol) {
        if (returnProtocol.getId() == null) {
            entityManager.persist(returnProtocol);
            return returnProtocol;
        }

        return entityManager.merge(returnProtocol);
    }
}
