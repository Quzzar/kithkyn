import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style({ background: vars.color.forest, color: vars.color.cream });
export const life = style({
  background: vars.color.forest,
  paddingBlock: vars.space.section,
  borderTop: `${vars.size.border} solid ${vars.color.line}`,
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
export const lifeLead = style({
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
  marginTop: vars.space.lg,
});
export const lifeRows = style({ display: "grid", gap: vars.space.lg });
export const lifeRow = style({
  paddingBottom: vars.space.lg,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${lifeRow} h3`, { fontSize: vars.fontSize.subheading, marginBottom: vars.space.xs });
globalStyle(`${lifeRow} p`, { color: vars.color.muted });
