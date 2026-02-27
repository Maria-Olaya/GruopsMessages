package eafit.gruopChat.presence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import eafit.gruopChat.presence.model.UserPresence;

public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {
    // findById(userId) heredado — suficiente
}