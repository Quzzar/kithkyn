import type { ReactElement } from "react";
import { Link } from "react-router-dom";
import * as styles from "./directions-index.css";

type Direction = {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly path: string;
  readonly image: string;
};
const DIRECTIONS: readonly Direction[] = [
  {
    id: "A",
    title: "Title screen",
    description: "Gameplay first. A centered title and game menu.",
    path: "/play",
    image: "title-screen",
  },
  {
    id: "B",
    title: "Village atlas",
    description: "Exploration first. Browse every regional building style.",
    path: "/atlas",
    image: "village-atlas",
  },
  {
    id: "C",
    title: "Village stories",
    description: "People first. A comic strip about shared village life.",
    path: "/stories",
    image: "village-stories",
  },
];

/** A review surface gives each concept equal weight and leaves the choice open. */
export function HomePage(): ReactElement {
  return (
    <main id="main" className={styles.page}>
      <header className={styles.header}>
        <span className={styles.brand}>KithKyn</span>
        <span className={styles.note}>Design study · September 2026</span>
      </header>
      <div className={styles.intro}>
        <p className={styles.label}>Three low-fidelity directions</p>
        <h1>Which world do we step into?</h1>
        <p>Compare the structure and feel. Artwork and branding come after the choice.</p>
      </div>
      <div className={styles.options}>
        {DIRECTIONS.map((direction: Direction): ReactElement => (
          <Link className={styles.option} to={direction.path} key={direction.id}>
            <img
              src={`/previews/${direction.image}.png`}
              alt={`${direction.title} wireframe preview`}
              width={1280}
              height={900}
            />
            <div className={styles.optionCopy}>
              <h2>
                {direction.id} · {direction.title}
              </h2>
              <p>{direction.description}</p>
              <span className={styles.open}>Open prototype ↗</span>
            </div>
          </Link>
        ))}
      </div>
      <footer className={styles.footer}>
        <span>No direction selected.</span>
        <Link to="/setup">Existing setup requirements ↗</Link>
      </footer>
    </main>
  );
}
