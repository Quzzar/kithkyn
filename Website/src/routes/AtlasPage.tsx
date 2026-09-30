import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { DirectionBar } from "../components/DirectionBar";
import { VillageAtlas } from "../components/VillageAtlas";
import * as styles from "./concepts.css";

/** Direction B: regional architecture is the entry point, with a persistent index. */
export function AtlasPage(): ReactElement {
  return (
    <>
      <DirectionBar />
      <main id="main" className={styles.page}>
        <header className={styles.masthead}>
          <span className={styles.brand}>
            KithKyn <span className={styles.annotation}>/ Village atlas</span>
          </span>
          <nav className={styles.links} aria-label="Atlas navigation">
            <Link to="/stories">Village life</Link>
            <Link to="/setup">Installation guide</Link>
          </nav>
        </header>
        <div className={styles.atlasIntro}>
          <h1 className={styles.atlasTitle}>
            Where will they
            <br />
            make a home?
          </h1>
          <p className={styles.atlasSubtitle}>
            Autonomous communities.
            <br />
            17 regional building styles.
          </p>
        </div>
        <VillageAtlas />
        <footer className={styles.footer}>
          <p>Minecraft Java 1.21.1 · NeoForge</p>
          <a href="https://github.com/Quzzar/kithkyn">Source & development ↗</a>
        </footer>
      </main>
    </>
  );
}
