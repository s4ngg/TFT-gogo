from typing import Literal

from pydantic import BaseModel, Field


GuideType = Literal["TRAIT", "ITEM", "AUGMENT", "CHAMPION"]
GuideTab = Literal["traits", "items", "augments", "champions"]
PathfinderMode = Literal["AUTO"]
PathfinderPhase = Literal["EARLY", "MID", "LATE", "ANY"]


class GuideRef(BaseModel):
    guide_type: GuideType
    target_key: str = Field(max_length=120)
    name: str | None = Field(default=None, max_length=100)


class SelectedGuideEntry(GuideRef):
    summary: str | None = Field(default=None, max_length=1000)
    data: dict = Field(default_factory=dict)


class CandidateGuideRef(GuideRef):
    reason_hint: str | None = Field(default=None, max_length=200)


class GameGuidePathfinderRequest(BaseModel):
    patch_version: str = Field(max_length=20)
    active_tab: GuideTab
    mode: PathfinderMode = "AUTO"
    selected_entries: list[SelectedGuideEntry] = Field(default_factory=list, max_length=5)
    candidate_refs: list[CandidateGuideRef] = Field(default_factory=list, max_length=20)
    question: str = Field(min_length=1, max_length=500)


class PhasePlan(BaseModel):
    phase: PathfinderPhase
    title: str = Field(max_length=80)
    description: str = Field(max_length=500)
    guide_refs: list[GuideRef] = Field(default_factory=list, max_length=5)


class RecommendedRef(GuideRef):
    reason: str = Field(max_length=200)


class GameGuidePathfinderResponse(BaseModel):
    title: str = Field(max_length=80)
    summary: str = Field(max_length=500)
    core_concepts: list[str] = Field(default_factory=list, max_length=5)
    evidence_notes: list[str] = Field(default_factory=list, max_length=4)
    creative_suggestions: list[str] = Field(default_factory=list, max_length=4)
    phase_plan: list[PhasePlan] = Field(default_factory=list, max_length=4)
    recommended_refs: list[RecommendedRef] = Field(default_factory=list, max_length=5)
    avoid_mistakes: list[str] = Field(default_factory=list, max_length=4)
    source_refs: list[GuideRef] = Field(default_factory=list, max_length=5)
    limitations: list[str] = Field(default_factory=list, max_length=4)
    is_fallback: bool = False
