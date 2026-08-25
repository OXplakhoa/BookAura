import { Component, lazy, Suspense, useState, type ErrorInfo, type ReactNode } from "react";
import { List as ListIcon, Sparkles } from "lucide-react";
import type { AuraRecommendation } from "./aura-api";
import { chooseAuraViewMode, supportsAuraShelf, usePrefersReducedMotion, type AuraViewMode } from "./aura-view";

const LazyAuraShelf3D = lazy(() => import("./AuraShelf3D").then(({ AuraShelf3D }) => ({ default: AuraShelf3D })));

interface AuraShelfBoundaryProps {
  children: ReactNode;
  fallback: ReactNode;
}

interface AuraShelfBoundaryState {
  hasError: boolean;
}

class ShelfErrorBoundary extends Component<AuraShelfBoundaryProps, AuraShelfBoundaryState> {
  state: AuraShelfBoundaryState = { hasError: false };

  static getDerivedStateFromError(): AuraShelfBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(_error: unknown, _errorInfo: ErrorInfo): void {
    // The list fallback is intentionally silent here: it is the reliable reading path.
  }

  render(): ReactNode {
    return this.state.hasError ? this.props.fallback : this.props.children;
  }
}

export function AuraResultView({ books, fallback }: { books: AuraRecommendation[]; fallback: ReactNode }) {
  const reducedMotion = usePrefersReducedMotion();
  const shelfSupported = supportsAuraShelf();
  const [requestedMode, setRequestedMode] = useState<AuraViewMode>("shelf");
  const preferredMode = chooseAuraViewMode({ reducedMotion, supports3d: shelfSupported });
  const mode = preferredMode === "cards" ? "cards" : requestedMode;

  const loadingFallback = (
    <div aria-busy="true">
      <p className="mb-5 flex items-center gap-2 text-sm font-semibold text-muted" role="status">
        <Sparkles size={16} className="text-primary" aria-hidden="true" /> Preparing the shelf — your list stays available while it loads.
      </p>
      {fallback}
    </div>
  );
  const unavailableFallback = (
    <div>
      <p className="mb-5 flex items-center gap-2 text-sm font-semibold text-muted" role="status">
        <ListIcon size={16} className="text-primary" aria-hidden="true" /> The accessible list is shown because the 3D shelf could not render.
      </p>
      {fallback}
    </div>
  );

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="eyebrow">Your ranked shelf</p>
          <h2 className="mt-2 font-display text-3xl font-bold">Six possible next chapters</h2>
        </div>
        <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Choose recommendation view">
          {shelfSupported && (
            <button
              type="button"
              className={`button min-h-11 border ${mode === "shelf" ? "border-primary bg-primary text-white" : "border-line bg-surface text-ink hover:bg-canvas"}`}
              aria-pressed={mode === "shelf"}
              disabled={reducedMotion}
              title={reducedMotion ? "3D shelf disabled because reduced motion is enabled" : undefined}
              onClick={() => setRequestedMode("shelf")}
            >
              <Sparkles size={16} aria-hidden="true" /> 3D shelf
            </button>
          )}
          <button
            type="button"
            className={`button min-h-11 border ${mode === "cards" ? "border-primary bg-primary text-white" : "border-line bg-surface text-ink hover:bg-canvas"}`}
            aria-pressed={mode === "cards"}
            onClick={() => setRequestedMode("cards")}
          >
            <ListIcon size={16} aria-hidden="true" /> List view
          </button>
        </div>
      </div>

      {reducedMotion && <p className="mb-5 border-l-4 border-primary bg-primary/5 p-3 text-sm text-muted" role="status">Reduced motion is enabled, so BookAura is showing the calm list view.</p>}
      {!reducedMotion && !shelfSupported && <p className="mb-5 border-l-4 border-primary bg-primary/5 p-3 text-sm text-muted" role="status">This browser does not support the 3D shelf, so the accessible list view is ready below.</p>}

      {mode === "shelf" ? (
        <ShelfErrorBoundary key={books.map(({ bookId }) => bookId).join(",")} fallback={unavailableFallback}>
          <Suspense fallback={loadingFallback}>
            <LazyAuraShelf3D books={books} />
          </Suspense>
        </ShelfErrorBoundary>
      ) : fallback}
    </div>
  );
}
