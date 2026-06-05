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
- Changes are categorized by backend enum values: CHAMPION, TRAIT, ITEM, AUGMENT, SYSTEM.
- Change type enum values are BUFF, NERF, ADJUST, NEW.
- Impact enum values are HIGH, MEDIUM, LOW.
- version param format matches backend storage key (e.g., "17.3").
- Public list response uses ApiResponse&lt;List&lt;PatchNoteResponse&gt;&gt; and does not include full changes.
- Change list response uses ApiResponse&lt;PatchChangePageResponse&gt; and includes items, page, pageSize, totalItems, totalPages, stats.
- stats are calculated from the selected patch version as a whole, not from the currently filtered page.
- Local DB smoke data lives in patch_notes and patch_changes. patch_notes.version identifies one patch note.
- Admin writes use /api/admin/patch-notes and /api/admin/patch-note-changes.
- isCurrent must stay unique among active patch notes.
- highlightsJson and tagsJson must validate as JSON string arrays.
- Admin delete uses soft delete through isActive/deletedAt.
</business-rules>

<frontend-structure>
- frontend/src/pages/PatchNotes/
</frontend-structure>

</spec>
