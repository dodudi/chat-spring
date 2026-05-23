package com.chat.profile.infrastructure;

import com.chat.profile.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @Query("SELECT p FROM Profile p WHERE p.userId = :userId ORDER BY p.createdAt ASC")
    List<Profile> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT p FROM Profile p WHERE p.id = :id AND p.userId = :userId")
    Optional<Profile> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);

    @Query("SELECT p FROM Profile p WHERE p.userId = :userId ORDER BY p.createdAt ASC LIMIT 1")
    Optional<Profile> findFirstByUserId(@Param("userId") String userId);
}
