import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import * as styles from "./village-hero.css";

/** The village scene and concise copy introduce the mod's autonomous neighbors. */
export function VillageHero(): ReactElement {
  return (
    <section className={styles.hero} aria-labelledby="hero-title">
      <div className={styles.copy}>
        <div>
          <p className={styles.label}>Autonomous villagers for Minecraft</p>
          <h1 id="hero-title">
            A world with
            <br />
            neighbors.
          </h1>
        </div>
        <div>
          <p className={styles.lead}>Villagers who build, belong, and think for themselves.</p>
          <div className={styles.actions}>
            <a className={styles.primary} href="#villages">
              Explore the villages <ArrowUpRight aria-hidden="true" />
            </a>
            <a className={styles.secondary} href="#get-started">
              Installation guide
            </a>
          </div>
          <p className={styles.platform}>Minecraft Java 1.21.1 · NeoForge</p>
        </div>
      </div>
      <figure className={styles.figure}>
        <img
          src="/art/village-site.webp"
          alt="Kithkyn village buildings beside a mangrove coast in Minecraft"
          width={1708}
          height={960}
          fetchPriority="high"
        />
        <figcaption>In-game village capture · site review</figcaption>
      </figure>
    </section>
  );
}
