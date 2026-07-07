interface ResolvePatchSelectionOptions {
  currentPatchVersion?: string
  hasUserSelectedPatch: boolean
  isApiData: boolean
  patchVersions: string[]
  selectedPatchVersion: string
}

export function resolvePatchSelection({
  currentPatchVersion = '',
  hasUserSelectedPatch,
  isApiData,
  patchVersions,
  selectedPatchVersion,
}: ResolvePatchSelectionOptions) {
  const defaultPatchVersion = currentPatchVersion && patchVersions.includes(currentPatchVersion)
    ? currentPatchVersion
    : patchVersions[0] ?? ''
  if (!defaultPatchVersion) return selectedPatchVersion

  const hasSelectedPatch = patchVersions.includes(selectedPatchVersion)
  if (!hasSelectedPatch) return defaultPatchVersion

  if (isApiData && !hasUserSelectedPatch) return defaultPatchVersion

  return selectedPatchVersion
}
