import { Coins, Swords } from 'lucide-react'
import { itemsFromUrls, tftChampSquareUrl } from '../../../api/communityDragonAssets'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import ChampionCard from '../../../components/common/ChampionCard'
import type { MatchSummaryResponse } from '../../../api/summonerApi'
import { detailRankClass, formatDate, timeAgo } from '../utils/summonerUtils'
import styles from '../SummonerDetail.module.css'

export default function MatchDetailPanel({ match, myPuuid }: { match: MatchSummaryResponse; myPuuid: string }) {
  return (
    <div className={styles.matchDetailPanel}>
      <p className={styles.matchDetailDate}>
        {formatDate(match.gameDateTime)} · {timeAgo(match.gameDateTime)}
      </p>
      <div className={styles.matchDetailHeader}>
        <span>#</span><span>소환사</span><span>스테이지</span><span>시너지</span>
        <span>챔피언</span><span>킬</span><span>잔여골드</span>
      </div>
      {match.participants.map((p) => {
        const isMe = p.puuid === myPuuid
        return (
          <div key={p.puuid} className={`${styles.matchDetailRow} ${isMe ? styles.myMatchDetailRow : ''}`}>
            <span className={`${styles.detailRank} ${detailRankClass(p.placement, styles)}`}>{p.placement}위</span>
            <div className={styles.detailPlayer}>
              <span className={styles.detailName}>{p.riotIdGameName}</span>
              <span className={styles.detailTag}>#{p.riotIdTagline}</span>
            </div>
            <span className={styles.detailStage}>{p.stage}</span>
            <div className={styles.detailTraits}>
              {p.traits.slice(0, 3).map((tr) => (
                <TraitHexBadge
                  key={tr.traitId}
                  count={tr.count}
                  iconUrl={tr.iconUrl}
                  name={tr.name}
                  tone={tr.tone}
                />
              ))}
            </div>
            <div className={styles.detailUnits}>
              {p.units.map((unit, i) => (
                <ChampionCard
                  key={`${unit.characterId}-${i}`}
                  imageUrl={unit.imageUrl || tftChampSquareUrl(unit.characterId)}
                  stars={unit.stars}
                  label=""
                  items={itemsFromUrls(unit.itemImageUrls)}
                  toneIndex={0}
                />
              ))}
            </div>
            <span className={styles.detailKills}><Swords size={11} />{p.playersEliminated}</span>
            <span className={styles.detailGold}><Coins size={11} />{p.goldLeft}</span>
          </div>
        )
      })}
    </div>
  )
}
