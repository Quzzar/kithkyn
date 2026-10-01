import type { ReactElement } from "react";
import type { Identity } from "../data/identities";

type IdentityLogoProps = {
  readonly identity: Identity;
  readonly className?: string;
  readonly isDecorative?: boolean;
};

/** Transparent original raster studies stay intact while the chosen identity is still open. */
export function IdentityLogo({
  identity,
  className,
  isDecorative = false,
}: IdentityLogoProps): ReactElement {
  return (
    <img
      src={`/studies/${identity.id}.png`}
      alt={isDecorative ? "" : `Kithkyn: ${identity.name} icon and wordmark`}
      width={identity.width}
      height={identity.height}
      className={className}
    />
  );
}
