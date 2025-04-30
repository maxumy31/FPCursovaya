

import styles from "./../../public/CssModules/AuthPage.module.css"

export default function AuthPage() {
    const id : number = 10
    return (<div className={styles.wrap}>
        <div></div>
        <div className={styles.center_block}>
            <div>Ваш ID = {id}</div>
            <div>Создать новую игру или присоединиться к уже созданной?</div>
            <div><input/></div>
            <div><button>Зайти</button></div>
            <div><button>Создать новую</button></div>
            </div>
        <div></div>
    </div>)
}