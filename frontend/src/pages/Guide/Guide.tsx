import { BookOpen } from 'lucide-react'
import { useState } from 'react'
import type { GameGuideAiPathfinderRef } from '../../api/gameGuideAiPathfinderApi'
import { AppLayout } from '../../components/layout'
import { useGuideCatalog } from '../../hooks/useGuide'
import { guideFallbackData } from '../../mocks/guideResponseMock'
import GameGuideAiChatWidget from './components/GameGuideAiChatWidget'
import GuideControls from './components/GuideControls'
import GuideQuickAccess from './components/GuideQuickAccess'
import { StatBadge } from './components/GuideShared'
import GuideTabPanels from './components/GuideTabPanels'
import { useGameGuideAiCandidateRefs } from './hooks/useGameGuideAiCandidateRefs'
import { useGuidePageState } from './hooks/useGuidePageState'
import styles from './Guide.module.css'

function Guide() {
  const [isGameGuideAiOpen, setIsGameGuideAiOpen] = useState(false)
  const [gameGuideAiSelectedRefs, setGameGuideAiSelectedRefs] = useState<GameGuideAiPathfinderRef[]>([])
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
  const gameGuideAiCandidateRefs = useGameGuideAiCandidateRefs(guideData, activeTab)

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
          onJump={jumpToGuide}
          recentGuides={recentGuides}
        />

        <GuideTabPanels
          activeTab={activeTab}
          favoriteChampions={favoriteChampions}
          guideData={guideData}
          isGuideFallbackData={isGuideFallbackData}
          isGuideFetching={isGuideFetching}
          onFavoriteToggle={handleFavoriteToggle}
          onGameGuideAiAsk={handleGameGuideAiAsk}
          onGuideJump={jumpToGuide}
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
          onOpenChange={handleGameGuideAiOpenChange}
          onGuideJump={jumpToGuide}
          patchVersion={guideData.patchVersion}
          selectedRefs={gameGuideAiSelectedRefs}
        />
      </div>
    </AppLayout>
  )
}

export default Guide
