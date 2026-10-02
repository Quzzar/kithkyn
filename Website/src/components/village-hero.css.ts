import { globalStyle, style } from "@vanilla-extract/css";
import { vars } from "../styles/theme.css";
export const lead = style({
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
  maxWidth: vars.size.copy,
});
export const primary = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  background: vars.color.accent,
  color: vars.color.canvas,
  borderRadius: vars.radius.small,
  fontWeight: vars.weight.medium,
});
globalStyle(`${primary}:hover`, { background: vars.color.text });
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
