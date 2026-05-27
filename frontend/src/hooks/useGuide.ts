import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  GUIDE_TABS,
  getGuideCatalog,
  type GuideCatalog,
  type GuideTab,
  type RecentGuide,
} from '../api/guide'

interface UseGuideOptions {
  fallbackData: GuideCatalog
}

export function useGuide({ fallbackData }: UseGuideOptions) {
  const [activeTab, setActiveTab] = useState<GuideTab>('traits')
  const [favoriteChampions, setFavoriteChampions] = useState<string[]>([])
  const [recentGuides, setRecentGuides] = useState<RecentGuide[]>([])
  const [search, setSearch] = useState('')

  const guideQuery = useQuery({
    initialData: fallbackData,
    queryFn: () => getGuideCatalog(fallbackData),
    queryKey: ['guide', 'catalog'],
    staleTime: 1000 * 60 * 5,
  })

  const activeTabInfo = useMemo(
    () => GUIDE_TABS.find((tab) => tab.key === activeTab) ?? GUIDE_TABS[0],
    [activeTab],
  )

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
    guideData: guideQuery.data,
    handleFavoriteToggle,
    isFetching: guideQuery.isFetching,
    jumpToGuide,
    recentGuides,
    search,
    selectTab,
    setSearch,
  }
}
