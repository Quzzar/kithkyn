import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { DirectionBar } from "../components/DirectionBar";
import * as styles from "./concepts.css";

const SCENES: readonly string[] = ["The first fire", "The first home", "A living village"];

/** Direction A: enter the site through a game-like title menu. */
export function TitleScreenPage(): ReactElement {
  return (
    <>
      <DirectionBar />
      <main id="main" className={styles.game}>
        <section className={styles.gameScene} aria-labelledby="game-title">
          <div className={styles.sceneBorder} aria-hidden="true" />
          <span className={styles.sceneNote}>[ Real gameplay loop goes here ]</span>
          <h1 id="game-title" className={styles.gameTitle}>
            KITHKYN
          </h1>
          <p className={styles.gameLead}>A village with a life of its own.</p>
          <div className={styles.menu}>
            <Link className={styles.menuAction} to="/atlas">
              Explore the villages
            </Link>
            <Link className={styles.menuSecondary} to="/setup">
              Installation guide
            </Link>
          </div>
          <div className={styles.sceneBottom}>
            <span>Minecraft Java 1.21.1 · NeoForge</span>
            <span>17 village styles · Autonomous villagers</span>
          </div>
        </section>
        <div className={styles.sceneStrip}>
          {SCENES.map((scene: string): ReactElement => (
            <figure key={scene}>
              <div className={styles.sceneThumb}>[ Gameplay still ]</div>
              <figcaption>{scene}</figcaption>
            </figure>
          ))}
        </div>
        <footer className={styles.gameFoot}>
          <p>Villagers who work, build, and grow together.</p>
          <a href="https://github.com/Quzzar/kithkyn">Follow development ↗</a>
        </footer>
      </main>
    </>
  );
}
