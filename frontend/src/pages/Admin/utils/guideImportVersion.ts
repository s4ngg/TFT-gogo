export function isLatestGuideImportPatchVersion(input: string): boolean {
  return input.trim().toLowerCase() === 'latest'
}

export function resolveGuideImportPatchVersion(input: string, currentPatchVersion: string | null): string {
  const trimmed = input.trim()

  if (isLatestGuideImportPatchVersion(trimmed) && currentPatchVersion) {
    return currentPatchVersion
  }

  return trimmed
}
