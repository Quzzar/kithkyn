import { createGlobalTheme, styleVariants } from "@vanilla-extract/css";
import type { IdentityId } from "../data/identities";

/** Quiet surfaces leave personality in the icon and bespoke logo lettering. */
export const vars = createGlobalTheme(":root", {
  color: {
    forest: "#fafbfc",
    raised: "#eff3f6",
    deep: "#e6edf2",
    cream: "#22384a",
    leaf: "#657584",
    muted: "#657584",
    line: "#d8dfe5",
    oak: "#9d6c36",
    ink: "#22384a",
    paper: "#ffffff",
    paperRaised: "#f7f9fb",
    paperLine: "#d8dfe5",
    paperMuted: "#657584",
    focus: "#386885",
    sky: "#bbc6ce",
    photoInk: "#ffffff",
    overlayBottom: "#22384ad9",
  },
  font: { display: '"Outfit", sans-serif', body: '"Outfit", sans-serif' },
  fontSize: {
    tiny: "0.78rem",
    small: "0.9rem",
    body: "1rem",
    lead: "1.2rem",
    heading: "clamp(2.5rem, 4.5vw, 4rem)",
    subheading: "1.5rem",
    title: "clamp(3rem, 6vw, 5rem)",
    catalog: "clamp(1.75rem, 3vw, 2.5rem)",
    hero: "clamp(2.6rem, 5vw, 4.6rem)",
  },
  weight: { body: "400", medium: "500", strong: "600", display: "600" },
  line: { body: "1.55", heading: "1.05" },
  tracking: { heading: "-0.035em", label: "0.06em" },
  space: {
    none: "0",
    xxs: "0.25rem",
    xs: "0.5rem",
    sm: "0.75rem",
    md: "1rem",
    lg: "1.5rem",
    xl: "2rem",
    xxl: "3rem",
    xxxl: "4rem",
    section: "clamp(4rem, 7vw, 6rem)",
    gutter: "clamp(1.25rem, 4vw, 3.5rem)",
  },
  radius: { small: "0.625rem", scene: "1.5rem", pill: "999rem" },
  size: {
    content: "78rem",
    copy: "34rem",
    reading: "46rem",
    compactLogo: "12rem",
    emblem: "2.75rem",
    icon: "1.2rem",
    border: "0.0625rem",
    focus: "0.1875rem",
    button: "3rem",
    header: "5rem",
    photo: "27rem",
    mobilePhoto: "17rem",
    outline: "0.125rem",
    brandTile: "16rem",
    studyLogo: "8rem",
    heroPhoto: "34rem",
    widePhoto: "26rem",
  },
  motion: { short: "180ms", ease: "cubic-bezier(0.2, 0.8, 0.2, 1)" },
});

type Palette = {
  readonly canvas: string;
  readonly surface: string;
  readonly ink: string;
  readonly accent: string;
  readonly muted: string;
  readonly line: string;
};
const PALETTES: Record<IdentityId, Palette> = {
  joinery: {
    canvas: "#fafbfc",
    surface: "#eff3f6",
    ink: "#22384a",
    accent: "#9d6c36",
    muted: "#657584",
    line: "#d8dfe5",
  },
  gather: {
    canvas: "#f7faf7",
    surface: "#e9f1ec",
    ink: "#254b43",
    accent: "#8d692c",
    muted: "#5d7469",
    line: "#d1ded6",
  },
  offcut: {
    canvas: "#f9f9f8",
    surface: "#f0ece7",
    ink: "#29313e",
    accent: "#995d37",
    muted: "#687079",
    line: "#dcd9d3",
  },
  neighbor: {
    canvas: "#f8f9fc",
    surface: "#edf0f7",
    ink: "#343f67",
    accent: "#936c20",
    muted: "#68718a",
    line: "#d9deeb",
  },
};
export const identityTheme = styleVariants(PALETTES, (palette: Palette) => ({
  vars: {
    [vars.color.forest]: palette.canvas,
    [vars.color.raised]: palette.surface,
    [vars.color.deep]: palette.surface,
    [vars.color.cream]: palette.ink,
    [vars.color.ink]: palette.ink,
    [vars.color.oak]: palette.accent,
    [vars.color.leaf]: palette.muted,
    [vars.color.muted]: palette.muted,
    [vars.color.paperMuted]: palette.muted,
    [vars.color.line]: palette.line,
    [vars.color.paperLine]: palette.line,
  },
}));

export const breakpoint = {
  compact: "screen and (max-width: 64rem)",
  narrow: "screen and (max-width: 44rem)",
} as const;
