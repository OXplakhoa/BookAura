import { AlertTriangle, Inbox, LoaderCircle, RefreshCw } from "lucide-react";

export function QueryError({ message = "We could not load this shelf.", retry }: { message?: string; retry: () => void }) {
  return <div className="border border-red-200 bg-red-50 p-7 text-red-950" role="alert"><AlertTriangle size={28} className="text-danger" /><h2 className="mt-4 font-display text-2xl font-bold">Something interrupted the request</h2><p className="mt-2 text-sm leading-6">{message}</p><button type="button" className="button mt-5 border border-red-300 bg-white text-red-950 hover:bg-red-100" onClick={retry}><RefreshCw size={17} />Try again</button></div>;
}

export function EmptyState({ title, message }: { title: string; message: string }) {
  return <div className="border border-dashed border-line bg-surface p-10 text-center"><Inbox className="mx-auto text-muted" size={36} /><h2 className="mt-5 font-display text-2xl font-bold">{title}</h2><p className="mx-auto mt-2 max-w-lg leading-7 text-muted">{message}</p></div>;
}

export function InlineLoading({ label }: { label: string }) {
  return <div className="flex min-h-52 items-center justify-center gap-3 text-sm font-semibold text-muted" role="status"><LoaderCircle className="animate-spin text-primary motion-reduce:animate-none" size={22} />{label}</div>;
}
