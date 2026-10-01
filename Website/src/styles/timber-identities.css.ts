import { assignVars, styleVariants } from "@vanilla-extract/css";
import { TIMBER_IDENTITIES, type TimberIdentity } from "../data/timber-identities";
import { vars } from "./theme.css";

/** Scoped palettes make each full preview internally consistent without changing the chosen site. */
export const identityTheme = styleVariants(TIMBER_IDENTITIES, (identity: TimberIdentity) => ({
  vars: assignVars(vars.color, {
    canvas: identity.palette.canvas,
    surface: identity.palette.surface,
    raised: identity.palette.raised,
    text: identity.palette.ink,
    muted: identity.palette.muted,
    line: `${identity.palette.ink}28`,
    accent: identity.palette.oak,
    focus: identity.palette.oak,
    imageOverlay: `${identity.palette.canvas}e6`,
  }),
}));
