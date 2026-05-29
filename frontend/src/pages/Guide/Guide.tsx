import {
  BookOpen,
  Package,
  Search,
  Shield,
  Sparkles,
  Swords,
} from 'lucide-react'
import {
  GUIDE_TABS,
  type GuideTab,
} from '../../api/guide'
import { AppLayout } from '../../components/layout'
import { useGuide } from '../../hooks/useGuide'
import { guideFallbackData } from '../../mocks/guideResponseMock'
import AugmentGuideView from './components/AugmentGuideView'
import ChampionGuideView from './components/ChampionGuideView'
import GuideQuickAccess from './components/GuideQuickAccess'
import { StatBadge } from './components/GuideShared'
import ItemStatsView from './components/ItemStatsView'
import TraitGuideView from './components/TraitGuideView'
import styles from './Guide.module.css'

const GUIDE_TAB_ICONS: Record<GuideTab, typeof BookOpen> = {
  augments: Sparkles,
  champions: Swords,
  items: Package,
  traits: Shield,
}

function getNextGuideTabIndex(key: string, currentIndex: number): number | undefined {
  const lastIndex = GUIDE_TABS.length - 1

  if (key === 'ArrowLeft' || key === 'ArrowUp') {
    return currentIndex === 0 ? lastIndex : currentIndex - 1
  }

  if (key === 'ArrowRight' || key === 'ArrowDown') {
    return currentIndex === lastIndex ? 0 : currentIndex + 1
  }

  if (key === 'Home') return 0
  if (key === 'End') return lastIndex

  return undefined
}

function Guide() {
  const {
    activeTab,
    activeTabInfo,
    addRecentGuide,
    favoriteChampions,
    guideData,
    handleFavoriteToggle,
    jumpToGuide,
    recentGuides,
    search,
    selectTab,
    setSearch,
  } = useGuide({ fallbackData: guideFallbackData })

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

        <section className={styles.controlPanel}>
          <div className={styles.tabBar} role="tablist" aria-label="게임 가이드 탭">
            {GUIDE_TABS.map(({ key, label, meta }, guideTabIndex) => {
              const Icon = GUIDE_TAB_ICONS[key]

              return (
                <button
                  aria-controls={`guide-panel-${key}`}
                  aria-selected={activeTab === key}
                  className={activeTab === key ? styles.activeTab : ''}
                  id={`guide-tab-${key}`}
                  key={key}
                  onClick={() => selectTab(key)}
                  onKeyDown={(event) => {
                    const nextTabIndex = getNextGuideTabIndex(event.key, guideTabIndex)
                    if (nextTabIndex === undefined) return

                    event.preventDefault()
                    const nextTab = GUIDE_TABS[nextTabIndex]
                    selectTab(nextTab.key)
                    window.requestAnimationFrame(() => {
                      document.getElementById(`guide-tab-${nextTab.key}`)?.focus()
                    })
                  }}
                  role="tab"
                  tabIndex={activeTab === key ? 0 : -1}
                  type="button"
                >
                  <Icon size={18} />
                  <span>{label}</span>
                  <small>{meta}</small>
                </button>
              )
            })}
          </div>
          <label className={styles.searchBox}>
            <Search size={15} />
            <input
              onChange={(event) => setSearch(event.target.value)}
              placeholder={`${activeTabInfo.label} 검색`}
              value={search}
            />
          </label>
        </section>

        <GuideQuickAccess
          favoriteChampions={favoriteChampions}
          onJump={jumpToGuide}
          recentGuides={recentGuides}
        />

        {activeTab === 'traits' && (
          <div id="guide-panel-traits" role="tabpanel" aria-labelledby="guide-tab-traits">
            <TraitGuideView
              fallbackData={guideData}
              onChampionSelect={(championName) => jumpToGuide('champions', championName, championName)}
              query={search}
            />
          </div>
        )}
        {activeTab === 'items' && (
          <div id="guide-panel-items" role="tabpanel" aria-labelledby="guide-tab-items">
            <ItemStatsView
              fallbackData={guideData}
              onChampionSelect={(championName) => jumpToGuide('champions', championName, championName)}
              query={search}
            />
          </div>
        )}
        {activeTab === 'augments' && (
          <div id="guide-panel-augments" role="tabpanel" aria-labelledby="guide-tab-augments">
            <AugmentGuideView
              augmentPlans={guideData.augmentPlans}
              fallbackData={guideData}
              query={search}
              rewardRows={guideData.rewards}
            />
          </div>
        )}
        {activeTab === 'champions' && (
          <div id="guide-panel-champions" role="tabpanel" aria-labelledby="guide-tab-champions">
            <ChampionGuideView
              fallbackData={guideData}
              favoriteChampions={favoriteChampions}
              onChampionOpen={(championName) => addRecentGuide({ label: championName, query: championName, tab: 'champions' })}
              onFavoriteToggle={handleFavoriteToggle}
              onItemSelect={(itemName) => jumpToGuide('items', itemName, itemName)}
              query={search}
            />
          </div>
        )}
      </div>
    </AppLayout>
  )
}

export default Guide
