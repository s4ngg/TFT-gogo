<spec domain="decks">

<purpose>
Meta deck list and deck detail pages.
Pages: Decks (/decks), DeckDetail (/decks/:deckId).
</purpose>

<routes>
- /decks            → deck list grouped by tier
- /decks/:deckId    → deck detail (hex board, synergies, recommended items)
</routes>

<api>
<backend>
- GET /api/decks/meta?rankFilter=  — returns meta deck list; rankFilter: MASTER_PLUS | DIAMOND_PLUS | EMERALD_PLUS
</backend>
<frontend>
- frontend/src/api/deckApi.ts
</frontend>
</api>

<business-rules>

<deck-list>
- Decks are grouped by tier: S / A+ / A / B / C / D.
- Rank filter options: 마스터+ / 다이아+ / 에메랄드+.
- Deck name format: "[Primary Synergy] [Carry Champion]" — auto-generated; admin can override.
- Each deck card shows: champion icons, synergy badges, win rate, TOP4 rate, avg placement, pick rate.
- Minimum thresholds for display: ≥10 games, ≥0.5% pick rate.
</deck-list>

<deck-detail>
- Hex board tab range: Lv.5 ~ Lv.9.
- Unit display level rules:
  - 5-cost carry → Lv.9 (includes 5-cost units)
  - 4-cost carry → Lv.8 (1–4 cost only)
  - 3-cost reroll → Lv.7 (1–4 cost only)
  - 1–2 cost reroll → Lv.8 (1–4 cost only)
- When tab changes, both hex board layout and synergy panel update together.
- Units are sorted ascending by cost (reflects build-up order).
- If admin has set positions for a deck, show admin positions; otherwise use auto-layout based on CDragon range/role data.
- Synergy calculation uses visibleUnits of the current level tab + CDragon trait data.
- Recommended items: show top 3 core items per carry unit.
- Augment data: NOT available from Riot API — show manual curation if provided, otherwise omit.
</deck-detail>

<aggregation>
- Signature: top-2 trait name combination.
- Deck merging: Jaccard similarity (core units ≥0.55, board units ≥0.65) OR same primaryCarryId + ≥40% board overlap.
- Curation data persists by signature key — survives re-aggregation.
</aggregation>

</business-rules>

<frontend-structure>
- frontend/src/pages/Decks/       — deck list
- frontend/src/pages/DeckDetail/  — deck detail with hex board
</frontend-structure>

</spec>
