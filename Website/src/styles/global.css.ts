import { globalFontFace, globalStyle } from "@vanilla-extract/css";

import { vars } from "./theme.css";

globalFontFace("Pixelify Sans", {
  src: "url('/fonts/pixelify-sans.woff2') format('woff2')",
  fontWeight: "400",
  fontDisplay: "swap",
});
globalFontFace("Outfit", {
  src: "url('/fonts/outfit.woff2') format('woff2')",
  fontWeight: "100 900",
  fontDisplay: "swap",
});

globalStyle("*", {
  boxSizing: "border-box",
  "@media": {
    "(prefers-reduced-motion: reduce)": {
      scrollBehavior: "auto",
      transitionDuration: "0.01ms !important",
      animationDuration: "0.01ms !important",
      animationIterationCount: "1 !important",
    },
  },
});

globalStyle("html", {
  scrollBehavior: "smooth",
  background: vars.color.night,
  "@media": {
    "(prefers-reduced-motion: reduce)": { scrollBehavior: "auto" },
  },
});

globalStyle("body", {
  margin: vars.space.none,
  minWidth: "20rem",
  background: vars.color.night,
  color: vars.color.cream,
  fontFamily: vars.font.body,
  fontSize: vars.fontSize.body,
  lineHeight: vars.lineHeight.body,
  textRendering: "optimizeLegibility",
});

globalStyle("button, a", {
  font: "inherit",
});

globalStyle("a", {
  color: "inherit",
});

globalStyle("a:focus-visible, button:focus-visible", {
  borderRadius: vars.radius.small,
  outline: `${vars.size.focus} solid ${vars.color.focus}`,
  outlineOffset: vars.space.xxs,
});

globalStyle("::selection", {
  background: vars.color.oak,
  color: vars.color.cream,
});
