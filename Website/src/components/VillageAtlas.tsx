import { parseAsString, useQueryState } from "nuqs";
import { Tabs } from "radix-ui";
import type { ReactElement } from "react";
import { DEFAULT_VILLAGE, VILLAGES, type Village } from "../data/villages";
import * as styles from "./village-atlas.css";

/** The atlas wireframe retains the full roster, keyboard selection and shareable URLs. */
export function VillageAtlas(): ReactElement {
  const [villageId, setVillageId] = useQueryState(
    "village",
    parseAsString.withDefault(DEFAULT_VILLAGE.id).withOptions({ history: "push" }),
  );
  const village: Village =
    VILLAGES.find((entry: Village): boolean => entry.id === villageId) ?? DEFAULT_VILLAGE;
  return (
    <section className={styles.section} aria-label="Regional village catalog">
      <Tabs.Root
        className={styles.atlas}
        value={village.id}
        orientation="vertical"
        onValueChange={(value: string): void => {
          void setVillageId(value);
        }}
      >
        <div className={styles.index}>
          <p className={styles.indexTitle}>Village index / 17 styles</p>
          <Tabs.List className={styles.selector} aria-label="Village styles">
            {VILLAGES.map((entry: Village): ReactElement => (
              <Tabs.Trigger className={styles.tab} value={entry.id} key={entry.id}>
                {entry.name}
                <span aria-hidden="true">↗</span>
              </Tabs.Trigger>
            ))}
          </Tabs.List>
        </div>
        {VILLAGES.map((entry: Village): ReactElement => (
          <Tabs.Content className={styles.detail} value={entry.id} key={entry.id}>
            <div className={styles.preview}>
              <div className={styles.previewFrame}>
                <span>[ {entry.name} architecture ]</span>
                <span className={styles.previewNote}>Real in-game capture goes here</span>
              </div>
              <span className={styles.previewLabel}>Preview placeholder / not a world map</span>
            </div>
            <div className={styles.description}>
              <p className={styles.biome}>{entry.biome}</p>
              <h2>{entry.name}</h2>
              <p>{entry.detail}</p>
              <ul aria-label="Building materials">
                {entry.materials.map((material: string): ReactElement => (
                  <li key={material}>{material}</li>
                ))}
              </ul>
            </div>
          </Tabs.Content>
        ))}
      </Tabs.Root>
      <p className={styles.note}>
        Overworld land styles. Ocean and Nether settlements remain future work.
      </p>
    </section>
  );
}
