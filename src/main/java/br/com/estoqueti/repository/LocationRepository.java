package br.com.estoqueti.repository;

import br.com.estoqueti.model.entity.Location;

import java.util.List;
import java.util.Optional;

public interface LocationRepository {

    List<Location> findAllActiveOrderByName();

    Optional<Location> findActiveById(Long id);
}