export function resolveGuideImportPatchVersion(input: string, currentPatchVersion: string | null): string {
  const trimmed = input.trim()

  if (trimmed.toLowerCase() === 'latest' && currentPatchVersion) {
    return currentPatchVersion
  }

  return trimmed
}
