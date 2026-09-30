import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style({
  background: vars.color.white,
  minHeight: "100vh",
  padding: `${vars.space.xl} ${vars.space.gutter}`,
  color: vars.color.ink,
});
export const header = style({
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: vars.space.md,
  maxWidth: vars.size.content,
  marginInline: "auto",
  flexWrap: "wrap",
});
export const brand = style({ fontSize: vars.fontSize.subheading, fontWeight: vars.weight.strong });
export const note = style({
  color: vars.color.muted,
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
});
export const intro = style({
  paddingBlock: vars.space.xxl,
  maxWidth: vars.size.content,
  marginInline: "auto",
});
export const label = style({
  marginBottom: vars.space.md,
  color: vars.color.muted,
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.small,
});
globalStyle(`${intro} h1`, {
  fontSize: vars.fontSize.menuTitle,
  marginBottom: vars.space.lg,
  fontWeight: vars.weight.medium,
});
globalStyle(`${intro} > p:last-child`, { color: vars.color.muted, maxWidth: vars.size.reading });
export const options = style({
  display: "grid",
  gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
  gap: vars.space.lg,
  maxWidth: vars.size.content,
  marginInline: "auto",
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
export const option = style({
  minWidth: 0,
  border: `${vars.size.border} solid ${vars.color.line}`,
  selectors: { "&:hover": { borderColor: vars.color.muted } },
});
globalStyle(`${option} img`, {
  width: "100%",
  height: "auto",
  aspectRatio: "1280 / 900",
  objectFit: "cover",
  objectPosition: "top",
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
export const optionCopy = style({ padding: vars.space.lg });
globalStyle(`${optionCopy} h2`, {
  fontSize: vars.fontSize.subheading,
  marginBottom: vars.space.sm,
  fontWeight: vars.weight.medium,
});
globalStyle(`${optionCopy} p`, { color: vars.color.muted, minHeight: vars.size.button });
export const open = style({
  display: "block",
  marginTop: vars.space.lg,
  fontWeight: vars.weight.strong,
  fontSize: vars.fontSize.small,
});
export const footer = style({
  display: "flex",
  justifyContent: "space-between",
  flexWrap: "wrap",
  gap: vars.space.lg,
  maxWidth: vars.size.content,
  marginInline: "auto",
  marginTop: vars.space.xxl,
  paddingBlock: vars.space.md,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
