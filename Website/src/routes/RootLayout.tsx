import { NuqsAdapter } from "nuqs/adapters/react-router/v7";
import { useEffect, type ReactElement } from "react";
import { Outlet, ScrollRestoration, useLocation } from "react-router-dom";

/** Restore prototype navigation and the atlas's shareable selection. */
export function RootLayout(): ReactElement {
  const { pathname } = useLocation();
  useEffect((): void => {
    const titles: Readonly<Record<string, string>> = {
      "/": "Compare directions",
      "/play": "A · Title screen",
      "/atlas": "B · Village atlas",
      "/stories": "C · Village stories",
      "/setup": "Setup reference",
    };
    document.title = `KithKyn | ${titles[pathname] ?? "Design study"}`;
  }, [pathname]);
  return (
    <NuqsAdapter>
      <Outlet />
      <ScrollRestoration />
    </NuqsAdapter>
  );
}
