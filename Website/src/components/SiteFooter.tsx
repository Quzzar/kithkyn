import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import type { Identity } from "../data/identities";
import { IdentityLogo } from "./IdentityLogo";
import * as styles from "./site.css";

/** Preview navigation stays separate from real public release destinations. */
export function SiteFooter({ identity }: { readonly identity: Identity }): ReactElement {
  return (
    <footer className={styles.footer}>
      <div className={styles.footerInner}>
        <div>
          <Link to="/" className={styles.footerBrand} aria-label="Compare identities">
            <IdentityLogo identity={identity} isDecorative />
          </Link>
          <p className={styles.footerNote}>Minecraft NeoForge</p>
        </div>
        <div>
          <nav className={styles.footerLinks} aria-label="Footer navigation">
            <Link to="/">Compare identities</Link>
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
