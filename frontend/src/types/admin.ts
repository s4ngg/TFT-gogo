export type AdminRole = 'MASTER' | 'EDITOR' | 'VIEWER'

export interface AdminSession {
  username: string
  role: AdminRole
}
