from fastapi import APIRouter

from app.models.match import AnalyzeRequest, AnalyzeResponse, AnalyzeWithMetaRequest
from app.services import analyzer, recommender

router = APIRouter(prefix="/analyze", tags=["analyze"])


@router.post("", response_model=AnalyzeResponse)
async def analyze_matches(request: AnalyzeRequest) -> AnalyzeResponse:
    """
    소환사 전적 데이터를 받아 AI 분석 결과를 반환한다.

    Spring 백엔드가 전적 데이터를 수집해 이 엔드포인트로 전달하면
    시너지 통계 집계 + OpenAI 추천 이유를 생성해 응답한다.
    """
    result = analyzer.analyze(request)

    return AnalyzeResponse(
        stats=result["stats"],
        good_traits=result["good_traits"],
        bad_traits=result["bad_traits"],
        augments=[],        # TODO: Riot API 증강 데이터 제공 시 확장
        deck_reasons=[],    # 메타 덱 없이는 추천 불가 — /analyze/with-meta 사용
    )


@router.post("/with-meta", response_model=AnalyzeResponse)
async def analyze_with_meta(request: AnalyzeWithMetaRequest) -> AnalyzeResponse:
    """
    전적 데이터 + 현재 메타 덱을 함께 받아 완전한 AI 추천 결과를 반환한다.

    Spring 백엔드가 메타 덱 목록도 함께 전달하는 경우 사용.
    단일 JSON 바디로 수신: { summoner_name, tag_line, matches, meta_decks }
    """
    result = analyzer.analyze(request)
    good_traits = result["good_traits"]
    stats = result["stats"]

    # 메타 덱 중 내 스타일과 잘 맞는 덱 순위 재정렬
    ranked_decks = recommender.rank_meta_decks(request.meta_decks, good_traits)

    stats_summary = (
        f"평균 {stats.avg_place}등, TOP4율 {stats.top4_rate}, "
        f"분석 {stats.recent_games}게임"
    )
    deck_reasons = await recommender.generate_reasons(
        good_traits=good_traits,
        recommended_decks=ranked_decks[:3],
        stats_summary=stats_summary,
    )

    return AnalyzeResponse(
        stats=stats,
        good_traits=good_traits,
        bad_traits=result["bad_traits"],
        augments=[],
        deck_reasons=deck_reasons,
    )
