package kr.it.rudy.chat.server.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {
    boolean existsByServerIdAndUserId(Long serverId, Long userId);
    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);
    List<ServerMember> findAllByServerId(Long serverId);
}
