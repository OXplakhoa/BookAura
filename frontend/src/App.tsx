import { Route, Routes } from "react-router-dom";

function LandingPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-stone-950 text-stone-100">
      <h1 className="text-4xl font-bold tracking-tight">BookAura</h1>
      <p className="mt-3 text-stone-400">
        Library management — browse, borrow, return. Frontend screens arrive in the core-frontend slice.
      </p>
    </main>
  );
}

function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <p className="text-lg">404 — page not found</p>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
