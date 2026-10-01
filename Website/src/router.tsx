import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { BrandPage } from "./routes/BrandPage";
import { HomePage } from "./routes/HomePage";
import { RootLayout } from "./routes/RootLayout";
import { SectionRedirect } from "./routes/SectionRedirect";
import { TimberDirectionsPage } from "./routes/TimberDirectionsPage";
import { IdentityPreviewPage } from "./routes/IdentityPreviewPage";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "brand", element: <BrandPage /> },
      { path: "brand/directions", element: <TimberDirectionsPage /> },
      { path: "brand/directions/:directionId", element: <IdentityPreviewPage /> },
      { path: "studies/:round", element: <SectionRedirect /> },
      { path: "directions/:identityId", element: <SectionRedirect /> },
      { path: "play", element: <SectionRedirect /> },
      { path: "atlas", element: <SectionRedirect section="villages" /> },
      { path: "stories", element: <SectionRedirect section="life" /> },
      { path: "setup", element: <SectionRedirect section="get-started" /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
];

export const router = createBrowserRouter(routes);
