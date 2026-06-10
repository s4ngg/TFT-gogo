import { useRef, useState } from 'react'
import type { ChampionGuide } from '../../../api/guide'

export function useChampionDetailDialog(onChampionOpen: (championName: string) => void) {
  const [selectedChampion, setSelectedChampion] = useState<ChampionGuide | null>(null)
  const lastFocusedElementRef = useRef<HTMLElement | null>(null)

  function openChampionDetail(championGuide: ChampionGuide) {
    lastFocusedElementRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setSelectedChampion(championGuide)
    onChampionOpen(championGuide.name)
  }

  function closeChampionDetail() {
    setSelectedChampion(null)
    window.requestAnimationFrame(() => {
      if (lastFocusedElementRef.current?.isConnected) {
        lastFocusedElementRef.current.focus()
      }
    })
  }

  return {
    closeChampionDetail,
    openChampionDetail,
    selectedChampion,
  }
}
