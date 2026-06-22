package com.tftgogo.domain.community.dto.request;

import com.tftgogo.domain.community.entity.PartyGameMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class PartyPostCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private PartyGameMode gameMode;

    @Min(2)
    @Max(8)
    private int maxMembers;

    @Future(message = "마감 시간은 현재 시간 이후여야 합니다.")
    private LocalDateTime deadline;

    @Size(max = 4)
    private List<@NotBlank @Size(max = 30) String> tags = List.of();
}
