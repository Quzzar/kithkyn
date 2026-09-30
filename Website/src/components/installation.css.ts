import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const section = style({ paddingBlock: vars.space.section });
export const setup = style({
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) minmax(0, 0.9fr)",
  gap: vars.space.xxxl,
  alignItems: "start",
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "minmax(0, 1fr)", gap: vars.space.xxl } },
});
globalStyle(`${setup} h2`, { marginTop: vars.space.md });
export const intro = style({
  marginTop: vars.space.lg,
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});
export const models = style({ marginTop: vars.space.xl });
export const modelList = style({
  display: "flex",
  gap: vars.space.xs,
  background: vars.color.soft,
  padding: vars.space.xxs,
  borderRadius: vars.radius.pill,
  width: "fit-content",
});
export const modelTab = style({
  display: "flex",
  alignItems: "center",
  gap: vars.space.xs,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  borderRadius: vars.radius.pill,
  color: vars.color.pine,
  background: "transparent",
  border: 0,
  selectors: { '&[data-state="active"]': { background: vars.color.white } },
});
globalStyle(`${modelTab} svg`, { width: vars.size.icon, height: vars.size.icon });
export const modelContent = style({ paddingTop: vars.space.lg });
globalStyle(`${modelContent} h3`, {
  fontSize: vars.fontSize.subheading,
  marginBottom: vars.space.xs,
});
globalStyle(`${modelContent} p`, { color: vars.color.muted });
globalStyle(`${modelContent} dl`, { marginTop: vars.space.lg, marginBottom: vars.space.none });
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
  background: vars.color.white,
  border: `${vars.size.border} solid ${vars.color.line}`,
  "@media": { [breakpoint.narrow]: { padding: vars.space.xl } },
});
export const releaseLabel = style({
  display: "inline-block",
  padding: `${vars.space.xxs} ${vars.space.sm}`,
  fontSize: vars.fontSize.tiny,
  background: vars.color.soft,
  borderRadius: vars.radius.pill,
  marginBottom: vars.space.lg,
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
  gridTemplateColumns: "minmax(0, 0.8fr) minmax(0, 1.2fr)",
  gap: vars.space.xxl,
  marginTop: vars.space.section,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "minmax(0, 1fr)" } },
});
globalStyle(`${faq} h2`, { marginTop: vars.space.md });
export const questions = style({ minWidth: 0 });
export const question = style({ borderBottom: `${vars.size.border} solid ${vars.color.line}` });
export const questionTrigger = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: vars.space.md,
  width: "100%",
  paddingBlock: vars.space.lg,
  paddingInline: 0,
  background: "transparent",
  color: vars.color.pine,
  border: 0,
  fontFamily: vars.font.body,
  fontWeight: vars.weight.medium,
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
