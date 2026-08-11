import { useEffect, useState } from "react";

// The landing page owns the light/dark toggle and writes `data-theme` onto
// <html> plus `mtTheme` into localStorage. Everything else in the app only
// reads it, so this module is the single place that knows those two keys.

export const readTheme = () =>
  document.documentElement.getAttribute("data-theme") ||
  localStorage.getItem("mtTheme") ||
  "dark";

/**
 * Applies the stored theme to <html> at startup.
 *
 * Without this the attribute only exists after the landing page has mounted,
 * so opening an inner route directly (a bookmarked /adminQuizzes, say) left
 * every other page with no theme to read.
 */
export const applyStoredTheme = () => {
  document.documentElement.setAttribute("data-theme", readTheme());
};

/**
 * Current theme, re-rendering when it changes.
 *
 * Watches the attribute rather than sharing React state, so the landing page's
 * existing toggle keeps working untouched and any future toggle is picked up
 * for free.
 */
export default function useTheme() {
  const [theme, setTheme] = useState(readTheme);

  useEffect(() => {
    const observer = new MutationObserver(() => setTheme(readTheme()));
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["data-theme"],
    });
    return () => observer.disconnect();
  }, []);

  return theme;
}

/** Logo variant that stays legible on the current theme's background. */
export const logoForTheme = (theme) =>
  theme === "light" ? "/mentalist/logo-light.png" : "/mentalist/logo-dark.png";
