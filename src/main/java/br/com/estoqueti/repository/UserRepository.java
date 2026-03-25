package br.com.estoqueti.repository;

import br.com.estoqueti.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findAllOrderedByName();

    boolean existsByUsernameIgnoreCase(String username);

    User save(User user);
}
