import { NuqsAdapter } from "nuqs/adapters/react-router/v7";
import { useEffect, type ReactElement } from "react";
import { Outlet, ScrollRestoration, useLocation } from "react-router-dom";

/** Restore section navigation and the village carousel's shareable selection. */
export function RootLayout(): ReactElement {
  const { pathname } = useLocation();
  useEffect((): void => {
    document.title = pathname.startsWith("/directions/")
      ? "Kithkyn | Bringing villages to life"
      : pathname === "/studies/first"
        ? "Kithkyn | First identity studies"
        : pathname === "/studies/timber"
          ? "Kithkyn | Pixel timber studies"
          : "Kithkyn | Timber lettering studies";
  }, [pathname]);
  return (
    <NuqsAdapter>
      <Outlet />
      <ScrollRestoration />
    </NuqsAdapter>
  );
}
