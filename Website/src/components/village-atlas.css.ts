import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";

export const section = style({
  maxWidth: vars.size.content,
  marginInline: "auto",
  paddingInline: vars.space.gutter,
  paddingBottom: vars.space.xl,
});
export const atlas = style({
  display: "grid",
  gridTemplateColumns: `${vars.size.index} minmax(0, 1fr)`,
  border: `${vars.size.border} solid ${vars.color.line}`,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});
export const index = style({
  background: vars.color.mist,
  padding: vars.space.md,
  borderRight: `${vars.size.border} solid ${vars.color.line}`,
  "@media": {
    [breakpoint.narrow]: {
      borderRight: "none",
      borderBottom: `${vars.size.border} solid ${vars.color.line}`,
    },
  },
});
export const indexTitle = style({
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
  marginBottom: vars.space.md,
  color: vars.color.muted,
});
export const selector = style({
  display: "grid",
  gap: vars.space.xxs,
  "@media": { [breakpoint.narrow]: { maxHeight: vars.size.mobileIndex, overflowY: "auto" } },
});
export const tab = style({
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  textAlign: "left",
  padding: `${vars.space.xs} ${vars.space.sm}`,
  minHeight: vars.size.draftUnit,
  gap: vars.space.sm,
  background: "transparent",
  border: `${vars.size.border} solid transparent`,
  color: vars.color.pine,
  fontSize: vars.fontSize.small,
  selectors: {
    "&[data-state='active']": {
      background: vars.color.selection,
      borderColor: vars.color.line,
      fontWeight: vars.weight.strong,
    },
    "&:hover": { borderColor: vars.color.line },
  },
});
export const detail = style({ minWidth: 0 });
export const preview = style({
  minHeight: vars.size.preview,
  backgroundColor: vars.color.soft,
  backgroundImage: `linear-gradient(${vars.color.line} ${vars.size.border}, transparent ${vars.size.border}), linear-gradient(90deg, ${vars.color.line} ${vars.size.border}, transparent ${vars.size.border})`,
  backgroundSize: `${vars.size.draftUnit} ${vars.size.draftUnit}`,
  display: "flex",
  flexDirection: "column",
  justifyContent: "center",
  alignItems: "center",
  padding: vars.space.xl,
  gap: vars.space.lg,
  "@media": { [breakpoint.narrow]: { minHeight: vars.size.chapter, padding: vars.space.lg } },
});
export const previewFrame = style({
  width: "100%",
  maxWidth: vars.size.copy,
  minHeight: vars.size.portrait,
  border: `${vars.size.border} dashed ${vars.color.muted}`,
  background: vars.color.mist,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  textAlign: "center",
  gap: vars.space.sm,
  fontFamily: vars.font.utility,
  padding: vars.space.lg,
  fontSize: vars.fontSize.small,
});
export const previewNote = style({ fontSize: vars.fontSize.tiny, color: vars.color.muted });
export const previewLabel = style({
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
  color: vars.color.muted,
  textAlign: "center",
});
export const description = style({
  padding: vars.space.xl,
  display: "grid",
  gap: vars.space.md,
  "@media": { [breakpoint.narrow]: { padding: vars.space.lg } },
});
export const biome = style({
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.small,
  color: vars.color.muted,
});
globalStyle(`${description} h2`, {
  fontSize: vars.fontSize.heading,
  fontWeight: vars.weight.medium,
});
globalStyle(`${description} ul`, {
  listStyle: "none",
  padding: 0,
  margin: 0,
  display: "flex",
  flexWrap: "wrap",
  gap: vars.space.sm,
});
globalStyle(`${description} li`, {
  border: `${vars.size.border} solid ${vars.color.line}`,
  padding: `${vars.space.xxs} ${vars.space.sm}`,
  fontFamily: vars.font.utility,
  fontSize: vars.fontSize.tiny,
});
export const note = style({
  marginTop: vars.space.lg,
  color: vars.color.muted,
  fontSize: vars.fontSize.small,
});
