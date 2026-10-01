import { ArrowUpRight } from "lucide-react";
import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import { IdentityLogo } from "../components/IdentityLogo";
import {
  SELECTED_IDENTITY,
  STUDY_ROUNDS,
  type Identity,
  type IdentityStudyRound,
  type IdentityRoundContent,
} from "../data/identities";
import { identityTheme } from "../styles/theme.css";
import * as styles from "./identity-study.css";

/** An equal-weight comparison puts identity and page context in front of the owner. */
export function IdentityStudyPage({
  round = "lettering",
}: {
  readonly round?: IdentityStudyRound;
}): ReactElement {
  const studyRound: IdentityRoundContent = STUDY_ROUNDS[round];
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Link to="/" className={styles.brand}>
          Kithkyn
        </Link>
        <Link to={studyRound.previous}>
          {round === "first" ? "Latest studies" : "Earlier studies"}
        </Link>
      </header>
      <main id="main">
        <div className={styles.intro}>
          <h1>
            {studyRound.headline[0]}
            <br />
            {studyRound.headline[1]}
          </h1>
          <p>{studyRound.description}</p>
        </div>
        <div className={styles.grid}>
          {studyRound.studies.map((identity: Identity): ReactElement => {
            const isSelectedIcon: boolean =
              round === "lettering" && identity.id === SELECTED_IDENTITY.id;
            return (
              <article className={styles.study} key={identity.id}>
                <div className={`${styles.stage} ${identityTheme[identity.presentation]}`}>
                  <IdentityLogo identity={identity} isIconOnly={isSelectedIcon} />
                  <p>{isSelectedIcon ? "The K you selected." : identity.character}</p>
                </div>
                <div className={styles.caption}>
                  <div>
                    <h2>
                      {isSelectedIcon
                        ? "Selected icon"
                        : `${String(identity.number)}. ${identity.name}`}
                    </h2>
                    <p>
                      {isSelectedIcon
                        ? "Three joined oak planks. Our starting point."
                        : identity.description}
                    </p>
                  </div>
                  <Link
                    to={`/directions/${identity.id}`}
                    aria-label={`Preview ${String(identity.number)}: ${identity.name}`}
                  >
                    {isSelectedIcon ? "Original preview" : "View design"}{" "}
                    <ArrowUpRight aria-hidden="true" />
                  </Link>
                </div>
              </article>
            );
          })}
        </div>
      </main>
      <footer className={styles.footer}>{studyRound.footer}</footer>
    </div>
  );
}
