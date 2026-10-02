import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { HomePage } from "./routes/HomePage";
import { RootLayout } from "./routes/RootLayout";
import { SectionRedirect } from "./routes/SectionRedirect";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "brand/*", element: <SectionRedirect /> },
      { path: "studies/:round", element: <SectionRedirect /> },
      { path: "directions/:identityId", element: <SectionRedirect /> },
      { path: "play", element: <SectionRedirect /> },
      { path: "atlas", element: <SectionRedirect section="villages" /> },
      { path: "stories", element: <SectionRedirect /> },
      { path: "setup", element: <SectionRedirect section="get-started" /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
];

export const router = createBrowserRouter(routes);
