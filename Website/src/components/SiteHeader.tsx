import type { ReactElement } from "react";
import { Link, useLocation } from "react-router-dom";
import { BrandLogo } from "./BrandLogo";
import * as styles from "./site.css";

/** Compact branding leaves the scene and message room to carry the page. */
export function SiteHeader(): ReactElement {
  const { search } = useLocation();
  return (
    <>
      <a className={styles.skipLink} href="#main">
        Skip to content
      </a>
      <header className={styles.header}>
        <Link to="/" className={styles.brand} aria-label="Kithkyn home">
          <BrandLogo />
        </Link>
        <nav className={styles.headerNav} aria-label="Main navigation">
          <Link to={{ pathname: "/", search, hash: "#villages" }}>Villages</Link>
          <Link to={{ pathname: "/", search, hash: "#get-started" }}>Setup</Link>
          <a href="https://github.com/Quzzar/kithkyn" className={styles.sourceText}>
            GitHub
          </a>
        </nav>
      </header>
    </>
  );
}
