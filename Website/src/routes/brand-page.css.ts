import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";
import { container } from "../components/site.css";

export const page = style([
  container,
  { paddingTop: vars.space.xxl, paddingBottom: vars.space.section },
]);
export const intro = style({ maxWidth: vars.size.reading, marginBottom: vars.space.xxl });
globalStyle(`${intro} h1`, { fontSize: vars.fontSize.title, marginBlock: vars.space.lg });
globalStyle(`${intro} > p:last-of-type`, {
  marginBottom: vars.space.xl,
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});
export const logoGrid = style({
  display: "grid",
  gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
  gap: vars.space.lg,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
const tile = style({
  minHeight: vars.size.brandTile,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.xl,
  padding: vars.space.lg,
  border: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${tile} img`, { width: "100%", height: "auto" });
globalStyle(`${tile} figcaption`, { fontSize: vars.fontSize.small });
export const lightTile = style([tile, { background: vars.color.paper, color: vars.color.ink }]);
export const darkTile = style([tile, { background: vars.color.deep, color: vars.color.cream }]);
export const emblemTile = style([tile, { background: vars.color.paper, color: vars.color.ink }]);
globalStyle(`${emblemTile} img`, { width: vars.size.brandEmblem });
export const iconTile = style([tile, { background: vars.color.deep }]);
globalStyle(`${iconTile} img`, { width: vars.size.brandIcon });
export const section = style({ marginTop: vars.space.section });
globalStyle(`${section} h2`, { fontSize: vars.fontSize.heading, marginBlock: vars.space.md });
export const swatches = style({
  display: "grid",
  gridTemplateColumns: "repeat(6, minmax(0, 1fr))",
  gap: vars.space.md,
  marginTop: vars.space.xl,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "repeat(2, minmax(0, 1fr))" } },
});
globalStyle(`${swatches} > div > div`, {
  height: vars.size.swatch,
  border: `${vars.size.border} solid ${vars.color.line}`,
  marginBottom: vars.space.sm,
});
globalStyle(`${swatches} strong, ${swatches} span`, {
  display: "block",
  fontSize: vars.fontSize.small,
});
globalStyle(`${swatches} span`, { color: vars.color.muted });
export const forest = style({ background: vars.color.forest });
export const oak = style({ background: vars.color.oak });
export const paint = style({ background: vars.color.cream });
export const timber = style({ background: vars.color.timber });
export const leaf = style({ background: vars.color.leaf });
export const paper = style({ background: vars.color.paper });
export const typeSection = style({
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: vars.space.xxl,
  marginTop: vars.space.section,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
globalStyle(`${typeSection} h2`, { fontSize: vars.fontSize.heading, marginBlock: vars.space.md });
globalStyle(`${typeSection} p`, { color: vars.color.muted });
export const typeSample = style({
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.heading,
  lineHeight: vars.line.heading,
});
globalStyle(`${typeSample} span`, {
  display: "block",
  fontFamily: vars.font.body,
  fontSize: vars.fontSize.body,
  marginTop: vars.space.lg,
  color: vars.color.muted,
});
export const social = style({
  width: "100%",
  maxWidth: vars.size.reading,
  marginTop: vars.space.xl,
  border: `${vars.size.border} solid ${vars.color.line}`,
});
export const usage = style({
  marginTop: vars.space.section,
  maxWidth: vars.size.reading,
  display: "grid",
  gap: vars.space.lg,
  color: vars.color.muted,
});
globalStyle(`${usage} h2`, { fontSize: vars.fontSize.heading, color: vars.color.cream });
