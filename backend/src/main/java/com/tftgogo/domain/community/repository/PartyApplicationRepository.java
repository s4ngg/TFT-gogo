package com.tftgogo.domain.community.repository;

import com.tftgogo.domain.community.entity.PartyApplication;
import com.tftgogo.domain.community.entity.PartyApplicationStatus;
import com.tftgogo.domain.community.entity.PartyPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartyApplicationRepository extends JpaRepository<PartyApplication, Long> {

    boolean existsByPartyPostAndUserIdAndStatus(
            PartyPost partyPost,
            Long userId,
            PartyApplicationStatus status
    );

    Optional<PartyApplication> findByPartyPostAndUserIdAndStatus(
            PartyPost partyPost,
            Long userId,
            PartyApplicationStatus status
    );

    @Query("""
            select application.partyPost.id
            from PartyApplication application
            where application.userId = :userId
              and application.status = :status
              and application.partyPost.id in :partyPostIds
            """)
    List<Long> findPartyPostIdsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") PartyApplicationStatus status,
            @Param("partyPostIds") Collection<Long> partyPostIds
    );
}
