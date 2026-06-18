import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { resolveApiBaseUrl } from '../axiosInstance'

describe('resolveApiBaseUrl', () => {
  it('환경변수가 없으면 기본 /api를 사용한다', () => {
    assert.equal(resolveApiBaseUrl({}), '/api')
  })

  it('환경변수가 공백이면 기본 /api를 사용한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiBaseUrl: '   ',
        apiUrl: '',
      }),
      '/api',
    )
  })

  it('VITE_API_URL을 VITE_API_BASE_URL보다 우선한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiBaseUrl: 'https://fallback.example.com/api',
        apiUrl: 'https://primary.example.com/api',
      }),
      'https://primary.example.com/api',
    )
  })

  it('현재 프론트 origin만 지정되면 /api로 보정한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:3000',
        currentOrigin: 'http://localhost:3000',
      }),
      '/api',
    )
  })

  it('현재 프론트 origin에 trailing slash가 있어도 /api로 보정한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:3000/',
        currentOrigin: 'http://localhost:3000',
      }),
      '/api',
    )
  })

  it('현재 origin이라도 /api 경로가 명시되어 있으면 유지한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:3000/api',
        currentOrigin: 'http://localhost:3000',
      }),
      'http://localhost:3000/api',
    )
  })

  it('별도 API origin의 /api 경로는 유지한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:8080/api',
        currentOrigin: 'http://localhost:3000',
      }),
      'http://localhost:8080/api',
    )
  })
})
