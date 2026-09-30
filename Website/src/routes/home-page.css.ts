import { globalStyle, style } from "@vanilla-extract/css";
import { container } from "../components/site.css";
import { breakpoint, vars } from "../styles/theme.css";

export const hero = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "minmax(0, 0.95fr) minmax(0, 1.25fr)",
    alignItems: "center",
    gap: vars.space.md,
    paddingTop: vars.space.xxl,
    paddingBottom: vars.space.xxxl,
    "@media": {
      [breakpoint.narrow]: {
        gridTemplateColumns: "minmax(0, 1fr)",
        paddingTop: vars.space.xl,
        paddingBottom: vars.space.xl,
      },
    },
  },
]);
export const heroCopy = style({ position: "relative", zIndex: 1 });
globalStyle(`${heroCopy} h1`, {
  fontSize: vars.fontSize.hero,
  lineHeight: vars.line.hero,
  marginBlock: `${vars.space.lg} ${vars.space.xl}`,
});
globalStyle(`${heroCopy} h1 span`, { color: vars.color.pine });
export const lead = style({
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
  maxWidth: vars.size.heroCopy,
});
export const heroActions = style({
  display: "flex",
  flexWrap: "wrap",
  gap: vars.space.sm,
  marginTop: vars.space.xl,
});
export const compatibility = style({
  fontSize: vars.fontSize.tiny,
  color: vars.color.muted,
  marginTop: vars.space.lg,
});
globalStyle(`${compatibility} span`, { paddingInline: vars.space.xs });
export const heroArt = style({
  width: "110%",
  marginLeft: "-5%",
  "@media": { [breakpoint.narrow]: { width: "100%", marginLeft: 0, marginTop: vars.space.lg } },
});
globalStyle(`${heroArt} img`, { width: "100%", height: "auto" });
globalStyle(`${heroArt} figcaption`, {
  color: vars.color.muted,
  fontSize: vars.fontSize.tiny,
  textAlign: "center",
  marginTop: vars.space.md,
});
export const promise = style({
  background: vars.color.pine,
  color: vars.color.mist,
  paddingBlock: vars.space.lg,
});
export const promiseInner = style([
  container,
  {
    display: "flex",
    justifyContent: "space-between",
    gap: vars.space.md,
    fontSize: vars.fontSize.small,
    "@media": { [breakpoint.narrow]: { display: "block" } },
  },
]);
globalStyle(`${promiseInner} > span`, {
  color: vars.color.lightText,
  "@media": { [breakpoint.narrow]: { display: "none" } },
});
export const life = style([
  container,
  {
    display: "grid",
    gridTemplateColumns: "minmax(0, 1fr) minmax(0, 1.1fr)",
    gap: vars.space.xxl,
    paddingBlock: vars.space.section,
    "@media": { [breakpoint.narrow]: { gridTemplateColumns: "minmax(0, 1fr)" } },
  },
]);
export const lifeHeading = style({ display: "flex", flexDirection: "column", gap: vars.space.lg });
globalStyle(`${lifeHeading} > p:last-child`, {
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});
export const lifeDetails = style({ alignSelf: "center" });
globalStyle(`${lifeDetails} article`, {
  display: "flex",
  gap: vars.space.lg,
  paddingBlock: vars.space.lg,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
});
globalStyle(`${lifeDetails} article:first-child`, { paddingTop: 0 });
globalStyle(`${lifeDetails} article:last-child`, { borderBottom: "none", paddingBottom: 0 });
globalStyle(`${lifeDetails} svg`, { width: vars.size.bigIcon, height: vars.size.bigIcon });
globalStyle(`${lifeDetails} h3`, {
  fontSize: vars.fontSize.subheading,
  marginBottom: vars.space.xs,
});
globalStyle(`${lifeDetails} p`, { color: vars.color.muted, maxWidth: vars.size.copy });
