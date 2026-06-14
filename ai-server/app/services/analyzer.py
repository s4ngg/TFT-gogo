"""
전적 통계 분석 서비스

Spring 백엔드로부터 받은 매치 데이터를 집계해
시너지별 승률, 최근 요약 통계를 계산한다.
"""
from collections import defaultdict

from app.models.match import AnalyzeRequest, MatchRecord, RecentStats, TraitStat


RARITY_TO_COST = {0: 1, 1: 2, 2: 3, 4: 4, 6: 5}

STYLE_TO_TONE = {
    1: "bronze",
    2: "silver",
    3: "gold",
    4: "gold",   # prismatic → gold 계열로 표시
}

# CDragon trait 아이콘 URL 생성 (suffix 기반)
CDN_BASE = "https://raw.communitydragon.org/latest/game/assets/ux/traiticons"


def _trait_icon_url(trait_name: str) -> str:
    """trait name(e.g. 'TFT17_Bruiser') → CDragon 아이콘 URL"""
    suffix = trait_name.split("_")[-1].lower()
    set_num = ""
    for part in trait_name.split("_"):
        digits = "".join(c for c in part if c.isdigit())
        if digits:
            set_num = digits
            break
    return f"{CDN_BASE}/trait_icon_{set_num}_{suffix}.tft_set{set_num}.png"


def _fmt_place(total: float, count: int) -> str:
    if count == 0:
        return "0.00"
    return f"{total / count:.2f}"


def _fmt_rate(numerator: int, denominator: int) -> str:
    if denominator == 0:
        return "0.0%"
    return f"{numerator / denominator * 100:.1f}%"


def compute_recent_stats(matches: list[MatchRecord]) -> RecentStats:
    if not matches:
        return RecentStats(
            recent_games=0,
            avg_place="0.00",
            top4_rate="0.0%",
            win_rate="0.0%",
        )

    total_place = sum(m.placement for m in matches)
    top4_count = sum(1 for m in matches if m.placement <= 4)
    win_count = sum(1 for m in matches if m.placement == 1)
    n = len(matches)

    return RecentStats(
        recent_games=n,
        avg_place=_fmt_place(total_place, n),
        top4_rate=_fmt_rate(top4_count, n),
        win_rate=_fmt_rate(win_count, n),
        recent_placements=[m.placement for m in matches[:20]],
    )


def compute_trait_stats(matches: list[MatchRecord]) -> tuple[list[TraitStat], list[TraitStat]]:
    """
    시너지별 통계 집계 후 잘 맞는/안 맞는 시너지 분리.
    활성화된 시너지(style > 0)만 집계한다.
    """
    stats: dict[str, dict] = defaultdict(lambda: {
        "games": 0,
        "total_place": 0,
        "top4": 0,
        "style": 0,
        "num_units": 0,
    })

    for match in matches:
        for trait in match.traits:
            if trait.style == 0:
                continue
            key = trait.name
            s = stats[key]
            s["games"] += 1
            s["total_place"] += match.placement
            s["top4"] += 1 if match.placement <= 4 else 0
            # 최대 style 기록 (여러 게임 중 가장 높은 활성 단계)
            if trait.style > s["style"]:
                s["style"] = trait.style
                s["num_units"] = trait.num_units

    MIN_GAMES = 3
    result: list[TraitStat] = []

    for trait_name, s in stats.items():
        if s["games"] < MIN_GAMES:
            continue
        suffix = trait_name.split("_")[-1].lower()
        result.append(TraitStat(
            name=suffix,
            icon_url=_trait_icon_url(trait_name),
            tone=STYLE_TO_TONE.get(s["style"], "bronze"),
            count=s["num_units"],
            games=s["games"],
            avg_place=_fmt_place(s["total_place"], s["games"]),
            top4_rate=_fmt_rate(s["top4"], s["games"]),
        ))

    # 평균 등수 기준 정렬 (낮을수록 좋음)
    result.sort(key=lambda t: float(t.avg_place))

    half = max(1, len(result) // 2)
    good = result[:half]
    bad = list(reversed(result[half:]))

    return good, bad


def analyze(request: AnalyzeRequest) -> dict:
    """전적 데이터를 받아 통계 집계 결과를 반환한다."""
    matches = request.matches
    stats = compute_recent_stats(matches)
    good_traits, bad_traits = compute_trait_stats(matches)

    return {
        "stats": stats,
        "good_traits": good_traits,
        "bad_traits": bad_traits,
    }
