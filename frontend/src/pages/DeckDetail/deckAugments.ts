/**
 * 덱별 추천 증강체 목록
 * tier: prismatic(프리즈매틱) > gold(금빛) > silver(은빛)
 * ※ 실제 서비스 시 백엔드 API로 교체 예정
 */

export interface AugmentRec {
  name: string
  tier: 'prismatic' | 'gold' | 'silver'
  description: string
}

export const DECK_AUGMENTS: Record<number, AugmentRec[]> = {
  1: [ // 선봉대 벡스
    { name: '선봉대의 심장', tier: 'prismatic', description: '선봉대 시너지 인원 +1, 선봉대 유닛 공격력 · 체력 +30%' },
    { name: '벡스: 빠른 절망', tier: 'gold', description: '벡스 스킬 발동마다 주변 적에게 공포 추가 피해' },
    { name: '근거리 전문가', tier: 'gold', description: '근접 유닛 공격력 +18%, 방어력 +10' },
    { name: '방어의 의지', tier: 'silver', description: '라운드 시작 시 앞줄 유닛에게 최대 체력 20% 보호막' },
  ],
  2: [ // 6암흑의 별 진
    { name: '암흑의 심장', tier: 'prismatic', description: '암흑의 별 시너지 인원 +1, 암흑의 별 유닛 피해 +25%' },
    { name: '진: 커튼 콜 강화', tier: 'gold', description: '진 4번째 공격 피해 +50%, 사정거리 +2' },
    { name: '마법의 대포', tier: 'gold', description: '스킬 피해 +20%, 치명타 확률 +15%' },
    { name: '저격수의 준비', tier: 'silver', description: '원거리 유닛 공격 속도 +15%' },
  ],
  3: [ // 정령족 코르키 백류
    { name: '정령족의 심장', tier: 'prismatic', description: '정령족 시너지 인원 +1, 정령족 유닛 마력 충전 속도 +25%' },
    { name: '코르키: 폭발 패키지', tier: 'gold', description: '코르키 스킬 폭발 범위 +1, 피해 +35%' },
    { name: '마법사 단체교습', tier: 'gold', description: '팀 전체 마법 피해 +15%' },
    { name: '별돌보미의 가르침', tier: 'silver', description: '스킬 사용 시 가장 낮은 체력 아군 최대 체력 15% 회복' },
  ],
  4: [ // 습격자 마스터 이
    { name: '습격자의 심장', tier: 'prismatic', description: '습격자 시너지 인원 +1, 습격자 치명타 피해 +30%' },
    { name: '마스터 이: 알파 강화', tier: 'gold', description: '알파 스트라이크 추가 타겟 +1, 치명타 시 쿨타임 초기화' },
    { name: '암살자 학교', tier: 'gold', description: '전투 시작 시 점프 유닛 치명타 확률 +20%' },
    { name: '그림자의 칼날', tier: 'silver', description: '근접 유닛 방어 관통 +15' },
  ],
  5: [ // 별돌보미 룰루
    { name: '별돌보미 전설', tier: 'prismatic', description: '별돌보미 시너지 인원 +1, 별돌보미 유닛 스킬 강화 효과 +50%' },
    { name: '룰루: 요정의 마법', tier: 'gold', description: '룰루 스킬 버프 지속 시간 +50%, 아군 피해 +20%' },
    { name: '지원 전문', tier: 'gold', description: '힐 · 보호막 효과 +20%' },
    { name: '별의 축복', tier: 'silver', description: '전투 시작 시 팀 전체 최대 체력 10% 보호막' },
  ],
  6: [ // 8요새 럼블
    { name: '요새의 심장', tier: 'prismatic', description: '요새 시너지 인원 +1, 요새 유닛 방어력 +40' },
    { name: '아우솔: 별의 폭발', tier: 'gold', description: '아우솔 궁극기 피해 범위 +2, 피해 +30%' },
    { name: '철벽 방어', tier: 'gold', description: '앞줄 유닛 최대 체력 +20%, 방어력 +15' },
    { name: '철갑 훈련', tier: 'silver', description: '아군 유닛 사망 시 근처 아군 방어력 임시 +15' },
  ],
  7: [ // 4그림자 암살자
    { name: '그림자의 심장', tier: 'prismatic', description: '그림자 시너지 인원 +1, 그림자 유닛 피해 +25%' },
    { name: '제드: 그림자 복사', tier: 'gold', description: '제드 그림자 개수 +1, 그림자 피해 +20%' },
    { name: '암살자 연합', tier: 'gold', description: '암살자 유닛 치명타 피해 +40%' },
    { name: '야습 전문', tier: 'silver', description: '전투 시작 3초간 암살자 유닛 회피율 +20%' },
  ],
  8: [ // 발명의 대가 하이머딩거
    { name: '발명가의 심장', tier: 'prismatic', description: '발명가 시너지 인원 +1, 발명품 소환 속도 +50%' },
    { name: '빅토르: 진화 가속', tier: 'gold', description: '빅토르 스킬 업그레이드 단계 +1로 시작' },
    { name: '기계 공학', tier: 'gold', description: '소환 유닛 체력 · 공격력 +25%' },
    { name: '과부하', tier: 'silver', description: '스킬 사용 시 10% 확률로 주변 적에게 전기 충격' },
  ],
  9: [ // 4저격수 징크스
    { name: '저격수의 심장', tier: 'prismatic', description: '저격수 시너지 인원 +1, 원거리 유닛 공격력 +25%' },
    { name: '징크스: 발사 축제', tier: 'gold', description: '징크스 치명타 발생 시 로켓 추가 발사' },
    { name: '총잡이 훈련', tier: 'gold', description: '원거리 유닛 사정거리 +1, 공격 속도 +10%' },
    { name: '탄약 보급', tier: 'silver', description: '매 3라운드마다 원거리 유닛 공격력 임시 +20%' },
  ],
  10: [ // 복제자 빅토르
    { name: '복제자의 심장', tier: 'prismatic', description: '복제자 시너지 인원 +1, 복제 유닛 스탯 +30%' },
    { name: '빅토르: 완전 진화', tier: 'gold', description: '빅토르 진화 완료 시 팀 전체 공격력 · 방어력 +15%' },
    { name: '마력 증폭', tier: 'gold', description: '마법 피해 유닛 스킬 충전 속도 +20%' },
    { name: '기계의 부품', tier: 'silver', description: '전투 시작 시 빅토르 추가 방어력 +30 획득' },
  ],
  11: [ // 우주그루브 소나
    { name: '우주그루브의 심장', tier: 'prismatic', description: '우주그루브 시너지 인원 +1, 주기 피해 · 치유 효과 +40%' },
    { name: '소나: 크레센도', tier: 'gold', description: '소나 궁극기 범위 +2, 기절 지속 시간 +0.5초' },
    { name: '리듬의 축복', tier: 'gold', description: '스킬 사용마다 아군 공격 속도 +8% 중첩 (최대 5중첩)' },
    { name: '음악의 힘', tier: 'silver', description: '라운드 시작 시 팀 전체 마법 저항력 +15' },
  ],
  12: [ // 운명술사 트페
    { name: '운명술사의 심장', tier: 'prismatic', description: '운명술사 시너지 인원 +1, 트페 스킬 효과 +50%' },
    { name: '트페: 운명의 카드', tier: 'gold', description: '트페 카드 발사 속도 +30%, 골드 카드 확률 +25%' },
    { name: '점술사 교습', tier: 'gold', description: '운명술사 유닛 스킬 피해 +25%' },
    { name: '행운의 패', tier: 'silver', description: '라운드 승리 시 추가 골드 +1' },
  ],
  13: [ // 6도전자 자야
    { name: '도전자의 심장', tier: 'prismatic', description: '도전자 시너지 인원 +1, 도전자 공격 속도 보너스 +50%' },
    { name: '자야: 질풍 날개', tier: 'gold', description: '자야 스킬 피해 +40%, 회피 +15%' },
    { name: '전투의 흥분', tier: 'gold', description: '킬 발생 시 공격 속도 +8% 중첩 (최대 5중첩)' },
    { name: '도전 정신', tier: 'silver', description: '전투 시작 시 도전자 유닛 공격 속도 +10%' },
  ],
  14: [ // 길잡이 나미
    { name: '길잡이의 심장', tier: 'prismatic', description: '길잡이 시너지 인원 +1, 길잡이 유닛 힐 · 버프 효과 +35%' },
    { name: '나미: 조류 역전', tier: 'gold', description: '나미 스킬 치유량 +40%, 범위 +1' },
    { name: '지원의 정석', tier: 'gold', description: '힐 · 보호막 효과 +25%, 지원 유닛 마력 충전 속도 +15%' },
    { name: '생명의 물결', tier: 'silver', description: '매 3초마다 팀 전체 최대 체력 5% 회복' },
  ],
}
