-- Local smoke-test seed data for Guide and PatchNotes.
-- Apply after guide-patchnote-local-schema.sql.
-- The seed uses patch version 17.3 to match the current frontend fallback sample set.

START TRANSACTION;

INSERT INTO guides (
    guide_type,
    target_key,
    name,
    summary,
    image_url,
    data_json,
    patch_version,
    sort_order,
    is_active
) VALUES
(
    'TRAIT',
    'TFT17_DarkStar',
    '다크 스타',
    '처치 관여 후 전투력을 키우는 후반 캐리형 시너지입니다.',
    'https://raw.communitydragon.org/latest/game/assets/ux/traiticons/trait_icon_17_darkstar.tft_set17.png',
    '{
        "type": "계열",
        "count": 6,
        "levels": ["2: 피해량 소폭 증가", "4: 처치 관여 시 추가 성장", "6: 핵심 캐리 집중 강화"],
        "tone": "gold",
        "champions": [
            {
                "name": "징크스",
                "cost": 4,
                "imageUrl": "https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png"
            }
        ],
        "tips": ["초반에는 2시너지로 버티고 8레벨부터 핵심 캐리를 찾습니다.", "피해량 아이템을 캐리에게 몰아주면 효율이 좋습니다."]
    }',
    '17.3',
    10,
    TRUE
),
(
    'ITEM',
    'TFT_Item_SpearOfShojin',
    '쇼진의 창',
    '스킬 회전이 중요한 AP 캐리에게 우선 고려하는 마나 아이템입니다.',
    'https://raw.communitydragon.org/latest/game/assets/maps/tft/icons/items/hexcore/tft_item_spearofshojin.tft_set13.png',
    '{
        "category": "마나",
        "avgPlace": "4.12",
        "pickRate": "18.4%",
        "top4": "55.2%",
        "winRate": "13.1%",
        "bestUsers": [
            {
                "name": "징크스",
                "cost": 4,
                "imageUrl": "https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png"
            }
        ],
        "combinations": [
            {
                "label": "스킬 순환",
                "note": "스킬을 자주 쓰는 캐리에게 우선 장착합니다.",
                "items": [
                    {
                        "name": "쇼진의 창",
                        "imageUrl": "https://raw.communitydragon.org/latest/game/assets/maps/tft/icons/items/hexcore/tft_item_spearofshojin.tft_set13.png"
                    }
                ]
            }
        ]
    }',
    '17.3',
    20,
    TRUE
),
(
    'AUGMENT',
    'TFT17_Augment_Guidebook',
    '가이드북',
    '초중반 방향성을 빠르게 정하고 안정적인 전환을 돕는 증강입니다.',
    NULL,
    '{
        "description": "조합 방향을 빠르게 확정해야 할 때 선택하기 좋은 범용 증강입니다.",
        "type": "범용",
        "reward": "조합 전환 보조",
        "tags": ["전환", "운영", "안정"],
        "tier": "A",
        "avgPlace": "4.20",
        "pickRate": "9.8%",
        "winRate": "11.7%"
    }',
    '17.3',
    30,
    TRUE
),
(
    'CHAMPION',
    'TFT17_Jinx',
    '징크스',
    '후방에서 지속 피해를 넣는 4코스트 원거리 캐리입니다.',
    'https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png',
    '{
        "cost": 4,
        "position": "후열",
        "role": "원거리 캐리",
        "traits": ["다크 스타", "저격수"],
        "bestItems": [
            {
                "name": "쇼진의 창",
                "imageUrl": "https://raw.communitydragon.org/latest/game/assets/maps/tft/icons/items/hexcore/tft_item_spearofshojin.tft_set13.png"
            },
            {
                "name": "구인수의 격노검",
                "imageUrl": "https://raw.communitydragon.org/latest/game/assets/maps/tft/icons/items/hexcore/tft_item_guinsoosrageblade.tft_set13.png"
            }
        ],
        "stats": {
            "hp": 850,
            "ad": 65,
            "armor": 35,
            "mr": 35,
            "attackSpeed": "0.80",
            "mana": "20/80",
            "range": 4
        }
    }',
    '17.3',
    40,
    TRUE
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    summary = VALUES(summary),
    image_url = VALUES(image_url),
    data_json = VALUES(data_json),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO patch_notes (
    version,
    title,
    summary,
    description,
    focus,
    image_url,
    published_at,
    is_current,
    highlights_json,
    is_active
) VALUES (
    '17.3',
    '17.3 패치노트',
    '캐리 챔피언, 핵심 시너지, 아이템 효율을 함께 조정한 로컬 smoke test 패치입니다.',
    '로컬 DB와 공개 패치노트 API 연결을 확인하기 위한 큐레이션 샘플입니다. 실제 운영 패치 내용이 아니라 서버 기동 검증용 데이터입니다.',
    '캐리 밸런스와 조합 전환 안정성 점검',
    'https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png',
    '2026-06-02 09:00:00',
    TRUE,
    '["현재 패치 1건 노출 확인", "카테고리/타입/영향도 필터 확인", "Riot API 키 없이 공개 API smoke test"]',
    TRUE
)
ON DUPLICATE KEY UPDATE
    id = LAST_INSERT_ID(id),
    title = VALUES(title),
    summary = VALUES(summary),
    description = VALUES(description),
    focus = VALUES(focus),
    image_url = VALUES(image_url),
    published_at = VALUES(published_at),
    is_current = VALUES(is_current),
    highlights_json = VALUES(highlights_json),
    is_active = VALUES(is_active),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

SET @patch_note_173_id = LAST_INSERT_ID();

DELETE FROM patch_changes
WHERE patch_note_id = @patch_note_173_id
  AND target_key IN (
      'TFT17_Jinx',
      'TFT17_DarkStar',
      'TFT_Item_SpearOfShojin',
      'TFT17_Augment_Guidebook',
      'system_shop_refresh'
  );

INSERT INTO patch_changes (
    patch_note_id,
    category,
    change_type,
    impact,
    target_key,
    target_name,
    summary,
    before_value,
    after_value,
    image_url,
    tags_json,
    sort_order,
    is_active
) VALUES
(
    @patch_note_173_id,
    'CHAMPION',
    'BUFF',
    'HIGH',
    'TFT17_Jinx',
    '징크스',
    '스킬 피해량이 증가해 4코스트 캐리 선택지로 더 안정적입니다.',
    '스킬 피해량 280/420/900',
    '스킬 피해량 300/450/950',
    'https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png',
    '["champion", "buff", "carry"]',
    10,
    TRUE
),
(
    @patch_note_173_id,
    'TRAIT',
    'NERF',
    'MEDIUM',
    'TFT17_DarkStar',
    '다크 스타',
    '6시너지의 후반 폭발력이 소폭 낮아져 아이템 의존도가 높아졌습니다.',
    '6시너지 추가 피해 18%',
    '6시너지 추가 피해 15%',
    'https://raw.communitydragon.org/latest/game/assets/ux/traiticons/trait_icon_17_darkstar.tft_set17.png',
    '["trait", "nerf", "late-game"]',
    20,
    TRUE
),
(
    @patch_note_173_id,
    'ITEM',
    'ADJUST',
    'LOW',
    'TFT_Item_SpearOfShojin',
    '쇼진의 창',
    '마나 회복량은 유지하되 주문력 보정이 조정되어 특정 캐리 편중을 줄였습니다.',
    '주문력 15',
    '주문력 10, 기본 마나 회복 유지',
    'https://raw.communitydragon.org/latest/game/assets/maps/tft/icons/items/hexcore/tft_item_spearofshojin.tft_set13.png',
    '["item", "adjust", "mana"]',
    30,
    TRUE
),
(
    @patch_note_173_id,
    'AUGMENT',
    'NEW',
    'HIGH',
    'TFT17_Augment_Guidebook',
    '가이드북',
    '조합 방향을 제시하는 신규 증강이 추가되어 초중반 전환 선택지가 늘었습니다.',
    NULL,
    '신규 증강 추가',
    NULL,
    '["augment", "new", "transition"]',
    40,
    TRUE
),
(
    @patch_note_173_id,
    'SYSTEM',
    'ADJUST',
    'MEDIUM',
    'system_shop_refresh',
    '상점 갱신',
    '중반 레벨 구간의 상점 기대값을 조정해 리롤과 레벨업 선택의 균형을 맞췄습니다.',
    '7레벨 4코스트 등장 확률 15%',
    '7레벨 4코스트 등장 확률 14%',
    NULL,
    '["system", "adjust", "shop"]',
    50,
    TRUE
);

COMMIT;
