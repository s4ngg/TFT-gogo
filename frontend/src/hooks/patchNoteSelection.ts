interface ResolvePatchSelectionOptions {
  hasUserSelectedPatch: boolean
  isApiData: boolean
  patchVersions: string[]
  selectedPatchVersion: string
}

export function resolvePatchSelection({
  hasUserSelectedPatch,
  isApiData,
  patchVersions,
  selectedPatchVersion,
}: ResolvePatchSelectionOptions) {
  const latestPatchVersion = patchVersions[0] ?? ''
  if (!latestPatchVersion) return selectedPatchVersion

  const hasSelectedPatch = patchVersions.includes(selectedPatchVersion)
  if (!hasSelectedPatch) return latestPatchVersion

  if (isApiData && !hasUserSelectedPatch) return latestPatchVersion

  return selectedPatchVersion
}
