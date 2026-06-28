package kr.it.rudy.chat.server.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerInviteRepository extends JpaRepository<ServerInvite, Long> {
    Optional<ServerInvite> findByCode(String code);
}
