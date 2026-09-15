import { globalStyle, style } from "@vanilla-extract/css";

import { breakpoint, vars } from "../styles/theme.css";

export const page = style({ minHeight: "100vh", background: vars.color.night });

export const skipLink = style({
  position: "fixed",
  zIndex: 20,
  top: vars.space.sm,
  left: vars.space.sm,
  transform: "translateY(-200%)",
  padding: vars.space.sm,
  background: vars.color.cream,
  color: vars.color.night,
  selectors: { "&:focus": { transform: "none" } },
});

export const header = style({
  position: "absolute",
  zIndex: 5,
  top: 0,
  insetInline: 0,
  minHeight: vars.size.header,
  padding: `${vars.space.sm} ${vars.space.gutter}`,
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: vars.space.xl,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
  background: vars.background.header,
});

export const brand = style({
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.sm,
  flexShrink: 0,
  textDecoration: "none",
});

export const brandIcon = style({
  width: vars.size.navIcon,
  borderRadius: vars.radius.small,
  boxShadow: vars.shadow.brand,
});

export const brandWordmark = style({
  width: vars.size.navWordmark,
  height: "auto",
  display: "block",
  filter: vars.shadow.wordmark,
});

globalStyle(`${header} nav`, {
  display: "flex",
  alignItems: "center",
  gap: "clamp(1rem, 2.5vw, 2rem)",
});

globalStyle(`${header} nav a`, {
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.xs,
  color: vars.color.muted,
  textDecoration: "none",
  whiteSpace: "nowrap",
});

globalStyle(`${header} nav svg`, { width: vars.size.iconSmall, height: vars.size.iconSmall });

globalStyle(`${header} nav a:hover`, { color: vars.color.cream });
globalStyle(`${header} nav a:last-child`, { color: vars.color.oak });

export const secondaryNavLink = style({});

globalStyle(`${header} nav ${secondaryNavLink}`, {
  "@media": { [breakpoint.compact]: { display: "none" } },
});

export const hero = style({
  position: "relative",
  minHeight: "100svh",
  display: "grid",
  placeItems: "center",
  overflow: "hidden",
  background: vars.color.night,
  color: vars.color.cream,
  textAlign: "center",
  "@media": {
    [breakpoint.narrow]: {
      minHeight: vars.size.titleHeroMobile,
    },
  },
});

export const heroMedia = style({ position: "absolute", inset: 0 });

export const heroPoster = style({
  position: "absolute",
  inset: 0,
  width: "100%",
  height: "100%",
  display: "block",
  objectFit: "cover",
});

export const heroVideo = style({
  position: "absolute",
  inset: 0,
  width: "100%",
  height: "100%",
  display: "block",
  objectFit: "cover",
  opacity: 0,
  transition: `opacity ${vars.motion.slow} ${vars.motion.ease}`,
});

export const heroVideoReady = style({ opacity: 1 });

export const heroShade = style({
  position: "absolute",
  inset: 0,
  background: vars.background.heroShade,
});

export const heroContent = style({
  position: "relative",
  zIndex: 1,
  width: "100%",
  padding: `calc(${vars.size.header} + ${vars.space.xxl}) ${vars.space.gutter} ${vars.space.xxl}`,
  display: "grid",
  placeItems: "center",
  alignContent: "center",
  "@media": {
    [breakpoint.narrow]: {
      paddingTop: `calc(${vars.size.header} + ${vars.space.section})`,
    },
  },
});

export const heroLogo = style({
  width: `min(100%, ${vars.size.logoHero})`,
  margin: 0,
  filter: vars.shadow.logo,
});

globalStyle(`${heroContent} > p`, {
  margin: `${vars.space.md} 0 ${vars.space.xl}`,
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});

export const heroLink = style({
  padding: `${vars.space.xs} 0`,
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.sm,
  borderBottom: `${vars.size.border} solid ${vars.color.oak}`,
  color: vars.color.oak,
  fontFamily: vars.font.display,
  textDecoration: "none",
  selectors: {
    "&:hover": { color: vars.color.cream, borderBottomColor: vars.color.cream },
  },
});

globalStyle(`${heroLink} svg`, { width: vars.size.icon, height: vars.size.icon });

export const villageSection = style({
  padding: `${vars.space.section} ${vars.space.gutter}`,
  background: vars.color.night,
  color: vars.color.cream,
});

export const sectionHeading = style({
  maxWidth: vars.size.content,
  margin: `0 auto ${vars.space.xl}`,
  display: "flex",
  alignItems: "end",
  justifyContent: "space-between",
  gap: vars.space.lg,
});

export const howIntroduction = style({ maxWidth: vars.size.narrowCopy });

globalStyle(`${sectionHeading} > div:first-child > p, ${howIntroduction} > p:first-child`, {
  margin: `0 0 ${vars.space.sm}`,
  color: vars.color.oak,
  fontFamily: vars.font.utility,
  textTransform: "uppercase",
  letterSpacing: vars.letterSpacing.label,
});

globalStyle(`${sectionHeading} h2, ${howIntroduction} h2`, {
  margin: 0,
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.section,
  fontWeight: 400,
  lineHeight: vars.lineHeight.heading,
});

export const carouselControls = style({ display: "flex", gap: vars.space.xs });

globalStyle(`${carouselControls} button`, {
  width: vars.size.iconButton,
  height: vars.size.iconButton,
  padding: 0,
  display: "grid",
  placeItems: "center",
  border: `${vars.size.border} solid ${vars.color.line}`,
  background: "transparent",
  color: vars.color.cream,
  cursor: "pointer",
});

globalStyle(`${carouselControls} button:hover`, {
  borderColor: vars.color.oak,
  color: vars.color.oak,
});

globalStyle(`${carouselControls} svg`, { width: vars.size.icon, height: vars.size.icon });

export const villageCarousel = style({
  maxWidth: vars.size.content,
  margin: "0 auto",
  paddingBottom: vars.space.md,
  display: "flex",
  gap: vars.space.md,
  overflowX: "auto",
  overscrollBehaviorInline: "contain",
  scrollSnapType: "x mandatory",
  scrollbarColor: `${vars.color.line} transparent`,
});

globalStyle(`${villageCarousel} figure`, {
  position: "relative",
  flex: `0 0 ${vars.size.carouselCard}`,
  margin: 0,
  minHeight: vars.size.postcard,
  overflow: "hidden",
  borderRadius: vars.radius.small,
  background: vars.color.surface,
  scrollSnapAlign: "start",
  "@media": {
    [breakpoint.compact]: {
      flexBasis: vars.size.carouselCardMobile,
      minHeight: vars.size.postcardMobile,
    },
  },
});

globalStyle(`${villageCarousel} img`, {
  width: "100%",
  height: "100%",
  display: "block",
  objectFit: "cover",
  transition: `transform ${vars.motion.medium} ${vars.motion.ease}`,
});

globalStyle(`${villageCarousel} figure:hover img`, { transform: "scale(1.025)" });

globalStyle(`${villageCarousel} figcaption`, {
  position: "absolute",
  inset: "auto 0 0",
  padding: vars.space.lg,
  display: "grid",
  gap: vars.space.xxs,
  background: vars.background.caption,
});

globalStyle(`${villageCarousel} figcaption > span`, {
  color: vars.color.oak,
  fontFamily: vars.font.utility,
  textTransform: "uppercase",
  letterSpacing: vars.letterSpacing.label,
});

globalStyle(`${villageCarousel} strong`, {
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.subheading,
  fontWeight: 400,
});

globalStyle(`${villageCarousel} figcaption > p`, {
  margin: 0,
  color: vars.color.muted,
});

export const howSection = style({
  padding: `${vars.space.section} ${vars.space.gutter}`,
  display: "grid",
  gridTemplateColumns: "minmax(18rem, 0.8fr) minmax(0, 1.2fr)",
  gap: "clamp(3rem, 8vw, 8rem)",
  maxWidth: vars.size.wide,
  margin: "0 auto",
  background: vars.color.surface,
  color: vars.color.cream,
  "@media": { [breakpoint.compact]: { gridTemplateColumns: "1fr", gap: vars.space.xxl } },
});

globalStyle(`${howIntroduction} > p:last-child`, {
  margin: `${vars.space.lg} 0 0`,
  color: vars.color.muted,
  fontSize: vars.fontSize.lead,
});

export const modelDetails = style({ display: "grid", gap: vars.space.lg });

export const modelChoices = style({
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
  borderTop: `${vars.size.border} solid ${vars.color.line}`,
  borderBottom: `${vars.size.border} solid ${vars.color.line}`,
  "@media": { [breakpoint.narrow]: { gridTemplateColumns: "1fr" } },
});

globalStyle(`${modelChoices} article`, {
  padding: `${vars.space.xl} ${vars.space.lg}`,
  borderRight: `${vars.size.border} solid ${vars.color.line}`,
  "@media": {
    [breakpoint.narrow]: {
      borderRight: 0,
      borderBottom: `${vars.size.border} solid ${vars.color.line}`,
    },
  },
});

globalStyle(`${modelChoices} article:last-child`, {
  borderRight: 0,
  "@media": { [breakpoint.narrow]: { borderBottom: 0 } },
});

globalStyle(`${modelChoices} svg`, {
  width: vars.size.iconLarge,
  height: vars.size.iconLarge,
  marginBottom: vars.space.xl,
  color: vars.color.oak,
  strokeWidth: vars.size.iconStroke,
});

globalStyle(`${modelChoices} h3`, {
  margin: 0,
  color: vars.color.cream,
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.subheading,
  fontWeight: 400,
});

globalStyle(`${modelChoices} p`, {
  margin: `${vars.space.sm} 0 0`,
  color: vars.color.muted,
});

export const guardrail = style({
  display: "grid",
  gridTemplateColumns: "auto 1fr",
  alignItems: "start",
  gap: vars.space.md,
  color: vars.color.muted,
});

globalStyle(`${guardrail} svg`, {
  width: vars.size.icon,
  height: vars.size.icon,
  color: vars.color.oak,
});

globalStyle(`${guardrail} p`, { margin: 0 });
globalStyle(`${guardrail} strong`, { color: vars.color.cream });

export const downloadSection = style({
  padding: `${vars.space.section} ${vars.space.gutter}`,
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  alignItems: "end",
  gap: vars.space.xxl,
  background: vars.color.parchment,
  color: vars.color.night,
  "@media": { [breakpoint.compact]: { gridTemplateColumns: "1fr" } },
});

globalStyle(`${downloadSection} > div:first-child`, {
  width: "100%",
  maxWidth: vars.size.reading,
});

globalStyle(`${downloadSection} > div:first-child > p:first-child`, {
  margin: `0 0 ${vars.space.sm}`,
  color: vars.color.timber,
  fontFamily: vars.font.utility,
  textTransform: "uppercase",
  letterSpacing: vars.letterSpacing.label,
});

globalStyle(`${downloadSection} h2`, {
  margin: 0,
  fontFamily: vars.font.display,
  fontSize: vars.fontSize.section,
  fontWeight: 400,
  lineHeight: vars.lineHeight.heading,
});

globalStyle(`${downloadSection} > div:first-child > p:last-child`, {
  margin: `${vars.space.lg} 0 0`,
  color: vars.color.timber,
  fontSize: vars.fontSize.lead,
});

export const downloadActions = style({
  width: `min(100%, ${vars.size.menuWidth})`,
  display: "grid",
  gap: vars.space.sm,
});

globalStyle(`${downloadActions} > span, ${downloadActions} > a`, {
  minHeight: vars.size.button,
  padding: `${vars.space.sm} ${vars.space.lg}`,
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: vars.space.md,
  border: `${vars.size.border} solid ${vars.color.timber}`,
  color: vars.color.night,
  fontFamily: vars.font.display,
  textDecoration: "none",
});

globalStyle(`${downloadActions} svg`, { width: vars.size.iconSmall, height: vars.size.iconSmall });

globalStyle(`${downloadActions} > span`, {
  borderColor: vars.color.timber,
  color: vars.color.timber,
  opacity: 0.6,
});

globalStyle(`${downloadActions} small`, {
  fontFamily: vars.font.body,
  fontSize: vars.fontSize.tiny,
  fontWeight: 600,
  letterSpacing: vars.letterSpacing.label,
  textTransform: "uppercase",
});

globalStyle(`${downloadActions} > a`, {
  background: "transparent",
  color: vars.color.night,
});

globalStyle(`${downloadActions} > a:hover`, { background: vars.color.cream });

export const footer = style({
  padding: `${vars.space.xxl} ${vars.space.gutter}`,
  display: "grid",
  gridTemplateColumns: "auto 1fr auto",
  alignItems: "center",
  gap: vars.space.xxl,
  color: vars.color.muted,
  borderTop: `${vars.size.border} solid ${vars.color.line}`,
  "@media": { [breakpoint.compact]: { gridTemplateColumns: "1fr", gap: vars.space.xl } },
});

export const footerBrand = style({
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.sm,
  justifySelf: "start",
  textDecoration: "none",
});

export const footerIcon = style({ width: vars.size.footerIcon, borderRadius: vars.radius.small });

globalStyle(`${footerBrand} > img:last-child`, {
  width: vars.size.footerWordmark,
  height: "auto",
});

globalStyle(`${footer} nav`, {
  display: "flex",
  justifyContent: "center",
  flexWrap: "wrap",
  gap: vars.space.xl,
  "@media": { [breakpoint.compact]: { justifyContent: "start" } },
});

globalStyle(`${footer} nav a`, {
  display: "inline-flex",
  alignItems: "center",
  gap: vars.space.xs,
  color: vars.color.muted,
  textDecoration: "none",
});

globalStyle(`${footer} nav a:hover`, { color: vars.color.oak });
globalStyle(`${footer} nav svg`, { width: vars.size.iconSmall, height: vars.size.iconSmall });
globalStyle(`${footer} > p`, { margin: 0, whiteSpace: "nowrap" });
