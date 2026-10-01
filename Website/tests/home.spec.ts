import { expect, test, type Page, type TestInfo } from "@playwright/test";

/** Wait for displayed assets before checking layout or saving a render. */
async function waitForImages(page: Page): Promise<void> {
  await page.waitForFunction((): boolean =>
    Array.from(document.images).every(
      (image: HTMLImageElement): boolean => image.complete && image.naturalWidth > 0,
    ),
  );
}

test("opens the wooden title screen and navigates its menu", async ({ page }): Promise<void> => {
  await page.goto("/play");
  await expect(page).toHaveURL("/");
  await expect(page).toHaveTitle("KithKyn | Bringing villages to life");
  await expect(page.getByRole("heading", { level: 1, name: "KithKyn" })).toBeInViewport();
  await expect(page.locator('img[src*="diorama"]')).toHaveCount(0);
  for (const [link, section] of [
    ["Explore the villages", "Where will they settle?"],
    ["Meet your neighbors", "They’ve got things to do."],
    ["Installation guide", "New neighbors. Your kind of world."],
  ] as const) {
    await page.goto("/");
    await page.getByRole("link", { name: link, exact: true }).click();
    await expect(page.getByRole("heading", { name: section, exact: true })).toBeInViewport();
  }
});

test("browses all 17 styles without rendering inactive frames", async ({ page }): Promise<void> => {
  await page.goto("/atlas");
  const tabs = page.getByRole("tablist", { name: "Village styles" }).getByRole("tab");
  await expect(tabs).toHaveCount(17);
  let imageCount: number = 0;
  let notesCount: number = 0;
  for (let index: number = 0; index < 17; index += 1) {
    const tab = tabs.nth(index);
    const name: string = (await tab.innerText()).trim();
    await tab.click();
    await expect(tab).toHaveAttribute("aria-selected", "true");
    const panel = page.getByRole("tabpanel", { name, exact: true });
    await expect(panel).toBeVisible();
    await expect(panel.getByRole("heading", { level: 3, name, exact: true })).toBeVisible();
    await expect(page.locator('#villages [role="tabpanel"]:visible')).toHaveCount(1);
    if (await panel.getByRole("img").count()) {
      await expect(
        panel.getByText("In-game building preview · catalog review world"),
      ).toBeVisible();
      await waitForImages(page);
      imageCount += 1;
    } else {
      await expect(panel.getByText("Village field notes")).toBeVisible();
      notesCount += 1;
    }
  }
  expect(imageCount).toBe(12);
  expect(notesCount).toBe(5);
  await expect(page.getByRole("button", { name: "Next village style" })).toBeDisabled();
});

test("uses carousel arrows, shareable selection and browser history", async ({
  page,
}): Promise<void> => {
  await page.goto("/atlas?village=jungle");
  await expect(page.getByRole("tab", { name: "Jungle Tribal", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.getByRole("button", { name: "Next village style" }).click();
  await expect(page).toHaveURL(/village=desert/);
  await page.getByRole("button", { name: "Previous village style" }).click();
  await expect(page).toHaveURL(/village=jungle/);
  await page.goBack();
  await expect(page.getByRole("tab", { name: "Desert Oasis", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.goto("/atlas?village=unknown");
  await expect(page.getByRole("tab", { name: "Mediterranean", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await expect(page.getByRole("button", { name: "Previous village style" })).toBeDisabled();
});

test("supports horizontal keyboard selection and visible focus", async ({
  page,
}): Promise<void> => {
  await page.goto("/atlas");
  const first = page.getByRole("tab", { name: "Mediterranean", exact: true });
  await first.focus();
  await first.press("ArrowRight");
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

test("loads the complete brand suite and downloads the kit", async ({ page }): Promise<void> => {
  await page.goto("/brand");
  await expect(page).toHaveTitle("KithKyn | Brand kit");
  await expect(page.getByText("Primary wooden sign")).toBeVisible();
  await expect(page.getByText("One-color stamp")).toBeVisible();
  await expect(page.getByText("Signpost emblem")).toBeVisible();
  await waitForImages(page);
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("link", { name: "Download the brand kit" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe("kithkyn-brand-kit.zip");
  expect(await download.failure()).toBeNull();
  await page.getByRole("link", { name: "Villages", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Where will they settle?" })).toBeInViewport();
});

test("renders home and brand pages without errors or narrow-screen overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  for (const path of ["/", "/brand"]) {
    await page.goto(path);
    await waitForImages(page);
    expect(
      await page.evaluate(
        (): boolean => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    ).toBe(true);
    await page.screenshot({
      path: testInfo.outputPath(`${path === "/" ? "home" : "brand"}.png`),
      fullPage: true,
    });
  }
  await page.setViewportSize({ width: 320, height: 720 });
  for (const path of ["/", "/brand", "/atlas?village=cherry", "/setup"]) {
    await page.goto(path);
    expect(
      await page.evaluate(
        (): boolean => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    ).toBe(true);
  }
  expect(errors).toEqual([]);
});

test("respects reduced motion and keeps planks still on hover", async ({ page }): Promise<void> => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");
  await expect(page.locator("html")).toHaveCSS("scroll-behavior", "auto");
  const plank = page.getByRole("link", { name: "Explore the villages", exact: true });
  await expect(plank).toHaveCSS("transform", "none");
  await plank.hover();
  await expect(plank).toHaveCSS("transform", "none");
});
