import { NuqsAdapter } from "nuqs/adapters/react-router/v7";
import type { ReactElement } from "react";
import { Outlet, ScrollRestoration } from "react-router-dom";

/** Restore section navigation and the village carousel's shareable selection. */
export function RootLayout(): ReactElement {
  return (
    <NuqsAdapter>
      <Outlet />
      <ScrollRestoration />
    </NuqsAdapter>
  );
}
