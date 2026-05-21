import { ClipboardList, Search } from 'lucide-react'
import styles from '../Dashboard.module.css'

function PatchMetaCard() {
  return (
    <section className={`${styles.panel} ${styles.patchCard}`}>
      <div className={styles.patchEmblemArt} aria-hidden="true" />
      <div className={styles.patchCopy}>
        <h2>17.3 추천 메타</h2>
        <p>5월 20일 업데이트</p>
      </div>
      <button type="button">
        <ClipboardList size={19} />
        패치 노트 보기
      </button>
    </section>
  )
}

function SummonerSearchCard() {
  return (
    <section className={`${styles.panel} ${styles.searchPanel}`}>
      <h1>소환사 전적 검색</h1>
      <p>소환사명, 태그#KR 등을 입력하세요</p>
      <form className={styles.searchBox}>
        <input aria-label="소환사명 검색" placeholder="소환사명#태그 입력" />
        <button type="submit" aria-label="검색">
          <Search size={28} />
        </button>
      </form>
      <div className={styles.searchTags}>
        <span>인기 검색</span>
        <button type="button">정동글#KR1</button>
        <button type="button">새벽의달#KR</button>
        <button type="button">응의자#KR1</button>
        <button type="button">TFT잘하고싶다#1234</button>
      </div>
    </section>
  )
}

function Mascot() {
  return <div className={styles.apiMascotArt} aria-hidden="true" />
}

function RiotApiStatusCard() {
  return (
    <section className={`${styles.panel} ${styles.apiPanel}`}>
      <div className={styles.apiText}>
        <h2>
          <span />
          Riot API 연동
        </h2>
        <p>마지막 갱신: 1분 전</p>
        <div className={styles.apiStats}>
          <div>
            <small>현재 접속자</small>
            <strong>184,236</strong>
          </div>
          <div>
            <small>대기열</small>
            <strong>없음</strong>
          </div>
        </div>
      </div>
      <Mascot />
    </section>
  )
}

function TopSummaryCards() {
  return (
    <>
      <PatchMetaCard />
      <SummonerSearchCard />
      <RiotApiStatusCard />
    </>
  )
}

export default TopSummaryCards
