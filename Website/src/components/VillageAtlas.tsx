import { ArrowUpRight, MapPin, Trees } from "lucide-react";
import { parseAsString, useQueryState } from "nuqs";
import { Tabs } from "radix-ui";
import type { ReactElement } from "react";
import { DEFAULT_VILLAGE, VILLAGES, type Village } from "../data/villages";
import { LogoMark } from "./LogoMark";
import * as site from "./site.css";
import * as styles from "./village-atlas.css";

/** A keyboard-accessible, shareable field guide to every bundled village catalog. */
export function VillageAtlas(): ReactElement {
  const [villageId, setVillageId] = useQueryState(
    "village",
    parseAsString.withDefault(DEFAULT_VILLAGE.id).withOptions({ history: "push" }),
  );
  const village: Village =
    VILLAGES.find((entry: Village): boolean => entry.id === villageId) ?? DEFAULT_VILLAGE;
  return (
    <section className={styles.section} id="villages" aria-labelledby="atlas-title">
      <div className={site.container}>
        <div className={styles.heading}>
          <div>
            <p className={site.label}>
              <Trees aria-hidden="true" /> Rooted in their surroundings
            </p>
            <h2 className={site.sectionHeading} id="atlas-title">
              Find a place
              <br />
              to call home.
            </h2>
          </div>
          <p className={styles.intro}>
            17 regional building styles.
            <br />A home for every kind of neighbor.
          </p>
        </div>
        <Tabs.Root
          className={styles.atlas}
          value={village.id}
          onValueChange={(value: string): void => {
            void setVillageId(value);
          }}
          orientation="vertical"
        >
          <Tabs.List className={styles.selector} aria-label="Village styles">
            {VILLAGES.map((entry: Village): ReactElement => (
              <Tabs.Trigger className={styles.tab} value={entry.id} key={entry.id}>
                {entry.name}
                <ArrowUpRight aria-hidden="true" />
              </Tabs.Trigger>
            ))}
          </Tabs.List>
          {VILLAGES.map((entry: Village): ReactElement => (
            <Tabs.Content className={styles.detail} value={entry.id} key={entry.id}>
              {entry.image ? (
                <figure className={styles.picture}>
                  <img
                    src={`/images/${entry.image}.webp`}
                    alt={`${entry.name} building catalog preview`}
                    width={1200}
                    height={675}
                    loading="lazy"
                  />
                  <figcaption>In-game building preview · catalog review world</figcaption>
                </figure>
              ) : (
                <div className={styles.fieldNote}>
                  <LogoMark variant="emblem" alt="" />
                  <p>Village field notes</p>
                  <h3>{entry.name}</h3>
                  <span>{entry.materials.join(" · ")}</span>
                </div>
              )}
              <div className={styles.description}>
                <p className={styles.biome}>
                  <MapPin aria-hidden="true" />
                  {entry.biome}
                </p>
                <h3>{entry.name}</h3>
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
          Built for the Overworld. Ocean settlements and Nether villages are future explorations.
        </p>
      </div>
    </section>
  );
}
