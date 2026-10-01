import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "./site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const hero = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    alignItems: "center",
    gap: vars.space.xxl,
    paddingTop: vars.space.xxl,
    paddingBottom: vars.space.section,
    "@media": {
      [breakpoint.narrow]: {
        gridTemplateColumns: "1fr",
        gap: vars.space.xl,
        paddingTop: vars.space.xl,
      },
    },
  },
]);
export const copy = style({
  display: "flex",
  flexDirection: "column",
  gap: vars.space.lg,
  minWidth: 0,
});
globalStyle(`${copy} h1`, {
  fontSize: vars.fontSize.hero,
  fontWeight: vars.weight.medium,
  marginTop: vars.space.lg,
});
export const label = style({
  color: vars.color.accent,
  fontSize: vars.fontSize.small,
  fontWeight: vars.weight.medium,
});
export const lead = style({
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
  maxWidth: vars.size.copy,
});
export const actions = style({
  display: "flex",
  gap: vars.space.lg,
  alignItems: "center",
  flexWrap: "wrap",
  marginTop: vars.space.xl,
});
export const primary = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  background: vars.color.text,
  color: vars.color.canvas,
  borderRadius: vars.radius.small,
  fontWeight: vars.weight.medium,
});
globalStyle(`${primary}:hover`, { background: vars.color.accent });
globalStyle(`${primary} svg`, { width: vars.size.icon, height: vars.size.icon });
export const secondary = style({
  display: "inline-flex",
  minHeight: vars.size.button,
  alignItems: "center",
  fontSize: vars.fontSize.small,
  textDecoration: "underline",
  textUnderlineOffset: vars.space.xxs,
});
export const platform = style({
  marginTop: vars.space.lg,
  color: vars.color.muted,
  fontSize: vars.fontSize.tiny,
});
export const figure = style({
  position: "relative",
  height: vars.size.heroPhoto,
  minWidth: 0,
  overflow: "hidden",
  borderRadius: vars.radius.scene,
  background: vars.color.surface,
  "@media": { [breakpoint.narrow]: { height: vars.size.mobilePhoto } },
});
globalStyle(`${figure} img`, {
  width: "100%",
  height: "100%",
  objectFit: "cover",
  objectPosition: "65% center",
});
globalStyle(`${figure} figcaption`, {
  position: "absolute",
  bottom: vars.space.md,
  left: vars.space.md,
  right: vars.space.md,
  width: "fit-content",
  background: vars.color.imageOverlay,
  color: vars.color.text,
  padding: `${vars.space.xxs} ${vars.space.sm}`,
  borderRadius: vars.radius.small,
  fontSize: vars.fontSize.tiny,
});
