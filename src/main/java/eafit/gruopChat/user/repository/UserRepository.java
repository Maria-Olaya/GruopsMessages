package eafit.gruopChat.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eafit.gruopChat.user.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByEnabledTrue();

    // Busca por email SOLO si está activo — para búsquedas públicas
    Optional<User> findByEmailAndEnabledTrue(String email);
}