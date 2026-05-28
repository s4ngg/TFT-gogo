export interface NavItem {
  label: string
  key: 'home' | 'decks' | 'ai' | 'guide' | 'community' | 'patch'
  path: string
}

export const navItems: NavItem[] = [
  { label: '홈', key: 'home', path: '/' },
  { label: '덱모음', key: 'decks', path: '/decks' },
  { label: '덱추천(ai)', key: 'ai', path: '/ai-recommend' },
  { label: '게임 가이드', key: 'guide', path: '/guide' },
  { label: '커뮤니티', key: 'community', path: '/community' },
  { label: '패치노트', key: 'patch', path: '/patch-notes' },
]
