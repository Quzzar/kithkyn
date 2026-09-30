import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { DirectionBar } from "../components/DirectionBar";
import * as styles from "./concepts.css";

/** Direction C: a comic-strip introduction leads with villagers and shared life. */
export function StoriesPage(): ReactElement {
  return (
    <>
      <DirectionBar />
      <main id="main" className={styles.page}>
        <header className={styles.masthead}>
          <span className={styles.brand}>KithKyn</span>
          <nav className={styles.links} aria-label="Story navigation">
            <a href="#village-life">Village life</a>
            <Link to="/atlas">Places</Link>
            <Link to="/setup">Installation guide</Link>
          </nav>
        </header>
        <div className={styles.storyIntro}>
          <h1 className={styles.storyTitle}>Meet your new neighbors.</h1>
          <p className={styles.storyLead}>Little lives. A world of possibilities.</p>
        </div>
        <section id="village-life" className={styles.comic} aria-label="Village life storyboard">
          <figure className={styles.panel}>
            <h2 className={styles.panelTitle}>Meet a neighbor.</h2>
            <div className={styles.comicPlaceholder}>
              <span className={styles.speech}>“What should we build?”</span>
              <span>[ Villager close-up ]</span>
            </div>
            <figcaption className={styles.caption}>Individual traits. Shared goals.</figcaption>
          </figure>
          <figure className={styles.panel}>
            <h2 className={styles.panelTitle}>Make a home.</h2>
            <div className={styles.comicPlaceholder}>
              <span>[ Villagers building together ]</span>
              <span className={styles.speech}>“I’ll take the roof.”</span>
            </div>
            <figcaption className={styles.caption}>They gather, work, and build.</figcaption>
          </figure>
          <figure className={styles.widePanel}>
            <h2 className={styles.panelTitle}>Watch a community grow.</h2>
            <div className={styles.widePlaceholder}>[ Wide village-life scene ]</div>
            <figcaption className={styles.caption}>
              Families, homes, and lives that unfold together.
            </figcaption>
          </figure>
        </section>
        <div className={styles.storyEnd}>
          <Link className={styles.button} to="/atlas">
            Explore all 17 village styles
          </Link>
          <Link className={styles.button} to="/setup">
            Bring them to your world
          </Link>
        </div>
        <footer className={styles.footer}>
          <p>Minecraft Java 1.21.1 · NeoForge</p>
          <span className={styles.annotation}>Storyboard wireframe · dialogue is sample copy</span>
        </footer>
      </main>
    </>
  );
}
