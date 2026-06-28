package kr.it.rudy.chat.channel.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelCategoryRepository extends JpaRepository<ChannelCategory, Long> {
    List<ChannelCategory> findAllByServerIdOrderByPosition(Long serverId);
}
