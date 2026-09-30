import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { LogoMark } from "./LogoMark";
import * as styles from "./site.css";

/** Shared navigation keeps the atlas, village life and setup within reach. */
export function SiteHeader(): ReactElement {
  return (
    <>
      <a className={styles.skipLink} href="#main">
        Skip to content
      </a>
      <header className={styles.header}>
        <Link className={styles.brand} to="/" aria-label="KithKyn home">
          <LogoMark />
        </Link>
        <nav className={styles.nav} aria-label="Primary navigation">
          <Link to="/#villages">The villages</Link>
          <Link to="/#life">Village life</Link>
          <Link className={styles.navAction} to="/#get-started">
            Get started
          </Link>
        </nav>
      </header>
    </>
  );
}
