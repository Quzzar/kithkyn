import { ArrowDown, Hammer, Heart, Sprout, Users } from "lucide-react";
import type { ReactElement } from "react";
import { Installation } from "../components/Installation";
import { SiteFooter } from "../components/SiteFooter";
import { SiteHeader } from "../components/SiteHeader";
import { VillageAtlas } from "../components/VillageAtlas";
import * as site from "../components/site.css";
import * as styles from "./home-page.css";

/** A new public introduction centered on the people who make a village. */
export function HomePage(): ReactElement {
  return (
    <>
      <SiteHeader />
      <main id="main">
        <section className={styles.hero} aria-labelledby="hero-title">
          <div className={styles.heroCopy}>
            <p className={site.label}>
              <Sprout aria-hidden="true" /> Meet your new neighbors
            </p>
            <h1 id="hero-title">
              Your world.
              <br />
              <span>Their story.</span>
            </h1>
            <p className={styles.lead}>Autonomous villagers who work, build, and grow together.</p>
            <div className={styles.heroActions}>
              <a className={site.primaryButton} href="#villages">
                Explore the villages <ArrowDown aria-hidden="true" />
              </a>
            </div>
            <p className={styles.compatibility}>
              Minecraft Java 1.21.1 <span>·</span> NeoForge
            </p>
          </div>
          <figure className={styles.heroArt}>
            <img
              src="/art/village-diorama.webp"
              alt="An illustrated miniature village with homes, gardens, a river, and neighbors around a campfire"
              width={1500}
              height={1000}
              fetchPriority="high"
            />
            <figcaption>Our little world, imagined. Original promotional illustration.</figcaption>
          </figure>
        </section>
        <div className={styles.promise}>
          <div className={styles.promiseInner}>
            <p>From the first campfire to a thriving town.</p>
            <span>Every village, a story.</span>
          </div>
        </div>
        <section className={styles.life} id="life" aria-labelledby="life-title">
          <div className={styles.lifeHeading}>
            <p className={site.label}>
              <Heart aria-hidden="true" /> More than a place on the map
            </p>
            <h2 className={site.sectionHeading} id="life-title">
              A village is
              <br />
              its people.
            </h2>
            <p>
              Give them a place to begin.
              <br />
              They’ll take it from there.
            </p>
          </div>
          <div className={styles.lifeDetails}>
            <article>
              <Hammer aria-hidden="true" />
              <div>
                <h3>Hands to build. Places to grow.</h3>
                <p>
                  Gather resources, tend farms, and turn a small settlement into a working
                  community.
                </p>
              </div>
            </article>
            <article>
              <Users aria-hidden="true" />
              <div>
                <h3>Neighbors with lives of their own.</h3>
                <p>
                  Personalities, friendships, partners, children, and the pets that follow them
                  home.
                </p>
              </div>
            </article>
            <article>
              <Sprout aria-hidden="true" />
              <div>
                <h3>A community that makes decisions.</h3>
                <p>A village brain weighs what its people need and helps choose what comes next.</p>
              </div>
            </article>
          </div>
        </section>
        <VillageAtlas />
        <Installation />
      </main>
      <SiteFooter />
    </>
  );
}
