import type {
  GuideCatalog,
  GuideTab,
  GuideTabItems,
  RecentGuide,
} from '../../../api/guide'
import AugmentGuideView from './AugmentGuideView'
import ChampionGuideView from './ChampionGuideView'
import ItemStatsView from './ItemStatsView'
import TraitGuideView from './TraitGuideView'
import type { GameGuideAiAskHandler } from '../utils/gameGuideAiRefs'
import type { HighlightedGuide } from '../utils/guideHighlight'

interface GuideTabPanelsProps {
  activeTab: GuideTab
  favoriteChampions: string[]
  guideData: GuideCatalog
  highlightedGuide: HighlightedGuide | null
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onFavoriteToggle: (championName: string) => void
  onGameGuideAiAsk: GameGuideAiAskHandler
  onGameGuideAiVisibleItemsChange: (items: GuideTabItems[GuideTab][number][]) => void
  onGuideJump: (tab: GuideTab, query: string, label?: string, targetKey?: string) => void
  onGuideRetry: () => void
  onRecentGuideAdd: (guide: RecentGuide) => void
  query: string
}

function GuideTabPanels({
  activeTab,
  favoriteChampions,
  guideData,
  highlightedGuide,
  isGuideFallbackData,
  isGuideFetching,
  onFavoriteToggle,
  onGameGuideAiAsk,
  onGameGuideAiVisibleItemsChange,
  onGuideJump,
  onGuideRetry,
  onRecentGuideAdd,
  query,
}: GuideTabPanelsProps) {
  if (activeTab === 'traits') {
    return (
      <div id="guide-panel-traits" role="tabpanel" aria-labelledby="guide-tab-traits">
        <TraitGuideView
          fallbackData={guideData}
          highlightedGuide={highlightedGuide}
          isGuideFallbackData={isGuideFallbackData}
          isGuideFetching={isGuideFetching}
          onGameGuideAiAsk={onGameGuideAiAsk}
          onVisibleItemsChange={onGameGuideAiVisibleItemsChange}
          onChampionSelect={(championName) => onGuideJump('champions', championName, championName)}
          onGuideRetry={onGuideRetry}
          patchVersion={guideData.patchVersion}
          query={query}
        />
      </div>
    )
  }

  if (activeTab === 'items') {
    return (
      <div id="guide-panel-items" role="tabpanel" aria-labelledby="guide-tab-items">
        <ItemStatsView
          fallbackData={guideData}
          highlightedGuide={highlightedGuide}
          isGuideFallbackData={isGuideFallbackData}
          isGuideFetching={isGuideFetching}
          onGameGuideAiAsk={onGameGuideAiAsk}
          onVisibleItemsChange={onGameGuideAiVisibleItemsChange}
          onGuideRetry={onGuideRetry}
          patchVersion={guideData.patchVersion}
          query={query}
        />
      </div>
    )
  }

  if (activeTab === 'augments') {
    return (
      <div id="guide-panel-augments" role="tabpanel" aria-labelledby="guide-tab-augments">
        <AugmentGuideView
          fallbackData={guideData}
          highlightedGuide={highlightedGuide}
          isGuideFallbackData={isGuideFallbackData}
          isGuideFetching={isGuideFetching}
          onGameGuideAiAsk={onGameGuideAiAsk}
          onVisibleItemsChange={onGameGuideAiVisibleItemsChange}
          onGuideRetry={onGuideRetry}
          patchVersion={guideData.patchVersion}
          query={query}
        />
      </div>
    )
  }

  return (
    <div id="guide-panel-champions" role="tabpanel" aria-labelledby="guide-tab-champions">
      <ChampionGuideView
        fallbackData={guideData}
        favoriteChampions={favoriteChampions}
        highlightedGuide={highlightedGuide}
        isGuideFallbackData={isGuideFallbackData}
        isGuideFetching={isGuideFetching}
        onGameGuideAiAsk={onGameGuideAiAsk}
        onVisibleItemsChange={onGameGuideAiVisibleItemsChange}
        onChampionOpen={(championName) => onRecentGuideAdd({
          label: championName,
          query: championName,
          tab: 'champions',
        })}
        onFavoriteToggle={onFavoriteToggle}
        onGuideRetry={onGuideRetry}
        patchVersion={guideData.patchVersion}
        query={query}
      />
    </div>
  )
}

export default GuideTabPanels
