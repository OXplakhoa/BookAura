import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { LanguageSwitcher } from "../components/LanguageSwitcher";
import { LanguageProvider, useLanguage } from "./language";

function Probe() {
  const { t } = useLanguage();
  return <p>{t("nav.browse")}</p>;
}

describe("BookAura language preference", () => {
  const storage = new Map<string, string>();
  beforeEach(() => {
    storage.clear();
    Object.defineProperty(window, "localStorage", {
      configurable: true,
      value: {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => storage.set(key, value),
        removeItem: (key: string) => storage.delete(key),
        clear: () => storage.clear(),
      },
    });
  });
  afterEach(() => storage.clear());

  it("starts in Vietnamese and switches to English", async () => {
    const user = userEvent.setup();
    render(<LanguageProvider><LanguageSwitcher /><Probe /></LanguageProvider>);

    expect(screen.getByText("Duyệt sách")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "EN" }));
    expect(screen.getByText("Browse books")).toBeInTheDocument();
    expect(window.localStorage.getItem("bookaura-language")).toBe("en");
  });
});
