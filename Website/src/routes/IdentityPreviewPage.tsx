import type { ReactElement } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { TIMBER_IDENTITIES, type TimberIdentity } from "../data/timber-identities";
import { identityTheme } from "../styles/timber-identities.css";
import { HomePage } from "./HomePage";
import * as styles from "./timber-directions.css";

/** Each direction is a working homepage, including the village atlas and installation content. */
export function IdentityPreviewPage(): ReactElement {
  const { directionId } = useParams<"directionId">();
  const identity: TimberIdentity | undefined = Object.values(TIMBER_IDENTITIES).find(
    (candidate: TimberIdentity): boolean => candidate.id === directionId,
  );
  if (!identity) return <Navigate to="/brand/directions" replace />;
  return (
    <div className={identityTheme[identity.id]}>
      <div className={styles.toolbar}>
        <div className={styles.toolbarInner}>
          <Link to="/brand/directions">All directions</Link>
          <span className={styles.directionLabel}>
            <img
              src={identity.icon.source}
              alt={`${identity.name} K icon`}
              width={identity.icon.width}
              height={identity.icon.height}
            />
            {identity.name}
          </span>
          <a href={identity.wordmark.source} download>
            Logo PNG
          </a>
        </div>
      </div>
      <HomePage identity={identity} />
    </div>
  );
}
