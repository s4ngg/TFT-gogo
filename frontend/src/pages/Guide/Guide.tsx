import { BookOpen } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import type { GameGuideAiPathfinderRef } from '../../api/gameGuideAiPathfinderApi'
import type { GuideTab, GuideTabItems } from '../../api/guide'
import { AppLayout } from '../../components/layout'
import { useGuideCatalog } from '../../hooks/useGuide'
import { guideFallbackData } from './guideFallbackData'
import GameGuideAiChatWidget from './components/GameGuideAiChatWidget'
import GuideControls from './components/GuideControls'
import GuideQuickAccess from './components/GuideQuickAccess'
import { StatBadge } from './components/GuideShared'
import GuideTabPanels from './components/GuideTabPanels'
import { useGameGuideAiCandidateRefs } from './hooks/useGameGuideAiCandidateRefs'
import { useGuideHighlight } from './hooks/useGuideHighlight'
import { useGuidePageState } from './hooks/useGuidePageState'
import {
  createGameGuideAiScopeKey,
  createGameGuideAiWidgetKey,
  readGameGuideAiScopedValue,
  type GameGuideAiScopedValue,
} from './utils/gameGuideAiContext'
import styles from './Guide.module.css'

function Guide() {
  const [isGameGuideAiOpen, setIsGameGuideAiOpen] = useState(false)
  const [gameGuideAiSelectedState, setGameGuideAiSelectedState] = useState<
    GameGuideAiScopedValue<GameGuideAiPathfinderRef[]>
  >({ scopeKey: '', value: [] })
  const [gameGuideAiVisibleState, setGameGuideAiVisibleState] = useState<
    GameGuideAiScopedValue<GuideTabItems[GuideTab][number][]>
  >({ scopeKey: '', value: [] })
  const {
    dataSource: guideDataSource,
    isFallbackData: isGuideFallbackData,
    isFetching: isGuideFetching,
    patchVersion,
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
  const isGameGuideAiAvailable = guideDataSource === 'api'
  const gameGuideAiScopeKey = createGameGuideAiScopeKey(patchVersion, activeTab)
  const gameGuideAiSelectedRefs = readGameGuideAiScopedValue(
    gameGuideAiSelectedState,
    gameGuideAiScopeKey,
    [],
  )
  const gameGuideAiVisibleItems = readGameGuideAiScopedValue(
    gameGuideAiVisibleState,
    gameGuideAiScopeKey,
    [],
  )
  const gameGuideAiCandidateRefs = useGameGuideAiCandidateRefs(activeTab, gameGuideAiVisibleItems)
  const gameGuideAiWidgetKey = createGameGuideAiWidgetKey(
    gameGuideAiScopeKey,
    gameGuideAiSelectedRefs,
  )
  const handleGameGuideAiVisibleItemsChange = useCallback((items: GuideTabItems[GuideTab][number][]) => {
    setGameGuideAiVisibleState({ scopeKey: gameGuideAiScopeKey, value: items })
  }, [gameGuideAiScopeKey])
  const { handleGuideJump, highlightedGuide } = useGuideHighlight({ onJump: jumpToGuide })

  useEffect(() => {
    if (isGameGuideAiAvailable) return
    setIsGameGuideAiOpen(false)
    setGameGuideAiSelectedState({ scopeKey: gameGuideAiScopeKey, value: [] })
    setGameGuideAiVisibleState({ scopeKey: gameGuideAiScopeKey, value: [] })
  }, [gameGuideAiScopeKey, isGameGuideAiAvailable])

  function handleGameGuideAiAsk(ref: GameGuideAiPathfinderRef) {
    setGameGuideAiSelectedState({ scopeKey: gameGuideAiScopeKey, value: [ref] })
    setIsGameGuideAiOpen(true)
  }

  function handleGameGuideAiOpenChange(nextIsOpen: boolean) {
    setIsGameGuideAiOpen(nextIsOpen)
    if (!nextIsOpen) {
      setGameGuideAiSelectedState({ scopeKey: gameGuideAiScopeKey, value: [] })
    }
  }

  return (
    <AppLayout sunTheme>
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
            <StatBadge label="기준 패치" value={patchVersion} />
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
          fallbackData={guideFallbackData}
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
          patchVersion={patchVersion}
          query={debouncedSearch}
        />

        {isGameGuideAiAvailable && (
          <GameGuideAiChatWidget
            activeTab={activeTab}
            activeTabLabel={activeTabInfo.label}
            candidateRefs={gameGuideAiCandidateRefs}
            isOpen={isGameGuideAiOpen}
            key={gameGuideAiWidgetKey}
            onOpenChange={handleGameGuideAiOpenChange}
            onGuideJump={handleGuideJump}
            patchVersion={patchVersion}
            selectedRefs={gameGuideAiSelectedRefs}
          />
        )}
      </div>
    </AppLayout>
  )
}

export default Guide
