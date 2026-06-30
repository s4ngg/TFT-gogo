package com.tftgogo.domain.member.repository;

import com.tftgogo.domain.member.entity.AccessTokenBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AccessTokenBlocklistRepository extends JpaRepository<AccessTokenBlocklist, String> {

    boolean existsByTokenIdAndExpiresAtAfter(String tokenId, LocalDateTime now);
}
