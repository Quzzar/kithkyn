import { expect, test, type Page, type TestInfo } from "@playwright/test";

/** Wait for displayed assets before checking layout or saving a render. */
async function waitForImages(page: Page): Promise<void> {
  await page.waitForFunction((): boolean =>
    Array.from(document.images).every(
      (image: HTMLImageElement): boolean => image.complete && image.naturalWidth > 0,
    ),
  );
}

test("compares four identities and opens their homepage previews", async ({
  page,
}): Promise<void> => {
  await page.goto("/play");
  await expect(page).toHaveURL("/");
  await expect(page).toHaveTitle("Kithkyn | Four identity studies");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Four ways to feel");
  await expect(page.getByRole("img")).toHaveCount(4);
  await expect(page.locator('img[src*="diorama"]')).toHaveCount(0);
  for (const [id, name, headline] of [
    ["joinery", "Joinery", "A world with"],
    ["gather", "Gather", "A little life"],
    ["offcut", "Offcut", "Life finds a way"],
    ["neighbor", "Neighbor", "Good neighbors."],
  ] as const) {
    await page.goto("/");
    await page.getByRole("link", { name: new RegExp(`Preview \\d: ${name}`) }).click();
    await expect(page).toHaveURL(`/directions/${id}`);
    await expect(page.getByRole("heading", { level: 1 })).toContainText(headline);
    await expect(page.getByRole("heading", { level: 1 })).toBeInViewport();
    await page.getByRole("link", { name: "Explore the villages", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Where will they settle?" })).toBeInViewport();
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

test("returns to the comparison from previews and handles old links", async ({
  page,
}): Promise<void> => {
  await page.goto("/brand");
  await expect(page).toHaveURL("/");
  await waitForImages(page);
  await page.getByRole("link", { name: "Preview 4: Neighbor" }).click();
  await page.getByRole("link", { name: "Compare identities", exact: true }).first().click();
  await expect(page).toHaveURL("/");
  await page.goto("/directions/unknown");
  await expect(page).toHaveURL("/");
});

test("renders the comparison and every preview without errors or narrow-screen overflow", async ({
  page,
}, testInfo: TestInfo): Promise<void> => {
  const errors: string[] = [];
  page.on("pageerror", (error: Error): void => {
    errors.push(error.message);
  });
  const paths: readonly string[] = [
    "/",
    "/directions/joinery",
    "/directions/gather",
    "/directions/offcut",
    "/directions/neighbor",
  ];
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
      path: testInfo.outputPath(
        `${path === "/" ? "comparison" : (path.split("/").at(-1) ?? "preview")}.png`,
      ),
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
  await page.goto("/directions/offcut");
  await expect(page.locator("html")).toHaveCSS("scroll-behavior", "auto");
  const action = page.getByRole("link", { name: "Explore the villages", exact: true });
  await expect(action).toHaveCSS("transform", "none");
  await action.hover();
  await expect(action).toHaveCSS("transform", "none");
});
