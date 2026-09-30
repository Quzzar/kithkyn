import { expect, test, type Page, type TestInfo } from "@playwright/test";

async function waitForImages(page: Page): Promise<void> {
  await page.waitForFunction((): boolean =>
    Array.from(document.images).every(
      (image: HTMLImageElement): boolean => image.complete && image.naturalWidth > 0,
    ),
  );
}

test("opens each direction and returns to the comparison", async ({ page }): Promise<void> => {
  await page.goto("/");
  await expect(page).toHaveTitle("KithKyn | Compare directions");
  for (const direction of ["A · Title screen", "B · Village atlas", "C · Village stories"]) {
    await page.getByRole("link", { name: new RegExp(direction) }).click();
    await expect(page.getByRole("heading", { level: 1 })).toBeInViewport();
    await expect(page.locator('img[src*="diorama"], img[src*="social"]')).toHaveCount(0);
    await page.getByRole("link", { name: "← Compare directions", exact: true }).click();
    await expect(page).toHaveTitle("KithKyn | Compare directions");
  }
});

test("browses every bundled village style", async ({ page }): Promise<void> => {
  await page.goto("/atlas");
  const tabs = page.getByRole("tablist", { name: "Village styles" }).getByRole("tab");
  await expect(tabs).toHaveCount(17);
  for (let index: number = 0; index < 17; index += 1) {
    const tab = tabs.nth(index);
    const name: string = (await tab.innerText()).replace("↗", "").trim();
    await tab.click();
    await expect(tab).toHaveAttribute("aria-selected", "true");
    await expect(page.getByRole("tabpanel", { name, exact: true })).toBeVisible();
    await expect(page.getByRole("heading", { level: 2, name, exact: true })).toBeVisible();
  }
});

test("restores a shareable village selection and browser history", async ({
  page,
}): Promise<void> => {
  await page.goto("/atlas?village=jungle");
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
  await page.goto("/atlas?village=unknown");
  await expect(page.getByRole("tab", { name: "Mediterranean", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
});

test("supports keyboard selection and visible focus", async ({ page }): Promise<void> => {
  await page.goto("/atlas");
  const first = page.getByRole("tab", { name: "Mediterranean", exact: true });
  await first.focus();
  await first.press("ArrowDown");
  const next = page.getByRole("tab", { name: "Birch Forest", exact: true });
  await expect(next).toBeFocused();
  await expect(next).toHaveAttribute("aria-selected", "true");
  await expect(next).toHaveCSS("outline-style", "solid");
});

test("retains offline and cloud setup requirements", async ({ page }): Promise<void> => {
  await page.goto("/setup");
  await expect(page.getByText("About 2 GB", { exact: true })).toBeVisible();
  await expect(page.getByText("Roughly 3 GB RAM", { exact: true })).toBeVisible();
  await page.getByRole("tab", { name: "Cloud", exact: true }).click();
  await expect(page.getByText(/Connect OpenAI, Claude, or DeepSeek/)).toBeVisible();
  await expect(page.getByText("Billed by your provider")).toBeVisible();
  await page.getByRole("tab", { name: "Offline", exact: true }).click();
  await expect(page.getByText("None needed")).toBeVisible();
});

test("retains multiplayer guidance and real installation destinations", async ({
  page,
}): Promise<void> => {
  await page.goto("/setup");
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

test("renders the comparison and prototypes without errors or horizontal overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  for (const path of ["/", "/play", "/atlas", "/stories", "/setup"]) {
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
      path: testInfo.outputPath(`${path === "/" ? "comparison" : path.slice(1)}.png`),
      fullPage: true,
    });
  }
  await page.setViewportSize({ width: 320, height: 720 });
  for (const path of ["/", "/play", "/atlas", "/stories", "/setup"]) {
    await page.goto(path);
    expect(
      await page.evaluate(
        (): boolean => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    ).toBe(true);
  }
  expect(errors).toEqual([]);
});

test("respects reduced motion", async ({ page }): Promise<void> => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/play");
  await expect(page.locator("html")).toHaveCSS("scroll-behavior", "auto");
});
