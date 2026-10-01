import { ArrowDownToLine } from "lucide-react";
import type { ReactElement } from "react";
import { LogoMark } from "../components/LogoMark";
import { SiteFooter } from "../components/SiteFooter";
import { SiteHeader } from "../components/SiteHeader";
import * as site from "../components/site.css";
import * as styles from "./brand-page.css";

type Swatch = { readonly name: string; readonly hex: string; readonly className: string };
const SWATCHES: readonly Swatch[] = [
  { name: "Forest", hex: "#172d27", className: styles.forest },
  { name: "Oak", hex: "#c58a4c", className: styles.oak },
  { name: "Paint", hex: "#fff3d8", className: styles.paint },
  { name: "Timber", hex: "#4f301d", className: styles.timber },
  { name: "Leaf", hex: "#a9bb94", className: styles.leaf },
  { name: "Paper", hex: "#f1e3c5", className: styles.paper },
];

/** The brand gallery exposes every logo variant and its editable source exports. */
export function BrandPage(): ReactElement {
  return (
    <>
      <SiteHeader />
      <main id="main" className={styles.page}>
        <div className={styles.intro}>
          <p className={site.label}>The KithKyn brand kit</p>
          <h1>
            A little rough
            <br />
            around the edges.
          </h1>
          <p>Painted letters. Sturdy planks. A place to belong.</p>
          <a className={site.primaryButton} href="/brand/kithkyn-brand-kit.zip" download>
            Download the brand kit <ArrowDownToLine aria-hidden="true" />
          </a>
        </div>
        <div className={styles.logoGrid}>
          <figure className={styles.lightTile}>
            <LogoMark />
            <figcaption>Primary wooden sign</figcaption>
          </figure>
          <figure className={styles.darkTile}>
            <LogoMark variant="reversed" />
            <figcaption>Reversed sign</figcaption>
          </figure>
          <figure className={styles.lightTile}>
            <LogoMark variant="monochrome" />
            <figcaption>One-color stamp</figcaption>
          </figure>
          <figure className={styles.darkTile}>
            <LogoMark variant="wordmark" />
            <figcaption>Outlined wordmark</figcaption>
          </figure>
          <figure className={styles.emblemTile}>
            <LogoMark variant="emblem" />
            <figcaption>Signpost emblem</figcaption>
          </figure>
          <figure className={styles.iconTile}>
            <img
              src="/brand/kithkyn-icon-64.png"
              alt="KithKyn 64-pixel icon"
              width={64}
              height={64}
            />
            <figcaption>Icons · 32, 64, 180 & 512 px</figcaption>
          </figure>
        </div>
        <section className={styles.section} aria-labelledby="palette-title">
          <p className={site.label}>Color</p>
          <h2 id="palette-title">Forest. Timber. Painted letters.</h2>
          <div className={styles.swatches}>
            {SWATCHES.map((swatch: Swatch): ReactElement => (
              <div key={swatch.name}>
                <div className={swatch.className} />
                <strong>{swatch.name}</strong>
                <span>{swatch.hex}</span>
              </div>
            ))}
          </div>
        </section>
        <section className={styles.typeSection} aria-labelledby="type-title">
          <div>
            <p className={site.label}>Typography</p>
            <h2 id="type-title">A game at heart.</h2>
            <p>
              Pixelify Sans for headings.
              <br />
              Outfit for readable details.
            </p>
          </div>
          <div className={styles.typeSample}>
            Bringing villages
            <br />
            to life.<span>Aa Bb Cc · 0123456789</span>
          </div>
        </section>
        <section className={styles.section} aria-labelledby="social-title">
          <p className={site.label}>Out in the world</p>
          <h2 id="social-title">The sharing card.</h2>
          <img
            className={styles.social}
            src="/brand/kithkyn-social.png"
            alt="KithKyn wooden logo and Bringing villages to life on forest green"
            width={1200}
            height={630}
          />
        </section>
        <section className={styles.usage} aria-labelledby="usage-title">
          <h2 id="usage-title">Give it a little room.</h2>
          <p>
            Keep the sign’s proportions and leave clear space around it. Use the emblem at small
            sizes and the reversed sign on dark backgrounds.
          </p>
          <p>
            The kit includes editable SVGs with outlined logo lettering, transparent PNG and WebP
            exports, icons, the sharing card, local fonts and usage notes.
          </p>
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
