import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        main: 'index.html',
        aiRecommend: 'src/pages/AiRecommend/index.html',
        auth: 'src/pages/Auth/index.html',
        dashboard: 'src/pages/Dashboard/index.html',
        deckDetail: 'src/pages/DeckDetail/index.html',
        decks: 'src/pages/Decks/index.html',
        guide: 'src/pages/Guide/index.html',
        metaStats: 'src/pages/MetaStats/index.html',
        party: 'src/pages/Party/index.html',
        patchNotes: 'src/pages/PatchNotes/index.html',
        summonerDetail: 'src/pages/SummonerDetail/index.html',
      },
    },
  },
})
