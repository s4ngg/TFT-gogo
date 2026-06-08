-- PR #168: 영웅 증강 관리자 입력 기능 추가
-- DeckCuration 엔티티에 hero_augments 컬럼 추가
-- ddl-auto=none 환경에서 수동 적용 필요

ALTER TABLE deck_curations
    ADD COLUMN hero_augments TEXT NULL
    COMMENT '영웅 증강 JSON (관리자 직접 입력 — Riot API 미제공) [{"championId":"tft17_jinx","championName":"징크스","augmentName":"화약 소녀"}, ...]';
