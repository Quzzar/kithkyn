import { globalStyle, style } from "@vanilla-extract/css";
import { vars } from "../styles/theme.css";

export const container = style({
  width: "100%",
  maxWidth: vars.size.content,
  marginInline: "auto",
  paddingInline: vars.space.gutter,
});
export const primaryButton = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  background: vars.color.soft,
  color: vars.color.pine,
  fontWeight: vars.weight.strong,
  border: `${vars.size.border} solid ${vars.color.line}`,
  selectors: { "&:hover": { textDecoration: "underline" } },
});
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
globalStyle(`${primaryButton} svg, ${label} svg`, {
  width: vars.size.icon,
  height: vars.size.icon,
});
