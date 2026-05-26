// riot-account-v1: GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}
export interface RiotAccountDto {
  puuid: string
  gameName: string
  tagLine: string
}

export const mockRiotAccount: RiotAccountDto = {
  puuid: 'mock-puuid-player-01',
  gameName: 'TFTgogo',
  tagLine: 'KR1',
}

// tft-summoner-v1: GET /tft/summoner/v1/summoners/by-puuid/{encryptedPUUID}
export interface RiotTftSummonerDto {
  puuid: string
  profileIconId: number
  revisionDate: number
  summonerLevel: number
}

export interface RiotMiniSeriesDto {
  losses: number
  progress: string
  target: number
  wins: number
}

// tft-league-v1: GET /tft/league/v1/by-puuid/{puuid}
export interface RiotTftLeagueEntryDto {
  puuid: string
  leagueId: string
  queueType: string           // 'RANKED_TFT' | 'RANKED_TFT_TURBO'
  tier?: string               // RANKED_TFT only
  rank?: string               // RANKED_TFT only
  leaguePoints?: number       // RANKED_TFT only
  ratedTier?: string          // RANKED_TFT_TURBO only (ORANGE|PURPLE|BLUE|GREEN|GRAY)
  ratedRating?: number        // RANKED_TFT_TURBO only
  wins: number                // 1등 횟수
  losses: number              // 2~8등 횟수
  hotStreak?: boolean
  veteran?: boolean
  freshBlood?: boolean
  inactive?: boolean
  miniSeries?: RiotMiniSeriesDto
}

export interface RiotTftTraitDto {
  name: string
  num_units: number
  style: number
  tier_current: number
  tier_total: number
}

export interface RiotTftUnitDto {
  items: number[]        // 정수 아이템 ID 목록 (신규)
  character_id: string
  itemNames: string[]    // 문자열 아이템 ID 목록 (구형, 병행 제공)
  chosen?: string        // 선택된 특성 (특정 세트 한정, 보통 응답에 없음)
  name: string           // 유닛 이름 (often left blank — character_id 사용 권장)
  rarity: number         // 희귀도 (코스트와 다름: 0=1코, 1=2코, 2=3코, 4=4코, 6=5코)
  tier: number           // 별 수 (1/2/3)
}

export interface RiotTftParticipantDto {
  augments: string[]
  companion: {
    content_ID: string
    item_ID: number
    skin_ID: number
    species: string
  }
  gold_left: number
  last_round: number
  level: number
  placement: number
  players_eliminated: number
  puuid: string
  riotIdGameName: string
  riotIdTagline: string
  time_eliminated: number
  total_damage_to_players: number
  traits: RiotTftTraitDto[]
  units: RiotTftUnitDto[]
  win: boolean
}

export interface RiotTftMatchDto {
  metadata: {
    data_version: string
    match_id: string
    participants: string[]
  }
  info: {
    endOfGameResult: string
    gameCreation: number
    gameId: number
    game_datetime: number
    game_length: number
    game_version: string
    mapId: number
    queue_id: number
    tft_game_type: string
    tft_set_number: number
    participants: RiotTftParticipantDto[]
  }
}

export const mockTftSummoner: RiotTftSummonerDto = {
  puuid: 'mock-puuid-player-01',
  profileIconId: 29,
  revisionDate: 1779788400000,
  summonerLevel: 387,
}

export const mockTftLeagueEntry: RiotTftLeagueEntryDto = {
  puuid: mockRiotAccount.puuid,
  leagueId: 'f7a22f2e-kr-tft-diamond',
  queueType: 'RANKED_TFT',
  tier: 'DIAMOND',
  rank: 'IV',
  leaguePoints: 45,
  wins: 256,
  losses: 137,
  veteran: false,
  inactive: false,
  freshBlood: false,
  hotStreak: true,
}

export const mockTftMatchIds = [
  'KR_7600010001',
  'KR_7600010002',
  'KR_7600010003',
]

const playerPuuid = mockTftSummoner.puuid

// items: number[] — 정수 아이템 ID (정확한 값은 TFT Data Dragon 참고)
// itemNames: string[] — 문자열 아이템 ID (두 필드 병행 제공)
export const mockTftMatches: RiotTftMatchDto[] = [
  // ── 매치 1: 플레이어 1등 ──────────────────────────────────────
  {
    metadata: {
      data_version: '2',
      match_id: 'KR_7600010001',
      participants: [playerPuuid, 'mock-puuid-02', 'mock-puuid-03', 'mock-puuid-04', 'mock-puuid-05', 'mock-puuid-06', 'mock-puuid-07', 'mock-puuid-08'],
    },
    info: {
      endOfGameResult: 'GameComplete',
      gameCreation: 1779782400000,
      gameId: 7600010001,
      game_datetime: 1779784800000,
      game_length: 2142.8,
      game_version: 'Version 16.10.1',
      mapId: 22,
      queue_id: 1100,
      tft_game_type: 'standard',
      tft_set_number: 17,
      participants: [
        {
          augments: ['TFT17_Augment_Guidebook', 'TFT17_Augment_Manaflow', 'TFT17_Augment_FinalPolish'],
          companion: { content_ID: 'd5f6a9', item_ID: 115, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 8, last_round: 38, level: 9, placement: 1, players_eliminated: 3,
          puuid: playerPuuid, riotIdGameName: 'TFTgogo', riotIdTagline: 'KR1',
          time_eliminated: 2142.8, total_damage_to_players: 162, win: true,
          traits: [
            { name: 'TFT17_Guide', num_units: 6, style: 3, tier_current: 3, tier_total: 4 },
            { name: 'TFT17_StarGuardian', num_units: 3, style: 2, tier_current: 2, tier_total: 3 },
            { name: 'TFT17_Bastion', num_units: 2, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3039, 3165, 3003], character_id: 'TFT17_Nami', itemNames: ['TFT_Item_SpearOfShojin', 'TFT_Item_Morellonomicon', 'TFT_Item_ArchangelsStaff'], name: '', rarity: 3, tier: 3 },
            { items: [3083, 3193], character_id: 'TFT17_Poppy', itemNames: ['TFT_Item_WarmogsArmor', 'TFT_Item_GargoyleStoneplate'], name: '', rarity: 2, tier: 2 },
            { items: [3068], character_id: 'TFT17_Aurora', itemNames: ['TFT_Item_BlueBuff'], name: '', rarity: 4, tier: 2 },
            { items: [], character_id: 'TFT17_Jinx', itemNames: [], name: '', rarity: 4, tier: 2 },
            { items: [3046], character_id: 'TFT17_Lulu', itemNames: ['TFT_Item_StatikkShiv'], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Bard', itemNames: [], name: '', rarity: 1, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_DarkStar', 'TFT17_Augment_TitanicStrength', 'TFT17_Augment_Shredder'],
          companion: { content_ID: 'a1b2c3', item_ID: 120, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 3, last_round: 36, level: 8, placement: 2, players_eliminated: 2,
          puuid: 'mock-puuid-02', riotIdGameName: 'Faker', riotIdTagline: 'T1',
          time_eliminated: 2098.4, total_damage_to_players: 134, win: false,
          traits: [
            { name: 'TFT17_DarkStar', num_units: 6, style: 3, tier_current: 3, tier_total: 4 },
            { name: 'TFT17_Sniper', num_units: 2, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3031, 3124, 3091], character_id: 'TFT17_Jhin', itemNames: ['TFT_Item_InfinityEdge', 'TFT_Item_GuinsoosRageblade', 'TFT_Item_GiantSlayer'], name: '', rarity: 4, tier: 3 },
            { items: [3046], character_id: 'TFT17_Kaisa', itemNames: ['TFT_Item_StatikkShiv'], name: '', rarity: 4, tier: 2 },
            { items: [], character_id: 'TFT17_Morgana', itemNames: [], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Belveth', itemNames: [], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_ItemGrabBag', 'TFT17_Augment_CyberneticBulk', 'TFT17_Augment_SecondWind'],
          companion: { content_ID: 'b2c3d4', item_ID: 130, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 5, last_round: 34, level: 8, placement: 3, players_eliminated: 1,
          puuid: 'mock-puuid-03', riotIdGameName: '마포이는카', riotIdTagline: 'HAPPY',
          time_eliminated: 1960.2, total_damage_to_players: 98, win: false,
          traits: [
            { name: 'TFT17_Vanguard', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
            { name: 'TFT17_StarGuardian', num_units: 3, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3068, 3003, 3165], character_id: 'TFT17_Corki', itemNames: ['TFT_Item_BlueBuff', 'TFT_Item_ArchangelsStaff', 'TFT_Item_Morellonomicon'], name: '', rarity: 2, tier: 3 },
            { items: [3083], character_id: 'TFT17_Ornn', itemNames: ['TFT_Item_WarmogsArmor'], name: '', rarity: 6, tier: 2 },
            { items: [], character_id: 'TFT17_Sona', itemNames: [], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Replicator', 'TFT17_Augment_ComponentGrabBag', 'TFT17_Augment_FinalPolish'],
          companion: { content_ID: 'c3d4e5', item_ID: 110, skin_ID: 3, species: 'PetTFTAvatar' },
          gold_left: 11, last_round: 32, level: 7, placement: 4, players_eliminated: 0,
          puuid: 'mock-puuid-04', riotIdGameName: '서은지', riotIdTagline: '봄봄7',
          time_eliminated: 1821.6, total_damage_to_players: 77, win: false,
          traits: [
            { name: 'TFT17_Rogue', num_units: 6, style: 3, tier_current: 3, tier_total: 3 },
          ],
          units: [
            { items: [3031, 3072], character_id: 'TFT17_MasterYi', itemNames: ['TFT_Item_InfinityEdge', 'TFT_Item_Bloodthirster'], name: '', rarity: 4, tier: 3 },
            { items: [], character_id: 'TFT17_Zed', itemNames: [], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Akali', itemNames: [], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Manaflow', 'TFT17_Augment_TitanicStrength', 'TFT17_Augment_Guidebook'],
          companion: { content_ID: 'd4e5f6', item_ID: 105, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 7, last_round: 29, level: 7, placement: 5, players_eliminated: 1,
          puuid: 'mock-puuid-05', riotIdGameName: '산천어풍보대사', riotIdTagline: '1234',
          time_eliminated: 1644.0, total_damage_to_players: 55, win: false,
          traits: [
            { name: 'TFT17_PsyOps', num_units: 3, style: 1, tier_current: 1, tier_total: 3 },
            { name: 'TFT17_Bastion', num_units: 2, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3116, 3040], character_id: 'TFT17_Viktor', itemNames: ['TFT_Item_RabadonsDeathcap', 'TFT_Item_ArchangelsStaff'], name: '', rarity: 6, tier: 2 },
            { items: [], character_id: 'TFT17_Rammus', itemNames: [], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_DarkStar', 'TFT17_Augment_SecondWind', 'TFT17_Augment_Shredder'],
          companion: { content_ID: 'e5f6a7', item_ID: 118, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 2, last_round: 26, level: 7, placement: 6, players_eliminated: 0,
          puuid: 'mock-puuid-06', riotIdGameName: '복조선특크', riotIdTagline: 'KR1',
          time_eliminated: 1489.3, total_damage_to_players: 41, win: false,
          traits: [
            { name: 'TFT17_Replicator', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3083, 3193, 3076], character_id: 'TFT17_Illaoi', itemNames: ['TFT_Item_WarmogsArmor', 'TFT_Item_GargoyleStoneplate', 'TFT_Item_DragonsClaw'], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Poppy', itemNames: [], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_ItemGrabBag', 'TFT17_Augment_Manaflow', 'TFT17_Augment_Replicator'],
          companion: { content_ID: 'f6a7b8', item_ID: 112, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 22, level: 6, placement: 7, players_eliminated: 0,
          puuid: 'mock-puuid-07', riotIdGameName: '판키', riotIdTagline: '5597',
          time_eliminated: 1267.5, total_damage_to_players: 28, win: false,
          traits: [
            { name: 'TFT17_Sniper', num_units: 2, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3046, 3124], character_id: 'TFT17_Xayah', itemNames: ['TFT_Item_StatikkShiv', 'TFT_Item_GuinsoosRageblade'], name: '', rarity: 4, tier: 2 },
            { items: [], character_id: 'TFT17_Gnar', itemNames: [], name: '', rarity: 1, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_CyberneticBulk', 'TFT17_Augment_FinalPolish', 'TFT17_Augment_ComponentGrabBag'],
          companion: { content_ID: 'a7b8c9', item_ID: 108, skin_ID: 3, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 18, level: 6, placement: 8, players_eliminated: 0,
          puuid: 'mock-puuid-08', riotIdGameName: '뻐코영', riotIdTagline: 'KR1',
          time_eliminated: 1022.1, total_damage_to_players: 14, win: false,
          traits: [
            { name: 'TFT17_Vanguard', num_units: 2, style: 0, tier_current: 0, tier_total: 3 },
          ],
          units: [
            { items: [], character_id: 'TFT17_Blitzcrank', itemNames: [], name: '', rarity: 2, tier: 1 },
            { items: [], character_id: 'TFT17_Shen', itemNames: [], name: '', rarity: 2, tier: 1 },
          ],
        },
      ],
    },
  },

  // ── 매치 2: 플레이어 3등 ──────────────────────────────────────
  {
    metadata: {
      data_version: '2',
      match_id: 'KR_7600010002',
      participants: [playerPuuid, 'mock-puuid-12', 'mock-puuid-13', 'mock-puuid-14', 'mock-puuid-15', 'mock-puuid-16', 'mock-puuid-17', 'mock-puuid-18'],
    },
    info: {
      endOfGameResult: 'GameComplete',
      gameCreation: 1779696000000,
      gameId: 7600010002,
      game_datetime: 1779698400000,
      game_length: 1988.4,
      game_version: 'Version 16.10.1',
      mapId: 22,
      queue_id: 1100,
      tft_game_type: 'standard',
      tft_set_number: 17,
      participants: [
        {
          augments: ['TFT17_Augment_DarkStar', 'TFT17_Augment_Shredder', 'TFT17_Augment_FinalPolish'],
          companion: { content_ID: 'x1y2z3', item_ID: 116, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 12, last_round: 37, level: 9, placement: 1, players_eliminated: 4,
          puuid: 'mock-puuid-12', riotIdGameName: '대화화화화', riotIdTagline: 'KR1',
          time_eliminated: 1988.4, total_damage_to_players: 178, win: true,
          traits: [
            { name: 'TFT17_DarkStar', num_units: 6, style: 3, tier_current: 3, tier_total: 4 },
            { name: 'TFT17_Rogue', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3031, 3072, 3091], character_id: 'TFT17_Jhin', itemNames: ['TFT_Item_InfinityEdge', 'TFT_Item_Bloodthirster', 'TFT_Item_GuinsoosRageblade'], name: '', rarity: 4, tier: 3 },
            { items: [], character_id: 'TFT17_Kaisa', itemNames: [], name: '', rarity: 4, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Guidebook', 'TFT17_Augment_Manaflow', 'TFT17_Augment_SecondWind'],
          companion: { content_ID: 'x2y3z4', item_ID: 117, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 6, last_round: 35, level: 8, placement: 2, players_eliminated: 2,
          puuid: 'mock-puuid-13', riotIdGameName: '별하나', riotIdTagline: 'KR2',
          time_eliminated: 1920.1, total_damage_to_players: 122, win: false,
          traits: [
            { name: 'TFT17_StarGuardian', num_units: 6, style: 3, tier_current: 3, tier_total: 3 },
          ],
          units: [
            { items: [3039, 3165, 3068], character_id: 'TFT17_Lulu', itemNames: ['TFT_Item_SpearOfShojin', 'TFT_Item_Morellonomicon', 'TFT_Item_BlueBuff'], name: '', rarity: 2, tier: 3 },
            { items: [], character_id: 'TFT17_Poppy', itemNames: [], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Shredder', 'TFT17_Augment_SecondWind', 'TFT17_Augment_ItemGrabBag'],
          companion: { content_ID: 'd5f6a9', item_ID: 115, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 2, last_round: 34, level: 8, placement: 3, players_eliminated: 1,
          puuid: playerPuuid, riotIdGameName: 'TFTgogo', riotIdTagline: 'KR1',
          time_eliminated: 1864.8, total_damage_to_players: 112, win: false,
          traits: [
            { name: 'TFT17_Rogue', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
            { name: 'TFT17_DarkStar', num_units: 4, style: 2, tier_current: 2, tier_total: 4 },
            { name: 'TFT17_Sniper', num_units: 2, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3031, 3101, 3124], character_id: 'TFT17_Jhin', itemNames: ['TFT_Item_InfinityEdge', 'TFT_Item_GiantSlayer', 'TFT_Item_GuinsoosRageblade'], name: '', rarity: 4, tier: 2 },
            { items: [3072], character_id: 'TFT17_Zed', itemNames: ['TFT_Item_Bloodthirster'], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Kaisa', itemNames: [], name: '', rarity: 4, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_CyberneticBulk', 'TFT17_Augment_TitanicStrength', 'TFT17_Augment_Replicator'],
          companion: { content_ID: 'x4y5z6', item_ID: 119, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 4, last_round: 30, level: 7, placement: 4, players_eliminated: 0,
          puuid: 'mock-puuid-15', riotIdGameName: '나는고수', riotIdTagline: 'KR1',
          time_eliminated: 1712.3, total_damage_to_players: 81, win: false,
          traits: [
            { name: 'TFT17_Vanguard', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3083, 3193], character_id: 'TFT17_Illaoi', itemNames: ['TFT_Item_WarmogsArmor', 'TFT_Item_GargoyleStoneplate'], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Ornn', itemNames: [], name: '', rarity: 6, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Manaflow', 'TFT17_Augment_Guidebook', 'TFT17_Augment_ComponentGrabBag'],
          companion: { content_ID: 'x5y6z7', item_ID: 114, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 8, last_round: 27, level: 7, placement: 5, players_eliminated: 0,
          puuid: 'mock-puuid-16', riotIdGameName: '달려라하니', riotIdTagline: 'KR3',
          time_eliminated: 1555.8, total_damage_to_players: 59, win: false,
          traits: [
            { name: 'TFT17_Guide', num_units: 4, style: 2, tier_current: 2, tier_total: 4 },
          ],
          units: [
            { items: [3039, 3003], character_id: 'TFT17_Nami', itemNames: ['TFT_Item_SpearOfShojin', 'TFT_Item_ArchangelsStaff'], name: '', rarity: 3, tier: 2 },
            { items: [], character_id: 'TFT17_Aurora', itemNames: [], name: '', rarity: 4, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_SecondWind', 'TFT17_Augment_DarkStar', 'TFT17_Augment_Shredder'],
          companion: { content_ID: 'x6y7z8', item_ID: 111, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 1, last_round: 24, level: 6, placement: 6, players_eliminated: 0,
          puuid: 'mock-puuid-17', riotIdGameName: '행복한고양이', riotIdTagline: 'KR1',
          time_eliminated: 1390.4, total_damage_to_players: 37, win: false,
          traits: [
            { name: 'TFT17_PsyOps', num_units: 3, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3116, 3068], character_id: 'TFT17_Viktor', itemNames: ['TFT_Item_RabadonsDeathcap', 'TFT_Item_BlueBuff'], name: '', rarity: 6, tier: 1 },
          ],
        },
        {
          augments: ['TFT17_Augment_ItemGrabBag', 'TFT17_Augment_FinalPolish', 'TFT17_Augment_Manaflow'],
          companion: { content_ID: 'x7y8z9', item_ID: 109, skin_ID: 3, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 20, level: 6, placement: 7, players_eliminated: 0,
          puuid: 'mock-puuid-14', riotIdGameName: '새벽감성', riotIdTagline: 'KR2',
          time_eliminated: 1198.7, total_damage_to_players: 22, win: false,
          traits: [
            { name: 'TFT17_Sniper', num_units: 2, style: 0, tier_current: 0, tier_total: 3 },
          ],
          units: [
            { items: [], character_id: 'TFT17_Jinx', itemNames: [], name: '', rarity: 4, tier: 1 },
          ],
        },
        {
          augments: ['TFT17_Augment_Replicator', 'TFT17_Augment_CyberneticBulk', 'TFT17_Augment_TitanicStrength'],
          companion: { content_ID: 'x8y9z0', item_ID: 106, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 16, level: 5, placement: 8, players_eliminated: 0,
          puuid: 'mock-puuid-18', riotIdGameName: '초보탈출', riotIdTagline: 'KR1',
          time_eliminated: 958.2, total_damage_to_players: 9, win: false,
          traits: [],
          units: [
            { items: [], character_id: 'TFT17_Vex', itemNames: [], name: '', rarity: 1, tier: 1 },
          ],
        },
      ],
    },
  },

  // ── 매치 3: 플레이어 6등 ──────────────────────────────────────
  {
    metadata: {
      data_version: '2',
      match_id: 'KR_7600010003',
      participants: [playerPuuid, 'mock-puuid-22', 'mock-puuid-23', 'mock-puuid-24', 'mock-puuid-25', 'mock-puuid-26', 'mock-puuid-27', 'mock-puuid-28'],
    },
    info: {
      endOfGameResult: 'GameComplete',
      gameCreation: 1779609600000,
      gameId: 7600010003,
      game_datetime: 1779612000000,
      game_length: 1814.2,
      game_version: 'Version 16.10.1',
      mapId: 22,
      queue_id: 1100,
      tft_game_type: 'standard',
      tft_set_number: 17,
      participants: [
        {
          augments: ['TFT17_Augment_Guidebook', 'TFT17_Augment_FinalPolish', 'TFT17_Augment_TitanicStrength'],
          companion: { content_ID: 'p1q2r3', item_ID: 113, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 15, last_round: 36, level: 9, placement: 1, players_eliminated: 3,
          puuid: 'mock-puuid-22', riotIdGameName: '최강자', riotIdTagline: 'KR1',
          time_eliminated: 1814.2, total_damage_to_players: 155, win: true,
          traits: [
            { name: 'TFT17_Guide', num_units: 6, style: 3, tier_current: 3, tier_total: 4 },
            { name: 'TFT17_Bastion', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3039, 3165, 3068], character_id: 'TFT17_Nami', itemNames: ['TFT_Item_SpearOfShojin', 'TFT_Item_Morellonomicon', 'TFT_Item_BlueBuff'], name: '', rarity: 3, tier: 3 },
            { items: [3083, 3193], character_id: 'TFT17_Poppy', itemNames: ['TFT_Item_WarmogsArmor', 'TFT_Item_GargoyleStoneplate'], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_DarkStar', 'TFT17_Augment_Manaflow', 'TFT17_Augment_Shredder'],
          companion: { content_ID: 'p2q3r4', item_ID: 121, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 9, last_round: 34, level: 8, placement: 2, players_eliminated: 2,
          puuid: 'mock-puuid-23', riotIdGameName: '다이아드림', riotIdTagline: 'KR2',
          time_eliminated: 1754.6, total_damage_to_players: 130, win: false,
          traits: [
            { name: 'TFT17_DarkStar', num_units: 6, style: 3, tier_current: 3, tier_total: 4 },
          ],
          units: [
            { items: [3031, 3091, 3072], character_id: 'TFT17_Jhin', itemNames: ['TFT_Item_InfinityEdge', 'TFT_Item_GuinsoosRageblade', 'TFT_Item_Bloodthirster'], name: '', rarity: 4, tier: 3 },
          ],
        },
        {
          augments: ['TFT17_Augment_SecondWind', 'TFT17_Augment_ItemGrabBag', 'TFT17_Augment_CyberneticBulk'],
          companion: { content_ID: 'p3q4r5', item_ID: 107, skin_ID: 3, species: 'PetTFTAvatar' },
          gold_left: 3, last_round: 31, level: 8, placement: 3, players_eliminated: 1,
          puuid: 'mock-puuid-24', riotIdGameName: '실버탈출', riotIdTagline: 'KR3',
          time_eliminated: 1680.9, total_damage_to_players: 95, win: false,
          traits: [
            { name: 'TFT17_Rogue', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3031, 3072], character_id: 'TFT17_MasterYi', itemNames: ['TFT_Item_InfinityEdge', 'TFT_Item_Bloodthirster'], name: '', rarity: 4, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Replicator', 'TFT17_Augment_Guidebook', 'TFT17_Augment_DarkStar'],
          companion: { content_ID: 'p4q5r6', item_ID: 123, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 6, last_round: 29, level: 7, placement: 4, players_eliminated: 0,
          puuid: 'mock-puuid-25', riotIdGameName: '골드향해', riotIdTagline: 'KR1',
          time_eliminated: 1587.3, total_damage_to_players: 74, win: false,
          traits: [
            { name: 'TFT17_StarGuardian', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3039, 3003], character_id: 'TFT17_Lulu', itemNames: ['TFT_Item_SpearOfShojin', 'TFT_Item_ArchangelsStaff'], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_FinalPolish', 'TFT17_Augment_SecondWind', 'TFT17_Augment_Manaflow'],
          companion: { content_ID: 'p5q6r7', item_ID: 104, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 27, level: 7, placement: 5, players_eliminated: 0,
          puuid: 'mock-puuid-26', riotIdGameName: '밤하늘별', riotIdTagline: 'KR2',
          time_eliminated: 1499.7, total_damage_to_players: 52, win: false,
          traits: [
            { name: 'TFT17_Vanguard', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
          ],
          units: [
            { items: [3083, 3076], character_id: 'TFT17_Illaoi', itemNames: ['TFT_Item_WarmogsArmor', 'TFT_Item_DragonsClaw'], name: '', rarity: 2, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_Replicator', 'TFT17_Augment_ComponentGrabBag', 'TFT17_Augment_CyberneticBulk'],
          companion: { content_ID: 'd5f6a9', item_ID: 115, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 1, last_round: 25, level: 7, placement: 6, players_eliminated: 0,
          puuid: playerPuuid, riotIdGameName: 'TFTgogo', riotIdTagline: 'KR1',
          time_eliminated: 1420.5, total_damage_to_players: 62, win: false,
          traits: [
            { name: 'TFT17_Replicator', num_units: 4, style: 2, tier_current: 2, tier_total: 3 },
            { name: 'TFT17_PsyOps', num_units: 3, style: 1, tier_current: 1, tier_total: 3 },
            { name: 'TFT17_Vanguard', num_units: 2, style: 1, tier_current: 1, tier_total: 3 },
          ],
          units: [
            { items: [3068, 3116], character_id: 'TFT17_Viktor', itemNames: ['TFT_Item_BlueBuff', 'TFT_Item_RabadonsDeathcap'], name: '', rarity: 6, tier: 1 },
            { items: [3076], character_id: 'TFT17_Rammus', itemNames: ['TFT_Item_DragonsClaw'], name: '', rarity: 2, tier: 2 },
            { items: [], character_id: 'TFT17_Vex', itemNames: [], name: '', rarity: 1, tier: 2 },
          ],
        },
        {
          augments: ['TFT17_Augment_TitanicStrength', 'TFT17_Augment_Shredder', 'TFT17_Augment_ItemGrabBag'],
          companion: { content_ID: 'p7q8r9', item_ID: 102, skin_ID: 1, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 21, level: 6, placement: 7, players_eliminated: 0,
          puuid: 'mock-puuid-27', riotIdGameName: '플레저니', riotIdTagline: 'KR1',
          time_eliminated: 1255.3, total_damage_to_players: 30, win: false,
          traits: [
            { name: 'TFT17_Sniper', num_units: 2, style: 0, tier_current: 0, tier_total: 3 },
          ],
          units: [
            { items: [], character_id: 'TFT17_Jinx', itemNames: [], name: '', rarity: 4, tier: 1 },
          ],
        },
        {
          augments: ['TFT17_Augment_Manaflow', 'TFT17_Augment_Guidebook', 'TFT17_Augment_DarkStar'],
          companion: { content_ID: 'p8q9r0', item_ID: 103, skin_ID: 2, species: 'PetTFTAvatar' },
          gold_left: 0, last_round: 17, level: 5, placement: 8, players_eliminated: 0,
          puuid: 'mock-puuid-28', riotIdGameName: '입문자', riotIdTagline: 'KR4',
          time_eliminated: 1044.6, total_damage_to_players: 11, win: false,
          traits: [],
          units: [
            { items: [], character_id: 'TFT17_Corki', itemNames: [], name: '', rarity: 2, tier: 1 },
          ],
        },
      ],
    },
  },
]
