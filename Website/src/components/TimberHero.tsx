import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import { MINECRAFT_SCENES, type MinecraftScene } from "../data/imagery";
import { BRAND_WORDMARK } from "../data/brand";
import * as hero from "./village-hero.css";
import * as styles from "./timber-hero.css";

/** The timber wordmark introduces the mod over credited Minecraft scenery. */
export function TimberHero(): ReactElement {
  const scene: MinecraftScene = MINECRAFT_SCENES.village;
  return (
    <section className={styles.layout} aria-labelledby="hero-title">
      <div className={styles.copy}>
        <img
          className={styles.wordmark}
          src={BRAND_WORDMARK.source}
          width={BRAND_WORDMARK.width}
          height={BRAND_WORDMARK.height}
          alt="Kithkyn"
          fetchPriority="high"
        />
        <h1 id="hero-title">Autonomous Villages</h1>
        <p className={hero.lead}>Villagers gather resources, build homes, and start families.</p>
        <div className={styles.actions}>
          <a className={hero.primary} href="#villages">
            Explore the villages <ArrowUpRight aria-hidden="true" />
          </a>
          <a className={hero.secondary} href="#get-started">
            Installation guide
          </a>
        </div>
        <p className={hero.platform}>Minecraft Java 1.21.1 · NeoForge</p>
      </div>
      <figure className={styles.picture}>
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
