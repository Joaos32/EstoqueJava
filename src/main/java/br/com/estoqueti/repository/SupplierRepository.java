package br.com.estoqueti.repository;

import br.com.estoqueti.model.entity.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {

    List<Supplier> findAllActiveOrderByCorporateName();

    Optional<Supplier> findActiveById(Long id);
}