import { ArrowUpRight, Download } from "lucide-react";
import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import {
  CURRENT_TIMBER_IDENTITIES,
  PREVIOUS_TIMBER_IDENTITIES,
  type TimberIdentity,
} from "../data/timber-identities";
import { identityTheme } from "../styles/timber-identities.css";
import * as styles from "./timber-directions.css";

/** Compare the wordmark, small K and actual website together before selecting an identity. */
export function TimberDirectionsPage({
  previous = false,
}: { readonly previous?: boolean } = {}): ReactElement {
  const identities: readonly TimberIdentity[] = previous
    ? PREVIOUS_TIMBER_IDENTITIES
    : CURRENT_TIMBER_IDENTITIES;
  return (
    <main id="main" className={styles.page}>
      <div className={styles.intro}>
        <div className={styles.introLinks}>
          <Link to={previous ? "/brand/directions" : "/brand"} className={styles.back}>
            {previous ? "New directions" : "Current brand"}
          </Link>
          {!previous && (
            <Link to="/brand/directions/previous" className={styles.back}>
              Earlier directions
            </Link>
          )}
        </div>
        <h1>{previous ? "Timber, together." : "Another kind of timber."}</h1>
        <p>
          {previous ? "The earlier pair." : "Three new directions."} Each with a logo, K and
          website.
        </p>
      </div>
      <div className={previous ? styles.previousGrid : styles.grid}>
        {identities.map((identity: TimberIdentity): ReactElement => (
          <section
            key={identity.id}
            className={`${styles.direction} ${identityTheme[identity.id]}`}
          >
            <div className={styles.cardIntro}>
              <h2>{identity.name}</h2>
              <p>{identity.description}</p>
            </div>
            <div className={previous ? styles.previousAssets : styles.assets}>
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
