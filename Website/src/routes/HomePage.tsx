import {
  ArrowDown,
  ChevronLeft,
  ChevronRight,
  Cloud,
  ExternalLink,
  HardDrive,
  ShieldCheck,
} from "lucide-react";
import { useRef, type ReactElement } from "react";

import { HeroBackground } from "../components/HeroBackground";
import { LogoMark } from "../components/LogoMark";
import * as styles from "./home-page.css";

const GITHUB_URL: string = "https://github.com/Quzzar/kithkyn";

type Village = {
  readonly name: string;
  readonly biome: string;
  readonly detail: string;
  readonly image: string;
  readonly alt: string;
};

const VILLAGES: readonly Village[] = [
  {
    name: "Mediterranean",
    biome: "Sunlit coast",
    detail: "White stone homes gather beneath warm terracotta roofs.",
    image: "/images/mediterranean-homes.png",
    alt: "Mediterranean village buildings with white stone walls and terracotta roofs",
  },
  {
    name: "Jungle",
    biome: "Deep canopy",
    detail: "Bamboo roofs and timber homes climb into an old tree.",
    image: "/images/jungle-homes.png",
    alt: "Jungle village buildings with bamboo roofs and a large tree house",
  },
  {
    name: "Mangrove",
    biome: "Tidal wetlands",
    detail: "Dark timber workshops stand above the shifting waterline.",
    image: "/images/mangrove-homes.png",
    alt: "Mangrove village buildings made from dark timber and red wood",
  },
];

/** Public introduction to KithKyn and its autonomous villages. */
export function HomePage(): ReactElement {
  const villageCarousel = useRef<HTMLDivElement>(null);

  /** Move the village rail by most of its visible width. */
  function scrollVillages(direction: -1 | 1): void {
    const carousel: HTMLDivElement | null = villageCarousel.current;
    if (carousel === null) return;
    const shouldReduceMotion: boolean = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    carousel.scrollBy({
      left: direction * carousel.clientWidth * 0.8,
      behavior: shouldReduceMotion ? "auto" : "smooth",
    });
  }

  return (
    <div className={styles.page}>
      <a className={styles.skipLink} href="#main">
        Skip to content
      </a>

      <header className={styles.header}>
        <a className={styles.brand} href="#top" aria-label="KithKyn home">
          <LogoMark variant="square" className={styles.brandIcon} alt="" />
          <img
            className={styles.brandWordmark}
            src="/brand/kithkyn-wordmark.png"
            alt="KithKyn"
            width={2018}
            height={510}
          />
        </a>
        <nav aria-label="Primary navigation">
          <a className={styles.secondaryNavLink} href="#how-it-works">
            How it works
          </a>
          <a className={styles.secondaryNavLink} href="#villages">
            Villages
          </a>
          <a className={styles.secondaryNavLink} href="#download">
            Download
          </a>
          <a href={GITHUB_URL}>
            GitHub <ExternalLink aria-hidden="true" />
          </a>
        </nav>
      </header>

      <main id="main">
        <section className={styles.hero} id="top" aria-labelledby="title-heading">
          <HeroBackground />
          <div className={styles.heroContent}>
            <h1 className={styles.heroLogo} id="title-heading">
              <LogoMark />
            </h1>
            <p>Autonomous villagers who build a life of their own.</p>
            <a className={styles.heroLink} href="#villages">
              Explore village styles <ArrowDown aria-hidden="true" />
            </a>
          </div>
        </section>

        <section className={styles.villageSection} id="villages" aria-labelledby="village-title">
          <div className={styles.sectionHeading}>
            <div>
              <p>Made for their surroundings</p>
              <h2 id="village-title">Different kinds of villages.</h2>
            </div>
            <div className={styles.carouselControls} aria-label="Village carousel controls">
              <button
                type="button"
                aria-label="Previous villages"
                onClick={(): void => {
                  scrollVillages(-1);
                }}
              >
                <ChevronLeft aria-hidden="true" />
              </button>
              <button
                type="button"
                aria-label="Next villages"
                onClick={(): void => {
                  scrollVillages(1);
                }}
              >
                <ChevronRight aria-hidden="true" />
              </button>
            </div>
          </div>
          <div
            className={styles.villageCarousel}
            ref={villageCarousel}
            role="region"
            aria-label="Village styles"
            tabIndex={0}
          >
            {VILLAGES.map((village: Village) => (
              <figure key={village.name}>
                <img src={village.image} alt={village.alt} width={1708} height={960} />
                <figcaption>
                  <span>{village.biome}</span>
                  <strong>{village.name}</strong>
                  <p>{village.detail}</p>
                </figcaption>
              </figure>
            ))}
          </div>
        </section>

        <section
          className={styles.howSection}
          id="how-it-works"
          aria-labelledby="how-it-works-title"
        >
          <div className={styles.howIntroduction}>
            <p>Village intelligence</p>
            <h2 id="how-it-works-title">A model helps each village decide.</h2>
            <p>
              The village brain summarizes its people, stockpiles, jobs and current problems. An LLM
              steers what the community should prioritize next.
            </p>
          </div>
          <div className={styles.modelDetails}>
            <div className={styles.modelChoices}>
              <article>
                <Cloud aria-hidden="true" />
                <h3>Online models</h3>
                <p>
                  Connect OpenAI, Claude or DeepSeek. The worker calls your chosen provider only
                  when a village needs a decision.
                </p>
              </article>
              <article>
                <HardDrive aria-hidden="true" />
                <h3>Offline models</h3>
                <p>
                  Run the bundled local model through llama.cpp. Decisions stay on your machine and
                  need no API key.
                </p>
              </article>
            </div>
            <div className={styles.guardrail}>
              <ShieldCheck aria-hidden="true" />
              <p>
                <strong>The game stays in control.</strong> Models choose from actions KithKyn has
                already validated. Safe rules take over if a model is slow or unavailable.
              </p>
            </div>
          </div>
        </section>

        <section className={styles.downloadSection} id="download" aria-labelledby="download-title">
          <div>
            <p>Minecraft NeoForge</p>
            <h2 id="download-title">Bring the village to your world.</h2>
            <p>
              Public downloads are being prepared. Follow development on GitHub, then choose your
              preferred launcher when the first release lands.
            </p>
          </div>
          <div className={styles.downloadActions} aria-label="Download destinations">
            <span aria-disabled="true">
              Modrinth <small>Coming soon</small>
            </span>
            <span aria-disabled="true">
              CurseForge <small>Coming soon</small>
            </span>
            <a href={GITHUB_URL}>
              Follow on GitHub <ExternalLink aria-hidden="true" />
            </a>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <a className={styles.footerBrand} href="#top" aria-label="KithKyn home">
          <LogoMark variant="square" className={styles.footerIcon} alt="" />
          <img src="/brand/kithkyn-wordmark.png" alt="KithKyn" width={2018} height={510} />
        </a>
        <nav aria-label="Footer navigation">
          <a href="#villages">Villages</a>
          <a href="#how-it-works">LLM models</a>
          <a href="#download">Download</a>
          <a href={GITHUB_URL}>
            GitHub <ExternalLink aria-hidden="true" />
          </a>
        </nav>
        <p>Minecraft NeoForge</p>
      </footer>
    </div>
  );
}
