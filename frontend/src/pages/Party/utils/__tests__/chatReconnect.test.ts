import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  CHAT_RECONNECT_DELAYS_MS,
  getChatReconnectDelay,
  getNextChatReconnectAttempt,
  MAX_CHAT_RECONNECT_ATTEMPTS,
  shouldRetryChatConnection,
} from '../chatReconnect'

describe('chatReconnect', () => {
  it('재연결 횟수와 지연 시간을 1-based attempt로 매핑한다', () => {
    assert.equal(MAX_CHAT_RECONNECT_ATTEMPTS, 3)
    assert.deepEqual([...CHAT_RECONNECT_DELAYS_MS], [1000, 2500, 5000])
    assert.equal(getChatReconnectDelay(1), 1000)
    assert.equal(getChatReconnectDelay(2), 2500)
    assert.equal(getChatReconnectDelay(3), 5000)
  })

  it('허용 범위 밖 attempt는 재시도하지 않는다', () => {
    assert.equal(shouldRetryChatConnection(0), false)
    assert.equal(shouldRetryChatConnection(1), true)
    assert.equal(shouldRetryChatConnection(3), true)
    assert.equal(shouldRetryChatConnection(4), false)
    assert.equal(shouldRetryChatConnection(1.5), false)
  })

  it('다음 재연결 attempt를 최대 횟수까지만 반환한다', () => {
    assert.equal(getNextChatReconnectAttempt(0), 1)
    assert.equal(getNextChatReconnectAttempt(1), 2)
    assert.equal(getNextChatReconnectAttempt(2), 3)
    assert.equal(getNextChatReconnectAttempt(3), null)
  })

  it('재시도 대상이 아니면 delay를 null로 반환한다', () => {
    assert.equal(getChatReconnectDelay(0), null)
    assert.equal(getChatReconnectDelay(4), null)
  })
})
