import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "./site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const layout = style({ position: "relative" });
export const copy = style([
  container,
  {
    minWidth: 0,
    position: "relative",
    zIndex: 1,
    minHeight: vars.size.backdropHero,
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    alignItems: "start",
    paddingBlock: vars.space.section,
    "@media": {
      [breakpoint.narrow]: {
        minHeight: "auto",
        paddingTop: vars.space.xxl,
        paddingBottom: vars.space.xxl,
      },
    },
  },
]);
globalStyle(`${copy} h1`, {
  fontSize: vars.fontSize.catalog,
  fontWeight: vars.weight.medium,
  maxWidth: vars.size.copy,
  marginBottom: vars.space.md,
  textWrap: "balance",
});
globalStyle(`${copy} p`, { maxWidth: vars.size.copy, color: vars.color.text });
export const wordmark = style({
  width: vars.size.backdropWordmark,
  height: "auto",
  marginTop: vars.space.lg,
  marginBottom: vars.space.xl,
  imageRendering: "pixelated",
  "@media": { [breakpoint.narrow]: { width: vars.size.compactLogo } },
});
export const actions = style({
  display: "flex",
  gap: vars.space.lg,
  flexWrap: "wrap",
  alignItems: "center",
  marginTop: vars.space.xl,
});
export const picture = style({
  position: "absolute",
  top: vars.space.none,
  left: 0,
  right: 0,
  bottom: 0,
  zIndex: 0,
  overflow: "hidden",
  background: vars.color.canvas,
  selectors: {
    "&::after": {
      content: '""',
      position: "absolute",
      inset: 0,
      background: `linear-gradient(180deg, color-mix(in srgb, ${vars.color.canvas} 85%, transparent) 0%, transparent 35%, transparent 55%, ${vars.color.canvas} 100%), linear-gradient(90deg, color-mix(in srgb, ${vars.color.canvas} 98%, transparent) 0%, color-mix(in srgb, ${vars.color.canvas} 92%, transparent) 42%, color-mix(in srgb, ${vars.color.canvas} 35%, transparent) 72%, color-mix(in srgb, ${vars.color.canvas} 12%, transparent) 100%)`,
      "@media": {
        [breakpoint.narrow]: {
          background: "none",
        },
      },
    },
  },
  "@media": {
    [breakpoint.narrow]: {
      position: "relative",
      inset: "auto",
      height: vars.size.mobilePhoto,
      marginInline: vars.space.gutter,
      borderRadius: vars.radius.small,
    },
  },
});
globalStyle(`${picture} img`, {
  width: "100%",
  height: "100%",
  objectFit: "cover",
  objectPosition: "center",
  "@media": { [breakpoint.narrow]: { objectPosition: "72% center" } },
});
globalStyle(`${picture} figcaption`, {
  position: "absolute",
  zIndex: 1,
  bottom: vars.space.lg,
  right: vars.space.gutter,
  color: vars.color.muted,
  fontSize: vars.fontSize.tiny,
  "@media": {
    [breakpoint.narrow]: {
      insetInline: vars.space.sm,
      bottom: vars.space.sm,
      width: "fit-content",
      padding: `${vars.space.xxs} ${vars.space.xs}`,
      borderRadius: vars.radius.small,
      background: vars.color.imageOverlay,
      color: vars.color.text,
    },
  },
});
globalStyle(`${picture} figcaption a:hover`, { textDecoration: "underline" });
