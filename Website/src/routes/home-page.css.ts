import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style({ background: vars.color.canvas, color: vars.color.text });
export const life = style({
  background: vars.color.canvas,
  paddingBlock: vars.space.section,
});
export const lifeInner = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: vars.space.xxxl,
    "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr", gap: vars.space.xxl } },
  },
]);
globalStyle(`${lifeInner} h2`, { marginTop: vars.space.md });
export const lifeRows = style({ display: "grid", gap: vars.space.lg });
export const lifeRow = style({
  paddingBottom: vars.space.lg,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${lifeRow} h3`, { fontSize: vars.fontSize.subheading, marginBottom: vars.space.xs });
globalStyle(`${lifeRow} p`, { color: vars.color.muted });
