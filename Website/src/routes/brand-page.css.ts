import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style([container, { paddingBlock: vars.space.xxl }]);
export const intro = style({ paddingBottom: vars.space.xxl });
globalStyle(`${intro} h1`, { fontSize: vars.fontSize.brand, marginBlock: vars.space.lg });
globalStyle(`${intro} > p:last-of-type`, {
  fontSize: vars.fontSize.lead,
  color: vars.color.muted,
  marginBottom: vars.space.xl,
});
export const logoGrid = style({
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
  gap: vars.space.md,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "minmax(0, 1fr)" } },
});
const logoPanel = style({
  display: "flex",
  flexDirection: "column",
  justifyContent: "center",
  alignItems: "center",
  padding: vars.space.xxl,
  minHeight: vars.size.mobileAtlas,
  borderRadius: vars.radius.panel,
  gap: vars.space.xl,
});
globalStyle(`${logoPanel} img`, { width: "100%", height: "auto", maxWidth: vars.size.copy });
globalStyle(`${logoPanel} figcaption`, { fontSize: vars.fontSize.small, textAlign: "center" });
export const primary = style([logoPanel, { background: vars.color.white }]);
export const reversed = style([logoPanel, { background: vars.color.pine, color: vars.color.mist }]);
export const mono = style([logoPanel, { background: vars.color.meadow }]);
export const emblem = style([logoPanel, { background: vars.color.honey }]);
globalStyle(`${emblem} img`, { width: vars.size.footerBrand });
export const wordmark = style([logoPanel, { background: vars.color.sky, gridColumn: "1 / -1" }]);
export const section = style({ paddingBlock: vars.space.xxl });
globalStyle(`${section} h2`, {
  fontSize: vars.fontSize.heading,
  marginBlock: `${vars.space.md} ${vars.space.xl}`,
});
export const swatches = style({
  display: "grid",
  gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
  gap: vars.space.md,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "repeat(2, minmax(0, 1fr))" } },
});
const swatch = style({
  height: vars.size.logoIcon,
  borderRadius: vars.radius.small,
  border: `${vars.size.border} solid ${vars.color.line}`,
  marginBottom: vars.space.sm,
});
export const pine = style([swatch, { background: vars.color.pine }]);
export const mist = style([swatch, { background: vars.color.mist }]);
export const meadow = style([swatch, { background: vars.color.meadow }]);
export const honey = style([swatch, { background: vars.color.honey }]);
export const sky = style([swatch, { background: vars.color.sky }]);
globalStyle(`${swatches} span`, {
  display: "block",
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
export const typeSection = style({
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
  paddingBlock: vars.space.xxl,
  gap: vars.space.xl,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "minmax(0, 1fr)" } },
});
globalStyle(`${typeSection} h2`, {
  fontSize: vars.fontSize.heading,
  marginBlock: `${vars.space.md} ${vars.space.lg}`,
});
export const typeSample = style({
  background: vars.color.pine,
  color: vars.color.mist,
  padding: vars.space.xxl,
  borderRadius: vars.radius.panel,
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.heading,
  lineHeight: vars.line.heading,
  fontWeight: vars.weight.display,
  letterSpacing: vars.tracking.heading,
});
globalStyle(`${typeSample} span`, {
  display: "block",
  fontFamily: vars.font.body,
  fontWeight: vars.weight.body,
  fontSize: vars.fontSize.small,
  letterSpacing: "normal",
  marginTop: vars.space.xl,
});
export const socialImage = style({
  width: "100%",
  height: "auto",
  borderRadius: vars.radius.panel,
});
export const usage = style({
  paddingBlock: vars.space.xxl,
  maxWidth: vars.size.reading,
  display: "flex",
  flexDirection: "column",
  gap: vars.space.lg,
});
globalStyle(`${usage} h2`, { fontSize: vars.fontSize.subheading });
globalStyle(`${usage} p`, { color: vars.color.muted });
