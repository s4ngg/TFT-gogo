from fastapi import APIRouter, Depends

from app.core.security import verify_internal_secret
from app.models.gameguide_pathfinder import GameGuidePathfinderRequest, GameGuidePathfinderResponse
from app.services import gameguide_pathfinder as gameguide_pathfinder_service

router = APIRouter(
    prefix="/gameguide/pathfinder",
    tags=["gameguide"],
    dependencies=[Depends(verify_internal_secret)],
)


@router.post("", response_model=GameGuidePathfinderResponse)
async def pathfind(request: GameGuidePathfinderRequest) -> GameGuidePathfinderResponse:
    """
    Guide 정적 데이터와 사용자 질문을 기반으로 GameGuide AI 응답을 생성한다.
    """
    return await gameguide_pathfinder_service.pathfind(request)
