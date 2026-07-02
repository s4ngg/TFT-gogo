import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { SearchRateLimitError } from '../../../../api/searchApi'
import {
  RECENT_SUMMONER_SEARCHES_KEY,
  formatSummonerDetailPath,
  formatSummonerTier,
  formatSummonerWinRate,
  getSummonerSearchRetryAfterSeconds,
  mapSummonerSearchError,
  parseSummonerSearchInput,
  readRecentSummonerSearches,
  saveRecentSummonerSearch,
  type SummonerSearchStorage,
} from '../summonerSearch'

class MemoryStorage implements SummonerSearchStorage {
  private readonly values = new Map<string, string>()

  getItem(key: string): string | null {
    return this.values.get(key) ?? null
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value)
  }
}

class ThrowingStorage implements SummonerSearchStorage {
  getItem(): string | null {
    throw new Error('storage unavailable')
  }

  setItem(): void {
    throw new Error('storage unavailable')
  }
}

describe('summonerSearch', () => {
  it('빈 검색어는 검색 불가 결과를 반환한다', () => {
    const result = parseSummonerSearchInput('   ')

    assert.equal(result.ok, false)
  })

  it('태그가 없는 검색어는 KR1을 기본 태그로 사용한다', () => {
    const result = parseSummonerSearchInput('Hide on Bush')

    assert.equal(result.ok, true)
    if (result.ok) {
      assert.deepEqual(result.value, {
        gameName: 'Hide on Bush',
        normalized: 'Hide on Bush#KR1',
        tagLine: 'KR1',
      })
    }
  })

  it('소환사명과 태그 양쪽 공백을 제거한다', () => {
    const result = parseSummonerSearchInput('  닉네임 # KR1  ')

    assert.equal(result.ok, true)
    if (result.ok) {
      assert.deepEqual(result.value, {
        gameName: '닉네임',
        normalized: '닉네임#KR1',
        tagLine: 'KR1',
      })
    }
  })

  it('소환사명 또는 태그가 비어 있으면 검색 불가 결과를 반환한다', () => {
    assert.equal(parseSummonerSearchInput('#KR1').ok, false)
    assert.equal(parseSummonerSearchInput('Hide on Bush#').ok, false)
  })

  it('구분자가 2개 이상이면 검색 불가 결과를 반환한다', () => {
    assert.equal(parseSummonerSearchInput('Hide on Bush#KR1#extra').ok, false)
  })

  it('상세 경로는 소환사명과 태그를 모두 인코딩한다', () => {
    const path = formatSummonerDetailPath({
      gameName: 'Hide on Bush',
      tagLine: 'KR 1',
    })

    assert.equal(path, '/summoner/Hide%20on%20Bush/KR%201')
  })

  it('티어와 승률 표시값을 대시보드 카드용으로 포맷한다', () => {
    assert.equal(formatSummonerTier('MASTER', 'I'), '마스터 I')
    assert.equal(formatSummonerTier('MASTER', null), '마스터')
    assert.equal(formatSummonerTier(null, null), '언랭크')
    assert.equal(formatSummonerWinRate(10, 5), '67%')
    assert.equal(formatSummonerWinRate(0, 0), '-')
  })

  it('프로필 조회 오류 메시지를 검색 결과 상태로 변환한다', () => {
    assert.equal(mapSummonerSearchError(new Error('NOT_FOUND')), 'notFound')
    assert.equal(mapSummonerSearchError(new Error('RATE_LIMITED')), 'rateLimited')
    assert.equal(mapSummonerSearchError(new SearchRateLimitError(42)), 'rateLimited')
    assert.equal(mapSummonerSearchError(new Error('network')), 'error')
  })

  it('rate limit 오류의 retry-after 초를 추출한다', () => {
    assert.equal(getSummonerSearchRetryAfterSeconds(new SearchRateLimitError(42)), 42)
    assert.equal(getSummonerSearchRetryAfterSeconds(new Error('RATE_LIMITED')), 120)
    assert.equal(getSummonerSearchRetryAfterSeconds(new Error('network')), null)
  })

  it('최근 검색은 최신 우선, 중복 제거, 최대 5개로 저장한다', () => {
    const storage = new MemoryStorage()

    saveRecentSummonerSearch('one#KR1', storage)
    saveRecentSummonerSearch('two#KR1', storage)
    saveRecentSummonerSearch('three#KR1', storage)
    saveRecentSummonerSearch('four#KR1', storage)
    saveRecentSummonerSearch('five#KR1', storage)
    const result = saveRecentSummonerSearch('two#KR1', storage)

    assert.deepEqual(result, ['two#KR1', 'five#KR1', 'four#KR1', 'three#KR1', 'one#KR1'])
    assert.deepEqual(readRecentSummonerSearches(storage), result)
  })

  it('깨진 recent search JSON은 빈 배열로 처리한다', () => {
    const storage = new MemoryStorage()
    storage.setItem(RECENT_SUMMONER_SEARCHES_KEY, '{broken')

    assert.deepEqual(readRecentSummonerSearches(storage), [])
  })

  it('storage 예외는 검색 흐름을 깨지 않는다', () => {
    const storage = new ThrowingStorage()

    assert.deepEqual(readRecentSummonerSearches(storage), [])
    assert.deepEqual(saveRecentSummonerSearch('Hide on Bush#KR1', storage), [])
  })
})
