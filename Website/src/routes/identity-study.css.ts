import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style([container, { paddingBottom: vars.space.xxl }]);
export const header = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  minHeight: vars.size.header,
  fontSize: vars.fontSize.small,
  color: vars.color.muted,
});
export const brand = style({
  fontSize: vars.fontSize.lead,
  fontWeight: vars.weight.strong,
  color: vars.color.cream,
});
export const intro = style({ paddingBlock: vars.space.xxl });
globalStyle(`${intro} h1`, {
  fontSize: vars.fontSize.title,
  fontWeight: vars.weight.strong,
  letterSpacing: vars.tracking.heading,
});
globalStyle(`${intro} p`, {
  marginTop: vars.space.lg,
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});
export const grid = style({
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: vars.space.xxl,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
export const study = style({ minWidth: 0 });
export const stage = style({
  padding: vars.space.xl,
  minHeight: vars.size.brandTile,
  background: vars.color.raised,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.lg,
  borderRadius: vars.radius.small,
});
globalStyle(`${stage} img`, { width: "100%", height: vars.size.studyLogo, objectFit: "contain" });
globalStyle(`${stage} p`, { color: vars.color.paperMuted, fontSize: vars.fontSize.small });
export const caption = style({
  display: "flex",
  justifyContent: "space-between",
  alignItems: "start",
  gap: vars.space.lg,
  paddingTop: vars.space.lg,
  "@media": { [breakpoint.compact]: { flexDirection: "column", gap: vars.space.md } },
});
globalStyle(`${caption} h2`, {
  fontSize: vars.fontSize.subheading,
  fontWeight: vars.weight.strong,
});
globalStyle(`${caption} p`, {
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
  marginTop: vars.space.xs,
  maxWidth: vars.size.copy,
});
globalStyle(`${caption} a`, {
  display: "flex",
  alignItems: "center",
  minHeight: vars.size.button,
  whiteSpace: "nowrap",
  gap: vars.space.xs,
  fontSize: vars.fontSize.small,
  fontWeight: vars.weight.strong,
});
globalStyle(`${caption} a:hover`, { textDecoration: "underline" });
globalStyle(`${caption} svg`, { width: vars.size.icon, height: vars.size.icon });
export const footer = style({
  paddingTop: vars.space.xxl,
  marginTop: vars.space.xxl,
  borderTop: `${vars.size.border} solid ${vars.color.line}`,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
