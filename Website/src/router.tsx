import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { IdentityStudyPage } from "./routes/IdentityStudyPage";
import { HomePage } from "./routes/HomePage";
import { RootLayout } from "./routes/RootLayout";
import { SectionRedirect } from "./routes/SectionRedirect";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <RootLayout />,
    children: [
      { index: true, element: <IdentityStudyPage /> },
      { path: "studies/first", element: <IdentityStudyPage round="first" /> },
      { path: "studies/timber", element: <IdentityStudyPage round="timber" /> },
      { path: "directions/:identityId", element: <HomePage /> },
      { path: "brand", element: <Navigate to="/" replace /> },
      { path: "play", element: <Navigate to="/" replace /> },
      { path: "atlas", element: <SectionRedirect section="villages" /> },
      { path: "stories", element: <SectionRedirect section="life" /> },
      { path: "setup", element: <SectionRedirect section="get-started" /> },
    ],
  },
];

export const router = createBrowserRouter(routes);
