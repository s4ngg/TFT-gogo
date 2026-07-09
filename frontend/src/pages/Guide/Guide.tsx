import { BookOpen } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { GameGuideAiPathfinderRef } from '../../api/gameGuideAiPathfinderApi'
import type { GuideTab, GuideTabItems, RecentGuide } from '../../api/guide'
import { AppLayout } from '../../components/layout'
import { useGuideCatalog } from '../../hooks/useGuide'
import { guideFallbackData } from './guideFallbackData'
import GameGuideAiChatWidget from './components/GameGuideAiChatWidget'
import GuideControls from './components/GuideControls'
import GuideQuickAccess from './components/GuideQuickAccess'
import { StatBadge } from './components/GuideShared'
import GuideTabPanels from './components/GuideTabPanels'
import { useGameGuideAiCandidateRefs } from './hooks/useGameGuideAiCandidateRefs'
import { useGuidePageState } from './hooks/useGuidePageState'
import styles from './Guide.module.css'

const GUIDE_HIGHLIGHT_DURATION_MS = 2600

function Guide() {
  const highlightTimeoutRef = useRef<number | null>(null)
  const [highlightedGuide, setHighlightedGuide] = useState<RecentGuide | null>(null)
  const [isGameGuideAiOpen, setIsGameGuideAiOpen] = useState(false)
  const [gameGuideAiSelectedRefs, setGameGuideAiSelectedRefs] = useState<GameGuideAiPathfinderRef[]>([])
  const [gameGuideAiVisibleItems, setGameGuideAiVisibleItems] = useState<GuideTabItems[GuideTab][number][]>([])
  const {
    guideData,
    isFallbackData: isGuideFallbackData,
    isFetching: isGuideFetching,
    refetchGuideData,
  } = useGuideCatalog({ fallbackData: guideFallbackData })
  const {
    activeTab,
    activeTabInfo,
    addRecentGuide,
    debouncedSearch,
    favoriteChampions,
    handleFavoriteToggle,
    jumpToGuide,
    recentGuides,
    search,
    selectTab,
    setSearch,
  } = useGuidePageState()
  const gameGuideAiCandidateRefs = useGameGuideAiCandidateRefs(activeTab, gameGuideAiVisibleItems)
  const gameGuideAiSelectionKey = gameGuideAiSelectedRefs
    .map((ref) => `${ref.guideType}:${ref.targetKey}`)
    .join('|')
  const handleGameGuideAiVisibleItemsChange = useCallback((items: GuideTabItems[GuideTab][number][]) => {
    setGameGuideAiVisibleItems(items)
  }, [])

  useEffect(() => () => {
    if (highlightTimeoutRef.current !== null) {
      window.clearTimeout(highlightTimeoutRef.current)
    }
  }, [])

  function handleGuideJump(tab: GuideTab, query: string, label = query) {
    const nextHighlightedGuide = { label, query, tab }

    jumpToGuide(tab, query, label)
    setHighlightedGuide(nextHighlightedGuide)
    if (highlightTimeoutRef.current !== null) {
      window.clearTimeout(highlightTimeoutRef.current)
    }
    highlightTimeoutRef.current = window.setTimeout(() => {
      setHighlightedGuide((current) => (
        current?.tab === nextHighlightedGuide.tab && current.query === nextHighlightedGuide.query
          ? null
          : current
      ))
    }, GUIDE_HIGHLIGHT_DURATION_MS)
  }

  function handleGameGuideAiAsk(ref: GameGuideAiPathfinderRef) {
    setGameGuideAiSelectedRefs([ref])
    setIsGameGuideAiOpen(true)
  }

  function handleGameGuideAiOpenChange(nextIsOpen: boolean) {
    setIsGameGuideAiOpen(nextIsOpen)
    if (!nextIsOpen) {
      setGameGuideAiSelectedRefs([])
    }
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <header className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <span className={styles.kicker}>
              <BookOpen size={15} />
              SET 17 GUIDE
            </span>
            <h1>게임 가이드</h1>
            <p>시너지, 아이템, 증강체, 챔피언 정보를 한 화면에서 빠르게 비교합니다.</p>
          </div>
          <div className={styles.headerStats}>
            <StatBadge label="기준 패치" value={guideData.patchVersion} />
          </div>
        </header>

        <GuideControls
          activeTab={activeTab}
          activeTabLabel={activeTabInfo.label}
          onSearchChange={setSearch}
          onTabSelect={selectTab}
          search={search}
        />

        <GuideQuickAccess
          favoriteChampions={favoriteChampions}
          onJump={handleGuideJump}
          recentGuides={recentGuides}
        />

        <GuideTabPanels
          activeTab={activeTab}
          favoriteChampions={favoriteChampions}
          guideData={guideData}
          highlightedGuide={highlightedGuide}
          isGuideFallbackData={isGuideFallbackData}
          isGuideFetching={isGuideFetching}
          onFavoriteToggle={handleFavoriteToggle}
          onGameGuideAiAsk={handleGameGuideAiAsk}
          onGameGuideAiVisibleItemsChange={handleGameGuideAiVisibleItemsChange}
          onGuideJump={handleGuideJump}
          onGuideRetry={() => {
            void refetchGuideData()
          }}
          onRecentGuideAdd={addRecentGuide}
          query={debouncedSearch}
        />

        <GameGuideAiChatWidget
          activeTab={activeTab}
          activeTabLabel={activeTabInfo.label}
          candidateRefs={gameGuideAiCandidateRefs}
          isOpen={isGameGuideAiOpen}
          key={gameGuideAiSelectionKey}
          onOpenChange={handleGameGuideAiOpenChange}
          onGuideJump={handleGuideJump}
          patchVersion={guideData.patchVersion}
          selectedRefs={gameGuideAiSelectedRefs}
        />
      </div>
    </AppLayout>
  )
}

export default Guide
