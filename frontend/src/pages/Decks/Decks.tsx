import { useState } from 'react'
import { AppLayout } from '../../components/layout'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { deduplicateDecks } from '../../api/deckApi'
import type { RankFilter } from '../Dashboard/dashboardData'
import DeckListView from './components/DeckListView'
import MetaStatsView from './components/MetaStatsView'
import styles from './Decks.module.css'

type Tab = '덱모음' | '메타통계'

interface RankFilterOption {
  label: string
  value: RankFilter
}

const RANK_FILTERS: RankFilterOption[] = [
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
  { label: '다이아+',   value: 'DIAMOND_PLUS' },
  { label: '마스터+',   value: 'MASTER_PLUS'  },
]

function Decks() {
  const [rankFilter, setRankFilter] = useState<RankFilter>('EMERALD_PLUS')
  const [tab, setTab] = useState<Tab>('덱모음')
  const { data: metaDeckResponse, isError: isDeckError } = useMetaSnapshot(rankFilter)
  const { data: locale } = useCDragonLocale()
  const decks = deduplicateDecks(metaDeckResponse?.decks ?? [])
  const patchVersion = metaDeckResponse?.patchVersion ?? '집계 대기'
  const dataRangeLabel = metaDeckResponse?.dataStartDate
    ? `${metaDeckResponse.dataStartDate} 이후 수집 데이터`
    : '집계 데이터 수집 전'

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>{patchVersion} 패치 기준 · {dataRangeLabel} · 선택률순 노출</p>
          </div>
          <div className={styles.rightControls}>
            <div className={styles.rankFilterBar}>
              {RANK_FILTERS.map((f) => (
                <button
                  key={f.value}
                  type="button"
                  className={rankFilter === f.value ? styles.rankFilterActive : styles.rankFilterBtn}
                  aria-pressed={rankFilter === f.value}
                  onClick={() => setRankFilter(f.value)}
                >
                  {f.label}
                </button>
              ))}
            </div>
            <div className={styles.tabBar}>
              <button
                type="button"
                className={tab === '덱모음' ? styles.activeTab : ''}
                aria-pressed={tab === '덱모음'}
                onClick={() => setTab('덱모음')}
              >
                덱모음
              </button>
              <button
                type="button"
                className={tab === '메타통계' ? styles.activeTab : ''}
                aria-pressed={tab === '메타통계'}
                onClick={() => setTab('메타통계')}
              >
                메타통계
              </button>
            </div>
          </div>
        </div>

        {isDeckError ? (
          <p className={styles.errorMessage}>메타 덱 정보를 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p>
        ) : tab === '덱모음'
          ? <DeckListView decks={decks} locale={locale} rankFilter={rankFilter} />
          : <MetaStatsView decks={decks} locale={locale} rankFilter={rankFilter} />
        }
      </div>
    </AppLayout>
  )
}

export default Decks
