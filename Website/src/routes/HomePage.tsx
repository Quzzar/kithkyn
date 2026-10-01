import { ArrowDown, Compass, Download, Users } from "lucide-react";
import type { ReactElement } from "react";
import { Installation } from "../components/Installation";
import { LogoMark } from "../components/LogoMark";
import { SiteFooter } from "../components/SiteFooter";
import { SiteHeader } from "../components/SiteHeader";
import { VillageAtlas } from "../components/VillageAtlas";
import * as site from "../components/site.css";
import * as styles from "./home-page.css";

type VillageLife = { readonly title: string; readonly detail: string };
const VILLAGE_LIFE: readonly VillageLife[] = [
  {
    title: "Work together",
    detail: "Gather resources, build homes, and keep the village running.",
  },
  { title: "Grow roots", detail: "Form families, take on jobs, and welcome new neighbors." },
  { title: "Think ahead", detail: "Make plans around what the community needs next." },
];

/** The selected title-screen direction becomes one complete, navigable public website. */
export function HomePage(): ReactElement {
  return (
    <>
      <div className={styles.entrance}>
        <img
          className={styles.backdrop}
          src="/art/village-site.webp"
          alt=""
          width={1708}
          height={960}
          fetchPriority="high"
        />
        <div className={styles.shade} aria-hidden="true" />
        <SiteHeader />
        <main id="main">
          <section className={styles.hero} aria-labelledby="game-title">
            <h1 id="game-title" className={styles.title}>
              <LogoMark />
            </h1>
            <p className={styles.tagline}>Bringing villages to life</p>
            <nav className={styles.menu} aria-label="Start exploring">
              <a className={styles.primaryPlank} href="#villages">
                <Compass aria-hidden="true" />
                Explore the villages
              </a>
              <a className={styles.middlePlank} href="#life">
                <Users aria-hidden="true" />
                Meet your neighbors
              </a>
              <a className={styles.lastPlank} href="#get-started">
                <Download aria-hidden="true" />
                Installation guide
              </a>
            </nav>
            <div className={styles.heroFoot}>
              <span>Minecraft Java 1.21.1 · NeoForge</span>
              <span>In-game village capture · site review</span>
            </div>
            <a className={styles.down} href="#life" aria-label="Discover village life">
              <ArrowDown aria-hidden="true" />
            </a>
          </section>
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
                {VILLAGE_LIFE.map((entry: VillageLife): ReactElement => (
                  <div className={styles.lifeRow} key={entry.title}>
                    <h3>{entry.title}</h3>
                    <p>{entry.detail}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>
          <VillageAtlas />
          <Installation />
        </main>
      </div>
      <SiteFooter />
    </>
  );
}
