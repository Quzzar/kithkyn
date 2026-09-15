import { useEffect, useState, type ReactElement } from "react";

import * as styles from "../routes/home-page.css";

const HERO_VIDEO_MEDIA_QUERY: string = "(min-width: 48rem)";
const REDUCED_MOTION_MEDIA_QUERY: string = "(prefers-reduced-motion: reduce)";

type NavigatorWithConnection = Navigator & {
  readonly connection?: { readonly saveData?: boolean };
};

/** Narrow the browser navigator to implementations exposing data-saving mode. */
function hasConnectionInformation(
  navigatorValue: Navigator,
): navigatorValue is NavigatorWithConnection {
  return "connection" in navigatorValue;
}

/** Scenic hero media with a static-first, playback-ready fade. */
export function HeroBackground(): ReactElement {
  const [shouldShowVideo, setShouldShowVideo] = useState<boolean>(false);
  const [isVideoReady, setIsVideoReady] = useState<boolean>(false);

  useEffect((): (() => void) => {
    const wideViewport: MediaQueryList = window.matchMedia(HERO_VIDEO_MEDIA_QUERY);
    const reducedMotion: MediaQueryList = window.matchMedia(REDUCED_MOTION_MEDIA_QUERY);

    function updateVideoPreference(): void {
      const isSavingData: boolean =
        hasConnectionInformation(navigator) && navigator.connection?.saveData === true;
      const shouldPlay: boolean = wideViewport.matches && !reducedMotion.matches && !isSavingData;
      setShouldShowVideo(shouldPlay);
      if (!shouldPlay) setIsVideoReady(false);
    }

    updateVideoPreference();
    wideViewport.addEventListener("change", updateVideoPreference);
    reducedMotion.addEventListener("change", updateVideoPreference);

    return (): void => {
      wideViewport.removeEventListener("change", updateVideoPreference);
      reducedMotion.removeEventListener("change", updateVideoPreference);
    };
  }, []);

  return (
    <div className={styles.heroMedia} aria-hidden="true">
      <img
        className={styles.heroPoster}
        src="/images/valecraft-reference-forest.jpg"
        alt=""
        fetchPriority="high"
      />
      {shouldShowVideo && (
        <video
          className={`${styles.heroVideo} ${isVideoReady ? styles.heroVideoReady : ""}`}
          src="/media/valecraft-reference-hero.mp4"
          autoPlay
          muted
          loop
          playsInline
          preload="metadata"
          onPlaying={(): void => {
            setIsVideoReady(true);
          }}
        />
      )}
      <div className={styles.heroShade} />
    </div>
  );
}
