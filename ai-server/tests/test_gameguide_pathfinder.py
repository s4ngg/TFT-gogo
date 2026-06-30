import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.models.gameguide_pathfinder import (
    CandidateGuideRef,
    ConversationMessage,
    GameGuidePathfinderRequest,
    SelectedGuideEntry,
)
from app.services import gameguide_pathfinder

_BUDGET_PATH = "app.services.gameguide_pathfinder.check_budget"
_BREAKER_PATH = "app.services.gameguide_pathfinder.openai_breaker"


def _make_request(
    question: str = "동물특공대 어떻게 운영해?",
    selected_entries: list[SelectedGuideEntry] | None = None,
    candidate_refs: list[CandidateGuideRef] | None = None,
) -> GameGuidePathfinderRequest:
    return GameGuidePathfinderRequest(
        patch_version="17.3",
        active_tab="traits",
        mode="AUTO",
        selected_entries=selected_entries or [],
        candidate_refs=candidate_refs or [],
        question=question,
    )


def _mock_openai_response(content: str) -> MagicMock:
    choice = MagicMock()
    choice.message.content = content
    response = MagicMock()
    response.choices = [choice]
    response.usage = None
    return response


@pytest.mark.asyncio
async def test_API키_미설정_시_fallback_응답_반환():
    # given
    request = _make_request()

    # when
    with patch("app.services.gameguide_pathfinder.settings") as mock_settings:
        mock_settings.openai_api_key = ""
        result = await gameguide_pathfinder.pathfind(request)

    # then
    assert result.is_fallback is True
    assert result.title == "시너지 가이드 질문"
    assert result.phase_plan[0].phase == "ANY"
    assert result.evidence_notes == ["현재 선택한 가이드 항목과 화면 후보만 기준으로 안내합니다."]
    assert result.creative_suggestions == []
    assert "질문: 동물특공대 어떻게 운영해?" in result.limitations


@pytest.mark.asyncio
async def test_OpenAI_JSON_응답을_구조화_응답으로_반환():
    # given
    selected = SelectedGuideEntry(
        guide_type="TRAIT",
        target_key="TFT17_AnimalSquad",
        name="동물특공대",
        summary="공격 속도와 지속 전투",
        data={},
    )
    candidate = CandidateGuideRef(
        guide_type="CHAMPION",
        target_key="TFT17_ExampleChampion",
        name="예시 챔피언",
        reason_hint="selected trait includes this champion",
    )
    request = _make_request(selected_entries=[selected], candidate_refs=[candidate])
    content = json.dumps({
        "title": "동물특공대 운영 루트",
        "summary": "지속 전투 중심으로 앞라인을 먼저 갖추세요.",
        "core_concepts": ["앞라인 유지", "공격 속도 활용"],
        "evidence_notes": ["선택한 시너지 설명에 지속 전투 성격이 포함됩니다."],
        "creative_suggestions": ["상황에 따라 공격 속도를 살릴 수 있는 후반 보강을 시도해볼 수 있습니다."],
        "phase_plan": [
            {
                "phase": "ANY",
                "title": "핵심 확인",
                "description": "선택한 시너지와 연결 챔피언을 같이 봅니다.",
                "guide_refs": [
                    {
                        "guide_type": "CHAMPION",
                        "target_key": "TFT17_ExampleChampion",
                        "name": "예시 챔피언",
                    }
                ],
            }
        ],
        "recommended_refs": [
            {
                "guide_type": "CHAMPION",
                "target_key": "TFT17_ExampleChampion",
                "name": "예시 챔피언",
                "reason": "같은 시너지 후보입니다.",
            }
        ],
        "avoid_mistakes": ["승률을 단정하지 마세요."],
        "source_refs": [
            {
                "guide_type": "TRAIT",
                "target_key": "TFT17_AnimalSquad",
                "name": "동물특공대",
            }
        ],
        "limitations": ["정적 가이드 기준입니다."],
        "is_fallback": False,
    }, ensure_ascii=False)
    mock_client = AsyncMock()
    mock_client.chat.completions.create.return_value = _mock_openai_response(content)

    # when
    with patch("app.services.gameguide_pathfinder.settings") as mock_settings, \
         patch("app.services.gameguide_pathfinder._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
        result = await gameguide_pathfinder.pathfind(request)

    # then
    assert result.is_fallback is False
    assert result.title == "동물특공대 운영 루트"
    assert result.evidence_notes == ["선택한 시너지 설명에 지속 전투 성격이 포함됩니다."]
    assert result.creative_suggestions == ["상황에 따라 공격 속도를 살릴 수 있는 후반 보강을 시도해볼 수 있습니다."]
    assert result.recommended_refs[0].target_key == "TFT17_ExampleChampion"
    mock_breaker.record_success.assert_called_once()


@pytest.mark.asyncio
async def test_OpenAI가_허용되지_않은_ref를_반환하면_제거한다():
    # given
    request = _make_request()
    content = json.dumps({
        "title": "임의 추천",
        "summary": "허용되지 않은 링크는 제거되어야 합니다.",
        "core_concepts": [],
        "phase_plan": [
            {
                "phase": "ANY",
                "title": "잘못된 링크",
                "description": "candidate에 없는 항목입니다.",
                "guide_refs": [
                    {
                        "guide_type": "CHAMPION",
                        "target_key": "TFT17_NotAllowed",
                        "name": "없는 챔피언",
                    }
                ],
            }
        ],
        "recommended_refs": [
            {
                "guide_type": "CHAMPION",
                "target_key": "TFT17_NotAllowed",
                "name": "없는 챔피언",
                "reason": "임의 추천",
            }
        ],
        "avoid_mistakes": [],
        "source_refs": [],
        "limitations": [],
        "is_fallback": False,
    }, ensure_ascii=False)
    mock_client = AsyncMock()
    mock_client.chat.completions.create.return_value = _mock_openai_response(content)

    # when
    with patch("app.services.gameguide_pathfinder.settings") as mock_settings, \
         patch("app.services.gameguide_pathfinder._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
        result = await gameguide_pathfinder.pathfind(request)

    # then
    assert result.recommended_refs == []
    assert result.phase_plan[0].guide_refs == []


@pytest.mark.asyncio
async def test_OpenAI_ref가_많아도_허용된_5개만_반환한다():
    # given
    candidates = [
        CandidateGuideRef(
            guide_type="CHAMPION",
            target_key=f"TFT17_Champion{index}",
            name=f"Champion {index}",
        )
        for index in range(6)
    ]
    request = _make_request(candidate_refs=candidates)
    refs = [
        {
            "guide_type": "CHAMPION",
            "target_key": f"TFT17_Champion{index}",
            "name": f"Champion {index}",
        }
        for index in range(6)
    ]
    content = json.dumps({
        "title": "N.O.V.A. route",
        "summary": "Use the allowed guide refs without falling back.",
        "core_concepts": [],
        "evidence_notes": [],
        "creative_suggestions": [],
        "phase_plan": [
            {
                "phase": "ANY",
                "title": "Check refs",
                "description": "Too many refs should be trimmed.",
                "guide_refs": refs,
            }
        ],
        "recommended_refs": [
            {**ref, "reason": "allowed candidate"}
            for ref in refs
        ],
        "avoid_mistakes": [],
        "source_refs": refs,
        "limitations": [],
        "is_fallback": False,
    }, ensure_ascii=False)
    mock_client = AsyncMock()
    mock_client.chat.completions.create.return_value = _mock_openai_response(content)

    # when
    with patch("app.services.gameguide_pathfinder.settings") as mock_settings, \
         patch("app.services.gameguide_pathfinder._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
        result = await gameguide_pathfinder.pathfind(request)

    # then
    assert result.is_fallback is False
    assert len(result.source_refs) == 5
    assert len(result.recommended_refs) == 5
    assert len(result.phase_plan[0].guide_refs) == 5


@pytest.mark.asyncio
async def test_OpenAI_JSON_파싱_실패_시_fallback_응답_반환():
    # given
    request = _make_request()
    mock_client = AsyncMock()
    mock_client.chat.completions.create.return_value = _mock_openai_response("not json")

    # when
    with patch("app.services.gameguide_pathfinder.settings") as mock_settings, \
         patch("app.services.gameguide_pathfinder._get_client", return_value=mock_client), \
         patch(_BUDGET_PATH, return_value=100), \
         patch(_BREAKER_PATH) as mock_breaker:
        mock_settings.openai_api_key = "sk-test"
        mock_settings.openai_model = "gpt-4o-mini"
        mock_settings.recommend_max_input_tokens = 6000
        mock_breaker.is_open.return_value = False
        result = await gameguide_pathfinder.pathfind(request)

    # then
    assert result.is_fallback is True
    mock_breaker.record_failure.assert_called_once()


def test_시스템_프롬프트는_없는_메트릭_추측을_금지한다():
    assert "승률, 평균 등수, 픽률, TOP4율" in gameguide_pathfinder._SYSTEM_PROMPT
    assert "생성하거나 추측하지 않습니다" in gameguide_pathfinder._SYSTEM_PROMPT


def test_시스템_프롬프트는_근거와_AI제안을_구분한다():
    assert "evidence_notes" in gameguide_pathfinder._SYSTEM_PROMPT
    assert "creative_suggestions" in gameguide_pathfinder._SYSTEM_PROMPT
    assert "가이드 데이터와 AI 제안이 충돌하면 가이드 데이터를 우선합니다" in gameguide_pathfinder._SYSTEM_PROMPT


def test_시스템_프롬프트는_이어묻기_맥락을_구분한다():
    assert "conversation_history" in gameguide_pathfinder._SYSTEM_PROMPT
    assert "질문 의도를 파악하는 데만 사용" in gameguide_pathfinder._SYSTEM_PROMPT


def test_selected_entries_payload는_큰_data를_압축한다():
    # given
    selected = SelectedGuideEntry(
        guide_type="TRAIT",
        target_key="TFT17_AnimalSquad",
        name="동물특공대",
        summary="요약" * 500,
        data={
            "long_text": "a" * 1000,
            "items": [
                {
                    "name": f"item-{index}",
                    "description": "b" * 1000,
                }
                for index in range(20)
            ],
        },
    )
    request = _make_request(selected_entries=[selected])

    # when
    payload = json.loads(gameguide_pathfinder._build_user_payload(request))

    # then
    entry = payload["selected_entries"][0]
    assert entry["summary"].endswith("...")
    assert len(entry["summary"]) <= gameguide_pathfinder._MAX_SUMMARY_LENGTH + 3
    assert entry["data"]["long_text"].endswith("...")
    assert len(entry["data"]["long_text"]) <= gameguide_pathfinder._MAX_DATA_STRING_LENGTH + 3
    assert len(entry["data"]["items"]) == gameguide_pathfinder._MAX_DATA_LIST_ITEMS
    assert entry["data"]["items"][0]["description"].endswith("...")


def test_conversation_history_payload는_최근_대화_맥락을_포함한다():
    # given
    request = _make_request(
        question="그럼 아이템은?",
        selected_entries=[],
        candidate_refs=[],
    )
    request.conversation_history = [
        ConversationMessage(role="user", content="N.O.V.A. 초반 운영 알려줘"),
        ConversationMessage(role="assistant", content="초반에는 핵심 유닛을 먼저 잡으세요."),
    ]

    # when
    payload = json.loads(gameguide_pathfinder._build_user_payload(request))

    # then
    assert payload["question"] == "그럼 아이템은?"
    assert payload["conversation_history"] == [
        {"role": "user", "content": "N.O.V.A. 초반 운영 알려줘"},
        {"role": "assistant", "content": "초반에는 핵심 유닛을 먼저 잡으세요."},
    ]
