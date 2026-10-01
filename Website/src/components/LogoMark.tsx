import type { ReactElement } from "react";

type LogoVariant = "primary" | "reversed" | "monochrome" | "wordmark" | "emblem";
type LogoMarkProps = {
  readonly variant?: LogoVariant;
  readonly className?: string;
  readonly alt?: string;
};

/** Self-contained SVGs preserve the outlined lettering and transparent board silhouette. */
export function LogoMark({
  variant = "primary",
  className,
  alt = "KithKyn",
}: LogoMarkProps): ReactElement {
  const width: number = variant === "emblem" ? 160 : variant === "wordmark" ? 648 : 940;
  const height: number = variant === "emblem" ? 160 : variant === "wordmark" ? 136 : 260;
  return (
    <img
      src={`/brand/kithkyn-${variant}.svg`}
      alt={alt}
      className={className}
      width={width}
      height={height}
    />
  );
}
