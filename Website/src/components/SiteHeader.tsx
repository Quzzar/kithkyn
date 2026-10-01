import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import type { Identity } from "../data/identities";
import { IdentityLogo } from "./IdentityLogo";
import * as styles from "./site.css";

/** Compact branding leaves the scene and message room to carry the page. */
export function SiteHeader({ identity }: { readonly identity: Identity }): ReactElement {
  return (
    <>
      <a className={styles.skipLink} href="#main">
        Skip to content
      </a>
      <header className={styles.header}>
        <Link to="/" className={styles.brand} aria-label="Compare identities">
          <IdentityLogo identity={identity} isDecorative />
        </Link>
        <nav className={styles.headerNav} aria-label="Main navigation">
          <a href="#villages">Villages</a>
          <a href="#get-started">Setup</a>
          <Link to="/" className={styles.sourceText}>
            Compare identities
          </Link>
        </nav>
      </header>
    </>
  );
}
