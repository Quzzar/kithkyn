import { ArrowUpRight, ChevronDown, Cloud, Download, HardDrive, ShieldCheck } from "lucide-react";
import { Accordion, Tabs } from "radix-ui";
import type { ReactElement } from "react";
import * as site from "./site.css";
import * as styles from "./installation.css";

type Question = { readonly question: string; readonly answer: string };
const QUESTIONS: readonly Question[] = [
  {
    question: "Can I play without a cloud account?",
    answer:
      "Yes. The default offline setup downloads a local model once, then runs on your machine without an API key. Allow about 2 GB of disk space and roughly 3 GB of extra RAM.",
  },
  {
    question: "Does it work on a multiplayer server?",
    answer:
      "Install NeoForge and KithKyn on the server and every client. The server runs the village simulation and its model. If you add the optional Curios mod, install a matching version on both sides.",
  },
  {
    question: "What happens if the model stops responding?",
    answer:
      "The game validates every available action. Safe rules keep the village moving when a model is slow or unavailable. You can also disable AI and use rules alone.",
  },
  {
    question: "Where are the public downloads?",
    answer:
      "The first release is being prepared. Modrinth and CurseForge links will appear when their project pages are ready. For now, follow development and release notes on GitHub.",
  },
  {
    question: "Is KithKyn open source?",
    answer:
      "Yes, under GPL-3.0-only. The source repository includes the full architecture credits and licenses. KithKyn is an independent project, not affiliated with Mojang or Microsoft.",
  },
];

/** Real setup requirements and accessible disclosures for common player questions. */
export function Installation(): ReactElement {
  return (
    <section className={styles.section} id="get-started" aria-labelledby="setup-title">
      <div className={site.container}>
        <div className={styles.setup}>
          <div>
            <p className={site.label}>
              <Download aria-hidden="true" /> Make yourself at home
            </p>
            <h2 className={site.sectionHeading} id="setup-title">
              New neighbors.
              <br />
              Your kind of world.
            </h2>
            <p className={styles.intro}>A local brain, or a cloud model you choose.</p>
            <Tabs.Root defaultValue="offline" className={styles.models}>
              <Tabs.List className={styles.modelList} aria-label="Village brain setup">
                <Tabs.Trigger className={styles.modelTab} value="offline">
                  <HardDrive aria-hidden="true" /> Offline
                </Tabs.Trigger>
                <Tabs.Trigger className={styles.modelTab} value="cloud">
                  <Cloud aria-hidden="true" /> Cloud
                </Tabs.Trigger>
              </Tabs.List>
              <Tabs.Content className={styles.modelContent} value="offline">
                <h3>Right at home.</h3>
                <p>The local model downloads once, then runs offline.</p>
                <dl>
                  <div>
                    <dt>Model download</dt>
                    <dd>About 2 GB</dd>
                  </div>
                  <div>
                    <dt>Extra memory</dt>
                    <dd>Roughly 3 GB RAM</dd>
                  </div>
                  <div>
                    <dt>Cloud account</dt>
                    <dd>None needed</dd>
                  </div>
                </dl>
              </Tabs.Content>
              <Tabs.Content className={styles.modelContent} value="cloud">
                <h3>Your model. Your choice.</h3>
                <p>Connect OpenAI, Claude, or DeepSeek with your own API key.</p>
                <dl>
                  <div>
                    <dt>Connection</dt>
                    <dd>Internet required</dd>
                  </div>
                  <div>
                    <dt>Model usage</dt>
                    <dd>Billed by your provider</dd>
                  </div>
                  <div>
                    <dt>API key</dt>
                    <dd>Set in the mod config</dd>
                  </div>
                </dl>
              </Tabs.Content>
            </Tabs.Root>
            <p className={styles.guardrail}>
              <ShieldCheck aria-hidden="true" /> The game validates actions. Safe rules keep things
              moving.
            </p>
          </div>
          <div className={styles.installCard}>
            <span className={styles.releaseLabel}>First release in preparation</span>
            <h3>
              Bring KithKyn
              <br />
              to your world.
            </h3>
            <p className={styles.platform}>Minecraft Java 1.21.1 · NeoForge 21.1</p>
            <ol>
              <li>Install the matching NeoForge loader.</li>
              <li>Add KithKyn to the server and every client.</li>
              <li>Start your world. The local brain sets itself up.</li>
            </ol>
            <a className={site.primaryButton} href="https://github.com/Quzzar/kithkyn#install">
              Read the installation guide <ArrowUpRight aria-hidden="true" />
            </a>
            <div className={styles.destinations}>
              <span aria-disabled="true">
                Modrinth <small>Coming soon</small>
              </span>
              <span aria-disabled="true">
                CurseForge <small>Coming soon</small>
              </span>
            </div>
            <a className={styles.sourceLink} href="https://github.com/Quzzar/kithkyn">
              Follow development on GitHub <ArrowUpRight aria-hidden="true" />
            </a>
          </div>
        </div>
        <div className={styles.faq}>
          <div>
            <p className={site.label}>A few things to know</p>
            <h2 className={site.sectionHeading}>Before you move in.</h2>
          </div>
          <Accordion.Root type="single" collapsible className={styles.questions}>
            {QUESTIONS.map((entry: Question): ReactElement => (
              <Accordion.Item
                value={entry.question}
                key={entry.question}
                className={styles.question}
              >
                <Accordion.Header>
                  <Accordion.Trigger className={styles.questionTrigger}>
                    {entry.question}
                    <ChevronDown aria-hidden="true" />
                  </Accordion.Trigger>
                </Accordion.Header>
                <Accordion.Content className={styles.answer}>{entry.answer}</Accordion.Content>
              </Accordion.Item>
            ))}
          </Accordion.Root>
        </div>
      </div>
    </section>
  );
}
