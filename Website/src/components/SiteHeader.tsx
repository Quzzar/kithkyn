import type { ReactElement } from "react";
import { Link, useLocation } from "react-router-dom";
import { BRAND_WORDMARK, type SiteBranding } from "../data/brand";
import { BrandLogo } from "./BrandLogo";
import * as styles from "./site.css";

/** Compact branding leaves the scene and message room to carry the page. */
export function SiteHeader({
  wordmark = BRAND_WORDMARK,
  homePath = "/",
  isStudy = false,
}: SiteBranding = {}): ReactElement {
  const { search } = useLocation();
  return (
    <>
      <a className={styles.skipLink} href="#main">
        Skip to content
      </a>
      <header className={styles.header}>
        <Link
          to={homePath}
          className={isStudy ? styles.studyBrand : styles.brand}
          aria-label="Kithkyn home"
        >
          <BrandLogo artwork={wordmark} />
        </Link>
        <nav className={styles.headerNav} aria-label="Main navigation">
          <Link to={{ pathname: homePath, search, hash: "#villages" }}>Villages</Link>
          <Link to={{ pathname: homePath, search, hash: "#get-started" }}>Setup</Link>
          <a href="https://github.com/Quzzar/kithkyn" className={styles.sourceText}>
            GitHub
          </a>
        </nav>
      </header>
    </>
  );
}
