import { NuqsAdapter } from "nuqs/adapters/react-router/v7";
import { useEffect, type ReactElement } from "react";
import { Outlet, ScrollRestoration, useLocation } from "react-router-dom";

/** Restore section navigation and the village carousel's shareable selection. */
export function RootLayout(): ReactElement {
  const { pathname } = useLocation();
  useEffect((): void => {
    document.title =
      pathname === "/brand" ? "Kithkyn | Brand assets" : "Kithkyn | Bringing villages to life";
  }, [pathname]);
  return (
    <NuqsAdapter>
      <Outlet />
      <ScrollRestoration />
    </NuqsAdapter>
  );
}
