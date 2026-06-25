package com.tftgogo.domain.member.repository;

import com.tftgogo.domain.member.entity.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {

    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    List<RefreshTokenSession> findByMemberUserIdAndRevokedFalse(Long userId);
}
