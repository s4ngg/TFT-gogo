from fastapi import APIRouter

from app.models.chat import ChatRequest, ChatResponse
from app.services import chat as chat_service

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    """
    사용자 메시지와 선택적 소환사 컨텍스트를 받아 AI 응답을 반환한다.

    messages: 대화 히스토리 (role: user|assistant)
    context:  소환사 전적 요약 (선택적, 제공 시 개인화된 답변 생성)
    """
    reply = await chat_service.chat(request)
    return ChatResponse(reply=reply)
