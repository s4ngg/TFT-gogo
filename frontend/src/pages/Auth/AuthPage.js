const socialProviders = [
  { label: 'Google', mark: 'G' },
  { label: 'Kakao', mark: 'K' },
  { label: 'Naver', mark: 'N' },
]

function authMode() {
  return new URLSearchParams(window.location.search).get('mode') === 'signup' ? 'signup' : 'login'
}

function inputBox(icon, input) {
  return `
    <div class="inputBox">
      <span aria-hidden="true">${icon}</span>
      ${input}
    </div>
  `
}

function renderSignupFields() {
  return `
    <label>
      <span>비밀번호 확인</span>
      ${inputBox('🔒', '<input type="password" placeholder="비밀번호 재입력" />')}
    </label>
    <label>
      <span>소환사명#태그</span>
      ${inputBox('👤', '<input placeholder="정동글#KR1" />')}
    </label>
  `
}

function renderAuthShell(mode) {
  const isSignup = mode === 'signup'

  return `
    <main class="authShell">
      <section class="formPanel">
        <a class="brand" href="../Dashboard/index.html">
          <span aria-hidden="true"></span>
          <strong>TFTgogo</strong>
        </a>

        <div class="authTabs">
          <a class="${!isSignup ? 'activeTab' : ''}" href="./index.html">로그인</a>
          <a class="${isSignup ? 'activeTab' : ''}" href="./index.html?mode=signup">회원가입</a>
        </div>

        <div class="formHeading">
          <h2>${isSignup ? '회원가입' : '로그인'}</h2>
        </div>

        <div class="socialLogin" aria-label="소셜 로그인">
          ${socialProviders.map((provider) => `
            <button type="button" class="socialButton" aria-label="${provider.label} 로그인">
              <span>${provider.mark}</span>
              ${provider.label}
            </button>
          `).join('')}
        </div>

        <div class="divider"><span>또는</span></div>

        <form class="authForm" id="authForm">
          <label>
            <span>이메일</span>
            ${inputBox('✉', '<input type="email" placeholder="tftgogo@example.com" />')}
          </label>

          <label>
            <span>비밀번호</span>
            ${inputBox('🔒', '<input type="password" placeholder="비밀번호 입력" />')}
          </label>

          ${isSignup ? renderSignupFields() : ''}

          <button type="submit" class="submitButton">
            ${isSignup ? '회원가입' : '로그인'}
            <span aria-hidden="true">→</span>
          </button>
        </form>

        <p class="switchText">
          ${isSignup ? '이미 계정이 있으신가요?' : '아직 계정이 없으신가요?'}
          <a href="${isSignup ? './index.html' : './index.html?mode=signup'}">${isSignup ? '로그인' : '회원가입'}</a>
        </p>
      </section>
    </main>
  `
}

function bindAuthEvents() {
  document.getElementById('authForm')?.addEventListener('submit', (event) => {
    event.preventDefault()
  })
}

export function renderAuth() {
  document.getElementById('app').innerHTML = renderAuthShell(authMode())
  bindAuthEvents()
}
