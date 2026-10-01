import { globalStyle, style, styleVariants } from "@vanilla-extract/css";
import { container } from "./site.css";
import { figure } from "./village-hero.css";
import { breakpoint, vars } from "../styles/theme.css";

const base = style([container, { paddingTop: vars.space.xxl, paddingBottom: vars.space.section }]);
export const layout = styleVariants({
  cabin: [
    base,
    {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      alignItems: "center",
      gap: vars.space.xxl,
      "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
    },
  ],
  patchwork: [base, { display: "grid", gap: vars.space.xxl }],
});
const copyBase = style({ minWidth: 0 });
export const copy = styleVariants({
  cabin: [copyBase],
  patchwork: [
    copyBase,
    { textAlign: "center", maxWidth: vars.size.timberWordmark, marginInline: "auto" },
  ],
});
globalStyle(`${copyBase} h1`, { fontSize: vars.fontSize.catalog, marginBottom: vars.space.md });
globalStyle(`${copy.patchwork} p`, { marginInline: "auto" });
export const wordmark = style({
  width: vars.size.timberSplitWordmark,
  height: "auto",
  marginBlock: vars.space.lg,
  imageRendering: "pixelated",
  "@media": { [breakpoint.narrow]: { width: vars.size.studyWordmark } },
});
globalStyle(`${copy.patchwork} .${wordmark}`, {
  width: vars.size.timberWordmark,
  marginInline: "auto",
  "@media": { [breakpoint.narrow]: { width: vars.size.studyWordmark } },
});
export const actions = styleVariants({
  cabin: {
    display: "flex",
    gap: vars.space.lg,
    flexWrap: "wrap",
    alignItems: "center",
    marginTop: vars.space.xl,
  },
  patchwork: {
    display: "flex",
    gap: vars.space.lg,
    flexWrap: "wrap",
    alignItems: "center",
    justifyContent: "center",
    marginTop: vars.space.xl,
  },
});
export const picture = styleVariants({
  cabin: [figure],
  patchwork: [
    figure,
    {
      height: vars.size.timberPanorama,
      borderRadius: vars.radius.small,
      "@media": { [breakpoint.narrow]: { height: vars.size.mobilePhoto } },
    },
  ],
});
