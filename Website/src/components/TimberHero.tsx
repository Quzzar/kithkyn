import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import type { TimberIdentity } from "../data/timber-identities";
import * as hero from "./village-hero.css";
import * as styles from "./timber-hero.css";

/** The artwork leads each identity, while real gameplay supplies the setting. */
export function TimberHero({ identity }: { readonly identity: TimberIdentity }): ReactElement {
  return (
    <section className={styles.layout[identity.id]} aria-labelledby="hero-title">
      <div className={styles.copy[identity.id]}>
        <p className={hero.label}>Autonomous villagers for Minecraft</p>
        <img
          className={styles.wordmark}
          src={identity.wordmark.source}
          width={identity.wordmark.width}
          height={identity.wordmark.height}
          alt="Kithkyn"
          fetchPriority="high"
        />
        <h1 id="hero-title">A world with neighbors.</h1>
        <p className={hero.lead}>Villagers who build, belong, and think for themselves.</p>
        <div className={styles.actions[identity.id]}>
          <a className={hero.primary} href="#villages">
            Explore the villages <ArrowUpRight aria-hidden="true" />
          </a>
          <a className={hero.secondary} href="#get-started">
            Installation guide
          </a>
        </div>
        <p className={hero.platform}>Minecraft Java 1.21.1 · NeoForge</p>
      </div>
      <figure className={styles.picture[identity.id]}>
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
