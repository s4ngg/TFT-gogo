function nav(active) {
  const items = [
    ['dashboard', '홈', '../Dashboard/index.html'],
    ['decks', '덱 모음', '../Decks/index.html'],
    ['ai', 'AI 덱 추천', '../AiRecommend/index.html'],
    ['guide', '게임 가이드', '../Guide/index.html'],
    ['meta', '메타 통계', '../MetaStats/index.html'],
    ['party', '커뮤니티', '../Party/index.html'],
    ['patch', '패치노트', '../PatchNotes/index.html'],
    ['summoner', '소환사 상세', '../SummonerDetail/index.html'],
  ]

  return items.map(([key, label, href]) => `<a class="nav-item ${active === key ? 'active' : ''}" href="${href}"><span>${label}</span></a>`).join('')
}

export function shell(active, content) {
  document.getElementById('app').innerHTML = `
    <div class="shell">
      <aside class="sidebar">
        <a class="brand" href="../Dashboard/index.html"><span class="brand-mark"></span><span>TFTgogo</span></a>
        <nav class="nav-list">${nav(active)}</nav>
        <section class="rank-card">
          <div class="rank-emblem"></div>
          <strong>SanChess</strong>
          <span>Diamond IV · 45 LP</span>
          <span>256승 137패 · 승률 65%</span>
        </section>
      </aside>
      <main class="main">
        <header class="topbar">
          <div class="patch-brief"><span>17.3 패치 요약</span><strong>선봉대 벡스와 6 다크스타 진 중심으로 메타 형성 중</strong></div>
          <div class="top-actions">
            <a class="ghost-button" href="../Auth/index.html">로그인</a>
            <a class="ghost-button" href="../SummonerDetail/index.html?name=SanChess&tag=KR1">전적 보기</a>
            <a class="primary-button" href="../Decks/index.html">덱 확인</a>
          </div>
        </header>
        ${content}
      </main>
    </div>
  `
}
