import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const container = style({
  width: "100%",
  maxWidth: vars.size.content,
  marginInline: "auto",
  paddingInline: vars.space.gutter,
});
export const header = style([
  container,
  {
    minHeight: vars.size.header,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: vars.space.lg,
    "@media": {
      [breakpoint.narrow]: { flexWrap: "wrap", paddingBlock: vars.space.lg, gap: vars.space.md },
    },
  },
]);
export const brand = style({ display: "block", width: vars.size.brand, flexShrink: 0 });
globalStyle(`${brand} img`, { width: "100%", height: "auto" });
export const nav = style({
  display: "flex",
  alignItems: "center",
  gap: vars.size.headerGap,
  fontSize: vars.fontSize.small,
  fontWeight: vars.weight.medium,
  "@media": {
    [breakpoint.narrow]: { width: "100%", justifyContent: "space-between", gap: vars.space.md },
  },
});
export const navAction = style({
  background: vars.color.pine,
  color: vars.color.mist,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  borderRadius: vars.radius.pill,
});
export const button = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  fontWeight: vars.weight.strong,
  borderRadius: vars.radius.pill,
  border: `${vars.size.border} solid transparent`,
  transition: `background ${vars.motion.short} ${vars.motion.ease}`,
  selectors: { "&:hover": { background: vars.color.soft } },
});
export const primaryButton = style([
  button,
  {
    background: vars.color.honey,
    color: vars.color.pine,
    boxShadow: vars.shadow.action,
    selectors: { "&:hover": { background: vars.color.honey, boxShadow: "none" } },
  },
]);
export const outlinedButton = style([button, { borderColor: vars.color.line }]);
export const label = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.xs,
  fontSize: vars.fontSize.tiny,
  fontWeight: vars.weight.strong,
  letterSpacing: vars.tracking.label,
  textTransform: "uppercase",
});
export const sectionHeading = style({ fontSize: vars.fontSize.heading, maxWidth: vars.size.copy });
export const footer = style({
  background: vars.color.deep,
  color: vars.color.mist,
  paddingBlock: vars.space.xxxl,
});
export const footerInner = style([
  container,
  {
    display: "flex",
    gap: vars.space.xxl,
    flexWrap: "wrap",
    alignItems: "start",
    justifyContent: "space-between",
  },
]);
export const footerBrand = style({ display: "block", width: vars.size.footerBrand });
globalStyle(`${footerBrand} img`, { width: "100%", height: "auto" });
export const footerNav = style({
  display: "flex",
  gap: vars.space.xxl,
  flexWrap: "wrap",
  fontSize: vars.fontSize.small,
});
export const footerNote = style({
  fontSize: vars.fontSize.tiny,
  color: vars.color.lightText,
  marginTop: vars.space.lg,
  maxWidth: vars.size.copy,
});
export const skipLink = style({
  position: "fixed",
  top: vars.space.md,
  left: vars.space.md,
  zIndex: 20,
  padding: vars.space.md,
  background: vars.color.white,
  borderRadius: vars.radius.small,
  transform: "translateY(-200%)",
  selectors: { "&:focus": { transform: "translateY(0)" } },
});
globalStyle(`${button} svg`, { width: vars.size.icon, height: vars.size.icon });
globalStyle(`${label} svg`, { width: vars.size.icon, height: vars.size.icon });
