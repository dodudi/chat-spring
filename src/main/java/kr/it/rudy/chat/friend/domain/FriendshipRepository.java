package kr.it.rudy.chat.friend.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :userId OR f.friend.id = :userId")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :minId AND f.friend.id = :maxId")
    Optional<Friendship> findByUserIds(@Param("minId") Long minId, @Param("maxId") Long maxId);
}
