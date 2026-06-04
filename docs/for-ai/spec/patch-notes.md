<spec domain="patch-notes">

<purpose>
Patch note browsing — latest patch and version history.
Page: PatchNotes (/patch-notes).
</purpose>

<routes>
- /patch-notes → patch note list + latest patch detail
</routes>

<api>
<backend>
- GET /api/patch-notes                    — fetch patch history list (all versions)
- GET /api/patch-notes/{version}/changes  — fetch change details for a specific version
</backend>
<frontend>
- frontend/src/api/patchNotes.ts
</frontend>
</api>

<business-rules>
- Latest patch note is shown by default on page load.
- Patch history list lets users navigate to previous patches.
- Changes are categorized by: champion / item / synergy.
- version param format matches backend storage key (e.g., "14.10").
</business-rules>

<frontend-structure>
- frontend/src/pages/PatchNotes/
</frontend-structure>

</spec>
