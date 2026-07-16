from unittest.mock import AsyncMock, patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.api.gameguide_pathfinder import router
from app.models.gameguide_pathfinder import GameGuidePathfinderResponse


TEST_SECRET = "test-secret-value"


def _create_app() -> FastAPI:
    app = FastAPI()
    app.include_router(router, prefix="/api")
    return app


def test_gameguide_pathfinder_api는_BaseResponse_envelope로_응답한다():
    app = _create_app()
    client = TestClient(app)
    response_payload = GameGuidePathfinderResponse(
        title="동물특공대 운영",
        summary="초반 운영을 설명합니다.",
        core_concepts=[],
        evidence_notes=[],
        creative_suggestions=[],
        phase_plan=[],
        recommended_refs=[],
        avoid_mistakes=[],
        source_refs=[],
        limitations=[],
        is_fallback=False,
    )

    with patch("app.core.security.settings") as mock_settings, \
         patch("app.services.gameguide_pathfinder.pathfind", new_callable=AsyncMock) as mock_pathfind:
        mock_settings.internal_secret = TEST_SECRET
        mock_pathfind.return_value = response_payload

        response = client.post(
            "/api/gameguide/pathfinder",
            headers={"X-Internal-Secret": TEST_SECRET},
            json={
                "patch_version": "17.3",
                "active_tab": "traits",
                "mode": "AUTO",
                "selected_entries": [],
                "candidate_refs": [],
                "conversation_history": [],
                "question": "동물특공대 초반 운영 알려줘",
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["code"] == "OK"
    assert body["data"]["title"] == "동물특공대 운영"
    assert body["data"]["is_fallback"] is False
