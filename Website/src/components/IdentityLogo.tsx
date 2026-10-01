import type { ReactElement } from "react";
import type { Identity, IdentityAsset } from "../data/identities";

type IdentityLogoProps = {
  readonly identity: Identity;
  readonly className?: string;
  readonly isDecorative?: boolean;
  readonly isIconOnly?: boolean;
};

/** Transparent original raster studies stay intact while the chosen identity is still open. */
export function IdentityLogo({
  identity,
  className,
  isDecorative = false,
  isIconOnly = false,
}: IdentityLogoProps): ReactElement {
  const asset: IdentityAsset =
    isIconOnly && identity.icon
      ? identity.icon
      : { source: `/studies/${identity.id}.png`, width: identity.width, height: identity.height };
  return (
    <img
      src={asset.source}
      alt={
        isDecorative
          ? ""
          : `Kithkyn: ${identity.name} ${isIconOnly ? "selected icon" : "icon and wordmark"}`
      }
      width={asset.width}
      height={asset.height}
      className={className}
    />
  );
}
