package com.tftgogo.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ────────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // ── 회원 ────────────────────────────────────────────
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인 처리에 실패했습니다."),
    SOCIAL_EMAIL_REQUIRED(HttpStatus.UNAUTHORIZED, "소셜 계정 이메일 정보를 확인할 수 없습니다."),
    SOCIAL_PROVIDER_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 설정이 완료되지 않았습니다."),

    // ── 덱 ─────────────────────────────────────────────
    DECK_NOT_FOUND(HttpStatus.NOT_FOUND, "덱 정보를 찾을 수 없습니다."),
    HERO_AUGMENT_DECK_NOT_FOUND(HttpStatus.NOT_FOUND, "영웅증강 덱을 찾을 수 없습니다."),
    AGGREGATION_ALREADY_RUNNING(HttpStatus.CONFLICT, "집계가 이미 실행 중입니다. 완료 후 다시 시도해주세요."),
    AGGREGATION_QUEUE_FULL(HttpStatus.SERVICE_UNAVAILABLE, "집계 작업 큐가 가득 찼습니다. 잠시 후 다시 시도해주세요."),
    CLIENT_VERSION_PATCH_MAPPING_NOT_FOUND(HttpStatus.NOT_FOUND, "클라이언트 버전 매핑을 찾을 수 없습니다."),
    CLIENT_VERSION_PATCH_MAPPING_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 클라이언트 버전입니다."),

    // ── 게임가이드 ─────────────────────────────────────
    GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "게임가이드를 찾을 수 없습니다."),
    GUIDE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 게임가이드입니다."),
    GUIDE_INVALID_TAB(HttpStatus.BAD_REQUEST, "지원하지 않는 게임가이드 탭입니다."),
    GUIDE_INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "게임가이드 데이터 형식이 올바르지 않습니다."),

    // ── 패치노트 ───────────────────────────────────────
    PATCH_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "패치노트를 찾을 수 없습니다."),
    PATCH_CHANGE_NOT_FOUND(HttpStatus.NOT_FOUND, "패치 변경사항을 찾을 수 없습니다."),
    PATCH_NOTE_INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "패치노트 데이터 형식이 올바르지 않습니다."),

    // ── 커뮤니티 ───────────────────────────────────────
    PARTY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "파티 모집글을 찾을 수 없습니다."),
    PARTY_POST_FULL(HttpStatus.CONFLICT, "파티 모집 정원이 가득 찼습니다."),
    PARTY_POST_CLOSED(HttpStatus.CONFLICT, "마감된 파티 모집글입니다."),
    PARTY_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 파티가 있습니다."),
    CHAT_STREAM_CONNECTION_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "채팅 연결 수가 많습니다. 잠시 후 다시 시도해주세요."),

    // ── 전적 ────────────────────────────────────────────
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "소환사 계정을 찾을 수 없습니다."),
    SUMMONER_NOT_FOUND(HttpStatus.NOT_FOUND, "소환사를 찾을 수 없습니다."),
    LEAGUE_NOT_FOUND(HttpStatus.NOT_FOUND, "랭크 정보를 찾을 수 없습니다."),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "전적 정보를 찾을 수 없습니다."),

    // ── 외부 API ─────────────────────────────────────────
    RIOT_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "Riot API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),
    RIOT_QUEUE_FULL(HttpStatus.TOO_MANY_REQUESTS, "Riot API 요청 대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요."),
    RIOT_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Riot API 응답 시간이 초과되었습니다."),
    RIOT_API_ERROR(HttpStatus.BAD_GATEWAY, "Riot API 호출 중 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 데이터 호출 중 오류가 발생했습니다."),
    AI_CHAT_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "AI 채팅 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),
    AI_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 연결에 실패했습니다."),

    // ── 관리자 인증 ─────────────────────────────────────
    ADMIN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "관리자 계정을 찾을 수 없습니다."),
    ADMIN_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ADMIN_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "비활성화된 관리자 계정입니다."),
    ADMIN_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    ADMIN_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다.");

    private final HttpStatus status;
    private final String message;
}
