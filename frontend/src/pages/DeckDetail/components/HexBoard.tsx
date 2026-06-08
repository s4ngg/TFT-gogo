import { useMemo } from 'react'
import { Map as MapIcon } from 'lucide-react'
import { getChampionApiName, getChampionName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import { tftItemIconUrl, tftItemIconOnError } from '../../../api/communityDragonAssets'
import type { ChampionSummary } from '../../Dashboard/dashboardData'
import {
  BOARD_ROWS,
  BOARD_COLS,
  buildBoardPositions,
  buildCustomBoardPositions,
  type PlacedChamp,
  type CellPosData,
} from '../utils/boardUtils'
import styles from '../DeckDetail.module.css'

interface HexBoardProps {
  visibleUnits: ChampionSummary[]
  level: number
  availableLevels: number[]
  onLevelChange: (lv: number) => void
  locale: TFTLocale | undefined
  customPosMap: Map<string, CellPosData>
}

function HexBoard({ visibleUnits, level, availableLevels, onLevelChange, locale, customPosMap }: HexBoardProps) {
  const placed = useMemo(() => {
    if (customPosMap.size === 0) return buildBoardPositions(visibleUnits, locale)

    const allMapped = visibleUnits.every((c) => {
      const apiName = getChampionApiName(c.imageUrl)
      return apiName ? customPosMap.has(apiName) : false
    })
    if (!allMapped) return buildBoardPositions(visibleUnits, locale)

    return buildCustomBoardPositions(visibleUnits, customPosMap)
  }, [visibleUnits, customPosMap, locale])

  if (visibleUnits.length === 0) return null

  function champAt(row: number, col: number): PlacedChamp | undefined {
    return placed.find((p) => p.row === row && p.col === col)
  }

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <MapIcon size={16} />
        <h2>추천 배치</h2>
        <span className={styles.panelSub}>위 2줄 탱커, 아래 2줄 딜러</span>
      </div>
      <div className={styles.boardWrap}>
        <div className={styles.board}>
          {Array.from({ length: BOARD_ROWS }, (_, vi) => {
            const row = BOARD_ROWS - 1 - vi
            const isOffset = row % 2 !== 0
            return (
              <div key={row} className={`${styles.boardRow} ${isOffset ? styles.boardRowOffset : ''}`}>
                {Array.from({ length: BOARD_COLS }, (_, col) => {
                  const p = champAt(row, col)
                  const itemUrls = p
                    ? (p.items ?? p.champ.recommendedItems ?? []).slice(0, 3).map((id) => tftItemIconUrl(id))
                    : []

                  return (
                    <div key={col} className={styles.hexCell}>
                      <div className={`${styles.hexOuter} ${p ? styles.hexFilled : styles.hexEmpty}`}>
                        {p && (
                          <>
                            <img src={p.champ.imageUrl} alt={p.champ.name} className={styles.hexImg} />
                            <span className={styles.hexStars}>{'★'.repeat(p.champ.stars ?? 2)}</span>
                          </>
                        )}
                      </div>
                      {p && itemUrls.length > 0 && (
                        <div className={styles.hexItems}>
                          {itemUrls.map((url, idx) => (
                            <img
                              key={idx}
                              src={url}
                              alt=""
                              className={styles.hexItemIcon}
                              onError={tftItemIconOnError}
                            />
                          ))}
                        </div>
                      )}
                      {p && (
                        <span className={styles.hexName}>
                          {getChampionName(p.champ.imageUrl, locale, p.champ.name)}
                        </span>
                      )}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>

        {availableLevels.length > 1 && (
          <div className={styles.levelTabs}>
            {availableLevels.map((lv) => (
              <button
                key={lv}
                type="button"
                className={`${styles.levelTab} ${lv === level ? styles.levelTabActive : ''}`}
                onClick={() => onLevelChange(lv)}
              >
                Lv.{lv}
              </button>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

export default HexBoard
