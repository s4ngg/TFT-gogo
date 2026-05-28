import { useState, useEffect } from "react";
import layoutStyles from "../../components/Layout/layout.module.css";
import adminStyles from "./admin.module.css";

/* ── 타입 ── */
type NavPage = "main" | "patch" | "community" | "stats";
type ReportTab = "recruit" | "chat";

interface PatchNote {
  version: string;
  title: string;
  date: string;
  author: string;
  views: string;
  status: "published" | "draft";
}

interface ReportUser {
  initial: string;
  username: string;
  reason: string;
  count: number;
  time: string;
}

interface RegionStat {
  name: string;
  percent: number;
  color: string;
  value: string;
}

/* ── 더미 데이터 ── */
const PATCH_NOTES: PatchNote[] = [
  {
    version: "17.3",
    title: "17.3 패치 노트 — 밸런스 조정 및 신규 증강 추가",
    date: "2025.05.20",
    author: "admin",
    views: "4,821",
    status: "published",
  },
  {
    version: "17.2",
    title: "17.2 패치 노트 — 챔피언 상향 및 아이템 수정",
    date: "2025.05.06",
    author: "admin",
    views: "6,103",
    status: "published",
  },
  {
    version: "17.4",
    title: "17.4 패치 노트 (작성 중) — 예정 업데이트 내용 정리",
    date: "2025.05.22",
    author: "admin",
    views: "비공개",
    status: "draft",
  },
];

const RECRUIT_REPORTS: ReportUser[] = [
  { initial: "김", username: "김철수#KR1", reason: "허위 티어 기재 — 다이아 사칭", count: 5, time: "3분 전" },
  { initial: "이", username: "이영희#KR2", reason: "욕설 및 비방 포함 모집글", count: 3, time: "11분 전" },
  { initial: "박", username: "박민준#KR3", reason: "홍보성 외부 링크 삽입", count: 2, time: "28분 전" },
  { initial: "최", username: "최지원#KR1", reason: "중복 모집글 반복 게시", count: 4, time: "45분 전" },
  { initial: "정", username: "정하늘#KR5", reason: "불쾌한 닉네임 사용 의심", count: 1, time: "1시간 전" },
];

const CHAT_REPORTS: ReportUser[] = [
  { initial: "강", username: "강동원#KR2", reason: "#일반 채널 — 반복 욕설 및 혐오 발언", count: 7, time: "2분 전" },
  { initial: "한", username: "한지민#KR4", reason: "#덱 공략 채널 — 스팸 메시지 도배", count: 4, time: "19분 전" },
  { initial: "오", username: "오세훈#KR1", reason: "#파티 모집 — 개인정보 수집 시도 의심", count: 2, time: "37분 전" },
];

const REGIONS: RegionStat[] = [
  { name: "KR", percent: 88, color: "#4ade80", value: "142,105명" },
  { name: "NA", percent: 25, color: "#60a5fa", value: "28,410명" },
  { name: "EUW", percent: 16, color: "#a78bfa", value: "13,721명" },
  { name: "JP", percent: 8, color: "#fb923c", value: "6,840명" },
];

const SUMMARY_ROWS = [
  { key: "신규 가입", value: "+1,284명" },
  { key: "처리된 신고", value: "23건" },
  { key: "채팅 메시지", value: "48,903건" },
  { key: "모집글 생성", value: "2,107건" },
  { key: "패치노트 조회", value: "10,924건" },
];

const NAV_ITEMS: { id: NavPage; label: string; icon: string }[] = [
  { id: "main", label: "메인", icon: "ti-layout-dashboard" },
  { id: "patch", label: "패치노트", icon: "ti-file-description" },
  { id: "community", label: "커뮤니티", icon: "ti-messages" },
  { id: "stats", label: "통계 대시보드", icon: "ti-chart-bar" },
];

/* ── 서브 컴포넌트 ── */
function ReportRow({ user }: { user: ReportUser }) {
  return (
    <div className={adminStyles.reportItem}>
      <div className={adminStyles.reportAvatar}>{user.initial}</div>
      <div className={adminStyles.reportUser}>
        <div className={adminStyles.reportUsername}>{user.username}</div>
        <div className={adminStyles.reportReason}>{user.reason}</div>
      </div>
      <div className={adminStyles.reportCountBox}>
        <div className={adminStyles.reportCountNum}>{user.count}</div>
        <div className={adminStyles.reportCountLabel}>신고수</div>
      </div>
      <div className={adminStyles.reportTime}>{user.time}</div>
      <div className={adminStyles.reportActions}>
        <button className={adminStyles.btnWarn}>경고</button>
        <button className={adminStyles.btnBan}>정지</button>
      </div>
    </div>
  );
}

/* ── 메인 컴포넌트 ── */
export default function AdminPage() {
  const [activePage, setActivePage] = useState<NavPage>("main");
  const [activeTab, setActiveTab] = useState<ReportTab>("recruit");
  const [clock, setClock] = useState("");
  const [onlineCount, setOnlineCount] = useState(184236);

  /* 시계 */
  useEffect(() => {
    const tick = () => setClock(new Date().toLocaleTimeString("ko-KR"));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  /* 접속자 수 실시간 변동 */
  useEffect(() => {
    const id = setInterval(() => {
      setOnlineCount(184236 + Math.round((Math.random() - 0.5) * 400));
    }, 3000);
    return () => clearInterval(id);
  }, []);

  return (
    /* Layout.module.css — appShell */
    <div className={layoutStyles.appShell}>

      {/* ── 사이드바 (Layout.module.css) ── */}
      <aside className={layoutStyles.sidebar}>

        {/* 브랜드 */}
        <div className={layoutStyles.brand}>
          <div className={layoutStyles.brandLogo} />
          <strong>TFTgogo</strong>
        </div>

        {/* 네비게이션 */}
        <nav className={layoutStyles.navList}>
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              className={`${layoutStyles.navItem} ${
                activePage === item.id ? layoutStyles.activeNav : ""
              }`}
              onClick={() => setActivePage(item.id)}
            >
              <i className={`ti ${item.icon}`} aria-hidden="true" />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className={adminStyles.sidebarFill} />

        {/* 관리자 정보*/}
        <div className={adminStyles.adminCard}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 14 }}>
            <div className={adminStyles.adminAvatar}>
              관
            </div>
            <div>
              <div style={{ fontSize: 14, color: "#f1f5f9", fontWeight: 600 }}>관리자</div>
              <div style={{ fontSize: 11, color: "#5a6a7a" }}>Super Admin</div>
            </div>
          </div>
          <div className={layoutStyles.rankRecord}>
            <span style={{ fontSize: 12, color: "#8899aa" }}>전체 권한 보유</span>
          </div>
        </div>

        <button className={layoutStyles.feedbackButton}>
          <i className="ti ti-logout" aria-hidden="true" />
          로그아웃
        </button>
      </aside>

      {/* ── 메인 영역 ── */}
      <div className={layoutStyles.main} style={{ display: "flex", flexDirection: "column", padding: 0 }}>

        {/* Topbar (admin.module.css) */}
        <div className={adminStyles.topbar}>
          <div className={adminStyles.topbarTitle}></div>
          <div className={adminStyles.topbarRight}>
            <div className={adminStyles.statusDot} />
            <span className={adminStyles.statusText}>서버 정상</span>
            <span className={adminStyles.clock}>{clock}</span>
          </div>
        </div>

        {/* ── 메인 대시보드 ── */}
        {activePage === "main" && (
          <div className={adminStyles.dashboard}>

            {/* 1. 최근 패치노트 */}
            <div className={`${adminStyles.card} ${adminStyles.cardLeftTop}`}>
              <div className={adminStyles.cardHeader}>
                <div className={adminStyles.cardHeaderLeft}>
                  <i className="ti ti-file-description" aria-hidden="true" />
                  최근 패치노트
                </div>
                <button className={adminStyles.cardHeaderRight}>
                  <i className="ti ti-plus" aria-hidden="true" />
                  새 글 작성
                </button>
              </div>

              {PATCH_NOTES.map((note) => (
                <div key={note.version} className={adminStyles.patchItem}>
                  <span
                    className={`${adminStyles.patchVersion} ${
                      note.status === "published"
                        ? adminStyles.patchVersionGreen
                        : adminStyles.patchVersionYellow
                    }`}
                  >
                    {note.version}
                  </span>
                  <div className={adminStyles.patchInfo}>
                    <div className={adminStyles.patchTitle}>{note.title}</div>
                    <div className={adminStyles.patchMeta}>
                      <span>{note.date}</span>
                      <span>작성자: {note.author}</span>
                      <span>조회 {note.views}</span>
                    </div>
                  </div>
                  <span
                    className={
                      note.status === "published"
                        ? adminStyles.patchStatusGreen
                        : adminStyles.patchStatusYellow
                    }
                  >
                    {note.status === "published" ? "게시됨" : "초안"}
                  </span>
                  <i className={`ti ti-chevron-right ${adminStyles.patchArrow}`} aria-hidden="true" />
                </div>
              ))}
            </div>

            {/* 3. 서버 현황 (우측 전체) */}
            <div className={`${adminStyles.card} ${adminStyles.cardRight}`}>
              <div className={adminStyles.cardHeader}>
                <div className={adminStyles.cardHeaderLeft}>
                  <i className="ti ti-server" aria-hidden="true" />
                  서버 현황
                </div>
                <button className={adminStyles.cardHeaderRight}>
                  <i className="ti ti-refresh" aria-hidden="true" />
                  갱신
                </button>
              </div>

              <div className={adminStyles.statGrid}>
                <div className={`${adminStyles.statBox} ${adminStyles.statGridFull}`}>
                  <div className={adminStyles.statBoxLabel}>
                    <i className="ti ti-users" aria-hidden="true" />
                    현재 접속자
                  </div>
                  <div className={`${adminStyles.statBoxValue} ${adminStyles.valueGreen}`}>
                    {onlineCount.toLocaleString("ko-KR")}
                  </div>
                  <div className={adminStyles.statBoxSub}>▲ 전일 대비 +2.1%</div>
                </div>

                <div className={adminStyles.statBox}>
                  <div className={adminStyles.statBoxLabel}>
                    <i className="ti ti-clock-hour-4" aria-hidden="true" />
                    대기열
                  </div>
                  <div className={`${adminStyles.statBoxValue} ${adminStyles.valueYellow}`}>없음</div>
                  <div className={adminStyles.statBoxSub}>평균 대기 0초</div>
                </div>

                <div className={adminStyles.statBox}>
                  <div className={adminStyles.statBoxLabel}>
                    <i className="ti ti-activity" aria-hidden="true" />
                    서버 상태
                  </div>
                  <div className={`${adminStyles.statBoxValue} ${adminStyles.valueBlue}`}>정상</div>
                  <div className={adminStyles.statBoxSub}>응답속도 12ms</div>
                </div>
              </div>

              <div className={adminStyles.regionSection}>
                <div className={adminStyles.regionHeader}>
                  <span>지역별 접속 현황</span>
                  <span>갱신: 1분 전</span>
                </div>
                {REGIONS.map((r) => (
                  <div key={r.name} className={adminStyles.regionRow}>
                    <span className={adminStyles.regionName}>{r.name}</span>
                    <div className={adminStyles.regionBarBg}>
                      <div
                        className={adminStyles.regionBar}
                        style={{ width: `${r.percent}%`, background: r.color }}
                      />
                    </div>
                    <span className={adminStyles.regionValue}>{r.value}</span>
                  </div>
                ))}
              </div>

              <div className={adminStyles.summarySection}>
                <div className={adminStyles.summaryTitle}>오늘의 주요 현황</div>
                {SUMMARY_ROWS.map((row) => (
                  <div key={row.key} className={adminStyles.summaryRow}>
                    <span className={adminStyles.summaryKey}>{row.key}</span>
                    <span className={adminStyles.summaryValue}>{row.value}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* 2. 신고 유저 (좌측 하단) */}
            <div className={`${adminStyles.card} ${adminStyles.cardLeftBot}`}>
              <div className={adminStyles.cardHeader}>
                <div className={adminStyles.cardHeaderLeft}>
                  <i className="ti ti-shield-exclamation" aria-hidden="true" />
                  신규 신고 유저
                  <span className={adminStyles.badgeNew}>NEW 8</span>
                </div>
                <button className={adminStyles.cardHeaderRight}>
                  전체 보기
                  <i className="ti ti-arrow-right" aria-hidden="true" />
                </button>
              </div>

              <div className={adminStyles.reportTabs}>
                <button
                  className={`${adminStyles.reportTab} ${activeTab === "recruit" ? adminStyles.reportTabActive : ""}`}
                  onClick={() => setActiveTab("recruit")}
                >
                  <i className="ti ti-users" aria-hidden="true" />
                  모집글 신고
                  <span className={adminStyles.tabCount}>5</span>
                </button>
                <button
                  className={`${adminStyles.reportTab} ${activeTab === "chat" ? adminStyles.reportTabActive : ""}`}
                  onClick={() => setActiveTab("chat")}
                >
                  <i className="ti ti-message-report" aria-hidden="true" />
                  실시간 채팅 신고
                  <span className={adminStyles.tabCount}>3</span>
                </button>
              </div>

              {(activeTab === "recruit" ? RECRUIT_REPORTS : CHAT_REPORTS).map((u) => (
                <ReportRow key={u.username} user={u} />
              ))}
            </div>
          </div>
        )}

        {/* ── 서브 페이지 빈 상태 ── */}
        {activePage !== "main" && (
          <div className={adminStyles.emptyPage}>
            <i className={`ti ti-tools ${adminStyles.emptyPageIcon}`} aria-hidden="true" />
            <div className={adminStyles.emptyPageText}>
              — 준비 중입니다.
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
