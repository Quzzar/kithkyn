import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const section = style({ background: vars.color.soft, paddingBlock: vars.space.section });
export const heading = style({
  display: "flex",
  justifyContent: "space-between",
  alignItems: "end",
  gap: vars.space.xl,
  marginBottom: vars.space.xxl,
  "@media": { [breakpoint.narrow]: { display: "block" } },
});
globalStyle(`${heading} h2`, { marginTop: vars.space.md });
export const intro = style({
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
  paddingBottom: vars.space.xs,
  "@media": { [breakpoint.narrow]: { marginTop: vars.space.lg } },
});
export const atlas = style({
  display: "grid",
  gridTemplateColumns: "minmax(0, 0.85fr) minmax(0, 1.4fr)",
  gap: vars.space.xxl,
  alignItems: "start",
  "@media": {
    [breakpoint.compact]: { gap: vars.space.lg },
    [breakpoint.narrow]: { gridTemplateColumns: "minmax(0, 1fr)" },
  },
});
export const selector = style({
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
  gap: vars.space.xs,
  "@media": {
    [breakpoint.narrow]: {
      maxHeight: vars.size.mobileAtlas,
      overflowY: "auto",
      padding: vars.space.xs,
      margin: `-${vars.space.xs}`,
    },
  },
});
export const tab = style({
  border: `${vars.size.border} solid ${vars.color.line}`,
  background: "transparent",
  color: vars.color.pine,
  borderRadius: vars.radius.small,
  padding: `${vars.space.md} ${vars.space.sm}`,
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  textAlign: "left",
  gap: vars.space.xs,
  fontSize: vars.fontSize.small,
  minHeight: vars.size.tile,
  transition: `background ${vars.motion.short}`,
  selectors: {
    "&:hover": { background: vars.color.mist },
    '&[data-state="active"]': {
      background: vars.color.pine,
      borderColor: vars.color.pine,
      color: vars.color.mist,
    },
  },
});
globalStyle(`${tab} svg`, { width: vars.size.icon, height: vars.size.icon });
export const detail = style({
  minWidth: 0,
  background: vars.color.white,
  borderRadius: vars.radius.panel,
  overflow: "hidden",
  boxShadow: vars.shadow.panel,
});
export const picture = style({ position: "relative", background: vars.color.sky });
globalStyle(`${picture} img`, {
  width: "100%",
  height: "auto",
  aspectRatio: "16 / 9",
  objectFit: "cover",
});
globalStyle(`${picture} figcaption`, {
  position: "absolute",
  bottom: vars.space.sm,
  left: vars.space.sm,
  right: vars.space.sm,
  width: "fit-content",
  padding: `${vars.space.xxs} ${vars.space.sm}`,
  fontSize: vars.fontSize.tiny,
  borderRadius: vars.radius.pill,
  background: vars.color.mist,
  color: vars.color.pine,
});
export const description = style({ padding: vars.space.xl });
globalStyle(`${description} h3`, {
  fontSize: vars.fontSize.subheading,
  marginBlock: `${vars.space.sm} ${vars.space.xs}`,
});
globalStyle(`${description} > p:last-of-type`, { color: vars.color.muted });
globalStyle(`${description} ul`, {
  listStyle: "none",
  display: "flex",
  flexWrap: "wrap",
  gap: vars.space.xs,
  padding: 0,
  margin: `${vars.space.lg} 0 0`,
});
globalStyle(`${description} li`, {
  background: vars.color.mist,
  padding: `${vars.space.xxs} ${vars.space.sm}`,
  fontSize: vars.fontSize.tiny,
  borderRadius: vars.radius.pill,
});
export const biome = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.xs,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
globalStyle(`${biome} svg`, { width: vars.size.icon, height: vars.size.icon });
export const fieldNote = style({
  minHeight: vars.size.mobileAtlas,
  background: vars.color.sky,
  padding: vars.space.xl,
  display: "flex",
  flexDirection: "column",
  justifyContent: "center",
  alignItems: "center",
  textAlign: "center",
  gap: vars.space.md,
});
globalStyle(`${fieldNote} img`, { width: vars.size.logoIcon, height: "auto" });
globalStyle(`${fieldNote} h3`, { fontSize: vars.fontSize.subheading });
globalStyle(`${fieldNote} p, ${fieldNote} span`, { fontSize: vars.fontSize.small });
export const note = style({
  fontSize: vars.fontSize.small,
  color: vars.color.muted,
  marginTop: vars.space.xl,
});
