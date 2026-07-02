package com.tftgogo.domain.search.repository;

import com.tftgogo.domain.search.entity.CachedRank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CachedRankRepository extends JpaRepository<CachedRank, String> {
}
