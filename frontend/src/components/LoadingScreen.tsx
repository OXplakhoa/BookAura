import { LoaderCircle } from "lucide-react";

export function LoadingScreen({ label }: { label: string }) {
  return (
    <main className="grid min-h-dvh place-items-center bg-canvas px-6">
      <div className="text-center" role="status" aria-live="polite">
        <LoaderCircle className="mx-auto animate-spin text-primary motion-reduce:animate-none" size={30} />
        <p className="mt-4 text-sm font-medium text-muted">{label}</p>
      </div>
    </main>
  );
}
