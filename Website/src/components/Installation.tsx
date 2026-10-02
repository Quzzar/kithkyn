import { ArrowUpRight, Cloud, Download, HardDrive } from "lucide-react";
import { Tabs } from "radix-ui";
import type { ReactElement } from "react";
import * as site from "./site.css";
import * as styles from "./installation.css";

/** Compare local and cloud AI requirements alongside the installation guide. */
export function Installation(): ReactElement {
  return (
    <section className={styles.section} id="get-started" aria-labelledby="setup-title">
      <div className={site.container}>
        <div className={styles.setup}>
          <div>
            <p className={site.label}>
              <Download aria-hidden="true" /> Setup
            </p>
            <h2 className={site.sectionHeading} id="setup-title">
              Choose how the AI runs.
            </h2>
            <p className={styles.intro}>Run a local model or use an AI cloud provider.</p>
            <Tabs.Root defaultValue="offline" className={styles.models}>
              <Tabs.List className={styles.modelList} aria-label="AI setup">
                <Tabs.Trigger className={styles.modelTab} value="offline">
                  <HardDrive aria-hidden="true" /> Offline
                </Tabs.Trigger>
                <Tabs.Trigger className={styles.modelTab} value="cloud">
                  <Cloud aria-hidden="true" /> Cloud
                </Tabs.Trigger>
              </Tabs.List>
              <Tabs.Content className={styles.modelContent} value="offline">
                <h3>Local model</h3>
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
                    <dt>AI cloud provider</dt>
                    <dd>None needed</dd>
                  </div>
                </dl>
              </Tabs.Content>
              <Tabs.Content className={styles.modelContent} value="cloud">
                <h3>AI cloud provider</h3>
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
          </div>
          <div className={styles.installCard}>
            <h3>Install Kithkyn</h3>
            <p className={styles.platform}>Minecraft Java · NeoForge</p>
            <ol>
              <li>Install the matching NeoForge loader.</li>
              <li>Add Kithkyn to the server and every client.</li>
              <li>Start your world.</li>
            </ol>
            <a className={site.primaryButton} href="https://github.com/Quzzar/kithkyn#install">
              Read the installation guide <ArrowUpRight aria-hidden="true" />
            </a>
            <a className={styles.sourceLink} href="https://github.com/Quzzar/kithkyn">
              Source on GitHub <ArrowUpRight aria-hidden="true" />
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
