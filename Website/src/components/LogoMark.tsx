import type { ReactElement } from "react";

import * as styles from "./logo-mark.css";

type LogoMarkProps = {
  readonly variant?: "full" | "square";
  readonly className?: string;
  readonly alt?: string;
};

/** Approved Sturdy timber identity and its square village companion. */
export function LogoMark({ variant = "full", className, alt }: LogoMarkProps): ReactElement {
  const isSquare: boolean = variant === "square";
  return (
    <img
      className={`${styles.mark} ${className ?? ""}`}
      src={isSquare ? "/brand/kithkyn-square.png" : "/brand/kithkyn-logo-transparent.png"}
      alt={alt ?? (isSquare ? "KithKyn village emblem" : "KithKyn. Bringing villages to life.")}
      width={isSquare ? 1254 : 1536}
      height={isSquare ? 1254 : 1024}
      fetchPriority={isSquare ? "auto" : "high"}
    />
  );
}
