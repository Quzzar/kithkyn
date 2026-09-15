import { expect, test, type Page, type TestInfo } from "@playwright/test";

/** Wait until every homepage image has loaded. */
async function waitForImages(page: Page): Promise<void> {
  await page.waitForFunction((): boolean =>
    Array.from(document.images).every(
      (image: HTMLImageElement): boolean => image.complete && image.naturalWidth > 0,
    ),
  );
}

test("introduces the villages and how they work", async ({ page }): Promise<void> => {
  await page.goto("/");
  await expect(page).toHaveTitle("KithKyn | Bringing villages to life");
  await expect(page.getByRole("heading", { level: 1 })).toHaveAccessibleName(
    "KithKyn. Bringing villages to life.",
  );
  await expect(page.getByRole("heading", { name: "Different kinds of villages." })).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "A model helps each village decide." }),
  ).toBeVisible();
  await expect(page.getByRole("heading", { name: "Online models" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Offline models" })).toBeVisible();
  await expect(page.getByText(/OpenAI, Claude or DeepSeek/)).toBeVisible();
  await expect(page.getByRole("button")).toHaveCount(2);
  await expect(page.getByText("A KithKyn project for Minecraft Java Edition.")).toHaveCount(0);
  await expect(page.getByText("Minecraft NeoForge")).toHaveCount(2);
  await expect(page.getByText(/Minecraft 1\.21\.1/)).toHaveCount(0);
});

test("scrolls through village styles with icon controls", async ({ page }): Promise<void> => {
  await page.goto("/");
  const carousel = page.getByRole("region", { name: "Village styles" });
  await expect(carousel).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous villages" })).toBeVisible();
  await page.getByRole("button", { name: "Next villages" }).click();
  await expect
    .poll(async (): Promise<number> =>
      carousel.evaluate((element: HTMLElement) => element.scrollLeft),
    )
    .toBeGreaterThan(0);
});

test("uses the wooden wordmark in the header", async ({ page }): Promise<void> => {
  await page.goto("/");
  const brand = page.getByRole("banner").getByRole("link", { name: "KithKyn home" });
  await expect(brand.getByRole("img", { name: "KithKyn", exact: true })).toHaveAttribute(
    "src",
    "/brand/kithkyn-wordmark.png",
  );
  await expect(brand).not.toContainText("KithKyn");
});

test("keeps unreleased download destinations honest", async ({ page }): Promise<void> => {
  await page.goto("/");
  await expect(
    page.locator('[aria-disabled="true"]').filter({ hasText: "Modrinth" }),
  ).toHaveAttribute("aria-disabled", "true");
  await expect(
    page.locator('[aria-disabled="true"]').filter({ hasText: "CurseForge" }),
  ).toHaveAttribute("aria-disabled", "true");
  await expect(page.getByRole("link", { name: "Follow on GitHub" })).toHaveAttribute(
    "href",
    "https://github.com/Quzzar/kithkyn",
  );
});

test("fades from the poster into the hero video when motion is allowed", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  await page.route("**/valecraft-reference-hero.mp4", async (route): Promise<void> => {
    await route.abort();
  });
  await page.goto("/");
  await expect(page.locator('img[src="/images/valecraft-reference-forest.jpg"]')).toBeVisible();

  const video = page.locator("video");
  if (testInfo.project.name.includes("mobile")) {
    await expect(video).toHaveCount(0);
    return;
  }

  await expect(video).toHaveCount(1);
  await expect(video).toHaveAttribute("autoplay", "");
  await expect(video).toHaveAttribute("loop", "");
  await expect(video).toHaveCSS("opacity", "0");
  await video.dispatchEvent("playing");
  await expect(video).toHaveCSS("opacity", "1");
});

test("plays the hero video on desktop", async ({ page }, testInfo: TestInfo): Promise<void> => {
  await page.goto("/");
  const video = page.locator("video");
  if (testInfo.project.name.includes("mobile")) {
    await expect(video).toHaveCount(0);
    return;
  }

  await expect(video).toHaveCount(1);
  await expect
    .poll(async (): Promise<number> =>
      video.evaluate((element: HTMLVideoElement) => element.currentTime),
    )
    .toBeGreaterThan(0);
  await expect(video).toHaveCSS("opacity", "1");
});

test("renders without errors or horizontal overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  await page.goto("/");
  await waitForImages(page);
  const overflow: {
    readonly documentHasOverflow: boolean;
    readonly offenders: readonly string[];
    readonly viewportWidth: number;
  } = await page.evaluate(
    (): {
      readonly documentHasOverflow: boolean;
      readonly offenders: readonly string[];
      readonly viewportWidth: number;
    } => ({
      documentHasOverflow:
        document.documentElement.scrollWidth > document.documentElement.clientWidth,
      viewportWidth: document.documentElement.clientWidth,
      offenders: Array.from(document.querySelectorAll("body *"))
        .filter((element: Element): boolean => {
          if (element.closest('[role="region"][aria-label="Village styles"]') !== null) {
            return false;
          }
          const bounds: DOMRect = element.getBoundingClientRect();
          return bounds.right > document.documentElement.clientWidth + 1 || bounds.left < -1;
        })
        .map((element: Element): string => {
          const bounds: DOMRect = element.getBoundingClientRect();
          return `${element.tagName}.${element.className}:${element.textContent.trim()}:${getComputedStyle(element).display}:${bounds.left.toString()}-${bounds.right.toString()}`;
        }),
    }),
  );
  expect(overflow.documentHasOverflow, `viewport: ${overflow.viewportWidth.toString()}`).toBe(
    false,
  );
  expect(overflow.offenders).toEqual([]);
  await page.screenshot({ path: testInfo.outputPath("homepage.png"), fullPage: true });
  expect(errors).toEqual([]);
});

test("keeps keyboard focus and reduced motion usable", async ({ page }): Promise<void> => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");
  await page.keyboard.press("Tab");
  await expect(page.getByRole("link", { name: "Skip to content" })).toBeFocused();
  const behavior: string = await page.evaluate(
    (): string => getComputedStyle(document.documentElement).scrollBehavior,
  );
  expect(behavior).toBe("auto");
  await expect(page.locator("video")).toHaveCount(0);
});

test("both approved logo assets have real transparency", async ({ page }): Promise<void> => {
  await page.goto("/");
  await waitForImages(page);
  const alphaRanges: readonly (readonly number[])[] = await page
    .locator("img")
    .evaluateAll((images: Element[]): readonly (readonly number[])[] =>
      images
        .filter(
          (element: Element): element is HTMLImageElement =>
            element instanceof HTMLImageElement &&
            (element.src.endsWith("kithkyn-logo-transparent.png") ||
              element.src.endsWith("kithkyn-wordmark.png")),
        )
        .map((image: HTMLImageElement): readonly number[] => {
          const canvas: HTMLCanvasElement = document.createElement("canvas");
          canvas.width = image.naturalWidth;
          canvas.height = image.naturalHeight;
          const context: CanvasRenderingContext2D | null = canvas.getContext("2d");
          if (!context) return [];
          context.drawImage(image, 0, 0);
          const pixels: Uint8ClampedArray = context.getImageData(
            0,
            0,
            canvas.width,
            canvas.height,
          ).data;
          let minimum: number = 255;
          let maximum: number = 0;
          for (let index = 3; index < pixels.length; index += 4) {
            const value: number = pixels[index] ?? 0;
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
          }
          return [minimum, maximum];
        }),
    );
  expect(alphaRanges).toContainEqual([0, 255]);
  expect(alphaRanges.filter(([minimum, maximum]) => minimum === 0 && maximum === 255)).toHaveLength(
    3,
  );
});
