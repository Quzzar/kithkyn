import { Download } from "lucide-react";
import type { ReactElement } from "react";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { BRAND_ICON, BRAND_WORDMARK, type BrandAsset } from "../data/brand";
import * as styles from "./brand-page.css";

/** Creators can inspect and download the chosen identity without selecting a variant. */
export function BrandPage(): ReactElement {
  return (
    <>
      <SiteHeader />
      <main id="main" className={styles.page}>
        <div className={styles.intro}>
          <h1>Made of timber.</h1>
          <p>The Kithkyn logo, icon and sharing artwork.</p>
          <a className={styles.kit} href="/brand/kithkyn-brand-kit.zip" download>
            Download the brand kit <Download aria-hidden="true" />
          </a>
        </div>
        <div className={styles.grid}>
          {[BRAND_WORDMARK, BRAND_ICON].map((asset: BrandAsset): ReactElement => (
            <section className={styles.asset} key={asset.name}>
              <div className={styles.stage}>
                <img
                  src={asset.source}
                  alt={`Kithkyn ${asset.name.toLowerCase()}`}
                  width={asset.width}
                  height={asset.height}
                />
              </div>
              <h2>{asset.name}</h2>
              <p>{asset.description}</p>
              <div className={styles.downloads}>
                <a href={asset.source} download aria-label={`Download ${asset.name} PNG`}>
                  PNG <Download aria-hidden="true" />
                </a>
                <a href={asset.svgSource} download aria-label={`Download ${asset.name} SVG`}>
                  SVG <Download aria-hidden="true" />
                </a>
              </div>
            </section>
          ))}
        </div>
        <section className={styles.sharing} aria-labelledby="sharing-title">
          <div>
            <h2 id="sharing-title">Ready to share.</h2>
            <p>A clean card for posts and project links.</p>
            <div className={styles.downloads}>
              <a href="/brand/social.jpg" download>
                Download sharing card <Download aria-hidden="true" />
              </a>
            </div>
          </div>
          <img
            src="/brand/social.svg"
            alt="Kithkyn sharing card: a world with neighbors"
            width={1200}
            height={630}
          />
        </section>
        <p className={styles.notes}>
          Keep the original colors, proportions and clear space. SVG files contain the original
          pixel artwork.
        </p>
      </main>
      <SiteFooter />
    </>
  );
}
