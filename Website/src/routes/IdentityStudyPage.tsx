import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { IdentityLogo } from "../components/IdentityLogo";
import { IDENTITIES, type Identity } from "../data/identities";
import { identityTheme } from "../styles/theme.css";
import * as styles from "./identity-study.css";

/** An equal-weight comparison puts identity and page context in front of the owner. */
export function IdentityStudyPage(): ReactElement {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Link to="/" className={styles.brand}>
          Kithkyn
        </Link>
        <span>Identity studies</span>
      </header>
      <main id="main">
        <div className={styles.intro}>
          <h1>
            Four ways to feel
            <br />
            like Kithkyn.
          </h1>
          <p>Small hints of timber. More room to breathe.</p>
        </div>
        <div className={styles.grid}>
          {IDENTITIES.map((identity: Identity): ReactElement => (
            <article className={styles.study} key={identity.id}>
              <div className={`${styles.stage} ${identityTheme[identity.id]}`}>
                <IdentityLogo identity={identity} />
                <p>{identity.character}</p>
              </div>
              <div className={styles.caption}>
                <div>
                  <h2>
                    {identity.number}. {identity.name}
                  </h2>
                  <p>{identity.description}</p>
                </div>
                <Link
                  to={`/directions/${identity.id}`}
                  aria-label={`Preview ${String(identity.number)}: ${identity.name}`}
                >
                  View design <ArrowUpRight aria-hidden="true" />
                </Link>
              </div>
            </article>
          ))}
        </div>
      </main>
      <footer className={styles.footer}>Four studies. One identity to choose.</footer>
    </div>
  );
}
