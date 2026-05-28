import { useEffect, useMemo, useState } from 'react'
import type { LucideIcon } from 'lucide-react'
import {
  AlertTriangle,
  ArrowUpRight,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Filter,
  History,
  Loader2,
  RefreshCw,
  Search,
  Shield,
  Sparkles,
  Swords,
  Trophy,
  Wand2,
  Zap,
} from 'lucide-react'
import { communityDragonAssetUrl } from '../../api/communityDragonAssets'
import {
  CHANGE_CATEGORIES,
  CHANGE_TYPE_FILTERS,
  PATCH_CATEGORIES,
  type ChangeCategory,
  type ChangeType,
  type ChangeTypeFilter,
  type ImpactLevel,
  type PatchCategory,
  type PatchChange,
  type PatchNoteDetail,
  type PatchNoteSummary,
} from '../../api/patchNotes'
import { AppLayout } from '../../components/layout'
import { usePatchNotes } from '../../hooks/usePatchNotes'
import styles from './PatchNotes.module.css'

const PATCH_PAGE_SIZE = 5
const SAMPLE_PAGE_COUNT = 7
const PAGE_NUMBER_WINDOW = 5

interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

const CATEGORY_ICON: Record<ChangeCategory, LucideIcon> = {
  챔피언: Swords,
  시너지: Shield,
  아이템: Wand2,
  증강체: Sparkles,
  시스템: Zap,
}

const CHANGE_TYPE_CLASS: Record<ChangeType, string> = {
  상향: styles.buff,
  하향: styles.nerf,
  조정: styles.adjust,
  신규: styles.new,
}

const IMPACT_CLASS: Record<ImpactLevel, string> = {
  높음: styles.highImpact,
  중간: styles.midImpact,
  낮음: styles.lowImpact,
}

const PATCH_FALLBACK_IMAGE = '/assets/emblems/patch-meta-emblem-pink.png'

const categoryImageUrl: Record<ChangeCategory, string> = {
  챔피언: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  시너지: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  아이템: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex'),
  증강체: communityDragonAssetUrl('ASSETS/UX/TFT/Augments/Augment_Silver.tex'),
  시스템: PATCH_FALLBACK_IMAGE,
}

const targetImageUrl: Record<string, string> = {
  아펠리오스: communityDragonAssetUrl('ASSETS/Characters/TFT17_Aphelios/Skins/Base/Images/TFT17_Aphelios_splash_tile_1.TFT_Set17.tex'),
  세주아니: communityDragonAssetUrl('ASSETS/Characters/TFT17_Sejuani/Skins/Base/Images/TFT17_Sejuani_splash_tile_1.TFT_Set17.tex'),
  럭스: communityDragonAssetUrl('ASSETS/Characters/TFT17_Lux/Skins/Base/Images/TFT17_Lux_splash_tile_1.TFT_Set17.tex'),
  카이사: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  오른: communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  학살자: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
  마법사: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Fateweaver.TFT_Set17.tex'),
  감시자: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
  전략가: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex'),
  결투가: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  '구인수의 격노검': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex'),
  '워모그의 갑옷': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
  '쇼진의 창': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  '이온 충격기': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_IonicSpark.TFT_Set13.tex'),
  '거인 학살자': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GiantSlayer.TFT_Set13.tex'),
}

const BASE_PATCH_CHANGES: PatchChange[] = [
  {
    id: 1,
    category: '챔피언',
    target: '아펠리오스',
    type: '상향',
    impact: '높음',
    summary: '스킬 피해량과 후반 캐리 안정성이 함께 올라갔습니다.',
    before: '스킬 피해량 320 / 480 / 760',
    after: '스킬 피해량 345 / 520 / 820',
    tags: ['후반 캐리', 'AD 조합', '아이템 효율'],
  },
  {
    id: 2,
    category: '챔피언',
    target: '세주아니',
    type: '상향',
    impact: '중간',
    summary: '탱킹 시간이 늘어나 전방 라인 유지력이 좋아졌습니다.',
    before: '보호막 380 / 480 / 620',
    after: '보호막 420 / 520 / 660',
    tags: ['탱커', '전방 라인', '군중 제어'],
  },
  {
    id: 3,
    category: '챔피언',
    target: '럭스',
    type: '하향',
    impact: '중간',
    summary: '초반 스킬 피해량이 낮아져 연승 출발 안정성이 줄었습니다.',
    before: '스킬 피해량 240 / 360 / 540',
    after: '스킬 피해량 220 / 340 / 520',
    tags: ['초반 AP', '연승', '저코스트'],
  },
  {
    id: 4,
    category: '챔피언',
    target: '카이사',
    type: '조정',
    impact: '높음',
    summary: '공격 속도 계수는 낮아지고 스킬 마무리 피해가 보강됐습니다.',
    before: '공속 계수 0.82, 마무리 피해 180%',
    after: '공속 계수 0.78, 마무리 피해 205%',
    tags: ['AD 캐리', '마무리 딜', '후반 전환'],
  },
  {
    id: 5,
    category: '챔피언',
    target: '오른',
    type: '신규',
    impact: '낮음',
    summary: '특정 조합에서 방어형 유틸 선택지로 테스트됩니다.',
    before: '이전 패치 없음',
    after: '전방 유닛 방어력 보조 효과 추가',
    tags: ['탱커', '신규', '유틸'],
  },
  {
    id: 6,
    category: '시너지',
    target: '학살자',
    type: '조정',
    impact: '높음',
    summary: '초반 단계는 낮아지고 6시너지 이상 보상이 강해졌습니다.',
    before: '2/4/6단계 피해 증폭 12% / 24% / 40%',
    after: '2/4/6단계 피해 증폭 10% / 24% / 45%',
    tags: ['고점 강화', '운영 전환', '6시너지'],
  },
  {
    id: 7,
    category: '시너지',
    target: '마법사',
    type: '상향',
    impact: '높음',
    summary: '4시너지 구간의 주문력 보상이 올라 중반 전환 가치가 커졌습니다.',
    before: '주문력 20 / 45 / 80',
    after: '주문력 20 / 50 / 82',
    tags: ['AP 조합', '중반 운영', '스킬 캐리'],
  },
  {
    id: 8,
    category: '시너지',
    target: '감시자',
    type: '상향',
    impact: '중간',
    summary: '전방 유지력이 올라 장기전 조합의 기본 안정성이 개선됐습니다.',
    before: '방어력/마법 저항력 18 / 35 / 60',
    after: '방어력/마법 저항력 20 / 38 / 64',
    tags: ['탱커', '장기전', '전방 라인'],
  },
  {
    id: 9,
    category: '시너지',
    target: '전략가',
    type: '하향',
    impact: '중간',
    summary: '후열 보호막 수치가 낮아져 초반 안정성이 완화됐습니다.',
    before: '보호막 180 / 300 / 520',
    after: '보호막 160 / 285 / 500',
    tags: ['후열', '보호막', '초반 안정성'],
  },
  {
    id: 10,
    category: '시너지',
    target: '결투가',
    type: '조정',
    impact: '낮음',
    summary: '저단계 공격 속도는 보강되고 고단계 보상은 소폭 낮아졌습니다.',
    before: '공격 속도 5% / 9% / 16%',
    after: '공격 속도 6% / 9% / 15%',
    tags: ['공속', '저단계 보강', '고단계 조정'],
  },
  {
    id: 11,
    category: '아이템',
    target: '구인수의 격노검',
    type: '하향',
    impact: '중간',
    summary: '공격 속도 누적 속도가 줄어 장기전 의존도가 조금 낮아졌습니다.',
    before: '공격 시 공격 속도 +5%',
    after: '공격 시 공격 속도 +4.5%',
    tags: ['공속 캐리', '장기전', '밸런스'],
  },
  {
    id: 12,
    category: '아이템',
    target: '워모그의 갑옷',
    type: '상향',
    impact: '낮음',
    summary: '최대 체력 증가량이 올라 순수 탱커 아이템 선택지가 넓어졌습니다.',
    before: '추가 체력 +700',
    after: '추가 체력 +750',
    tags: ['탱커', '체력', '방어 아이템'],
  },
  {
    id: 13,
    category: '아이템',
    target: '쇼진의 창',
    type: '상향',
    impact: '높음',
    summary: '스킬 사이클이 빨라져 AP 캐리와 서포터 모두 선택 가치가 생겼습니다.',
    before: '기본 마나 회복 +5',
    after: '기본 마나 회복 +6',
    tags: ['마나', 'AP 캐리', '서포터'],
  },
  {
    id: 14,
    category: '아이템',
    target: '이온 충격기',
    type: '조정',
    impact: '중간',
    summary: '마법 저항력 감소 범위가 안정화되고 피해량은 소폭 조정됐습니다.',
    before: '범위 2칸, 피해량 175%',
    after: '범위 2칸 고정, 피해량 160%',
    tags: ['마법 피해', '전방', '저항 감소'],
  },
  {
    id: 15,
    category: '아이템',
    target: '거인 학살자',
    type: '하향',
    impact: '낮음',
    summary: '고체력 대상 추가 피해가 줄어 탱커 대응력이 완화됐습니다.',
    before: '추가 피해량 +25%',
    after: '추가 피해량 +22%',
    tags: ['딜러', '탱커 대응', 'AD 조합'],
  },
  {
    id: 16,
    category: '증강체',
    target: '작지만 치명적인',
    type: '하향',
    impact: '중간',
    summary: '저코스트 리롤 조합의 초중반 압박력이 완화됐습니다.',
    before: '1~2코스트 유닛 공격 속도 +30%',
    after: '1~2코스트 유닛 공격 속도 +24%',
    tags: ['리롤', '저코스트', '초반 템포'],
  },
  {
    id: 17,
    category: '증강체',
    target: '찬란한 유물고',
    type: '신규',
    impact: '중간',
    summary: '찬란한 아이템 선택지가 추가되어 후반 아이템 고점이 늘었습니다.',
    before: '이전 패치 없음',
    after: '찬란한 아이템 4개 중 1개 선택',
    tags: ['신규', '찬란한 아이템', '후반 고점'],
  },
  {
    id: 18,
    category: '증강체',
    target: '프리즘 티켓',
    type: '조정',
    impact: '높음',
    summary: '무료 상점 갱신 빈도가 낮아져 3성 고점 접근 속도가 조정됐습니다.',
    before: '상점 갱신 확률 45%',
    after: '상점 갱신 확률 38%',
    tags: ['리롤', '3성', '상점'],
  },
  {
    id: 19,
    category: '증강체',
    target: '사이버네틱 덩치',
    type: '상향',
    impact: '중간',
    summary: '아이템을 가진 유닛의 체력 보상이 올라 전방 분산 배치가 좋아졌습니다.',
    before: '체력 +180 / +260 / +330',
    after: '체력 +200 / +280 / +360',
    tags: ['체력', '아이템 분산', '전방'],
  },
  {
    id: 20,
    category: '증강체',
    target: '영웅 꾸러미',
    type: '신규',
    impact: '낮음',
    summary: '중반 핵심 유닛을 찾는 보조 증강체가 실험적으로 추가됐습니다.',
    before: '이전 패치 없음',
    after: '2~4코스트 챔피언 꾸러미 선택',
    tags: ['신규', '챔피언 보상', '중반'],
  },
  {
    id: 21,
    category: '시스템',
    target: '공동 선택 라운드',
    type: '조정',
    impact: '낮음',
    summary: '라운드별 등장 아이템 분포가 일부 조정됐습니다.',
    before: '초반 방어 아이템 등장률 높음',
    after: '공격/방어 아이템 등장률 균형 조정',
    tags: ['시스템', '아이템 분포', '운영'],
  },
  {
    id: 22,
    category: '시스템',
    target: 'PvE 보상',
    type: '상향',
    impact: '중간',
    summary: '초반 보상 편차가 줄어 안정적인 운영 선택지가 늘었습니다.',
    before: '골드/아이템 보상 편차 큼',
    after: '최소 보상 기준 상향',
    tags: ['보상', '초반 운영', '안정성'],
  },
  {
    id: 23,
    category: '시스템',
    target: '스테이지 피해량',
    type: '하향',
    impact: '높음',
    summary: '중반 플레이어 피해량이 낮아져 전환 조합을 준비할 시간이 늘었습니다.',
    before: '4스테이지 기본 피해량 +7',
    after: '4스테이지 기본 피해량 +6',
    tags: ['체력 관리', '중반', '전환'],
  },
  {
    id: 24,
    category: '시스템',
    target: '상점 확률',
    type: '조정',
    impact: '중간',
    summary: '7레벨 4코스트 등장률이 소폭 낮아지고 8레벨 가치가 올라갔습니다.',
    before: '7레벨 4코스트 확률 15%',
    after: '7레벨 4코스트 확률 14%',
    tags: ['레벨업', '4코스트', '운영'],
  },
  {
    id: 25,
    category: '시스템',
    target: '증강체 새로고침',
    type: '상향',
    impact: '낮음',
    summary: '첫 증강체 선택의 보정 규칙이 완화되어 조합 시작이 쉬워졌습니다.',
    before: '동일 계열 보정 낮음',
    after: '동일 계열 보정 소폭 상향',
    tags: ['증강체', '선택지', '초반'],
  },
]

const PATCH_HISTORY: PatchNoteSummary[] = [
  {
    version: '17.3',
    date: '2026.05.26',
    title: 'AP 전환 조합 보강',
    status: '현재',
    focus: '마법사, 아펠리오스, 리롤 증강체 조정',
    description: 'AP 전환 조합과 후반 캐리 라인이 다시 올라오는 패치입니다.',
    highlights: [
      '마법사와 AP 캐리 조합의 중반 전환 가치가 상승했습니다.',
      '저코스트 리롤 증강체는 초반 압박력이 완화됐습니다.',
      '공속 기반 장기전 캐리는 아이템 의존도를 더 확인해야 합니다.',
    ],
    imageUrl: communityDragonAssetUrl('ASSETS/Characters/TFT17_Aphelios/Skins/Base/Images/TFT17_Aphelios_splash_tile_1.TFT_Set17.tex'),
  },
  {
    version: '17.2',
    date: '2026.05.12',
    title: '리롤 덱 속도 조절',
    status: '이전',
    focus: '저코스트 캐리, 초반 골드 증강체 하향',
    description: '초반 리롤 덱의 템포를 낮추고 운영 덱으로 넘어갈 시간을 만든 패치입니다.',
    highlights: [
      '1~2코스트 리롤 조합의 초반 압박력이 낮아졌습니다.',
      '연승 운영보다 체력 보존 후 7레벨 전환 가치가 커졌습니다.',
      '골드 증강체 선택 시 후반 전환 플랜을 함께 봐야 합니다.',
    ],
    imageUrl: communityDragonAssetUrl('ASSETS/Characters/TFT17_Lux/Skins/Base/Images/TFT17_Lux_splash_tile_1.TFT_Set17.tex'),
  },
  {
    version: '17.1',
    date: '2026.04.29',
    title: '세트 오픈 밸런스',
    status: '이전',
    focus: '신규 시너지 안정화, 일부 5코스트 조정',
    description: '세트 초반 과도하게 튀는 신규 시너지와 고코스트 유닛을 안정화한 패치입니다.',
    highlights: [
      '신규 시너지의 초반 활성 보상이 완만하게 조정됐습니다.',
      '일부 5코스트 캐리의 후반 영향력이 재분배됐습니다.',
      '아이템 선택 폭을 넓히기 위한 기초 밸런스가 정리됐습니다.',
    ],
    imageUrl: communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  },
  {
    version: '16.9',
    date: '2026.04.15',
    title: '시즌 종료 메타 정리',
    status: '이전',
    focus: '상위권 고정 조합 완화',
    description: '시즌 종료 전 상위권에서 고정되던 조합의 독주를 완화한 패치입니다.',
    highlights: [
      '상위권 고정 조합의 핵심 캐리 효율이 낮아졌습니다.',
      '중위권 조합의 순방 가능성이 소폭 올라갔습니다.',
      '시즌 말 랭크 환경에서 조합 선택지가 늘었습니다.',
    ],
    imageUrl: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  },
]

function getBaseTarget(target: string) {
  return target.replace(/\s샘플\s\d+$/, '')
}

function getChangeImageUrl(change: PatchChange) {
  return change.imageUrl ?? targetImageUrl[getBaseTarget(change.target)] ?? categoryImageUrl[change.category]
}

function buildVersionedChange(change: PatchChange, version: string, patchIndex: number): PatchChange {
  if (version === PATCH_HISTORY[0].version) {
    return change
  }

  return {
    ...change,
    id: patchIndex * 1000 + change.id,
    summary: `${version} 패치 기준 ${change.summary}`,
    before: `${change.before} · ${version} 이전 기준`,
    after: `${change.after} · ${version} 적용 기준`,
    tags: [version, ...change.tags.slice(0, 2)],
  }
}

function expandPatchSamples(changes: PatchChange[], patch: PatchNoteSummary) {
  const targetCount = PATCH_PAGE_SIZE * SAMPLE_PAGE_COUNT
  const patchIndex = PATCH_HISTORY.findIndex((historyItem) => historyItem.version === patch.version)

  return CHANGE_CATEGORIES.flatMap((category) => {
    const categoryChanges = changes.filter((change) => change.category === category)

    return Array.from({ length: targetCount }, (_, index) => {
      const source = buildVersionedChange(categoryChanges[index % categoryChanges.length], patch.version, patchIndex)
      const sampleRound = Math.floor(index / categoryChanges.length) + 1
      const isOriginal = sampleRound === 1
      const id = patchIndex * 1000 + CHANGE_CATEGORIES.indexOf(category) * targetCount + index + 1

      return {
        ...source,
        id,
        target: isOriginal ? source.target : `${source.target} 샘플 ${sampleRound}`,
        summary: isOriginal ? source.summary : `${source.summary} ${sampleRound}차 샘플 기준으로 표시했습니다.`,
        before: isOriginal ? source.before : `${source.before} · 샘플 ${sampleRound}`,
        after: isOriginal ? source.after : `${source.after} · 샘플 ${sampleRound}`,
        tags: isOriginal ? source.tags : [...source.tags.slice(0, 2), `샘플 ${sampleRound}`],
      }
    })
  })
}

const PATCH_NOTES_FALLBACK: PatchNoteDetail[] = PATCH_HISTORY.map((patch) => ({
  ...patch,
  changes: expandPatchSamples(BASE_PATCH_CHANGES, patch),
}))

function getCategoryCount(category: PatchCategory, changes: PatchChange[]) {
  if (category === '전체') return changes.length
  return changes.filter((change) => change.category === category).length
}

function getTotalPages(totalItems: number) {
  return Math.max(1, Math.ceil(totalItems / PATCH_PAGE_SIZE))
}

function getPageItems<T>(items: T[], page: number) {
  const startIndex = (page - 1) * PATCH_PAGE_SIZE
  return items.slice(startIndex, startIndex + PATCH_PAGE_SIZE)
}

function getPaginationWindow(currentPage: number, totalPages: number) {
  const windowStart = Math.floor((currentPage - 1) / PAGE_NUMBER_WINDOW) * PAGE_NUMBER_WINDOW + 1
  const windowEnd = Math.min(totalPages, windowStart + PAGE_NUMBER_WINDOW - 1)

  return Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)
}

function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const pages = getPaginationWindow(currentPage, totalPages)
  const windowStart = pages[0]
  const windowEnd = pages[pages.length - 1]

  return (
    <nav className={styles.pagination} aria-label="패치 변경사항 페이지">
      {windowStart > 1 && (
        <button type="button" className={styles.pageMore} onClick={() => onPageChange(windowStart - 1)}>
          이전
        </button>
      )}

      {pages.map((page) => (
        <button
          key={page}
          type="button"
          className={currentPage === page ? styles.activePage : undefined}
          onClick={() => onPageChange(page)}
          aria-current={currentPage === page ? 'page' : undefined}
        >
          {page}
        </button>
      ))}

      {windowEnd < totalPages && (
        <button type="button" className={styles.pageMore} onClick={() => onPageChange(windowEnd + 1)}>
          더보기
        </button>
      )}
    </nav>
  )
}

function PatchStatusBanner({
  isFallbackData,
  isFetching,
  onRetry,
}: {
  isFallbackData: boolean
  isFetching: boolean
  onRetry: () => void
}) {
  if (!isFetching && !isFallbackData) return null

  return (
    <div
      aria-live="polite"
      className={`${styles.statusBanner} ${isFetching ? styles.statusLoading : styles.statusFallback}`}
    >
      <span className={styles.statusIcon}>
        {isFetching ? <Loader2 size={16} /> : <AlertTriangle size={16} />}
      </span>
      <div>
        <strong>{isFetching ? '패치노트 데이터를 불러오는 중입니다.' : '샘플 패치노트로 표시 중입니다.'}</strong>
        <p>
          {isFetching
            ? '최신 패치노트 응답을 확인하는 동안 현재 데이터를 유지합니다.'
            : '패치노트 API 응답을 가져오지 못해 준비된 샘플 데이터를 보여주고 있습니다.'}
        </p>
      </div>
      {!isFetching && (
        <button onClick={onRetry} type="button">
          <RefreshCw size={14} />
          다시 시도
        </button>
      )}
    </div>
  )
}

function PatchNotes() {
  const {
    isFallbackData,
    isFetching,
    patchNotes: patchHistory,
    refetchPatchNotes,
    selectedPatch: selectedPatchFromQuery,
    selectedPatchVersion,
    setSelectedPatchVersion,
  } = usePatchNotes({ fallbackData: PATCH_NOTES_FALLBACK })
  const [activeCategory, setActiveCategory] = useState<PatchCategory>('전체')
  const [activeChangeType, setActiveChangeType] = useState<ChangeTypeFilter>('전체 변경')
  const [highImpactOnly, setHighImpactOnly] = useState(false)
  const [expandedChangeIds, setExpandedChangeIds] = useState<number[]>([])
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const selectedPatch = selectedPatchFromQuery ?? PATCH_NOTES_FALLBACK[0]
  const patchChanges = selectedPatch.changes
  const highImpactCount = patchChanges.filter((change) => change.impact === '높음').length
  const buffCount = patchChanges.filter((change) => change.type === '상향').length
  const nerfCount = patchChanges.filter((change) => change.type === '하향').length

  const filteredChanges = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return patchChanges.filter((change) => {
      const matchesCategory = activeCategory === '전체' || change.category === activeCategory
      const matchesType = activeChangeType === '전체 변경' || change.type === activeChangeType
      const matchesImpact = !highImpactOnly || change.impact === '높음'
      const searchableText = [change.target, change.summary, change.category, change.type, ...change.tags].join(' ').toLowerCase()
      const matchesQuery = !normalizedQuery || searchableText.includes(normalizedQuery)

      return matchesCategory && matchesType && matchesImpact && matchesQuery
    })
  }, [activeCategory, activeChangeType, highImpactOnly, patchChanges, query])

  const totalPages = getTotalPages(filteredChanges.length)
  const safePage = Math.min(currentPage, totalPages)
  const pagedChanges = useMemo(() => getPageItems(filteredChanges, safePage), [filteredChanges, safePage])

  const toggleExpandedChange = (id: number) => {
    setExpandedChangeIds((currentIds) => (
      currentIds.includes(id)
        ? currentIds.filter((currentId) => currentId !== id)
        : [...currentIds, id]
    ))
  }

  useEffect(() => {
    setCurrentPage(1)
    setExpandedChangeIds([])
  }, [activeCategory, activeChangeType, highImpactOnly, query, selectedPatchVersion])

  return (
    <AppLayout>
      <div className={styles.page}>
        <header className={styles.hero}>
          <div className={styles.heroCopy}>
            <span className={styles.kicker}>
              <CalendarDays size={16} />
              {selectedPatch.version} 패치 노트
            </span>
            <h1>패치 노트</h1>
            <p>{selectedPatch.description}</p>
            <div className={styles.heroMeta}>
              <span>적용일 {selectedPatch.date}</span>
              <span>변경 {patchChanges.length}건</span>
              <span>핵심 영향 {highImpactCount}건</span>
            </div>
          </div>

          <aside className={styles.releaseCard} aria-label="현재 패치 요약">
            <div className={styles.releaseArt} aria-hidden="true">
              <img
                src={selectedPatch.imageUrl}
                alt=""
                onError={(event) => {
                  event.currentTarget.src = PATCH_FALLBACK_IMAGE
                }}
              />
            </div>
            <div>
              <span className={styles.releaseLabel}>{selectedPatch.status === '현재' ? '현재 버전' : '선택한 버전'}</span>
              <strong>v{selectedPatch.version}</strong>
              <p>{selectedPatch.focus}</p>
            </div>
          </aside>
        </header>

        <PatchStatusBanner
          isFallbackData={isFallbackData}
          isFetching={isFetching}
          onRetry={() => {
            void refetchPatchNotes()
          }}
        />

        <section className={styles.summaryGrid} aria-label="패치 핵심 지표">
          <article className={styles.summaryCard}>
            <span className={styles.summaryIcon}>
              <ArrowUpRight size={18} />
            </span>
            <div>
              <strong>{buffCount}</strong>
              <p>상향 항목</p>
            </div>
          </article>
          <article className={styles.summaryCard}>
            <span className={`${styles.summaryIcon} ${styles.warnIcon}`}>
              <AlertTriangle size={18} />
            </span>
            <div>
              <strong>{nerfCount}</strong>
              <p>하향 항목</p>
            </div>
          </article>
          <article className={styles.summaryCard}>
            <span className={`${styles.summaryIcon} ${styles.goldIcon}`}>
              <Trophy size={18} />
            </span>
            <div>
              <strong>AP</strong>
              <p>주요 상승 메타</p>
            </div>
          </article>
          <article className={styles.summaryCard}>
            <span className={`${styles.summaryIcon} ${styles.blueIcon}`}>
              <Clock3 size={18} />
            </span>
            <div>
              <strong>중반</strong>
              <p>운영 전환 구간</p>
            </div>
          </article>
        </section>

        <div className={styles.contentGrid}>
          <section className={styles.changePanel}>
            <div className={styles.panelHeader}>
              <div>
                <span className={styles.sectionLabel}>변경사항</span>
                <h2>패치 상세 목록</h2>
              </div>
            </div>

            <div className={styles.toolBar}>
              <label className={styles.searchBox}>
                <Search size={16} />
                <input
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="챔피언, 아이템, 키워드 검색"
                />
              </label>
              <div className={styles.filterLabel}>
                <Filter size={15} />
                카테고리
              </div>
            </div>

            <div className={styles.categoryTabs} aria-label="패치 변경 카테고리">
              {PATCH_CATEGORIES.map((category) => (
                <button
                  key={category}
                  type="button"
                  className={activeCategory === category ? styles.activeTab : undefined}
                  onClick={() => setActiveCategory(category)}
                >
                  {category}
                  <span>{getCategoryCount(category, patchChanges)}</span>
                </button>
              ))}
            </div>

            <div className={styles.quickFilters} aria-label="패치 빠른 필터">
              <button
                type="button"
                className={highImpactOnly ? styles.activeQuickFilter : undefined}
                onClick={() => setHighImpactOnly((enabled) => !enabled)}
                aria-pressed={highImpactOnly}
              >
                영향 높음만
              </button>

              <div className={styles.typeFilters}>
                {CHANGE_TYPE_FILTERS.map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={activeChangeType === type ? styles.activeTypeFilter : undefined}
                    onClick={() => setActiveChangeType(type)}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            <div className={styles.changeList}>
              {pagedChanges.map((change) => {
                const CategoryIcon = CATEGORY_ICON[change.category]
                const isExpanded = expandedChangeIds.includes(change.id)
                const imageUrl = getChangeImageUrl(change)

                return (
                  <article key={change.id} className={styles.changeItem}>
                    <div className={styles.changeTop}>
                      <span className={styles.categoryBadge}>
                        <CategoryIcon size={15} />
                        {change.category}
                      </span>
                      <span className={`${styles.changeType} ${CHANGE_TYPE_CLASS[change.type]}`}>{change.type}</span>
                      <span className={`${styles.impactBadge} ${IMPACT_CLASS[change.impact]}`}>영향 {change.impact}</span>
                    </div>

                    <div className={styles.changeBody}>
                      <span className={styles.changeThumb}>
                        <img
                          src={imageUrl}
                          alt=""
                          onError={(event) => {
                            event.currentTarget.src = PATCH_FALLBACK_IMAGE
                          }}
                        />
                      </span>
                      <div className={styles.changeText}>
                        <h3>{change.target}</h3>
                        <p>{change.summary}</p>
                      </div>
                      <button
                        type="button"
                        className={styles.detailButton}
                        onClick={() => toggleExpandedChange(change.id)}
                        aria-expanded={isExpanded}
                      >
                        {isExpanded ? '접기' : '상세 보기'}
                        <ChevronRight size={16} className={isExpanded ? styles.expandedArrow : undefined} />
                      </button>
                    </div>

                    {isExpanded && (
                      <div className={styles.compareGrid}>
                        <div>
                          <span>이전</span>
                          <p>{change.before}</p>
                        </div>
                        <div>
                          <span>변경</span>
                          <p>{change.after}</p>
                        </div>
                      </div>
                    )}

                    <div className={styles.tagRow}>
                      {change.tags.map((tag) => (
                        <span key={tag}>{tag}</span>
                      ))}
                    </div>
                  </article>
                )
              })}

              {pagedChanges.length === 0 && (
                <div className={styles.emptyState}>
                  검색 조건에 맞는 패치 변경사항이 없습니다.
                </div>
              )}
            </div>

            <Pagination currentPage={safePage} totalPages={totalPages} onPageChange={setCurrentPage} />
          </section>

          <aside className={styles.sideRail}>
            <section className={styles.insightPanel}>
              <span className={styles.sectionLabel}>요약</span>
              <h2>이번 패치 핵심</h2>
              <ul>
                {selectedPatch.highlights.map((highlight) => (
                  <li key={highlight}>
                    <CheckCircle2 size={16} />
                    <span>{highlight}</span>
                  </li>
                ))}
              </ul>
            </section>

            <section className={styles.historyPanel}>
              <div className={styles.historyHeader}>
                <span className={styles.sectionLabel}>히스토리</span>
                <History size={17} />
              </div>
              <h2>이전 패치</h2>
              <div className={styles.historyList}>
                {patchHistory.map((patch) => (
                  <button
                    key={patch.version}
                    type="button"
                    className={`${patch.status === '현재' ? styles.currentPatch : ''} ${
                      selectedPatchVersion === patch.version ? styles.selectedHistory : ''
                    }`}
                    onClick={() => setSelectedPatchVersion(patch.version)}
                    aria-pressed={selectedPatchVersion === patch.version}
                  >
                    <span className={styles.historyThumb}>
                      <img
                        src={patch.imageUrl}
                        alt=""
                        onError={(event) => {
                          event.currentTarget.src = PATCH_FALLBACK_IMAGE
                        }}
                      />
                    </span>
                    <div>
                      <strong>{patch.version}</strong>
                      <span>{patch.date}</span>
                    </div>
                    <p>{patch.title}</p>
                    <small>{patch.focus}</small>
                  </button>
                ))}
              </div>
            </section>
          </aside>
        </div>
      </div>
    </AppLayout>
  )
}

export default PatchNotes
