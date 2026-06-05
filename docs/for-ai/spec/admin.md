<spec domain="admin">

<purpose>
Admin-only deck curation page. Compensates for automatic aggregation limitations.
Page: Admin (/admin).
</purpose>

<routes>
- /admin → admin deck management dashboard
</routes>

<api>
<backend>
- GET    /api/admin/decks                    — list all decks with curation state
- POST   /api/admin/decks/meta/aggregate     — trigger meta deck re-aggregation
- PATCH  /api/admin/decks/{deckId}           — update curation (name, hidden, order, positions)
- DELETE /api/admin/decks/{deckId}/curation  — reset curation data for a deck
</backend>
<frontend>
- frontend/src/api/adminApi.ts
</frontend>
</api>

<auth>
- Authentication: X-Admin-Token request header.
- Future plan: migrate to ROLE_ADMIN-based Spring Security.
- Never expose admin endpoints without the token check.
</auth>

<business-rules>
- Admin can override the auto-generated deck name.
- Admin can hide a deck (marks as hidden; does NOT delete the data).
- Admin can set manual display order for decks.
- Admin can assign hex-board positions per champion per deck level.
  - If positions are set, deck detail uses them instead of CDragon auto-layout.
  - Positions are stored per deck signature so they survive re-aggregation.
- Curation data key: deck signature (top-2 trait combination).
</business-rules>

<frontend-structure>
- frontend/src/pages/Admin/
</frontend-structure>

</spec>
