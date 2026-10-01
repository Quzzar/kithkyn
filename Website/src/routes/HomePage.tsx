import type { ReactElement } from "react";
import { Navigate, useParams } from "react-router-dom";
import { IdentityHero } from "../components/IdentityHero";
import { Installation } from "../components/Installation";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { VillageAtlas } from "../components/VillageAtlas";
import { IDENTITIES, type Identity } from "../data/identities";
import { identityTheme } from "../styles/theme.css";
import * as site from "../components/site.css";
import * as styles from "./home-page.css";

/** Shared product content puts each logo study in the same usable website context. */
export function HomePage(): ReactElement {
  const { identityId } = useParams();
  const identity: Identity | undefined = IDENTITIES.find(
    (entry: Identity): boolean => entry.id === identityId,
  );
  if (!identity) return <Navigate to="/" replace />;
  return (
    <div className={`${styles.page} ${identityTheme[identity.presentation]}`}>
      <SiteHeader identity={identity} />
      <main id="main">
        <IdentityHero identity={identity} />
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
      <SiteFooter identity={identity} />
    </div>
  );
}
