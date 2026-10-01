import { globalStyle, style, styleVariants } from "@vanilla-extract/css";
import { container } from "./site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const hero = style([
  container,
  {
    display: "grid",
    alignItems: "center",
    gap: vars.space.xxl,
    paddingTop: vars.space.xxl,
    paddingBottom: vars.space.section,
  },
]);
export const layout = styleVariants({
  joinery: { gridTemplateColumns: "1fr 1fr" },
  gather: { gridTemplateColumns: "1fr" },
  offcut: { gridTemplateColumns: "1fr" },
  neighbor: { gridTemplateColumns: "1fr 1fr" },
});
globalStyle(hero, {
  "@media": {
    [breakpoint.narrow]: {
      gridTemplateColumns: "1fr",
      gap: vars.space.xl,
      paddingTop: vars.space.xl,
    },
  },
});
export const copy = style({
  display: "flex",
  flexDirection: "column",
  gap: vars.space.lg,
  minWidth: 0,
});
export const copyLayout = styleVariants({
  joinery: {},
  gather: { textAlign: "center", alignItems: "center" },
  offcut: {
    display: "grid",
    gridTemplateColumns: "1.15fr 1fr",
    alignItems: "end",
    gap: vars.space.xxl,
    "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr", gap: vars.space.lg } },
  },
  neighbor: {},
});
globalStyle(`${copy} h1`, {
  fontSize: vars.fontSize.hero,
  fontWeight: vars.weight.medium,
  marginTop: vars.space.lg,
});
export const label = style({
  color: vars.color.oak,
  fontSize: vars.fontSize.small,
  fontWeight: vars.weight.medium,
});
export const actionArea = style({ minWidth: 0 });
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
globalStyle(`${copyLayout.gather} ${actions}`, { justifyContent: "center" });
export const primary = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  background: vars.color.cream,
  color: vars.color.paper,
  borderRadius: vars.radius.small,
  fontWeight: vars.weight.medium,
});
globalStyle(`${primary}:hover`, { background: vars.color.oak });
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
  background: vars.color.sky,
});
export const figureLayout = styleVariants({
  joinery: {},
  gather: { height: vars.size.widePhoto },
  offcut: { height: vars.size.widePhoto, borderRadius: vars.radius.small },
  neighbor: { order: -1 },
});
globalStyle(figure, {
  "@media": { [breakpoint.narrow]: { height: vars.size.mobilePhoto, order: 0 } },
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
  background: vars.color.overlayBottom,
  color: vars.color.photoInk,
  padding: `${vars.space.xxs} ${vars.space.sm}`,
  borderRadius: vars.radius.small,
  fontSize: vars.fontSize.tiny,
});
