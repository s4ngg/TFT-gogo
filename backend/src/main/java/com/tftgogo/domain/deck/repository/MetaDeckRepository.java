package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.MetaDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MetaDeckRepository extends JpaRepository<MetaDeck, Long> {

    Optional<MetaDeck> findBySignature(String signature);

    // win_rate 내림차순 정렬 (DB 레벨)
    @Query("SELECT d FROM MetaDeck d ORDER BY d.winRate DESC")
    List<MetaDeck> findAllOrderByWinRateDesc();
}
