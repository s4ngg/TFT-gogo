import styles from '../SummonerDetail.module.css'

export default function EmptyState({ name, tag }: { name: string; tag: string }) {
  return (
    <div className={styles.emptyState}>
      <p className={styles.emptyTitle}>소환사를 찾을 수 없습니다</p>
      <p className={styles.emptyDesc}>
        <strong>{name}#{tag}</strong>에 해당하는 소환사가 존재하지 않거나 한국 서버에 등록되지 않았습니다.
        소환사명과 태그를 다시 확인해 주세요.
      </p>
    </div>
  )
}
