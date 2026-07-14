import type { CommunityChatRoomId } from '../../constants/communityChatRooms'
import type { PartyFilter } from './partyFilters'

export type PartyMode = Exclude<PartyFilter, '전체'>
export type PartyIcon = 'crown' | 'leaf' | 'spark' | 'swords'
export type PartyTone = 'purple' | 'green' | 'cyan' | 'gold'

export interface PartyPost {
  capacity: string
  chatRoomId: CommunityChatRoomId
  close: string
  description: string
  icon: PartyIcon
  id: string
  isClosed: boolean
  isDeadlineExpired?: boolean
  isJoined?: boolean
  isOwner?: boolean
  mode: PartyMode
  status: '모집중' | '대기중'
  tags: string[]
  tier: string
  title: string
  tone: PartyTone
  userId?: string
}

export interface ChatRoom {
  id: CommunityChatRoomId
  lastMessage: string
  name: string
  users: string
}

export interface PartyPostStyle {
  icon: PartyIcon
  tone: PartyTone
}
