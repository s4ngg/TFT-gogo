from pydantic import BaseModel


# ── 요청 모델 (Spring 백엔드가 보내는 전적 데이터) ──────────────────────

class UnitInfo(BaseModel):
    character_id: str       # e.g. "TFT17_Jinx"
    name: str
    tier: int               # 성급 1~3
    rarity: int             # 코스트 등급 (0=1코, 1=2코, 2=3코, 4=4코, 6=5코)
    item_names: list[str]   # 장착 아이템 이름 목록


class TraitInfo(BaseModel):
    name: str               # e.g. "TFT17_Bruiser"
    num_units: int
    style: int              # 0=비활성, 1=브론즈, 2=실버, 3=골드, 4=프리즈매틱
    tier_current: int
    tier_total: int


class MatchRecord(BaseModel):
    match_id: str
    game_datetime: int      # epoch ms
    game_length: float
    game_version: str
    queue_type: str         # "RANKED" | "NORMAL"
    placement: int          # 최종 순위 1~8
    level: int
    last_round: int
    gold_left: int
    players_eliminated: int
    total_damage_to_players: int
    traits: list[TraitInfo]
    units: list[UnitInfo]


class AnalyzeRequest(BaseModel):
    summoner_name: str
    tag_line: str
    matches: list[MatchRecord]


# ── 응답 모델 (프론트 AiRecommend 페이지로 전달) ──────────────────────

class TraitStat(BaseModel):
    name: str               # 시너지 suffix (e.g. "bruiser")
    icon_url: str
    tone: str               # "gold" | "silver" | "bronze"
    count: int              # 최대 활성 단계 유닛 수
    games: int
    avg_place: str          # e.g. "3.25"
    top4_rate: str          # e.g. "62.5%"


class AugmentStat(BaseModel):
    name: str
    icon: str
    games: int
    avg_place: str
    top4_rate: str


class DeckReason(BaseModel):
    deck_rank: int
    is_patch_trend: bool
    reason: str             # AI가 생성한 추천 이유


class RecentStats(BaseModel):
    recent_games: int
    avg_place: str
    top4_rate: str
    win_rate: str           # 1등 비율


class AnalyzeResponse(BaseModel):
    stats: RecentStats
    good_traits: list[TraitStat]    # 잘 맞는 시너지 (평균 등수 낮은 순)
    bad_traits: list[TraitStat]     # 잘 안 맞는 시너지 (평균 등수 높은 순)
    augments: list[AugmentStat]     # 증강 성적 (현재는 빈 리스트, 추후 확장)
    deck_reasons: list[DeckReason]  # AI 추천 덱 이유


# ── 메타 덱 모델 (Spring에서 현재 메타 덱 데이터 전달 시) ──────────────

class MetaDeck(BaseModel):
    rank: int
    grade: str              # "S" | "A+" | "A" | "B" | "C" | "D"
    trait_suffixes: list[str]
    top4_rate: str
    avg_place: str
    pick_rate: str


class AnalyzeWithMetaRequest(BaseModel):
    summoner_name: str
    tag_line: str
    matches: list[MatchRecord]
    meta_decks: list[MetaDeck]
