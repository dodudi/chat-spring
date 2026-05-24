package com.chat.group.infrastructure;

import com.chat.group.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @Query("SELECT g FROM UserGroup g WHERE g.userId = :userId ORDER BY g.createdAt ASC")
    List<UserGroup> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT g FROM UserGroup g WHERE g.id = :id AND g.userId = :userId")
    Optional<UserGroup> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    @Query("SELECT g FROM UserGroup g WHERE g.userId = :userId AND g.isDefault = true")
    Optional<UserGroup> findDefaultByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(g) FROM UserGroup g WHERE g.userId = :userId AND g.isDefault = false")
    long countCustomByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(g) > 0 FROM UserGroup g WHERE g.userId = :userId AND g.name = :name")
    boolean existsByUserIdAndName(@Param("userId") String userId, @Param("name") String name);

    @Query("SELECT COUNT(g) > 0 FROM UserGroup g WHERE g.userId = :userId AND g.name = :name AND g.id <> :excludeId")
    boolean existsByUserIdAndNameExcluding(@Param("userId") String userId, @Param("name") String name, @Param("excludeId") Long excludeId);
}
