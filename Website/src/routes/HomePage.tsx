import type { ReactElement } from "react";
import { VillageHero } from "../components/VillageHero";
import { Installation } from "../components/Installation";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { VillageAtlas } from "../components/VillageAtlas";
import { TimberHero } from "../components/TimberHero";
import type { SiteBranding } from "../data/brand";
import type { TimberIdentity } from "../data/timber-identities";
import * as site from "../components/site.css";
import * as styles from "./home-page.css";

/** The selected identity introduces the mod and its regional village catalogs. */
export function HomePage({ identity }: { readonly identity?: TimberIdentity } = {}): ReactElement {
  const branding: SiteBranding = identity
    ? {
        wordmark: identity.wordmark,
        homePath: `/brand/directions/${identity.id}`,
        brandPath: "/brand/directions",
        isStudy: true,
      }
    : {};
  return (
    <div className={styles.page}>
      <SiteHeader {...branding} />
      <main id="main">
        {identity ? <TimberHero identity={identity} /> : <VillageHero />}
        <section className={styles.life} id="life" aria-labelledby="life-title">
          <div className={styles.lifeInner}>
            <div>
              <p className={site.label}>A village with a life of its own</p>
              <h2 className={site.sectionHeading} id="life-title">
                They’ve got
                <br />
                things to do.
              </h2>
              <p className={styles.lifeLead}>Neighbors who make plans of their own.</p>
            </div>
            <div className={styles.lifeRows}>
              <div className={styles.lifeRow}>
                <h3>Work together</h3>
                <p>Gather resources, build homes, and keep the village running.</p>
              </div>
              <div className={styles.lifeRow}>
                <h3>Grow roots</h3>
                <p>Form families, take on jobs, and welcome new neighbors.</p>
              </div>
              <div className={styles.lifeRow}>
                <h3>Think ahead</h3>
                <p>Make plans around what the community needs next.</p>
              </div>
            </div>
          </div>
        </section>
        <VillageAtlas />
        <Installation />
      </main>
      <SiteFooter {...branding} />
    </div>
  );
}
