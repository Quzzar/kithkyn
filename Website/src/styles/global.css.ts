import { globalFontFace, globalStyle } from "@vanilla-extract/css";
import { vars } from "./theme.css";

globalFontFace("Bricolage Grotesque", {
  src: "url('/fonts/bricolage-grotesque.woff2') format('woff2')",
  fontWeight: "200 800",
  fontDisplay: "swap",
});
globalFontFace("Outfit", {
  src: "url('/fonts/outfit.woff2') format('woff2')",
  fontWeight: "100 900",
  fontDisplay: "swap",
});
globalStyle("*", { boxSizing: "border-box" });
globalStyle("html", {
  background: vars.color.mist,
  scrollBehavior: "smooth",
  scrollPaddingTop: vars.space.xl,
});
globalStyle("body", {
  margin: vars.space.none,
  minWidth: vars.space.none,
  color: vars.color.pine,
  fontFamily: vars.font.body,
  fontSize: vars.fontSize.body,
  lineHeight: vars.line.body,
  textRendering: "optimizeLegibility",
});
globalStyle("h1, h2, h3, p, figure", { margin: vars.space.none });
globalStyle("h1, h2, h3", {
  fontFamily: vars.font.display,
  fontWeight: vars.weight.display,
  lineHeight: vars.line.heading,
  letterSpacing: vars.tracking.heading,
});
globalStyle("a", { color: "inherit", textDecoration: "none" });
globalStyle("button", { font: "inherit", cursor: "pointer" });
globalStyle("img", { display: "block", maxWidth: "100%" });
globalStyle("svg", { flexShrink: 0 });
globalStyle("a:focus-visible, button:focus-visible, [tabindex]:focus-visible", {
  outline: `${vars.size.focus} solid ${vars.color.focus}`,
  outlineOffset: vars.space.xxs,
});
globalStyle("::selection", { background: vars.color.honey, color: vars.color.pine });
globalStyle("html, *, *::before, *::after", {
  "@media": {
    "(prefers-reduced-motion: reduce)": {
      scrollBehavior: "auto",
      transitionDuration: "0.01ms !important",
      animationDuration: "0.01ms !important",
      animationIterationCount: "1 !important",
    },
  },
});
