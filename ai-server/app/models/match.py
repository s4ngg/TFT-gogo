from pydantic import BaseModel, Field, field_validator


# ── 요청 모델 (Spring 백엔드가 보내는 전적 데이터) ──────────────────────

class UnitInfo(BaseModel):
    character_id: str = Field(max_length=64)
    name: str = Field(max_length=64)
    tier: int               # 성급 1~3
    rarity: int             # 코스트 등급 (0=1코, 1=2코, 2=3코, 4=4코, 6=5코)
    item_names: list[str] = Field(default_factory=list, max_length=10)


class TraitInfo(BaseModel):
    name: str = Field(max_length=64)  # e.g. "TFT17_Bruiser"
    num_units: int
    style: int              # 0=비활성, 1=브론즈, 2=실버, 3=골드, 4=프리즈매틱
    tier_current: int
    tier_total: int


class MatchRecord(BaseModel):
    match_id: str = Field(max_length=64)
    game_datetime: int      # epoch ms
    game_length: float
    game_version: str = Field(max_length=128)
    queue_type: str = Field(max_length=16)  # "RANKED" | "NORMAL"
    placement: int = Field(ge=1, le=8)      # 최종 순위 1~8
    level: int = Field(ge=1, le=10)
    last_round: int = Field(ge=1)
    gold_left: int = Field(ge=0)
    players_eliminated: int = Field(ge=0)
    total_damage_to_players: int = Field(ge=0)
    traits: list[TraitInfo] = Field(default_factory=list, max_length=50)
    units: list[UnitInfo] = Field(default_factory=list, max_length=30)


class AnalyzeRequest(BaseModel):
    summoner_name: str = Field(min_length=1, max_length=64)
    tag_line: str = Field(min_length=1, max_length=16)
    matches: list[MatchRecord] = Field(default_factory=list, max_length=50)


class AnalyzeWithMetaRequest(BaseModel):
    summoner_name: str = Field(min_length=1, max_length=64)
    tag_line: str = Field(min_length=1, max_length=16)
    matches: list[MatchRecord] = Field(default_factory=list, max_length=50)
    meta_decks: list["MetaDeck"] = Field(default_factory=list, max_length=100)


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
    recent_placements: list[int] = Field(default_factory=list, max_length=20)


class AnalyzeResponse(BaseModel):
    stats: RecentStats
    good_traits: list[TraitStat]    # 잘 맞는 시너지 (평균 등수 낮은 순)
    bad_traits: list[TraitStat]     # 잘 안 맞는 시너지 (평균 등수 높은 순)
    augments: list[AugmentStat]     # 증강 성적 (현재는 빈 리스트, 추후 확장)
    deck_reasons: list[DeckReason]  # AI 추천 덱 이유


# ── 메타 덱 모델 (Spring에서 현재 메타 덱 데이터 전달 시) ──────────────

_VALID_GRADES = {"S", "A", "B", "C", "D"}

class MetaDeck(BaseModel):
    rank: int = Field(ge=1)
    grade: str = Field(max_length=2)          # "S" | "A" | "B" | "C" | "D"
    trait_suffixes: list[str] = Field(default_factory=list, max_length=20)
    top4_rate: str = Field(max_length=8)
    avg_place: str = Field(max_length=8)
    pick_rate: str = Field(max_length=8)

    @field_validator("grade")
    @classmethod
    def validate_grade(cls, v: str) -> str:
        if v not in _VALID_GRADES:
            raise ValueError(f"grade must be one of {_VALID_GRADES}")
        return v


# AnalyzeWithMetaRequest는 상단에서 forward reference로 선언 후 여기서 rebuild
AnalyzeWithMetaRequest.model_rebuild()
