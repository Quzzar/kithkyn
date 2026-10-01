import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { primary } from "../components/village-hero.css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style([container, { paddingBottom: vars.space.section }]);
export const intro = style({ paddingBlock: vars.space.xxl });
globalStyle(`${intro} h1`, { fontSize: vars.fontSize.title });
globalStyle(`${intro} p`, {
  fontSize: vars.fontSize.lead,
  color: vars.color.muted,
  marginTop: vars.space.lg,
});
export const kit = style([primary, { marginTop: vars.space.xl }]);
export const grid = style({
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: vars.space.xxl,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
export const asset = style({ minWidth: 0 });
export const stage = style({
  minHeight: vars.size.brandTile,
  padding: vars.space.xl,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: vars.color.raised,
  borderRadius: vars.radius.small,
});
globalStyle(`${stage} img`, {
  width: "100%",
  height: vars.size.brandPreview,
  objectFit: "contain",
});
globalStyle(`${asset} h2`, { marginTop: vars.space.lg, fontSize: vars.fontSize.subheading });
globalStyle(`${asset} p`, {
  marginTop: vars.space.xs,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
export const downloads = style({ display: "flex", gap: vars.space.xl, marginTop: vars.space.md });
globalStyle(`${downloads} a`, {
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.xs,
  minHeight: vars.size.button,
  fontSize: vars.fontSize.small,
  fontWeight: vars.weight.strong,
});
globalStyle(`${downloads} a:hover`, { textDecoration: "underline" });
globalStyle(`${downloads} svg`, { width: vars.size.icon, height: vars.size.icon });
export const sharing = style({
  display: "grid",
  gridTemplateColumns: "1fr 1.5fr",
  alignItems: "center",
  gap: vars.space.xxl,
  paddingTop: vars.space.section,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr", gap: vars.space.lg } },
});
globalStyle(`${sharing} h2`, { fontSize: vars.fontSize.heading });
globalStyle(`${sharing} p`, { marginTop: vars.space.lg, color: vars.color.muted });
globalStyle(`${sharing} img`, {
  border: `${vars.size.border} solid ${vars.color.line}`,
  borderRadius: vars.radius.small,
});
export const notes = style({
  paddingTop: vars.space.xxl,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
  maxWidth: vars.size.reading,
});
