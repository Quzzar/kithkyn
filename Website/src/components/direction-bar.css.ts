import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const bar = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: vars.space.md,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  minHeight: vars.size.review,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
  background: vars.color.white,
  color: vars.color.ink,
  fontSize: vars.fontSize.tiny,
  "@media": {
    [breakpoint.narrow]: { flexWrap: "wrap", gap: vars.space.sm, paddingInline: vars.space.md },
  },
});
export const compare = style({ fontWeight: vars.weight.strong });
export const nav = style({
  display: "flex",
  gap: vars.space.xs,
  flexWrap: "wrap",
  "@media": { [breakpoint.narrow]: { order: 3, width: "100%", justifyContent: "space-between" } },
});
globalStyle(`${nav} a`, { padding: `${vars.space.xs} ${vars.space.sm}` });
globalStyle(`${nav} a[aria-current='page']`, {
  background: vars.color.soft,
  fontWeight: vars.weight.strong,
});
export const note = style({
  color: vars.color.muted,
  "@media": { [breakpoint.compact]: { display: "none" } },
});
export const skip = style({
  position: "fixed",
  top: vars.space.md,
  left: vars.space.md,
  zIndex: 10,
  background: vars.color.white,
  padding: vars.space.md,
  transform: "translateY(-200%)",
  selectors: { "&:focus": { transform: "translateY(0)" } },
});
