import { ArrowUpRight, Download } from "lucide-react";
import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { TIMBER_IDENTITIES, type TimberIdentity } from "../data/timber-identities";
import { identityTheme } from "../styles/timber-identities.css";
import * as styles from "./timber-directions.css";

/** Compare the wordmark, small K and actual website together before selecting an identity. */
export function TimberDirectionsPage(): ReactElement {
  return (
    <main id="main" className={styles.page}>
      <div className={styles.intro}>
        <Link to="/brand" className={styles.back}>
          Current brand
        </Link>
        <h1>Timber, together.</h1>
        <p>Two identities. Each with a logo, K and website.</p>
      </div>
      <div className={styles.grid}>
        {Object.values(TIMBER_IDENTITIES).map((identity: TimberIdentity): ReactElement => (
          <section
            key={identity.id}
            className={`${styles.direction} ${identityTheme[identity.id]}`}
          >
            <div className={styles.cardIntro}>
              <h2>{identity.name}</h2>
              <p>{identity.description}</p>
            </div>
            <div className={styles.assets}>
              <img
                className={styles.wordmark}
                src={identity.wordmark.source}
                alt={`${identity.name} wordmark: Kithkyn`}
                width={identity.wordmark.width}
                height={identity.wordmark.height}
              />
              <img
                className={styles.icon}
                src={identity.icon.source}
                alt={`${identity.name} K icon`}
                width={identity.icon.width}
                height={identity.icon.height}
              />
            </div>
            <Link
              to={`/brand/directions/${identity.id}`}
              className={styles.preview}
              aria-label={`View ${identity.name} website`}
            >
              <img
                src={`/brand/directions/${identity.id}-preview.jpg`}
                alt={`${identity.name} website preview`}
                width={1265}
                height={889}
              />
            </Link>
            <div className={styles.links}>
              <Link className={styles.open} to={`/brand/directions/${identity.id}`}>
                View website <ArrowUpRight aria-hidden="true" />
              </Link>
              <a href={identity.wordmark.source} download>
                Logo <Download aria-hidden="true" />
              </a>
              <a href={identity.icon.source} download>
                K icon <Download aria-hidden="true" />
              </a>
            </div>
          </section>
        ))}
      </div>
    </main>
  );
}
