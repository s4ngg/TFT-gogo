import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const adminLoginStylePath = fileURLToPath(new URL('../AdminLogin.module.css', import.meta.url))

test('관리자 로그인 폼은 밝은 배경용 텍스트 토큰을 사용한다', () => {
  const css = readFileSync(adminLoginStylePath, 'utf8')
  const loginFormRule = css.match(/\.loginForm\s*\{(?<declarations>[^}]*)\}/)

  assert.ok(loginFormRule?.groups?.declarations)
  assert.match(
    loginFormRule.groups.declarations,
    /--text-primary:\s*var\(--text-onlight-main\);/,
  )
})
