import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { LogoMark } from "./LogoMark";
import * as styles from "./site.css";

export function SiteFooter(): ReactElement {
  return (
    <footer className={styles.footer}>
      <div className={styles.footerInner}>
        <div>
          <Link to="/" className={styles.footerBrand} aria-label="KithKyn home">
            <LogoMark variant="wordmark" alt="" />
          </Link>
          <p className={styles.footerNote}>Minecraft NeoForge</p>
        </div>
        <div>
          <nav className={styles.footerLinks} aria-label="Footer navigation">
            <Link to="/brand">Brand kit</Link>
            <a href="https://github.com/Quzzar/kithkyn">GitHub</a>
            <a href="https://github.com/Quzzar/kithkyn/issues">Report an issue</a>
            <a href="https://github.com/Quzzar/kithkyn#credits">Credits</a>
          </nav>
          <p className={styles.footerNote}>
            An independent Minecraft mod. Not affiliated with Mojang or Microsoft.
          </p>
        </div>
      </div>
    </footer>
  );
}
