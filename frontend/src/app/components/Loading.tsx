import styles from "@/../public/CssModules/Loading.module.css"

export default function Loading() {
    return (
        <div className={styles.spinnerOverlay}>
          <div className={styles.spinner} />
        </div>
      )
}