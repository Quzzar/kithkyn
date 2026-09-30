import type { ReactElement } from "react";
import { DirectionBar } from "../components/DirectionBar";
import { Installation } from "../components/Installation";

/** Keep real setup information available while the visual direction is undecided. */
export function SetupPage(): ReactElement {
  return (
    <>
      <DirectionBar />
      <main id="main">
        <Installation />
      </main>
    </>
  );
}
