import { createBrowserRouter, type RouteObject } from "react-router-dom";
import { BrandPage } from "./routes/BrandPage";
import { HomePage } from "./routes/HomePage";
import { RootLayout } from "./routes/RootLayout";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "brand", element: <BrandPage /> },
    ],
  },
];

/** Public website router. */
export const router = createBrowserRouter(routes);
