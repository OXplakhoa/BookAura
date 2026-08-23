/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "#f5f1e8",
        surface: "#fffdf8",
        ink: "#192536",
        muted: "#596577",
        primary: "#0f6b62",
        "primary-dark": "#0a514b",
        accent: "#b57925",
        line: "#d9d2c4",
        danger: "#b42318",
        success: "#16794f",
        "book-coral": "#c9785b",
        "book-navy": "#31455c",
        "book-olive": "#77735f",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["Iowan Old Style", "Palatino Linotype", "Georgia", "serif"],
      },
      boxShadow: {
        card: "0 12px 32px rgba(25, 37, 54, 0.08)",
      },
    },
  },
  plugins: [],
};
