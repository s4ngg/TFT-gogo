import type { ChampionSummary, TraitSummary } from '../Dashboard/dashboardData'

export interface HeroAugmentDeck {
  hero: string
  augment: string
  recommended: boolean
  winRate: string
  avgPlace: string
  pickRate: string
  description: string
  tags: string[]
  traits: TraitSummary[]
  champions: ChampionSummary[]
}

export interface ArtifactUnit {
  name: string
  imageUrl: string
  frequency: string
  winRate: string
  avgImprovement: string
  top4: string
}

export interface ArtifactRec {
  itemName: string
  itemIcon: string
  units: ArtifactUnit[]
}

export const INITIAL_ARTIFACT_COUNT = 4
