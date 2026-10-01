import type { ReactElement } from "react";
import { Navigate, useLocation } from "react-router-dom";

type SectionRedirectProps = { readonly section?: "villages" | "life" | "get-started" };

/** Existing preview links keep their selected village when they land on the finished site. */
export function SectionRedirect({ section }: SectionRedirectProps): ReactElement {
  const { search, hash } = useLocation();
  return <Navigate to={{ pathname: "/", search, hash: section ? `#${section}` : hash }} replace />;
}
