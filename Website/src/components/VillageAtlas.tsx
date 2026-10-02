import { ChevronLeft, ChevronRight, MapPin, Trees } from "lucide-react";
import { parseAsString, useQueryState } from "nuqs";
import { Tabs } from "radix-ui";
import { useEffect, useRef, type ReactElement } from "react";
import { DEFAULT_VILLAGE, VILLAGES, type Village } from "../data/villages";
import * as site from "./site.css";
import * as styles from "./village-atlas.css";

/** Regional postcards keep all catalogs reachable by pointer, keyboard or a shared link. */
export function VillageAtlas(): ReactElement {
  const [villageId, setVillageId] = useQueryState(
    "village",
    parseAsString.withDefault(DEFAULT_VILLAGE.id).withOptions({ history: "push" }),
  );
  const village: Village =
    VILLAGES.find((entry: Village): boolean => entry.id === villageId) ?? DEFAULT_VILLAGE;
  const index: number = VILLAGES.findIndex((entry: Village): boolean => entry.id === village.id);
  const selectorRef = useRef<HTMLDivElement>(null);
  const tabRefs = useRef<Map<string, HTMLButtonElement>>(new Map());
  useEffect((): void => {
    const selector: HTMLDivElement | null = selectorRef.current;
    const button: HTMLButtonElement | undefined = tabRefs.current.get(village.id);
    if (selector && button)
      selector.scrollTo({
        left: button.offsetLeft - (selector.clientWidth - button.offsetWidth) / 2,
        behavior: "instant",
      });
  }, [village.id]);
  function moveVillage(offset: number): void {
    const next: Village | undefined = VILLAGES[index + offset];
    if (next) void setVillageId(next.id);
  }
  return (
    <section className={styles.section} id="villages" aria-labelledby="villages-title">
      <div className={site.container}>
        <div className={styles.heading}>
          <div>
            <p className={site.label}>17 regional village styles</p>
            <h2 className={site.sectionHeading} id="villages-title">
              Where will they settle?
            </h2>
          </div>
          <div className={styles.controls}>
            <span>
              {index + 1} / {VILLAGES.length}
            </span>
            <button
              className={styles.arrow}
              type="button"
              aria-label="Previous village style"
              disabled={index === 0}
              onClick={(): void => {
                moveVillage(-1);
              }}
            >
              <ChevronLeft aria-hidden="true" />
            </button>
            <button
              className={styles.arrow}
              type="button"
              aria-label="Next village style"
              disabled={index === VILLAGES.length - 1}
              onClick={(): void => {
                moveVillage(1);
              }}
            >
              <ChevronRight aria-hidden="true" />
            </button>
          </div>
        </div>
        <Tabs.Root
          value={village.id}
          onValueChange={(value: string): void => {
            void setVillageId(value);
          }}
        >
          <Tabs.List className={styles.selector} aria-label="Village styles" ref={selectorRef}>
            {VILLAGES.map((entry: Village): ReactElement => (
              <Tabs.Trigger
                className={styles.tab}
                key={entry.id}
                value={entry.id}
                ref={(element: HTMLButtonElement | null): void => {
                  if (element) tabRefs.current.set(entry.id, element);
                  else tabRefs.current.delete(entry.id);
                }}
              >
                {entry.name}
              </Tabs.Trigger>
            ))}
          </Tabs.List>
          {VILLAGES.map((entry: Village): ReactElement => (
            <Tabs.Content className={styles.detail} key={entry.id} value={entry.id}>
              {entry.image ? (
                <figure className={styles.picture}>
                  <img
                    src={entry.image.source}
                    alt={entry.image.alt}
                    width={entry.image.width}
                    height={entry.image.height}
                  />
                  <figcaption>
                    <a href={entry.image.creditUrl}>Minecraft scenery · Placeholder</a>
                  </figcaption>
                </figure>
              ) : (
                <div className={styles.fieldNotes}>
                  <Trees aria-hidden="true" />
                  <span>Village field notes</span>
                  <p>{entry.materials.join(" · ")}</p>
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
        <p className={styles.note}>Overworld land biomes. Ocean and Nether villages are planned.</p>
      </div>
    </section>
  );
}
