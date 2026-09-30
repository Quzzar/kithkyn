import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { LogoMark } from "./LogoMark";
import * as styles from "./site.css";

/** Verified development and attribution destinations, without invented release links. */
export function SiteFooter(): ReactElement {
  return (
    <footer className={styles.footer}>
      <div className={styles.footerInner}>
        <div>
          <Link className={styles.footerBrand} to="/" aria-label="KithKyn home">
            <LogoMark variant="reversed" />
          </Link>
          <p className={styles.footerNote}>Every village, a story.</p>
          <p className={styles.footerNote}>
            A mod for Minecraft Java Edition.
            <br />
            Not affiliated with Mojang or Microsoft.
          </p>
        </div>
        <nav className={styles.footerNav} aria-label="Footer navigation">
          <a href="https://github.com/Quzzar/kithkyn">Source code</a>
          <a href="https://github.com/Quzzar/kithkyn/issues">Report an issue</a>
          <a href="https://github.com/Quzzar/kithkyn#credits-and-inspiration">Credits</a>
          <Link to="/brand">Brand kit</Link>
        </nav>
      </div>
    </footer>
  );
}
