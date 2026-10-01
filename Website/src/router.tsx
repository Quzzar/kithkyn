import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { BrandPage } from "./routes/BrandPage";
import { HomePage } from "./routes/HomePage";
import { RootLayout } from "./routes/RootLayout";
import { SectionRedirect } from "./routes/SectionRedirect";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "brand", element: <BrandPage /> },
      { path: "play", element: <Navigate to="/" replace /> },
      { path: "atlas", element: <SectionRedirect section="villages" /> },
      { path: "stories", element: <SectionRedirect section="life" /> },
      { path: "setup", element: <SectionRedirect section="get-started" /> },
    ],
  },
];

export const router = createBrowserRouter(routes);
