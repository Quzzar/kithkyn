import { NuqsAdapter } from "nuqs/adapters/react-router/v7";
import { useEffect, type ReactElement } from "react";
import { Outlet, ScrollRestoration, useLocation } from "react-router-dom";

/** Restore page navigation without losing the atlas's shareable URL state. */
export function RootLayout(): ReactElement {
  const { pathname } = useLocation();
  useEffect((): void => {
    document.title =
      pathname === "/brand" ? "KithKyn | Brand kit" : "KithKyn | Your world. Their story.";
  }, [pathname]);
  return (
    <NuqsAdapter>
      <Outlet />
      <ScrollRestoration />
    </NuqsAdapter>
  );
}
