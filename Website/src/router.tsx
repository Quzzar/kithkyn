import { createBrowserRouter, type RouteObject } from "react-router-dom";

import { HomePage } from "./routes/HomePage";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <HomePage />,
  },
];

/** Public website router. */
export const router = createBrowserRouter(routes);
