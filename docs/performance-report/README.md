# TFT-gogo 응답속도 개선 작업 기록

## 개요

초기 전적 검색 API가 **44초 후 타임아웃**으로 동작하던 것을 **프로필 ~4초, 매치 첫 10개 즉시 반환**으로 개선한 전체 과정을 단계별로 기록한 문서입니다.

## 파일 목록

| 파일 | 단계 | 핵심 내용 |
|------|------|-----------|
| 01_phase1_initial_problem.md | 1단계 | 초기 문제 분석 — 44초 순차 호출 |
| 02_phase2_api_split.md | 2단계 | 프로필 / 매치 API 분리 |
| 03_phase3_inMemory_cache.md | 3단계 | 인메모리 캐싱 + 이중 호출 제거 |
| 04_phase4_parallel.md | 4단계 | CompletableFuture 병렬화 |
| 05_phase5_db_cache_queue.md | 5단계 | DB 캐시 + RiotQueue 아키텍처 |
| 06_phase6_429_ui.md | 6단계 | 429 Rate Limit UI 피드백 |
| 07_phase7_url_encoding.md | 7단계 | URL 이중인코딩 버그 수정 |
| 08_phase8_dynamic_fast_target.md | 8단계 | FAST_RETURN_TARGET 동적 계산 |

## 최종 수치 비교

| 항목 | 초기 | 최종 |
|------|------|------|
| 첫 검색 응답 | 44초 후 타임아웃 | 프로필 ~4초, 매치 첫 10개 즉시 반환 |
| 재검색 응답 | 44초 (매번 Riot API 재호출) | DB 캐시 히트 → 즉시 반환 |
| 429 발생 시 UX | mock 폴백 / 무한 스피너 | Retry-After 카운트다운 UI |
| 동시 Riot API 호출 수 | 최대 65회 → 84초 대기 | RiotQueue 100ms 워커로 직렬 스로틀 |
| 프론트 axios timeout | 10초 | 60초 |
| 매치 조회 병렬화 | 순차 ~39초 | CompletableFuture ~13초 |
