package com.tftgogo.domain.match.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "매치 수집 상태 응답")
public class CollectionStatusResponse {

    @Schema(description = "DB에 저장된 해당 소환사의 매치 수", example = "14")
    private int collected;

    @Schema(description = "백그라운드 수집 진행 여부")
    private boolean inProgress;
}
