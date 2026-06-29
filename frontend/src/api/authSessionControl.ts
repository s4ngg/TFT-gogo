let logoutInProgress = false
let authSessionRevision = 0

export function setLogoutInProgress(value: boolean): void {
  if (value) {
    authSessionRevision += 1
  }

  logoutInProgress = value
}

export function isLogoutInProgress(): boolean {
  return logoutInProgress
}

export function getAuthSessionRevision(): number {
  return authSessionRevision
}
