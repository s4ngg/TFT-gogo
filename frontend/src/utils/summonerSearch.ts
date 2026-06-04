export function parseSummonerInput(input: string): { gameName: string; tagLine: string } | null {
  const trimmed = input.trim()
  const hashIndex = trimmed.indexOf('#')
  if (hashIndex <= 0 || hashIndex === trimmed.length - 1) return null
  return {
    gameName: trimmed.slice(0, hashIndex),
    tagLine: trimmed.slice(hashIndex + 1),
  }
}

export function summonerPath(gameName: string, tagLine: string): string {
  return `/summoner/${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}`
}
