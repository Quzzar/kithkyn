import { createGlobalTheme } from "@vanilla-extract/css";

/** Global design tokens for the public Kithkyn website. */
export const vars = createGlobalTheme(":root", {
  color: {
    night: "#0e1510",
    surface: "#171d15",
    parchment: "#dfc996",
    muted: "#c4cbb4",
    oak: "#e9b96e",
    cream: "#fff5dc",
    timber: "#5b3526",
    line: "#424536",
    focus: "#f5c968",
  },
  font: {
    display: '"Pixelify Sans", monospace',
    body: '"Outfit", sans-serif',
    utility: '"Pixelify Sans", monospace',
  },
  fontSize: {
    subheading: "clamp(1.3rem, 2vw, 1.65rem)",
    tiny: "0.85rem",
    body: "1rem",
    lead: "clamp(1.08rem, 1.8vw, 1.3rem)",
    section: "clamp(2rem, 3.5vw, 3.3rem)",
  },
  lineHeight: {
    heading: "1.02",
    body: "1.55",
  },
  letterSpacing: {
    label: "0.11em",
  },
  space: {
    none: "0",
    xxs: "0.25rem",
    xs: "0.5rem",
    sm: "0.75rem",
    md: "1rem",
    lg: "1.5rem",
    xl: "2rem",
    xxl: "3rem",
    section: "clamp(4.5rem, 9vw, 9rem)",
    gutter: "clamp(1.1rem, 4vw, 4rem)",
  },
  radius: {
    small: "0.4rem",
  },
  size: {
    postcard: "23rem",
    titleHeroMobile: "42rem",
    wide: "88rem",
    logoHero: "35rem",
    menuWidth: "25rem",
    navIcon: "2.2rem",
    navWordmark: "6.75rem",
    footerIcon: "2.4rem",
    footerWordmark: "7.5rem",
    iconSmall: "1rem",
    icon: "1.25rem",
    iconLarge: "2rem",
    iconButton: "2.75rem",
    iconStroke: "1.6",
    carouselCard: "clamp(20rem, 40vw, 31rem)",
    carouselCardMobile: "min(86vw, 24rem)",
    postcardMobile: "24rem",
    button: "3.2rem",
    narrowCopy: "34rem",
    content: "76rem",
    reading: "43rem",
    header: "5rem",
    border: "0.0625rem",
    focus: "0.1875rem",
  },
  shadow: {
    logo: "drop-shadow(0 0.6rem 1.5rem rgba(0,0,0,0.55))",
    brand: "0 0.35rem 1rem rgba(0, 0, 0, 0.35)",
    wordmark: "drop-shadow(0 0.25rem 0.4rem rgba(0, 0, 0, 0.6))",
  },
  motion: {
    medium: "360ms",
    slow: "800ms",
    ease: "cubic-bezier(0.2, 0.8, 0.2, 1)",
  },
  background: {
    header: "linear-gradient(180deg, rgba(14, 21, 16, 0.7), rgba(14, 21, 16, 0))",
    heroShade:
      "radial-gradient(ellipse 120% 90% at 50% 38%, transparent 40%, rgba(14, 21, 16, 0.5) 100%), linear-gradient(180deg, rgba(14, 21, 16, 0.28), rgba(14, 21, 16, 0.16) 30%, rgba(14, 21, 16, 0.44) 72%, #0e1510 100%)",
    caption: "linear-gradient(180deg, transparent, rgba(14, 21, 16, 0.94))",
  },
});

/** Responsive breakpoints shared by page styles. */
export const breakpoint = {
  compact: "screen and (max-width: 52rem)",
  narrow: "screen and (max-width: 38rem)",
} as const;
