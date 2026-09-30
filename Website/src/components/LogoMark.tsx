import type { ReactElement } from "react";

type LogoMarkProps = {
  readonly variant?: "primary" | "reversed" | "emblem" | "monochrome" | "wordmark";
  readonly className?: string;
  readonly alt?: string;
};

/** The hearth identity, with production variants demonstrated on the brand-kit page. */
export function LogoMark({ variant = "primary", className, alt }: LogoMarkProps): ReactElement {
  const isSquare: boolean = variant === "emblem";
  return (
    <img
      className={className}
      src={`/brand/kithkyn-${variant}.webp`}
      alt={alt ?? (isSquare ? "KithKyn hearth emblem" : "KithKyn")}
      width={isSquare ? 512 : 1000}
      height={isSquare ? 512 : 280}
    />
  );
}
