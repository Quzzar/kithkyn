import { defineConfig, devices } from "@playwright/test";

/** End-to-end and screenshot configuration for the public website. */
export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  use: {
    baseURL: "http://127.0.0.1:45175",
    trace: "on-first-retry",
  },
  webServer: {
    command: "bun run dev -- --port 45175",
    url: "http://127.0.0.1:45175",
    reuseExistingServer: true,
  },
  projects: [
    {
      name: "desktop-chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "mobile-chromium",
      use: { ...devices["Pixel 7"] },
    },
  ],
});
