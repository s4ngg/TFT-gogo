import { useEffect, useMemo, useState } from 'react'
import {
  GUIDE_TABS,
  type GuideTab,
  type RecentGuide,
} from '../../../api/guide'
import {
  readFavoriteChampions,
  readRecentGuides,
  writeFavoriteChampions,
  writeRecentGuides,
} from '../utils/guideQuickAccessStorage'

export function useGuidePageState() {
  const [activeTab, setActiveTab] = useState<GuideTab>('traits')
  const [favoriteChampions, setFavoriteChampions] = useState<string[]>(readFavoriteChampions)
  const [recentGuides, setRecentGuides] = useState<RecentGuide[]>(readRecentGuides)
  const [search, setSearch] = useState('')

  const activeTabInfo = useMemo(
    () => GUIDE_TABS.find((tab) => tab.key === activeTab) ?? GUIDE_TABS[0],
    [activeTab],
  )

  useEffect(() => {
    writeFavoriteChampions(favoriteChampions)
  }, [favoriteChampions])

  useEffect(() => {
    writeRecentGuides(recentGuides)
  }, [recentGuides])

  function addRecentGuide(guide: RecentGuide) {
    setRecentGuides((current) => [
      guide,
      ...current.filter((item) => item.query !== guide.query || item.tab !== guide.tab),
    ].slice(0, 6))
  }

  function selectTab(tab: GuideTab) {
    setActiveTab(tab)
    setSearch('')
  }

  function jumpToGuide(tab: GuideTab, query: string, label = query) {
    setActiveTab(tab)
    setSearch(query)
    addRecentGuide({ label, query, tab })
  }

  function handleFavoriteToggle(championName: string) {
    setFavoriteChampions((current) => (
      current.includes(championName)
        ? current.filter((name) => name !== championName)
        : [championName, ...current].slice(0, 12)
    ))
  }

  return {
    activeTab,
    activeTabInfo,
    addRecentGuide,
    favoriteChampions,
    handleFavoriteToggle,
    jumpToGuide,
    recentGuides,
    search,
    selectTab,
    setSearch,
  }
}
