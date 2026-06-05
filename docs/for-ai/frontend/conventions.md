<spec domain="frontend">

<stack>TypeScript · React · Vite · TanStack Query · Zustand · CSS Modules · Axios</stack>

<conventions>

<style>
- Use CSS Modules (*.module.css). Tailwind, styled-components, and inline styles are forbidden.
- Design tokens (color, size, radius) must come from frontend/src/styles/variables.css CSS variables.
  Available token prefixes: --color-*, --tone-*, --space-*, --font-size-*, --radius-*
  If a new token is needed, add it to variables.css first, then reference it.
- CSS Modules class names must be camelCase. snake_case is forbidden.
  BAD:  styles[`tone_${t.tone}`]
  GOOD: const TONE_CLASS_MAP: Record&lt;string, string&gt; = { gold: styles.toneGold, ... }; TONE_CLASS_MAP[t.tone]
- Do not hardcode CDN URLs inside components. Use helpers from frontend/src/api/communityDragonAssets.ts.
</style>

<typescript>
- any is forbidden. Use unknown or explicit types.
- Prefer interface for type definitions; use type only for unions/intersections.
- Use functional components only (no class components). Export as export default.
- Every async function must have try/catch error handling.
</typescript>

<file-structure>
- Component files: PascalCase.tsx
- Hook / util / API files: camelCase.ts
- Page-specific components: pages/&lt;PageName&gt;/components/
- Components shared across 2+ pages: components/common/
- Vite env vars must use VITE_ prefix. Never commit .env files.
</file-structure>

<state-and-http>
- Server state: TanStack Query only. Do not fetch with useEffect + useState.
- Global client state: Zustand only. Do not store server data in Zustand.
- HTTP requests: use frontend/src/api/axiosInstance.ts and api/ layer functions.
  Do not import fetch or axios directly inside components.
</state-and-http>

<security>
- Never log sensitive data (passwords, JWT tokens, auth codes).
</security>

</conventions>

</spec>
