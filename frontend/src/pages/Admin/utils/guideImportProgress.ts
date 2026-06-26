export function getEstimatedGuideImportProgress(elapsedSeconds: number): number {
  if (!Number.isFinite(elapsedSeconds) || elapsedSeconds <= 0) {
    return 8
  }

  return Math.min(95, 8 + Math.floor(elapsedSeconds * 4))
}
