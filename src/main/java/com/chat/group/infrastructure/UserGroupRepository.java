package com.chat.group.infrastructure;

import com.chat.group.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @Query("SELECT g FROM UserGroup g WHERE g.userId = :userId AND g.isDefault = true")
    Optional<UserGroup> findDefaultByUserId(@Param("userId") String userId);
}
