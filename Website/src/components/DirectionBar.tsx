import type { ReactElement } from "react";
import { Link, NavLink } from "react-router-dom";
import * as styles from "./direction-bar.css";

/** Review controls belong to the prototype, not the proposed public website. */
export function DirectionBar(): ReactElement {
  return (
    <>
      <a className={styles.skip} href="#main">
        Skip to content
      </a>
      <div className={styles.bar}>
        <Link to="/" className={styles.compare}>
          ← Compare directions
        </Link>
        <nav className={styles.nav} aria-label="Design directions">
          <NavLink to="/play">A · Title screen</NavLink>
          <NavLink to="/atlas">B · Atlas</NavLink>
          <NavLink to="/stories">C · Stories</NavLink>
        </nav>
        <span className={styles.note}>Low fidelity · no direction selected</span>
      </div>
    </>
  );
}
