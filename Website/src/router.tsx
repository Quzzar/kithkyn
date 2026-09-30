import { createBrowserRouter, type RouteObject } from "react-router-dom";
import { AtlasPage } from "./routes/AtlasPage";
import { HomePage } from "./routes/HomePage";
import { RootLayout } from "./routes/RootLayout";
import { SetupPage } from "./routes/SetupPage";
import { StoriesPage } from "./routes/StoriesPage";
import { TitleScreenPage } from "./routes/TitleScreenPage";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "play", element: <TitleScreenPage /> },
      { path: "atlas", element: <AtlasPage /> },
      { path: "stories", element: <StoriesPage /> },
      { path: "setup", element: <SetupPage /> },
    ],
  },
];

/** Public website router. */
export const router = createBrowserRouter(routes);
