import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style([container, { paddingBlock: vars.space.xxl }]);
export const intro = style({ paddingBottom: vars.space.xxl });
export const introLinks = style({ display: "flex", flexWrap: "wrap", gap: vars.space.lg });
globalStyle(`${intro} h1`, { fontSize: vars.fontSize.title, marginTop: vars.space.lg });
globalStyle(`${intro} p`, {
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
  marginTop: vars.space.md,
});
export const back = style({
  fontSize: vars.fontSize.small,
  textDecoration: "underline",
  textUnderlineOffset: vars.space.xxs,
});
export const grid = style({
  display: "grid",
  gridTemplateColumns: "repeat(3, 1fr)",
  gap: vars.space.xl,
  "@media": { [breakpoint.compact]: { gridTemplateColumns: "1fr" } },
});
export const previousGrid = style([
  grid,
  {
    gridTemplateColumns: "1fr 1fr",
    "@media": { [breakpoint.compact]: { gridTemplateColumns: "1fr" } },
  },
]);
export const direction = style({
  minWidth: 0,
  background: vars.color.canvas,
  color: vars.color.text,
  border: `${vars.size.border} solid ${vars.color.line}`,
  borderRadius: vars.radius.small,
  overflow: "hidden",
});
export const cardIntro = style({
  padding: vars.space.lg,
  minHeight: vars.size.studyIntro,
  "@media": { [breakpoint.compact]: { minHeight: vars.space.none } },
});
globalStyle(`${cardIntro} h2`, { fontSize: vars.fontSize.subheading });
globalStyle(`${cardIntro} p`, {
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
  marginTop: vars.space.xs,
});
export const assets = style({
  display: "grid",
  gridTemplateColumns: "1fr",
  alignItems: "center",
  justifyItems: "center",
  gap: vars.space.md,
  padding: vars.space.md,
  height: vars.size.studyArtwork,
  background: vars.color.raised,
  "@media": {
    [breakpoint.compact]: {
      gridTemplateColumns: "1fr auto",
      justifyItems: "start",
      height: "auto",
    },
  },
});
export const previousAssets = style([
  assets,
  { gridTemplateColumns: "1fr auto", justifyItems: "start" },
]);
export const wordmark = style({
  width: vars.size.studyWordmark,
  height: "auto",
  maxHeight: vars.size.studyArtwork,
  objectFit: "contain",
  imageRendering: "pixelated",
  "@media": { [breakpoint.narrow]: { width: vars.size.studyWordmarkMobile } },
});
export const icon = style({
  width: vars.size.studyIcon,
  height: vars.size.studyIcon,
  objectFit: "contain",
  imageRendering: "pixelated",
});
export const preview = style({
  display: "block",
  borderBlock: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${preview} img`, { width: "100%", height: "auto" });
export const links = style({
  display: "flex",
  alignItems: "center",
  flexWrap: "wrap",
  gap: vars.space.lg,
  padding: vars.space.lg,
});
globalStyle(`${links} a`, {
  minHeight: vars.size.button,
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.xs,
  fontSize: vars.fontSize.small,
});
globalStyle(`${links} a:hover`, { textDecoration: "underline" });
globalStyle(`${links} svg`, { width: vars.size.icon, height: vars.size.icon });
export const open = style({ fontWeight: vars.weight.strong, marginRight: "auto" });
export const toolbar = style({
  background: vars.color.surface,
  color: vars.color.text,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
export const toolbarInner = style([
  container,
  {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: vars.space.md,
    minHeight: vars.size.button,
    fontSize: vars.fontSize.small,
    "@media": { [breakpoint.narrow]: { fontSize: vars.fontSize.tiny, gap: vars.space.xs } },
  },
]);
globalStyle(`${toolbarInner} a`, {
  minHeight: vars.size.button,
  display: "inline-flex",
  alignItems: "center",
  textDecoration: "underline",
  textUnderlineOffset: vars.space.xxs,
});
export const directionLabel = style({
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.xs,
  fontWeight: vars.weight.strong,
});
globalStyle(`${directionLabel} img`, {
  width: vars.space.xl,
  height: vars.space.xl,
  imageRendering: "pixelated",
});
