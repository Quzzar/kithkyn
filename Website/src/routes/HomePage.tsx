import type { ReactElement } from "react";
import { Installation } from "../components/Installation";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { VillageAtlas } from "../components/VillageAtlas";
import { TimberHero } from "../components/TimberHero";
import * as styles from "./home-page.css";

/** The selected identity introduces the mod and its regional village catalogs. */
export function HomePage(): ReactElement {
  return (
    <div className={styles.page}>
      <SiteHeader />
      <main id="main">
        <TimberHero />
        <VillageAtlas />
        <Installation />
      </main>
      <SiteFooter />
    </div>
  );
}
