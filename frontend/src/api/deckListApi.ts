import axiosInstance from './axiosInstance'
import type { HeroAugmentDeck, ArtifactRec } from '../pages/Decks/deckListData'
import { mockHeroAugmentDecks, mockArtifactRecs } from '../mocks/deckListMock'

export const getHeroAugments = async (): Promise<HeroAugmentDeck[]> => {
  try {
    const { data } = await axiosInstance.get<HeroAugmentDeck[]>('/decks/hero-augments')
    return data
  } catch {
    return mockHeroAugmentDecks
  }
}

export const getArtifactRecs = async (): Promise<ArtifactRec[]> => {
  try {
    const { data } = await axiosInstance.get<ArtifactRec[]>('/decks/artifacts')
    return data
  } catch {
    return mockArtifactRecs
  }
}
