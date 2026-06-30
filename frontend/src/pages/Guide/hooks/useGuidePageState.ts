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

const GUIDE_SEARCH_DEBOUNCE_MS = 300

export function useGuidePageState() {
  const [activeTab, setActiveTab] = useState<GuideTab>('traits')
  const [favoriteChampions, setFavoriteChampions] = useState<string[]>(readFavoriteChampions)
  const [recentGuides, setRecentGuides] = useState<RecentGuide[]>(readRecentGuides)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')

  const activeTabInfo = useMemo(
    () => GUIDE_TABS.find((tab) => tab.key === activeTab) ?? GUIDE_TABS[0],
    [activeTab],
  )

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearch(search)
    }, GUIDE_SEARCH_DEBOUNCE_MS)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [search])

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
    setDebouncedSearch('')
  }

  function jumpToGuide(tab: GuideTab, query: string, label = query) {
    setActiveTab(tab)
    setSearch(query)
    setDebouncedSearch(query)
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
    debouncedSearch,
    favoriteChampions,
    handleFavoriteToggle,
    jumpToGuide,
    recentGuides,
    search,
    selectTab,
    setSearch,
  }
}
