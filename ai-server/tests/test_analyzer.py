"""
analyzer.py 단위 테스트

given / when / then 패턴으로 작성.
외부 의존성(OpenAI, DB) 없이 순수 로직만 테스트.
"""
import pytest

from app.models.match import AnalyzeRequest, MatchRecord, TraitInfo, UnitInfo
from app.services.analyzer import compute_recent_stats, compute_trait_stats, analyze


# ── 테스트용 픽스처 ──────────────────────────────────────────────────────

def _make_trait(name: str, style: int, num_units: int = 4) -> TraitInfo:
    return TraitInfo(name=name, num_units=num_units, style=style, tier_current=1, tier_total=3)


def _make_unit(character_id: str, rarity: int = 2) -> UnitInfo:
    return UnitInfo(character_id=character_id, name=character_id, tier=2, rarity=rarity, item_names=[])


def _make_match(placement: int, traits: list[TraitInfo] | None = None) -> MatchRecord:
    return MatchRecord(
        match_id=f"KR_test_{placement}",
        game_datetime=1717200000000,
        game_length=1800.0,
        game_version="16.11",
        queue_type="RANKED",
        placement=placement,
        level=8,
        last_round=25,
        gold_left=2,
        players_eliminated=1,
        total_damage_to_players=40,
        traits=traits or [],
        units=[_make_unit("TFT17_Jinx")],
    )


def _make_request(matches: list[MatchRecord]) -> AnalyzeRequest:
    return AnalyzeRequest(summoner_name="테스트소환사", tag_line="KR1", matches=matches)


# ── compute_recent_stats 테스트 ──────────────────────────────────────────

def test_최근_통계_빈_매치_목록():
    # given
    request = _make_request([])

    # when
    stats = compute_recent_stats([])

    # then
    assert stats.recent_games == 0
    assert stats.avg_place == "0.00"
    assert stats.top4_rate == "0.0%"
    assert stats.win_rate == "0.0%"


def test_최근_통계_정상_집계():
    # given: 1등 1번, 4등 1번, 6등 1번 → 총 3게임
    matches = [
        _make_match(placement=1),
        _make_match(placement=4),
        _make_match(placement=6),
    ]

    # when
    stats = compute_recent_stats(matches)

    # then
    assert stats.recent_games == 3
    assert stats.avg_place == "3.67"   # (1+4+6)/3
    assert stats.top4_rate == "66.7%"  # 2/3
    assert stats.win_rate == "33.3%"   # 1/3


def test_전부_1등이면_top4율과_승률_모두_100():
    # given
    matches = [_make_match(placement=1) for _ in range(5)]

    # when
    stats = compute_recent_stats(matches)

    # then
    assert stats.top4_rate == "100.0%"
    assert stats.win_rate == "100.0%"


# ── compute_trait_stats 테스트 ──────────────────────────────────────────

def test_비활성_시너지는_집계에서_제외():
    # given: style=0 (비활성) 시너지만 있는 게임
    matches = [
        _make_match(placement=3, traits=[_make_trait("TFT17_Bruiser", style=0)]),
        _make_match(placement=3, traits=[_make_trait("TFT17_Bruiser", style=0)]),
        _make_match(placement=3, traits=[_make_trait("TFT17_Bruiser", style=0)]),
    ]

    # when
    good, bad = compute_trait_stats(matches)

    # then
    assert len(good) == 0
    assert len(bad) == 0


def test_최소_게임수_미만_시너지는_제외():
    # given: 2게임만 플레이한 시너지 (MIN_GAMES=3)
    matches = [
        _make_match(placement=2, traits=[_make_trait("TFT17_Bruiser", style=2)]),
        _make_match(placement=3, traits=[_make_trait("TFT17_Bruiser", style=2)]),
    ]

    # when
    good, bad = compute_trait_stats(matches)

    # then
    assert len(good) == 0


def test_잘_맞는_시너지와_안_맞는_시너지_분리():
    # given: 브루저(평균 2등)와 스나이퍼(평균 6등) 각 3게임씩
    bruiser_matches = [
        _make_match(placement=1, traits=[_make_trait("TFT17_Bruiser", style=2)]),
        _make_match(placement=2, traits=[_make_trait("TFT17_Bruiser", style=2)]),
        _make_match(placement=3, traits=[_make_trait("TFT17_Bruiser", style=2)]),
    ]
    sniper_matches = [
        _make_match(placement=5, traits=[_make_trait("TFT17_Sniper", style=1)]),
        _make_match(placement=6, traits=[_make_trait("TFT17_Sniper", style=1)]),
        _make_match(placement=7, traits=[_make_trait("TFT17_Sniper", style=1)]),
    ]

    # when
    good, bad = compute_trait_stats(bruiser_matches + sniper_matches)

    # then
    assert len(good) >= 1
    assert len(bad) >= 1
    good_names = [t.name for t in good]
    bad_names = [t.name for t in bad]
    assert "bruiser" in good_names
    assert "sniper" in bad_names


# ── analyze (통합) 테스트 ────────────────────────────────────────────────

def test_전체_분석_응답_구조_검증():
    # given
    matches = [
        _make_match(placement=i, traits=[_make_trait("TFT17_Bruiser", style=2)])
        for i in range(1, 9)
    ]
    request = _make_request(matches)

    # when
    result = analyze(request)

    # then
    assert "stats" in result
    assert "good_traits" in result
    assert "bad_traits" in result
    assert result["stats"].recent_games == 8
