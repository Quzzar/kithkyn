import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const page = style({ minHeight: "100vh", background: vars.color.white });
export const container = style({
  maxWidth: vars.size.content,
  marginInline: "auto",
  paddingInline: vars.space.gutter,
});
export const masthead = style([
  container,
  {
    paddingBlock: vars.space.lg,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: vars.space.md,
    borderBottom: `${vars.size.border} solid ${vars.color.line}`,
    "@media": { [breakpoint.narrow]: { flexWrap: "wrap" } },
  },
]);
export const brand = style({
  fontSize: vars.fontSize.subheading,
  fontWeight: vars.weight.strong,
  letterSpacing: vars.tracking.heading,
});
export const links = style({
  display: "flex",
  gap: vars.space.lg,
  fontSize: vars.fontSize.small,
  flexWrap: "wrap",
});
export const annotation = style({
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
  color: vars.color.muted,
});
export const button = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  padding: `${vars.space.sm} ${vars.space.lg}`,
  minHeight: vars.size.button,
  border: `${vars.size.border} solid currentColor`,
  fontWeight: vars.weight.strong,
  selectors: { "&:hover": { textDecoration: "underline" } },
});
export const footer = style([
  container,
  {
    display: "flex",
    flexWrap: "wrap",
    justifyContent: "space-between",
    gap: vars.space.lg,
    paddingBlock: vars.space.xl,
    borderTop: `${vars.size.border} solid ${vars.color.line}`,
    fontSize: vars.fontSize.small,
  },
]);

export const game = style({
  background: vars.color.deep,
  color: vars.color.white,
  minHeight: "100vh",
});
export const gameScene = style({
  position: "relative",
  background: vars.color.scene,
  minHeight: vars.size.scene,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  padding: `${vars.space.xxxl} ${vars.space.gutter}`,
  textAlign: "center",
  gap: vars.space.md,
  "@media": {
    [breakpoint.narrow]: { minHeight: vars.size.sceneMobile, paddingBlock: vars.space.xxl },
  },
});
export const sceneBorder = style({
  position: "absolute",
  inset: vars.space.lg,
  border: `${vars.size.border} dashed ${vars.color.sceneLine}`,
  pointerEvents: "none",
  "@media": { [breakpoint.narrow]: { inset: vars.space.sm } },
});
export const sceneNote = style({
  position: "absolute",
  top: vars.space.xl,
  left: vars.space.xl,
  color: vars.color.lightText,
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
  "@media": { [breakpoint.narrow]: { top: vars.space.lg, left: vars.space.lg } },
});
export const gameTitle = style({
  fontSize: vars.fontSize.hero,
  letterSpacing: vars.tracking.game,
  fontWeight: vars.weight.strong,
  "@media": { [breakpoint.narrow]: { fontSize: vars.fontSize.mobileHero } },
});
export const gameLead = style({ fontSize: vars.fontSize.lead, color: vars.color.lightText });
export const menu = style({
  display: "grid",
  width: "100%",
  maxWidth: vars.size.menu,
  gap: vars.space.sm,
  marginTop: vars.space.lg,
});
export const menuAction = style([
  button,
  {
    background: vars.color.soft,
    color: vars.color.deep,
    borderColor: vars.color.line,
    boxShadow: vars.shadow.action,
  },
]);
export const menuSecondary = style([button, { color: vars.color.lightText }]);
export const sceneBottom = style({
  position: "absolute",
  bottom: vars.space.xl,
  insetInline: vars.space.xl,
  display: "flex",
  justifyContent: "space-between",
  gap: vars.space.md,
  color: vars.color.lightText,
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
  "@media": {
    [breakpoint.narrow]: {
      bottom: vars.space.lg,
      insetInline: vars.space.lg,
      flexDirection: "column",
      gap: vars.space.xxs,
    },
  },
});
export const sceneStrip = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
    paddingBlock: vars.space.xxl,
    gap: vars.space.lg,
    "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
  },
]);
export const sceneThumb = style({
  height: vars.size.portrait,
  border: `${vars.size.border} dashed ${vars.color.sceneLine}`,
  display: "grid",
  placeItems: "center",
  color: vars.color.lightText,
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
});
globalStyle(`${sceneStrip} figcaption`, { marginTop: vars.space.md, fontSize: vars.fontSize.lead });
export const gameFoot = style([
  footer,
  { borderColor: vars.color.sceneLine, color: vars.color.lightText },
]);

export const atlasIntro = style([
  container,
  {
    paddingTop: vars.space.xxl,
    paddingBottom: vars.space.xl,
    display: "flex",
    alignItems: "end",
    justifyContent: "space-between",
    gap: vars.space.xl,
    "@media": {
      [breakpoint.narrow]: { flexDirection: "column", alignItems: "start", gap: vars.space.md },
    },
  },
]);
export const atlasTitle = style({
  fontSize: vars.fontSize.atlas,
  maxWidth: vars.size.copy,
  fontWeight: vars.weight.medium,
});
export const atlasSubtitle = style({ color: vars.color.muted, maxWidth: vars.size.menu });

export const storyTitle = style({
  fontFamily: vars.font.story,
  fontWeight: vars.weight.body,
  fontSize: vars.fontSize.story,
  letterSpacing: vars.tracking.heading,
  textAlign: "center",
  marginBottom: vars.space.md,
});
export const storyIntro = style([container, { paddingBlock: vars.space.xxl, textAlign: "center" }]);
export const storyLead = style({ fontSize: vars.fontSize.lead, color: vars.color.muted });
export const comic = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "0.85fr 1.15fr",
    gap: vars.space.lg,
    paddingBottom: vars.space.xxl,
    "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
  },
]);
export const panel = style({
  padding: vars.space.lg,
  border: `${vars.size.heavyBorder} solid ${vars.color.ink}`,
  minWidth: 0,
});
export const widePanel = style([panel, { gridColumn: "1 / -1" }]);
export const panelTitle = style({
  fontFamily: vars.font.story,
  fontWeight: vars.weight.body,
  fontSize: vars.fontSize.subheading,
  marginBottom: vars.space.lg,
});
export const comicPlaceholder = style({
  minHeight: vars.size.chapter,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.lg,
  border: `${vars.size.border} dashed ${vars.color.line}`,
  background: vars.color.mist,
  fontFamily: vars.font.utility,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
  padding: vars.space.lg,
});
export const widePlaceholder = style([comicPlaceholder, { minHeight: vars.size.portrait }]);
export const speech = style({
  maxWidth: vars.size.menu,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  background: vars.color.white,
  color: vars.color.ink,
  border: `${vars.size.border} solid ${vars.color.ink}`,
  borderRadius: vars.radius.pill,
  fontFamily: vars.font.body,
  fontSize: vars.fontSize.lead,
  textAlign: "center",
});
export const caption = style({ display: "block", marginTop: vars.space.md });
export const storyEnd = style([
  container,
  {
    paddingBottom: vars.space.xxl,
    display: "flex",
    justifyContent: "center",
    flexWrap: "wrap",
    gap: vars.space.lg,
  },
]);
