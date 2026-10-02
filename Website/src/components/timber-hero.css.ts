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
  backdrop: { position: "relative" },
});
const copyBase = style({ minWidth: 0 });
export const copy = styleVariants({
  split: [copyBase],
  centered: [
    copyBase,
    { textAlign: "center", maxWidth: vars.size.timberWordmark, marginInline: "auto" },
  ],
  backdrop: [
    copyBase,
    container,
    {
      position: "relative",
      zIndex: 1,
      minHeight: vars.size.backdropHero,
      display: "flex",
      flexDirection: "column",
      justifyContent: "center",
      alignItems: "start",
      paddingBlock: vars.space.section,
      "@media": {
        [breakpoint.narrow]: {
          minHeight: "auto",
          paddingTop: vars.space.xxl,
          paddingBottom: vars.space.xxl,
        },
      },
    },
  ],
});
globalStyle(`${copyBase} h1`, {
  fontSize: vars.fontSize.catalog,
  marginBottom: vars.space.md,
  textWrap: "balance",
});
globalStyle(`${copy.centered} p`, { marginInline: "auto" });
globalStyle(`${copy.backdrop} p`, { maxWidth: vars.size.copy, color: vars.color.text });
globalStyle(`${copy.backdrop} h1`, {
  fontSize: vars.fontSize.catalog,
  fontWeight: vars.weight.medium,
  maxWidth: vars.size.copy,
});
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
globalStyle(`${copy.backdrop} .${wordmark}`, {
  width: vars.size.backdropWordmark,
  marginBottom: vars.space.xl,
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
  backdrop: {
    display: "flex",
    gap: vars.space.lg,
    flexWrap: "wrap",
    alignItems: "center",
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
  backdrop: {
    position: "absolute",
    top: `calc(-1 * ${vars.size.header})`,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 0,
    overflow: "hidden",
    background: vars.color.canvas,
    selectors: {
      "&::after": {
        content: '""',
        position: "absolute",
        inset: 0,
        background: `linear-gradient(180deg, color-mix(in srgb, ${vars.color.canvas} 85%, transparent) 0%, transparent 35%, transparent 55%, ${vars.color.canvas} 100%), linear-gradient(90deg, color-mix(in srgb, ${vars.color.canvas} 98%, transparent) 0%, color-mix(in srgb, ${vars.color.canvas} 92%, transparent) 42%, color-mix(in srgb, ${vars.color.canvas} 35%, transparent) 72%, color-mix(in srgb, ${vars.color.canvas} 12%, transparent) 100%)`,
        "@media": {
          [breakpoint.narrow]: {
            background: "none",
          },
        },
      },
    },
    "@media": {
      [breakpoint.narrow]: {
        position: "relative",
        inset: "auto",
        height: vars.size.mobilePhoto,
        marginInline: vars.space.gutter,
        borderRadius: vars.radius.small,
      },
    },
  },
});
globalStyle(`${picture.backdrop} img`, {
  width: "100%",
  height: "100%",
  objectFit: "cover",
  objectPosition: "center",
  "@media": { [breakpoint.narrow]: { objectPosition: "72% center" } },
});
globalStyle(`${picture.backdrop} figcaption`, {
  position: "absolute",
  zIndex: 1,
  bottom: vars.space.lg,
  right: vars.space.gutter,
  color: vars.color.muted,
  fontSize: vars.fontSize.tiny,
  "@media": {
    [breakpoint.narrow]: {
      insetInline: vars.space.sm,
      bottom: vars.space.sm,
      width: "fit-content",
      padding: `${vars.space.xxs} ${vars.space.xs}`,
      borderRadius: vars.radius.small,
      background: vars.color.imageOverlay,
      color: vars.color.text,
    },
  },
});
globalStyle(`${picture.backdrop} figcaption a:hover`, { textDecoration: "underline" });
