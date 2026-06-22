export type TierBadgeValue = 'S' | 'A' | 'B' | 'C' | 'D' | 'UNKNOWN'
export type RankedTierBadgeValue = Exclude<TierBadgeValue, 'UNKNOWN'>

export type TraitHexBadgeTone = 'gold' | 'silver' | 'bronze' | 'prismatic'
