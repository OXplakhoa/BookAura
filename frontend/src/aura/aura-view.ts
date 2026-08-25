import { useSyncExternalStore } from "react";

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";

export type AuraViewMode = "shelf" | "cards";

export function supportsAuraShelf(): boolean {
  if (typeof window === "undefined" || typeof document === "undefined") return false;
  try {
    const canvas = document.createElement("canvas");
    const options = { failIfMajorPerformanceCaveat: true };
    return Boolean(canvas.getContext("webgl2", options));
  } catch {
    return false;
  }
}

export function chooseAuraViewMode({ reducedMotion, supports3d }: { reducedMotion: boolean; supports3d: boolean }): AuraViewMode {
  return reducedMotion || !supports3d ? "cards" : "shelf";
}

function getMotionQuery(): MediaQueryList | null {
  return typeof window !== "undefined" && typeof window.matchMedia === "function"
    ? window.matchMedia(REDUCED_MOTION_QUERY)
    : null;
}

function readReducedMotion(): boolean {
  return getMotionQuery()?.matches ?? false;
}

function subscribeToMotionPreference(onChange: () => void): () => void {
  const media = getMotionQuery();
  if (!media) return () => undefined;

  if (typeof media.addEventListener === "function") {
    media.addEventListener("change", onChange);
    return () => media.removeEventListener("change", onChange);
  }

  media.addListener(onChange);
  return () => media.removeListener(onChange);
}

export function usePrefersReducedMotion(): boolean {
  return useSyncExternalStore(subscribeToMotionPreference, readReducedMotion, () => false);
}
