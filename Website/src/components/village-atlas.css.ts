import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const section = style({ background: vars.color.raised, paddingBlock: vars.space.section });
export const heading = style({
  display: "flex",
  alignItems: "end",
  justifyContent: "space-between",
  flexWrap: "wrap",
  gap: vars.space.lg,
  marginBottom: vars.space.xl,
});
globalStyle(`${heading} h2`, { marginTop: vars.space.md });
export const controls = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.sm,
  fontSize: vars.fontSize.small,
  color: vars.color.muted,
});
export const arrow = style({
  display: "grid",
  placeItems: "center",
  width: vars.size.button,
  height: vars.size.button,
  border: `${vars.size.border} solid ${vars.color.line}`,
  background: vars.color.forest,
  color: vars.color.cream,
  selectors: {
    "&:hover:not(:disabled)": { background: vars.color.deep },
    "&:disabled": { opacity: 0.35 },
  },
});
globalStyle(`${arrow} svg`, { width: vars.size.icon, height: vars.size.icon });
export const selector = style({
  position: "relative",
  display: "flex",
  gap: vars.space.xs,
  overflowX: "auto",
  padding: `${vars.space.xs} ${vars.space.xxs} ${vars.space.lg}`,
  marginBottom: vars.space.lg,
  scrollbarColor: `${vars.color.line} ${vars.color.raised}`,
});
export const tab = style({
  flexShrink: 0,
  padding: `${vars.space.sm} ${vars.space.md}`,
  color: vars.color.muted,
  background: "transparent",
  border: `${vars.size.border} solid transparent`,
  borderBottom: `${vars.size.outline} solid transparent`,
  fontSize: vars.fontSize.small,
  whiteSpace: "nowrap",
  selectors: {
    "&[data-state='active']": {
      color: vars.color.cream,
      borderBottomColor: vars.color.oak,
      fontWeight: vars.weight.strong,
    },
    "&:hover": { background: vars.color.forest },
  },
});
export const detail = style({
  display: "grid",
  gridTemplateColumns: "1.35fr 1fr",
  border: `${vars.size.border} solid ${vars.color.line}`,
  borderRadius: vars.radius.small,
  overflow: "hidden",
  minWidth: 0,
  selectors: { "&[hidden]": { display: "none" } },
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
export const picture = style({ position: "relative", background: vars.color.sky, minWidth: 0 });
globalStyle(`${picture} img`, {
  width: "100%",
  height: vars.size.photo,
  objectFit: "cover",
  "@media": { [breakpoint.narrow]: { height: vars.size.mobilePhoto } },
});
globalStyle(`${picture} figcaption`, {
  position: "absolute",
  insetInline: 0,
  bottom: 0,
  padding: `${vars.space.sm} ${vars.space.md}`,
  background: vars.color.overlayBottom,
  color: vars.color.photoInk,
  fontSize: vars.fontSize.tiny,
});
export const description = style({
  display: "flex",
  flexDirection: "column",
  justifyContent: "center",
  gap: vars.space.lg,
  padding: vars.space.xxl,
  background: vars.color.paper,
  color: vars.color.ink,
  minWidth: 0,
  "@media": { [breakpoint.narrow]: { padding: vars.space.lg } },
});
export const biome = style({
  display: "flex",
  alignItems: "start",
  gap: vars.space.xs,
  color: vars.color.paperMuted,
  fontSize: vars.fontSize.small,
});
globalStyle(`${biome} svg`, { width: vars.size.icon, height: vars.size.icon });
globalStyle(`${description} h3`, { fontSize: vars.fontSize.catalog });
globalStyle(`${description} ul`, {
  display: "flex",
  flexWrap: "wrap",
  gap: vars.space.sm,
  margin: 0,
  padding: 0,
  listStyle: "none",
});
globalStyle(`${description} li`, {
  fontSize: vars.fontSize.tiny,
  padding: `${vars.space.xxs} ${vars.space.xs}`,
  border: `${vars.size.border} solid ${vars.color.paperLine}`,
});
export const fieldNotes = style({
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  textAlign: "center",
  gap: vars.space.lg,
  padding: vars.space.xxl,
  minHeight: vars.size.photo,
  background: vars.color.forest,
  color: vars.color.leaf,
  "@media": { [breakpoint.narrow]: { minHeight: vars.size.mobilePhoto } },
});
globalStyle(`${fieldNotes} > svg`, { width: vars.size.emblem, height: vars.size.emblem });
globalStyle(`${fieldNotes} > span`, {
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.subheading,
});
export const note = style({
  marginTop: vars.space.xl,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
