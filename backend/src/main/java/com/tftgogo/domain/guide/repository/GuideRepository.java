package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc();

    List<Guide> findByGuideTypeAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(GuideType guideType);
}
