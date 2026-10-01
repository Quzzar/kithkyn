import { globalStyle, style } from "@vanilla-extract/css";
import { breakpoint, vars } from "../styles/theme.css";
import { container } from "../components/site.css";

export const entrance = style({ position: "relative", isolation: "isolate" });
export const backdrop = style({
  position: "absolute",
  top: 0,
  left: 0,
  width: "100%",
  height: `max(${vars.size.hero}, 100svh)`,
  objectFit: "cover",
  objectPosition: "center 60%",
  zIndex: -2,
  "@media": { [breakpoint.narrow]: { height: vars.size.heroMobile, objectPosition: "70% center" } },
});
export const shade = style({
  position: "absolute",
  top: 0,
  left: 0,
  width: "100%",
  height: `max(${vars.size.hero}, 100svh)`,
  background: `linear-gradient(${vars.color.overlayTop}, ${vars.color.overlayCenter} 35%, ${vars.color.overlayBottom} 80%, ${vars.color.forest})`,
  zIndex: -1,
  "@media": { [breakpoint.narrow]: { height: vars.size.heroMobile } },
});
export const hero = style({
  position: "relative",
  minHeight: `calc(max(${vars.size.hero}, 100svh) - ${vars.size.header})`,
  padding: `${vars.space.xxl} ${vars.space.gutter} ${vars.space.heroBottom}`,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  textAlign: "center",
  "@media": {
    [breakpoint.narrow]: {
      minHeight: `calc(${vars.size.heroMobile} - ${vars.size.header})`,
      paddingTop: vars.space.xl,
      paddingBottom: vars.space.heroBottom,
    },
  },
});
export const title = style({ width: "100%", maxWidth: vars.size.logo, filter: vars.shadow.title });
globalStyle(`${title} img`, { width: "100%", height: "auto" });
export const tagline = style({
  fontSize: vars.fontSize.tagline,
  fontWeight: vars.weight.strong,
  marginTop: vars.space.sm,
  textShadow: `0 ${vars.size.border} ${vars.space.xs} ${vars.color.deep}`,
});
export const menu = style({
  display: "grid",
  width: "100%",
  maxWidth: vars.size.menu,
  gap: vars.space.md,
  marginTop: vars.space.xxl,
});
const plank = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: vars.space.sm,
  minHeight: vars.size.plank,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  color: vars.color.ink,
  fontWeight: vars.weight.display,
  fontSize: vars.fontSize.menu,
  background: "url('/brand/menu-plank.svg') center / 100% 100% no-repeat",
  filter: vars.shadow.plank,
  transition: `transform ${vars.motion.short} ${vars.motion.ease}`,
  selectors: {
    "&:hover": { transform: `translateY(${vars.motion.lift}) rotate(0deg)` },
    "&:active": { transform: `translateY(${vars.motion.press}) rotate(${vars.angle.pressed})` },
  },
  "@media": {
    "(prefers-reduced-motion: reduce)": {
      selectors: { "&[href], &:hover, &:active": { transform: "none" } },
    },
  },
});
globalStyle(`${plank} svg`, { width: vars.size.icon, height: vars.size.icon });
export const primaryPlank = style([plank, { transform: `rotate(${vars.angle.left})` }]);
export const middlePlank = style([plank, { transform: `rotate(${vars.angle.right})` }]);
export const lastPlank = style([plank, { transform: `rotate(${vars.angle.gentle})` }]);
export const heroFoot = style({
  position: "absolute",
  bottom: vars.space.xxl,
  insetInline: vars.space.gutter,
  display: "flex",
  justifyContent: "space-between",
  flexWrap: "wrap",
  gap: vars.space.md,
  color: vars.color.cream,
  fontSize: vars.fontSize.tiny,
  textShadow: `0 ${vars.size.border} ${vars.space.xs} ${vars.color.deep}`,
  "@media": {
    [breakpoint.narrow]: { flexDirection: "column", gap: vars.space.xxs, bottom: vars.space.xl },
  },
});
export const down = style({
  position: "absolute",
  bottom: vars.space.xxl,
  left: "50%",
  transform: "translateX(-50%)",
  display: "grid",
  placeItems: "center",
  minHeight: vars.size.button,
  minWidth: vars.size.button,
  "@media": { [breakpoint.narrow]: { display: "none" } },
});
export const life = style({ background: vars.color.forest, paddingBlock: vars.space.section });
export const lifeInner = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: vars.space.xxxl,
    "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr", gap: vars.space.xxl } },
  },
]);
globalStyle(`${lifeInner} h2`, { marginTop: vars.space.md });
export const lifeLead = style({
  fontSize: vars.fontSize.lead,
  marginTop: vars.space.lg,
  color: vars.color.muted,
});
export const lifeRows = style({ display: "grid", gap: vars.space.lg });
export const lifeRow = style({
  paddingBottom: vars.space.lg,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${lifeRow} h3`, { fontSize: vars.fontSize.subheading, marginBottom: vars.space.xs });
globalStyle(`${lifeRow} p`, { color: vars.color.muted });
