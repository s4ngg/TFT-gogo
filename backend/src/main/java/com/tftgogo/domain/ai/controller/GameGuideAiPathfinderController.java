package com.tftgogo.domain.ai.controller;

import com.tftgogo.domain.ai.controller.docs.GameGuideAiPathfinderControllerDocs;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderRequest;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderResponse;
import com.tftgogo.domain.ai.service.GameGuideAiPathfinderService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class GameGuideAiPathfinderController implements GameGuideAiPathfinderControllerDocs {

    private final GameGuideAiPathfinderService gameGuideAiPathfinderService;

    @PostMapping("/gameguide-pathfinder")
    public ResponseEntity<ApiResponse<GameGuideAiPathfinderResponse>> pathfind(
            @Valid @RequestBody GameGuideAiPathfinderRequest request
    ) {
        GameGuideAiPathfinderResponse response = gameGuideAiPathfinderService.pathfind(request);
        return ResponseEntity.ok(ApiResponse.success("GameGuide AI 응답 완료", response));
    }
}
