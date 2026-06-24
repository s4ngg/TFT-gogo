package com.tftgogo.domain.match.repository;

import com.tftgogo.domain.match.entity.CachedRank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CachedRankRepository extends JpaRepository<CachedRank, String> {
}
