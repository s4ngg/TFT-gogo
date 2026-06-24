import type {
  GuideCatalog,
  GuideTab,
  RecentGuide,
} from '../../../api/guide'
import AugmentGuideView from './AugmentGuideView'
import ChampionGuideView from './ChampionGuideView'
import ItemStatsView from './ItemStatsView'
import TraitGuideView from './TraitGuideView'

interface GuideTabPanelsProps {
  activeTab: GuideTab
  favoriteChampions: string[]
  guideData: GuideCatalog
  onFavoriteToggle: (championName: string) => void
  onGuideJump: (tab: GuideTab, query: string, label?: string) => void
  onRecentGuideAdd: (guide: RecentGuide) => void
  query: string
}

function GuideTabPanels({
  activeTab,
  favoriteChampions,
  guideData,
  onFavoriteToggle,
  onGuideJump,
  onRecentGuideAdd,
  query,
}: GuideTabPanelsProps) {
  if (activeTab === 'traits') {
    return (
      <div id="guide-panel-traits" role="tabpanel" aria-labelledby="guide-tab-traits">
        <TraitGuideView
          fallbackData={guideData}
          onChampionSelect={(championName) => onGuideJump('champions', championName, championName)}
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
        onChampionOpen={(championName) => onRecentGuideAdd({
          label: championName,
          query: championName,
          tab: 'champions',
        })}
        onFavoriteToggle={onFavoriteToggle}
        query={query}
      />
    </div>
  )
}

export default GuideTabPanels
