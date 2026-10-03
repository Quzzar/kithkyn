import type { ReactElement } from "react";
import { Installation } from "../components/Installation";
import { SiteFooter } from "../components/SiteFooter";
import { VillageAtlas } from "../components/VillageAtlas";
import { TimberHero } from "../components/TimberHero";
import * as site from "../components/site.css";
import * as styles from "./home-page.css";

/** The selected identity introduces the mod and its regional village catalogs. */
export function HomePage(): ReactElement {
  return (
    <div className={styles.page}>
      <a className={site.skipLink} href="#main">
        Skip to content
      </a>
      <main id="main">
        <TimberHero />
        <VillageAtlas />
        <Installation />
      </main>
      <SiteFooter />
    </div>
  );
}
