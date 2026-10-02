import type { ReactElement } from "react";
import { Installation } from "../components/Installation";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { VillageAtlas } from "../components/VillageAtlas";
import { TimberHero } from "../components/TimberHero";
import * as site from "../components/site.css";
import * as styles from "./home-page.css";

/** The selected identity introduces the mod and its regional village catalogs. */
export function HomePage(): ReactElement {
  return (
    <div className={styles.page}>
      <SiteHeader />
      <main id="main">
        <TimberHero />
        <section className={styles.life} id="life" aria-labelledby="life-title">
          <div className={styles.lifeInner}>
            <div>
              <p className={site.label}>Village life</p>
              <h2 className={site.sectionHeading} id="life-title">
                Villagers do the building.
              </h2>
            </div>
            <div className={styles.lifeRows}>
              <div className={styles.lifeRow}>
                <h3>Gather and build</h3>
                <p>Cut timber, mine stone, and put up homes and workshops.</p>
              </div>
              <div className={styles.lifeRow}>
                <h3>Jobs and families</h3>
                <p>Villagers take on jobs, marry, and raise children.</p>
              </div>
              <div className={styles.lifeRow}>
                <h3>Make plans</h3>
                <p>The village’s AI chooses what to build and who does the work.</p>
              </div>
            </div>
          </div>
        </section>
        <VillageAtlas />
        <Installation />
      </main>
      <SiteFooter />
    </div>
  );
}
