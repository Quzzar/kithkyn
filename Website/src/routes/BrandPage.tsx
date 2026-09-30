import { ArrowDownToLine } from "lucide-react";
import type { ReactElement } from "react";
import { LogoMark } from "../components/LogoMark";
import { SiteFooter } from "../components/SiteFooter";
import { SiteHeader } from "../components/SiteHeader";
import * as site from "../components/site.css";
import * as styles from "./brand-page.css";

type Swatch = { readonly name: string; readonly hex: string; readonly className: string };
const SWATCHES: readonly Swatch[] = [
  { name: "Pine", hex: "#23483f", className: styles.pine },
  { name: "Mist", hex: "#eef3ed", className: styles.mist },
  { name: "Meadow", hex: "#9cba9e", className: styles.meadow },
  { name: "Honey", hex: "#f5c45b", className: styles.honey },
  { name: "Sky", hex: "#cddfe7", className: styles.sky },
];

/** A usable brand kit and visual gallery of the identity's production variants. */
export function BrandPage(): ReactElement {
  return (
    <>
      <SiteHeader />
      <main id="main" className={styles.page}>
        <div className={styles.intro}>
          <p className={site.label}>The KithKyn identity</p>
          <h1>
            A place to
            <br />
            belong.
          </h1>
          <p>Homes around a hearth. A little world worth caring for.</p>
          <a className={site.primaryButton} href="/brand/kithkyn-brand-kit.zip" download>
            Download the brand kit <ArrowDownToLine aria-hidden="true" />
          </a>
        </div>
        <div className={styles.logoGrid}>
          <figure className={styles.primary}>
            <LogoMark />
            <figcaption>Primary lockup · light backgrounds</figcaption>
          </figure>
          <figure className={styles.reversed}>
            <LogoMark variant="reversed" />
            <figcaption>Reversed lockup · dark backgrounds</figcaption>
          </figure>
          <figure className={styles.mono}>
            <LogoMark variant="monochrome" />
            <figcaption>One-color lockup</figcaption>
          </figure>
          <figure className={styles.emblem}>
            <LogoMark variant="emblem" />
            <figcaption>Hearth emblem · app icons & avatars</figcaption>
          </figure>
          <figure className={styles.wordmark}>
            <LogoMark variant="wordmark" />
            <figcaption>Wordmark · compact spaces</figcaption>
          </figure>
        </div>
        <section className={styles.section} aria-labelledby="palette-title">
          <p className={site.label}>Color comes from the world</p>
          <h2 id="palette-title">Meadow. Hearth. Sky.</h2>
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
            <h2 id="type-title">Friendly by nature.</h2>
            <p>
              Bricolage Grotesque for stories.
              <br />
              Outfit for the little details.
            </p>
          </div>
          <div className={styles.typeSample}>
            Every village,
            <br />a story.<span>Aa Bb Cc · 0123456789</span>
          </div>
        </section>
        <section className={styles.section} aria-labelledby="social-title">
          <p className={site.label}>Out in the world</p>
          <h2 id="social-title">One identity. Wherever we go.</h2>
          <img
            className={styles.socialImage}
            src="/brand/kithkyn-social.png"
            alt="KithKyn social banner with the village illustration and Your world. Their story. headline"
            width={1200}
            height={630}
            loading="lazy"
          />
        </section>
        <section className={styles.usage} aria-labelledby="usage-title">
          <h2 id="usage-title">A little care goes a long way.</h2>
          <p>
            Keep the logo’s proportions and give it room to breathe. Use the reversed version on
            dark backgrounds and the emblem at small sizes. The kit includes transparent
            high-resolution PNG originals, optimized WebP assets, 32–512 pixel icons, social
            artwork, fonts and usage notes.
          </p>
          <p>
            The village illustration is promotional artwork. Use actual game captures when showing
            gameplay.
          </p>
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
