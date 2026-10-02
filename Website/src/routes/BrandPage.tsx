import { Download } from "lucide-react";
import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { SiteHeader } from "../components/SiteHeader";
import { SiteFooter } from "../components/SiteFooter";
import { BRAND_ICON, BRAND_WORDMARK, type BrandAsset, type BrandExport } from "../data/brand";
import * as styles from "./brand-page.css";

/** Creators can inspect and download the chosen identity without selecting a variant. */
export function BrandPage(): ReactElement {
  return (
    <>
      <SiteHeader />
      <main id="main" className={styles.page}>
        <div className={styles.intro}>
          <h1>Brand assets</h1>
          <p>The Kithkyn logo, icon and sharing artwork.</p>
          <a className={styles.kit} href="/brand/kithkyn-brand-kit.zip" download>
            Download the brand kit <Download aria-hidden="true" />
          </a>
          <div className={styles.downloads}>
            <Link to="/brand/directions">Explore new timber directions</Link>
          </div>
        </div>
        <div className={styles.grid}>
          {[BRAND_WORDMARK, BRAND_ICON].map((asset: BrandAsset): ReactElement => (
            <section className={styles.asset} key={asset.name}>
              <div className={styles.stage}>
                <img
                  className={styles.artwork[asset.kind]}
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
              <div className={styles.downloads}>
                {asset.exports.map((exported: BrandExport): ReactElement => (
                  <a
                    href={exported.source}
                    download
                    key={exported.source}
                    aria-label={`Download ${asset.name} ${exported.label}`}
                  >
                    {exported.label}
                  </a>
                ))}
              </div>
            </section>
          ))}
        </div>
        <section className={styles.sharing} aria-labelledby="sharing-title">
          <div>
            <h2 id="sharing-title">Sharing card</h2>
            <p>1200 × 630 pixels, in JPEG and SVG.</p>
            <div className={styles.downloads}>
              <a href="/brand/social.jpg" download>
                Download sharing card <Download aria-hidden="true" />
              </a>
            </div>
          </div>
          <img
            src="/brand/social.svg"
            alt="Kithkyn sharing card: Autonomous Villages for Minecraft"
            width={1200}
            height={630}
          />
        </section>
        <p className={styles.notes}>
          Use whole-number scales and nearest-neighbor rendering. SVG files contain the native pixel
          artwork.
        </p>
      </main>
      <SiteFooter />
    </>
  );
}
