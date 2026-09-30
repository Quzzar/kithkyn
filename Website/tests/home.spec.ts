import { expect, test, type Page, type TestInfo } from "@playwright/test";

/** Images in the active page state must load without requiring hidden tab content. */
async function waitForImages(page: Page): Promise<void> {
  await page.waitForFunction((): boolean =>
    Array.from(document.images).every(
      (image: HTMLImageElement): boolean => image.complete && image.naturalWidth > 0,
    ),
  );
}

test("introduces the new identity and real village features", async ({ page }): Promise<void> => {
  await page.goto("/");
  await expect(page).toHaveTitle("KithKyn | Your world. Their story.");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Your world.Their story.");
  await expect(page.getByRole("heading", { name: "A village is its people." })).toBeVisible();
  await expect(page.getByText("17 regional building styles.")).toBeVisible();
  await expect(page.getByText(/Original promotional illustration/)).toBeVisible();
  await expect(page.locator("video")).toHaveCount(0);
});

test("explores every bundled catalog and loads each available preview", async ({
  page,
}): Promise<void> => {
  await page.goto("/#villages");
  const tabs = page.getByRole("tablist", { name: "Village styles" }).getByRole("tab");
  await expect(tabs).toHaveCount(17);
  for (let index: number = 0; index < 17; index += 1) {
    const tab = tabs.nth(index);
    const name: string = await tab.innerText();
    await tab.click();
    await expect(tab).toHaveAttribute("aria-selected", "true");
    await expect(page.getByRole("tabpanel", { name, exact: true })).toBeVisible();
    await waitForImages(page);
  }
  await expect(page.getByText("Village field notes")).toBeVisible();
});

test("restores a shareable village selection and browser history", async ({
  page,
}): Promise<void> => {
  await page.goto("/?village=jungle#villages");
  await expect(page.getByRole("tab", { name: "Jungle Tribal", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.getByRole("tab", { name: "Tundra", exact: true }).click();
  await expect(page).toHaveURL(/village=tundra/);
  await page.goBack();
  await expect(page.getByRole("tab", { name: "Jungle Tribal", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.goto("/?village=unknown#villages");
  await expect(page.getByRole("tab", { name: "Mediterranean", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
});

test("supports keyboard selection and visible focus", async ({ page }): Promise<void> => {
  await page.goto("/#villages");
  const first = page.getByRole("tab", { name: "Mediterranean", exact: true });
  await first.focus();
  await first.press("ArrowDown");
  const next = page.getByRole("tab", { name: "Birch Forest", exact: true });
  await expect(next).toBeFocused();
  await expect(next).toHaveAttribute("aria-selected", "true");
  await expect(next).toHaveCSS("outline-style", "solid");
});

test("compares real offline and cloud requirements", async ({ page }): Promise<void> => {
  await page.goto("/#get-started");
  await expect(page.getByText("About 2 GB", { exact: true })).toBeVisible();
  await expect(page.getByText("Roughly 3 GB RAM", { exact: true })).toBeVisible();
  await page.getByRole("tab", { name: "Cloud", exact: true }).click();
  await expect(page.getByText(/Connect OpenAI, Claude, or DeepSeek/)).toBeVisible();
  await expect(page.getByText("Billed by your provider")).toBeVisible();
  await page.getByRole("tab", { name: "Offline", exact: true }).click();
  await expect(page.getByText("None needed")).toBeVisible();
});

test("discloses multiplayer setup and keeps release links honest", async ({
  page,
}): Promise<void> => {
  await page.goto("/#get-started");
  const question = page.getByRole("button", { name: "Does it work on a multiplayer server?" });
  await question.click();
  await expect(question).toHaveAttribute("aria-expanded", "true");
  await expect(
    page.getByText(/Install NeoForge and KithKyn on the server and every client/),
  ).toBeVisible();
  await question.click();
  await expect(question).toHaveAttribute("aria-expanded", "false");
  await expect(page.locator('[aria-disabled="true"]')).toHaveCount(2);
  await expect(page.getByRole("link", { name: "Read the installation guide" })).toHaveAttribute(
    "href",
    "https://github.com/Quzzar/kithkyn#install",
  );
});

test("shows the complete brand suite and downloads the kit", async ({ page }): Promise<void> => {
  await page.goto("/brand");
  await waitForImages(page);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("A place tobelong.");
  for (const variant of ["primary", "reversed", "monochrome", "emblem", "wordmark"]) {
    await expect(page.locator(`main img[src="/brand/kithkyn-${variant}.webp"]`)).toBeVisible();
  }
  const downloadEvent = page.waitForEvent("download");
  await page.getByRole("link", { name: "Download the brand kit" }).click();
  const download = await downloadEvent;
  expect(download.suggestedFilename()).toBe("kithkyn-brand-kit.zip");
  expect(await download.failure()).toBeNull();
});

test("renders both pages without console errors or horizontal overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  for (const path of ["/", "/brand"]) {
    await page.goto(path);
    await waitForImages(page);
    await expect
      .poll(async (): Promise<boolean> =>
        page.evaluate(
          (): boolean =>
            document.documentElement.scrollWidth <= document.documentElement.clientWidth,
        ),
      )
      .toBe(true);
    await page.screenshot({
      path: testInfo.outputPath(path === "/" ? "home.png" : "brand.png"),
      fullPage: true,
    });
  }
  expect(errors).toEqual([]);
});

test("respects reduced motion", async ({ page }): Promise<void> => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");
  await expect(page.locator("html")).toHaveCSS("scroll-behavior", "auto");
  await expect(page.getByRole("tab", { name: "Mediterranean", exact: true })).toHaveCSS(
    "transition-duration",
    "1e-05s",
  );
});

test("opens the brand guide at its beginning from the footer", async ({ page }): Promise<void> => {
  await page.goto("/");
  await page.getByRole("link", { name: "Brand kit", exact: true }).click();
  await expect(page).toHaveTitle("KithKyn | Brand kit");
  await expect(page.getByRole("heading", { level: 1 })).toBeInViewport();
});

test("navigates from the brand guide to the requested homepage section", async ({
  page,
}): Promise<void> => {
  await page.goto("/brand");
  await page.getByRole("link", { name: "Get started", exact: true }).click();
  await expect(page).toHaveURL(/#get-started$/);
  await expect(
    page.getByRole("heading", { name: "New neighbors. Your kind of world." }),
  ).toBeInViewport();
});
