import type { PartyFilter } from './partyFilters'

export type PartyMode = Exclude<PartyFilter, '전체'>
export type PartyIcon = 'crown' | 'leaf' | 'spark' | 'swords'
export type PartyTone = 'purple' | 'green' | 'cyan' | 'gold'

export interface PartyPost {
  capacity: string
  close: string
  description: string
  icon: PartyIcon
  id: string
  mode: PartyMode
  status: '모집중' | '대기중'
  tags: string[]
  tier: string
  title: string
  tone: PartyTone
}

export interface ChatMessage {
  isMine?: boolean
  message: string
  name: string
  roomName: string
  time: string
  tier: string
}

export interface ChatRoom {
  lastMessage: string
  name: string
  users: string
}

export interface PartyPostStyle {
  icon: PartyIcon
  tone: PartyTone
}
