import { StrictMode, type ReactElement } from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";

import { router } from "./router";
import "./styles/global.css";

const rootElement: HTMLElement | null = document.getElementById("root");

if (rootElement === null) {
  throw new Error("Kithkyn website root element was not found.");
}

const application: ReactElement = (
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>
);

createRoot(rootElement).render(application);
