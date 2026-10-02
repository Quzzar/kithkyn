import {
  expect,
  test,
  type APIResponse,
  type Download,
  type Page,
  type TestInfo,
} from "@playwright/test";

/** Wait for displayed assets before checking layout or saving a render. */
async function waitForImages(page: Page): Promise<void> {
  await page.waitForFunction((): boolean =>
    Array.from(document.images).every(
      (image: HTMLImageElement): boolean => image.complete && image.naturalWidth > 0,
    ),
  );
}

test("uses the native timber identity on the main landing page", async ({
  page,
}): Promise<void> => {
  await page.goto("/play");
  await expect(page).toHaveURL("/");
  await expect(page).toHaveTitle("Kithkyn | Bringing villages to life");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("A world with");
  await expect(page.locator("header img")).toHaveAttribute("src", "/brand/icon.png");
  await expect(page.locator("header img")).toHaveCSS("image-rendering", "pixelated");
  expect(
    await page
      .locator("header img")
      .evaluate((image: HTMLImageElement): number[] => [image.naturalWidth, image.naturalHeight]),
  ).toEqual([32, 32]);
  for (const image of await page.locator('header img, footer img, img[alt="Kithkyn"]').all()) {
    const scales: number[] = await image.evaluate((element: HTMLImageElement): number[] => {
      const bounds: DOMRect = element.getBoundingClientRect();
      return [bounds.width / element.naturalWidth, bounds.height / element.naturalHeight];
    });
    expect(scales[0]).toBeGreaterThanOrEqual(1);
    expect(Number.isInteger(scales[0])).toBe(true);
    expect(scales[1]).toBe(scales[0]);
  }
  await expect(page.locator("footer img")).toHaveAttribute("src", "/brand/wordmark.png");
  await expect(page.getByRole("img", { name: "Kithkyn", exact: true })).toHaveAttribute(
    "src",
    "/brand/wordmark.png",
  );
  await expect(page.getByText("Minecraft village · Placeholder")).toBeVisible();
  await expect(page.locator("html")).toHaveCSS("color-scheme", "light");
  await expect(page.locator('link[rel="icon"]')).toHaveAttribute("href", "/brand/icon.svg");
  await expect(page.getByRole("link", { name: "Compare identities" })).toHaveCount(0);
  await expect(page.locator('img[src*="diorama"]')).toHaveCount(0);
  await expect(page.getByRole("heading", { level: 1 })).toBeInViewport();
  await page.getByRole("link", { name: "Explore the villages", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Where will they settle?" })).toBeInViewport();
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
      await expect(panel.getByText("Minecraft scenery · Placeholder")).toBeVisible();
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
    page.getByText(/Install NeoForge and Kithkyn on the server and every client/),
  ).toBeVisible();
  await question.click();
  await expect(question).toHaveAttribute("aria-expanded", "false");
  await expect(page.locator('[aria-disabled="true"]')).toHaveCount(2);
  await expect(page.getByRole("link", { name: "Read the installation guide" })).toHaveAttribute(
    "href",
    "https://github.com/Quzzar/kithkyn#install",
  );
});

test("downloads the chosen brand kit and preserves links from earlier previews", async ({
  page,
}): Promise<void> => {
  await page.goto("/brand");
  await expect(page).toHaveTitle("Kithkyn | Brand assets");
  await waitForImages(page);
  const pendingDownload: Promise<Download> = page.waitForEvent("download");
  await page.getByRole("link", { name: "Download the brand kit" }).click();
  const download: Download = await pendingDownload;
  expect(download.suggestedFilename()).toBe("kithkyn-brand-kit.zip");
  for (const source of [
    "wordmark.png",
    "icon.png",
    "wordmark-512.png",
    "wordmark-1024.png",
    "icon-64.png",
    "icon-128.png",
    "icon-256.png",
    "icon-512.png",
    "wordmark.svg",
    "icon.svg",
    "social.jpg",
    "kithkyn-brand-kit.zip",
  ]) {
    const response: APIResponse = await page.request.get(`/brand/${source}`);
    expect(response.ok(), source).toBe(true);
    if (source.endsWith(".png")) {
      expect(response.headers()["content-type"]).toContain("image/png");
      expect(Array.from((await response.body()).subarray(0, 8))).toEqual([
        137, 80, 78, 71, 13, 10, 26, 10,
      ]);
      if (source === "wordmark.png" || source === "icon.png") {
        const pixels: Buffer = await response.body();
        expect([pixels.readUInt32BE(16), pixels.readUInt32BE(20)]).toEqual(
          source === "wordmark.png" ? [128, 34] : [32, 32],
        );
      }
    }
    if (source.endsWith(".jpg")) {
      expect(response.headers()["content-type"]).toContain("image/jpeg");
      expect(Array.from((await response.body()).subarray(0, 3))).toEqual([255, 216, 255]);
    }
    if (source.endsWith(".svg"))
      expect(response.headers()["content-type"]).toContain("image/svg+xml");
    if (source.endsWith(".zip"))
      expect((await response.body()).subarray(0, 2).toString()).toBe("PK");
  }
  await page.getByRole("link", { name: "Villages", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Where will they settle?" })).toBeInViewport();
  for (const path of ["/play", "/directions/corner-frame", "/studies/first", "/studies/timber"]) {
    await page.goto(path);
    await expect(page).toHaveURL("/");
  }
  await page.goto("/directions/unknown?village=jungle#villages");
  await expect(page).toHaveURL("/?village=jungle#villages");
  await expect(page.getByRole("tab", { name: "Jungle Tribal", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.getByRole("link", { name: "Kithkyn home" }).first().click();
  await expect(page).toHaveURL("/");
});

test("renders the homepage and brand assets without errors or narrow-screen overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  const paths: readonly string[] = ["/", "/brand"];
  for (const path of paths) {
    await page.goto(path);
    await waitForImages(page);
    await expect(page.locator("body")).toHaveCSS("font-family", /Outfit/);
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
  for (const path of [...paths, "/atlas?village=cherry", "/setup"]) {
    await page.goto(path);
    expect(
      await page.evaluate(
        (): boolean => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
      `No overflow at 320px on ${path}`,
    ).toBe(true);
  }
  expect(errors).toEqual([]);
});

test("respects reduced motion and keeps actions still on hover", async ({
  page,
}): Promise<void> => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");
  await expect(page.locator("html")).toHaveCSS("scroll-behavior", "auto");
  const action = page.getByRole("link", { name: "Explore the villages", exact: true });
  await expect(action).toHaveCSS("transform", "none");
  await action.hover();
  await expect(action).toHaveCSS("transform", "none");
});
