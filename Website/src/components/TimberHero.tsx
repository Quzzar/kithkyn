import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import { MINECRAFT_SCENES, type MinecraftScene } from "../data/imagery";
import type { TimberIdentity } from "../data/timber-identities";
import * as hero from "./village-hero.css";
import * as styles from "./timber-hero.css";

/** The artwork leads each identity, while credited Minecraft scenery supplies the setting. */
export function TimberHero({ identity }: { readonly identity: TimberIdentity }): ReactElement {
  const scene: MinecraftScene = MINECRAFT_SCENES.village;
  return (
    <section className={styles.layout[identity.layout]} aria-labelledby="hero-title">
      <div className={styles.copy[identity.layout]}>
        <img
          className={styles.wordmark}
          src={identity.wordmark.source}
          width={identity.wordmark.width}
          height={identity.wordmark.height}
          alt="Kithkyn"
          fetchPriority="high"
        />
        <h1 id="hero-title">Autonomous Villages for Minecraft</h1>
        <p className={hero.lead}>Villagers gather resources, build homes, and start families.</p>
        <div className={styles.actions[identity.layout]}>
          <a className={hero.primary} href="#villages">
            Explore the villages <ArrowUpRight aria-hidden="true" />
          </a>
          <a className={hero.secondary} href="#get-started">
            Installation guide
          </a>
        </div>
        <p className={hero.platform}>Minecraft Java 1.21.1 · NeoForge</p>
      </div>
      <figure className={styles.picture[identity.layout]}>
        <img
          src={scene.source}
          alt={scene.alt}
          width={scene.width}
          height={scene.height}
          fetchPriority="high"
        />
        <figcaption>
          <a href={scene.creditUrl}>Minecraft village · Placeholder</a>
        </figcaption>
      </figure>
    </section>
  );
}
