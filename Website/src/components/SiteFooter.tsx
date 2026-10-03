import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { BrandLogo } from "./BrandLogo";
import * as styles from "./site.css";

/** Player links include the project's existing list of credited builders and model authors. */
export function SiteFooter(): ReactElement {
  return (
    <footer className={styles.footer}>
      <div className={styles.footerInner}>
        <div>
          <Link to="/" className={styles.footerBrand} aria-label="Kithkyn home">
            <BrandLogo />
          </Link>
          <p className={styles.footerNote}>Minecraft NeoForge</p>
        </div>
        <div>
          <nav className={styles.footerLinks} aria-label="Footer navigation">
            <a href="https://github.com/Quzzar/kithkyn">GitHub</a>
            <a href="https://github.com/Quzzar/kithkyn/issues">Report an issue</a>
            <a
              href="https://github.com/Quzzar/kithkyn#credits-and-inspiration"
              target="_blank"
              rel="noopener noreferrer"
            >
              Credits
            </a>
          </nav>
          <p className={styles.footerNote}>
            An independent Minecraft mod. Not affiliated with Mojang or Microsoft.
          </p>
        </div>
      </div>
    </footer>
  );
}
