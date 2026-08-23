import { Outlet } from "react-router-dom";
import { Brand } from "../components/Brand";

export function AuthLayout() {
  return (
    <main className="grid min-h-dvh bg-canvas lg:grid-cols-[minmax(0,1fr)_minmax(440px,0.72fr)]">
      <section className="relative hidden overflow-hidden bg-ink p-12 text-stone-100 lg:flex lg:flex-col lg:justify-between">
        <Brand inverted />
        <div className="max-w-xl">
          <p className="eyebrow text-amber-300">Your reading, organized</p>
          <h1 className="mt-5 font-display text-5xl font-bold leading-[1.08]">Every borrowed story deserves a clear path home.</h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-stone-300">Find available books, borrow with confidence, and keep your complete reading history in one calm space.</p>
        </div>
        <div className="flex gap-3" aria-hidden="true">
          {[56, 72, 48, 88, 64, 78].map((height, index) => <span key={height} className={`w-10 rounded-t-sm border border-stone-600 ${index === 3 ? "bg-primary" : "bg-stone-800"}`} style={{ height }} />)}
        </div>
      </section>
      <section className="flex min-h-dvh items-center justify-center px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden"><Brand /></div>
          <Outlet />
        </div>
      </section>
    </main>
  );
}
