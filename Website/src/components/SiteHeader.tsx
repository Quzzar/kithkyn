import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { LogoMark } from "./LogoMark";
import * as styles from "./site.css";

/** Quiet navigation leaves the large title sign as the homepage's focal point. */
export function SiteHeader(): ReactElement {
  return (
    <>
      <a className={styles.skipLink} href="#main">
        Skip to content
      </a>
      <header className={styles.header}>
        <Link to="/" className={styles.brand} aria-label="KithKyn home">
          <LogoMark variant="emblem" alt="" />
        </Link>
        <nav className={styles.headerNav} aria-label="Main navigation">
          <Link to="/#villages">Villages</Link>
          <Link to="/#get-started">Setup</Link>
          <a href="https://github.com/Quzzar/kithkyn" aria-label="Source code on GitHub">
            <ArrowUpRight aria-hidden="true" />
            <span className={styles.sourceText}>Source code</span>
          </a>
        </nav>
      </header>
    </>
  );
}
