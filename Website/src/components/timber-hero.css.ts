import { globalStyle, style, styleVariants } from "@vanilla-extract/css";
import { container } from "./site.css";
import { figure } from "./village-hero.css";
import { breakpoint, vars } from "../styles/theme.css";

const base = style([container, { paddingTop: vars.space.xxl, paddingBottom: vars.space.section }]);
export const layout = styleVariants({
  split: [
    base,
    {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      alignItems: "center",
      gap: vars.space.xxl,
      "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
    },
  ],
  centered: [base, { display: "grid", gap: vars.space.xxl }],
});
const copyBase = style({ minWidth: 0 });
export const copy = styleVariants({
  split: [copyBase],
  centered: [
    copyBase,
    { textAlign: "center", maxWidth: vars.size.timberWordmark, marginInline: "auto" },
  ],
});
globalStyle(`${copyBase} h1`, { fontSize: vars.fontSize.catalog, marginBottom: vars.space.md });
globalStyle(`${copy.centered} p`, { marginInline: "auto" });
export const wordmark = style({
  width: vars.size.timberSplitWordmark,
  height: "auto",
  marginBlock: vars.space.lg,
  imageRendering: "pixelated",
  "@media": { [breakpoint.narrow]: { width: vars.size.studyWordmark } },
});
globalStyle(`${copy.centered} .${wordmark}`, {
  width: vars.size.timberWordmark,
  marginInline: "auto",
  "@media": { [breakpoint.narrow]: { width: vars.size.studyWordmark } },
});
export const actions = styleVariants({
  split: {
    display: "flex",
    gap: vars.space.lg,
    flexWrap: "wrap",
    alignItems: "center",
    marginTop: vars.space.xl,
  },
  centered: {
    display: "flex",
    gap: vars.space.lg,
    flexWrap: "wrap",
    alignItems: "center",
    justifyContent: "center",
    marginTop: vars.space.xl,
  },
});
export const picture = styleVariants({
  split: [figure],
  centered: [
    figure,
    {
      height: vars.size.timberPanorama,
      borderRadius: vars.radius.small,
      "@media": { [breakpoint.narrow]: { height: vars.size.mobilePhoto } },
    },
  ],
});
