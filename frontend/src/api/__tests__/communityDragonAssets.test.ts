import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  TFT_ASSET_CONFIG,
  tftChampSquareUrl,
  tftItemIconUrl,
  tftSetTagFromId,
  tftTraitIconUrl,
} from '../communityDragonAssets'

describe('communityDragonAssets', () => {
  it('champion apiName의 set 번호로 HUD square 경로를 만든다', () => {
    assert.equal(
      tftChampSquareUrl('TFT18_Ahri'),
      'https://raw.communitydragon.org/latest/game/assets/characters/tft18_ahri/hud/tft18_ahri_square.tft_set18.png',
    )
  })

  it('traitId의 set 번호로 fallback trait icon 경로를 만든다', () => {
    assert.equal(
      tftTraitIconUrl('TFT18_Bruiser'),
      'https://raw.communitydragon.org/latest/game/assets/ux/traiticons/trait_icon_18_bruiser.tft_set18.png',
    )
  })

  it('검증된 trait override 경로는 유지한다', () => {
    assert.equal(
      tftTraitIconUrl('TFT17_Vanguard'),
      'https://raw.communitydragon.org/latest/game/assets/ux/traiticons/trait_icon_12_vanguard.tft_set12.png',
    )
  })

  it('item icon 기본 set tag는 현재 시즌 설정을 사용한다', () => {
    assert.equal(
      tftItemIconUrl('TFT_Item_InfinityEdge'),
      `https://raw.communitydragon.org/latest/game/assets/maps/tft/icons/items/hexcore/tft_item_infinityedge.${TFT_ASSET_CONFIG.currentSetTag}.png`,
    )
  })

  it('set 번호가 없는 값은 현재 시즌 설정으로 fallback한다', () => {
    assert.equal(tftSetTagFromId('TFT_Item_InfinityEdge'), TFT_ASSET_CONFIG.currentSetTag)
  })
})
