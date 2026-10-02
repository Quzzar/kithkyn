import { expect, test, type Page, type TestInfo } from "@playwright/test";

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
  await expect(page).toHaveTitle("Kithkyn | Autonomous Villages");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Autonomous Villages");
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
  await expect(page.getByRole("heading", { name: "Villages by biome" })).toBeInViewport();
});

test("browses all 17 biome groups without rendering inactive frames", async ({
  page,
}): Promise<void> => {
  test.slow();
  await page.goto("/atlas");
  const tabs = page.getByRole("tablist", { name: "Biome groups" }).getByRole("tab");
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
      await expect(panel.getByText("Building materials")).toBeVisible();
      notesCount += 1;
    }
  }
  expect(imageCount).toBe(12);
  expect(notesCount).toBe(5);
  await expect(page.getByRole("button", { name: "Next biome group" })).toBeDisabled();
});

test("uses carousel arrows, shareable selection and browser history", async ({
  page,
}): Promise<void> => {
  await page.goto("/atlas?village=jungle");
  await expect(page.getByRole("tab", { name: "Jungle", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.getByRole("button", { name: "Next biome group" }).click();
  await expect(page).toHaveURL(/village=desert/);
  await page.getByRole("button", { name: "Previous biome group" }).click();
  await expect(page).toHaveURL(/village=jungle/);
  await page.goBack();
  await expect(page.getByRole("tab", { name: "Desert", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.goto("/atlas?village=unknown");
  await expect(page.getByRole("tab", { name: "Plains", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await expect(page.getByRole("button", { name: "Previous biome group" })).toBeDisabled();
});

test("supports horizontal keyboard selection and visible focus", async ({
  page,
}): Promise<void> => {
  await page.goto("/atlas");
  const first = page.getByRole("tab", { name: "Plains", exact: true });
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
  await expect(page.getByText("Add Kithkyn to the server and every client.")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Before you move in." })).toHaveCount(0);
  await expect(page.getByText(/Coming soon|villages are planned/)).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Read the installation guide" })).toHaveAttribute(
    "href",
    "https://github.com/Quzzar/kithkyn#install",
  );
});

test("keeps design tools private and opens the actual project credits", async ({
  page,
}): Promise<void> => {
  await page.goto("/brand");
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("link", { name: "Brand assets" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Download the brand kit" })).toHaveCount(0);
  const credits = page.getByRole("link", { name: "Credits", exact: true });
  await expect(credits).toHaveAttribute(
    "href",
    "https://github.com/Quzzar/kithkyn#credits-and-inspiration",
  );
  await expect(credits).toHaveAttribute("target", "_blank");
  await expect(credits).toHaveAttribute("rel", "noopener noreferrer");
  for (const path of [
    "/play",
    "/brand/directions",
    "/brand/directions/hewn-planks",
    "/directions/corner-frame",
    "/studies/first",
    "/studies/timber",
  ]) {
    await page.goto(path);
    await expect(page).toHaveURL("/");
  }
  await page.goto("/directions/unknown?village=jungle#villages");
  await expect(page).toHaveURL("/?village=jungle#villages");
  await expect(page.getByRole("tab", { name: "Jungle", exact: true })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.getByRole("link", { name: "Kithkyn home" }).first().click();
  await expect(page).toHaveURL("/");
});

test("renders player content without errors or narrow-screen overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  const paths: readonly string[] = ["/"];
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
      path: testInfo.outputPath("home.png"),
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

/** A missing one-cell border made intact PNGs look cropped after center-sampling. */
test("preserves a dark outline around every native logo edge", async ({ page }): Promise<void> => {
  await page.goto("/");
  await waitForImages(page);
  const artwork = page.locator('header img, main img[alt="Kithkyn"]');
  for (const image of await artwork.all()) {
    const faults: number = await image.evaluate((element: HTMLImageElement): number => {
      const canvas: HTMLCanvasElement = document.createElement("canvas");
      canvas.width = element.naturalWidth;
      canvas.height = element.naturalHeight;
      const context: CanvasRenderingContext2D | null = canvas.getContext("2d");
      if (!context) throw new Error("Cannot inspect native pixels");
      context.drawImage(element, 0, 0);
      const pixels: Uint8ClampedArray = context.getImageData(
        0,
        0,
        canvas.width,
        canvas.height,
      ).data;
      let missing: number = 0;
      for (let y: number = 0; y < canvas.height; y += 1) {
        for (let x: number = 0; x < canvas.width; x += 1) {
          const index: number = (y * canvas.width + x) * 4;
          if (pixels[index + 3] === 0) continue;
          let boundary: boolean = false;
          for (let dy: number = -1; dy <= 1; dy += 1) {
            for (let dx: number = -1; dx <= 1; dx += 1) {
              const nx: number = x + dx;
              const ny: number = y + dy;
              if (
                nx < 0 ||
                ny < 0 ||
                nx >= canvas.width ||
                ny >= canvas.height ||
                pixels[(ny * canvas.width + nx) * 4 + 3] === 0
              )
                boundary = true;
            }
          }
          if (
            boundary &&
            pixels.subarray(index, index + 3).some((channel: number): boolean => channel >= 65)
          )
            missing += 1;
        }
      }
      return missing;
    });
    expect(faults, "Every exposed pixel edge retains its dark source outline").toBe(0);
  }
});
