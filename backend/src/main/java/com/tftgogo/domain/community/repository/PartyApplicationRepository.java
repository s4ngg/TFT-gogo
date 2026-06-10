package com.tftgogo.domain.community.repository;

import com.tftgogo.domain.community.entity.PartyApplication;
import com.tftgogo.domain.community.entity.PartyApplicationStatus;
import com.tftgogo.domain.community.entity.PartyPost;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
