import { Filter, Search } from 'lucide-react'
import {
  CHANGE_TYPE_FILTERS,
  PATCH_CATEGORIES,
  type ChangeTypeFilter,
  type PatchCategory,
  type PatchChangeStats,
} from '../../../api/patchNotes'
import styles from '../PatchNotes.module.css'

interface PatchChangeFiltersProps {
  activeCategory: PatchCategory
  activeChangeType: ChangeTypeFilter
  query: string
  stats: PatchChangeStats
  onCategoryChange: (category: PatchCategory) => void
  onChangeTypeChange: (changeType: ChangeTypeFilter) => void
  onQueryChange: (query: string) => void
}

function getCategoryCount(category: PatchCategory, stats: PatchChangeStats) {
  return stats.categoryCounts[category] ?? 0
}

function PatchChangeFilters({
  activeCategory,
  activeChangeType,
  onCategoryChange,
  onChangeTypeChange,
  onQueryChange,
  query,
  stats,
}: PatchChangeFiltersProps) {
  return (
    <>
      <div className={styles.toolBar}>
        <label className={styles.searchBox}>
          <Search size={16} />
          <input
            type="search"
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="챔피언, 아이템, 키워드 검색"
          />
        </label>
        <div className={styles.filterLabel}>
          <Filter size={15} />
          카테고리
        </div>
      </div>

      <div className={styles.categoryTabs} aria-label="패치 변경 카테고리">
        {PATCH_CATEGORIES.map((category) => (
          <button
            key={category}
            type="button"
            className={activeCategory === category ? styles.activeTab : undefined}
            onClick={() => onCategoryChange(category)}
          >
            {category}
            <span>{getCategoryCount(category, stats)}</span>
          </button>
        ))}
      </div>

      <div className={styles.quickFilters} aria-label="패치 빠른 필터">
        <div className={styles.typeFilters}>
          {CHANGE_TYPE_FILTERS.map((type) => (
            <button
              key={type}
              type="button"
              className={activeChangeType === type ? styles.activeTypeFilter : undefined}
              onClick={() => onChangeTypeChange(type)}
            >
              {type}
            </button>
          ))}
        </div>
      </div>
    </>
  )
}

export default PatchChangeFilters
