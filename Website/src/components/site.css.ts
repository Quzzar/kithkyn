import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const container = style({
  width: "100%",
  maxWidth: vars.size.content,
  marginInline: "auto",
  paddingInline: vars.space.gutter,
});
export const label = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.xs,
  color: vars.color.muted,
  fontSize: vars.fontSize.tiny,
  fontWeight: vars.weight.strong,
  letterSpacing: vars.tracking.label,
  textTransform: "uppercase",
});
export const sectionHeading = style({ fontSize: vars.fontSize.heading, maxWidth: vars.size.copy });
export const primaryButton = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  border: `${vars.size.outline} solid currentColor`,
  fontWeight: vars.weight.strong,
  selectors: { "&:hover": { background: vars.color.text, color: vars.color.canvas } },
});
globalStyle(`${primaryButton} svg, ${label} svg`, {
  width: vars.size.icon,
  height: vars.size.icon,
});
export const header = style([
  container,
  {
    minHeight: vars.size.header,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: vars.space.lg,
    position: "relative",
    zIndex: 2,
  },
]);
export const iconBrand = style({
  display: "grid",
  placeItems: "center",
  width: vars.size.button,
  minHeight: vars.size.button,
  flexShrink: 0,
});
globalStyle(`${iconBrand} img`, {
  width: vars.size.headerIcon,
  height: vars.size.headerIcon,
  imageRendering: "pixelated",
});
export const headerNav = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.xl,
  fontSize: vars.fontSize.small,
  "@media": { [breakpoint.narrow]: { gap: vars.space.lg } },
});
globalStyle(`${headerNav} a`, {
  display: "flex",
  alignItems: "center",
  minHeight: vars.size.button,
  gap: vars.space.xs,
});
globalStyle(`${headerNav} a:hover`, { textDecoration: "underline" });
globalStyle(`${headerNav} svg`, { width: vars.size.icon, height: vars.size.icon });
export const sourceText = style({});
globalStyle(`${headerNav} a.${sourceText}`, {
  "@media": { [breakpoint.narrow]: { display: "none" } },
});
export const footer = style({
  background: vars.color.canvas,
  paddingBlock: vars.space.xxl,
  borderTop: `${vars.size.border} solid ${vars.color.line}`,
});
export const footerInner = style([
  container,
  {
    display: "flex",
    justifyContent: "space-between",
    flexWrap: "wrap",
    alignItems: "start",
    gap: vars.space.xl,
  },
]);
export const footerBrand = style({ display: "block", width: vars.size.compactLogo });
globalStyle(`${footerBrand} img`, { width: "100%", imageRendering: "pixelated" });
export const footerLinks = style({
  display: "flex",
  flexWrap: "wrap",
  gap: vars.space.lg,
  fontSize: vars.fontSize.small,
});
globalStyle(`${footerLinks} a:hover`, { textDecoration: "underline" });
export const footerNote = style({
  marginTop: vars.space.lg,
  color: vars.color.muted,
  fontSize: vars.fontSize.tiny,
  maxWidth: vars.size.copy,
});
export const skipLink = style({
  position: "fixed",
  top: vars.space.md,
  left: vars.space.md,
  zIndex: 10,
  padding: vars.space.md,
  background: vars.color.canvas,
  color: vars.color.text,
  transform: "translateY(-200%)",
  opacity: 0,
  pointerEvents: "none",
  selectors: { "&:focus": { transform: "translateY(0)", opacity: 1, pointerEvents: "auto" } },
});
