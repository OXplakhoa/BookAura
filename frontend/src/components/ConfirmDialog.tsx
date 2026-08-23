import { X } from "lucide-react";
import { useEffect, useRef, type ReactNode } from "react";

interface Props {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  pending?: boolean;
  error?: ReactNode;
  onCancel: () => void;
  onConfirm: () => void;
}

export function ConfirmDialog({ open, title, description, confirmLabel, pending = false, error, onCancel, onConfirm }: Props) {
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return undefined;
    cancelRef.current?.focus();
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !pending) onCancel();
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [onCancel, open, pending]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink/60 p-5" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !pending) onCancel(); }}>
      <section className="w-full max-w-md rounded-2xl border border-line bg-surface p-6 shadow-card" role="dialog" aria-modal="true" aria-labelledby="dialog-title" aria-describedby="dialog-description">
        <div className="flex items-start justify-between gap-4"><h2 id="dialog-title" className="font-display text-2xl font-bold">{title}</h2><button type="button" className="icon-button -mr-2 -mt-2" onClick={onCancel} disabled={pending} aria-label="Close dialog"><X size={20} /></button></div>
        <p id="dialog-description" className="mt-3 leading-7 text-muted">{description}</p>
        {error}
        <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button ref={cancelRef} type="button" className="button button-ghost border border-line" onClick={onCancel} disabled={pending}>Cancel</button>
          <button type="button" className="button button-primary" onClick={onConfirm} disabled={pending}>{pending ? "Working…" : confirmLabel}</button>
        </div>
      </section>
    </div>
  );
}
