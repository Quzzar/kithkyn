import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import type { Identity } from "../data/identities";
import * as styles from "./identity-hero.css";

/** Four compositions reveal how an identity behaves beyond an isolated logo sample. */
export function IdentityHero({ identity }: { readonly identity: Identity }): ReactElement {
  return (
    <section
      className={`${styles.hero} ${styles.layout[identity.id]}`}
      aria-labelledby="hero-title"
    >
      <div className={`${styles.copy} ${styles.copyLayout[identity.id]}`}>
        <div>
          <p className={styles.label}>Autonomous villagers for Minecraft</p>
          <h1 id="hero-title">
            {identity.headline[0]}
            <br />
            {identity.headline[1]}
          </h1>
        </div>
        <div className={styles.actionArea}>
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
      <figure className={`${styles.figure} ${styles.figureLayout[identity.id]}`}>
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
