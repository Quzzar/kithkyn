import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const section = style({
  paddingBlock: vars.space.section,
  background: vars.color.canvas,
  color: vars.color.text,
});
export const setup = style({
  display: "grid",
  gridTemplateColumns: "1fr 0.9fr",
  gap: vars.space.xxxl,
  alignItems: "start",
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr", gap: vars.space.xxl } },
});
globalStyle(`${section} h2`, { marginTop: vars.space.md });
globalStyle(`${section} > div > div > div > p:first-child`, { color: vars.color.muted });
export const intro = style({
  marginTop: vars.space.lg,
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});
export const models = style({ marginTop: vars.space.xl });
export const modelList = style({
  display: "flex",
  gap: vars.space.xs,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
  width: "fit-content",
});
export const modelTab = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.xs,
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  color: vars.color.muted,
  background: "transparent",
  border: 0,
  borderBottom: `${vars.size.outline} solid transparent`,
  selectors: {
    '&[data-state="active"]': {
      color: vars.color.text,
      borderBottomColor: vars.color.accent,
      fontWeight: vars.weight.strong,
    },
  },
});
globalStyle(`${modelTab} svg`, { width: vars.size.icon, height: vars.size.icon });
export const modelContent = style({ paddingTop: vars.space.lg });
globalStyle(`${modelContent} h3`, {
  fontSize: vars.fontSize.subheading,
  marginBottom: vars.space.xs,
});
globalStyle(`${modelContent} p`, { color: vars.color.muted });
globalStyle(`${modelContent} dl`, { marginTop: vars.space.lg, marginBottom: 0 });
globalStyle(`${modelContent} dl > div`, {
  display: "flex",
  justifyContent: "space-between",
  gap: vars.space.md,
  paddingBlock: vars.space.sm,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
  fontSize: vars.fontSize.small,
});
globalStyle(`${modelContent} dt`, { color: vars.color.muted });
globalStyle(`${modelContent} dd`, { margin: 0, textAlign: "right" });
export const guardrail = style({
  display: "flex",
  gap: vars.space.xs,
  fontSize: vars.fontSize.tiny,
  color: vars.color.muted,
  marginTop: vars.space.lg,
});
globalStyle(`${guardrail} svg`, { width: vars.size.icon, height: vars.size.icon });
export const installCard = style({
  padding: vars.space.xxl,
  background: vars.color.surface,
  border: `${vars.size.outline} solid ${vars.color.line}`,
  "@media": { [breakpoint.narrow]: { padding: vars.space.lg } },
});
export const releaseLabel = style({
  display: "inline-block",
  fontSize: vars.fontSize.tiny,
  color: vars.color.muted,
  marginBottom: vars.space.lg,
  fontWeight: vars.weight.strong,
});
globalStyle(`${installCard} h3`, { fontSize: vars.fontSize.heading });
export const platform = style({
  color: vars.color.muted,
  marginTop: vars.space.md,
  fontSize: vars.fontSize.small,
});
globalStyle(`${installCard} ol`, {
  paddingLeft: vars.space.lg,
  marginBlock: vars.space.xl,
  display: "flex",
  flexDirection: "column",
  gap: vars.space.md,
});
export const destinations = style({
  display: "flex",
  gap: vars.space.xl,
  flexWrap: "wrap",
  paddingTop: vars.space.xl,
  marginTop: vars.space.xl,
  borderTop: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${destinations} span`, { fontWeight: vars.weight.strong });
globalStyle(`${destinations} small`, {
  display: "block",
  color: vars.color.muted,
  fontWeight: vars.weight.body,
  fontSize: vars.fontSize.tiny,
});
export const sourceLink = style({
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.xs,
  marginTop: vars.space.lg,
  fontSize: vars.fontSize.small,
});
globalStyle(`${sourceLink} svg`, { width: vars.size.icon, height: vars.size.icon });
export const faq = style({
  display: "grid",
  gridTemplateColumns: "0.8fr 1.2fr",
  gap: vars.space.xxl,
  marginTop: vars.space.section,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
export const questions = style({ minWidth: 0 });
export const question = style({
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
export const questionTrigger = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: vars.space.md,
  minHeight: vars.size.button,
  width: "100%",
  paddingBlock: vars.space.lg,
  paddingInline: 0,
  background: "transparent",
  color: vars.color.text,
  border: 0,
  fontFamily: vars.font.body,
  fontWeight: vars.weight.strong,
  fontSize: vars.fontSize.body,
  textAlign: "left",
  letterSpacing: "normal",
});
globalStyle(`${questionTrigger} svg`, {
  width: vars.size.icon,
  height: vars.size.icon,
  transition: `transform ${vars.motion.short}`,
});
globalStyle(`${questionTrigger}[data-state="open"] svg`, { transform: "rotate(180deg)" });
export const answer = style({
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
  paddingBottom: vars.space.lg,
  maxWidth: vars.size.reading,
});
