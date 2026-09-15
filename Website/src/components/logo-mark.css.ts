import { style } from "@vanilla-extract/css";

/** Natural aspect ratio keeps both approved marks undistorted. */
export const mark = style({ display: "block", maxWidth: "100%", height: "auto" });
