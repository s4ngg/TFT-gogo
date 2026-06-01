import {
  Package,
  Search,
  Shield,
  Sparkles,
  Swords,
  type LucideIcon,
} from 'lucide-react'
import {
  GUIDE_TABS,
  type GuideTab,
} from '../../../api/guide'
import styles from '../Guide.module.css'

interface GuideControlsProps {
  activeTab: GuideTab
  activeTabLabel: string
  onSearchChange: (query: string) => void
  onTabSelect: (tab: GuideTab) => void
  search: string
}

const GUIDE_TAB_ICONS: Record<GuideTab, LucideIcon> = {
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

function GuideControls({
  activeTab,
  activeTabLabel,
  onSearchChange,
  onTabSelect,
  search,
}: GuideControlsProps) {
  return (
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
              onClick={() => onTabSelect(key)}
              onKeyDown={(event) => {
                const nextTabIndex = getNextGuideTabIndex(event.key, guideTabIndex)
                if (nextTabIndex === undefined) return

                event.preventDefault()
                const nextTab = GUIDE_TABS[nextTabIndex]
                onTabSelect(nextTab.key)
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
        <Search size={15} aria-hidden="true" />
        <input
          aria-label={`${activeTabLabel} 검색`}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder={`${activeTabLabel} 검색`}
          type="search"
          value={search}
        />
      </label>
    </section>
  )
}

export default GuideControls
