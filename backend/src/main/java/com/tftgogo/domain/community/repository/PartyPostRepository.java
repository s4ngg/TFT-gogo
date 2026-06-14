package com.tftgogo.domain.community.repository;

import com.tftgogo.domain.community.entity.PartyGameMode;
import com.tftgogo.domain.community.entity.PartyPost;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartyPostRepository extends JpaRepository<PartyPost, Long> {

    @Query(value = """
            select p
            from PartyPost p
            where p.deletedAt is null
              and (:gameMode is null or p.gameMode = :gameMode)
              and (
                    :query is null
                    or lower(p.title) like lower(concat('%', :query, '%'))
                    or lower(p.content) like lower(concat('%', :query, '%'))
            )
            order by p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p)
            from PartyPost p
            where p.deletedAt is null
              and (:gameMode is null or p.gameMode = :gameMode)
              and (
                    :query is null
                    or lower(p.title) like lower(concat('%', :query, '%'))
                    or lower(p.content) like lower(concat('%', :query, '%'))
              )
            """)
    Page<PartyPost> search(
            @Param("gameMode") PartyGameMode gameMode,
            @Param("query") String query,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from PartyPost p
            where p.id = :partyPostId
              and p.deletedAt is null
            """)
    Optional<PartyPost> findActiveByIdForUpdate(@Param("partyPostId") Long partyPostId);
}
