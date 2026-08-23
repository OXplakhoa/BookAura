import { Construction } from "lucide-react";

export function PlaceholderPage({ title, description }: { title: string; description: string }) {
  return <section><p className="eyebrow">BookAura workspace</p><h1 className="mt-3 font-display text-4xl font-bold">{title}</h1><div className="mt-8 flex max-w-2xl gap-4 border border-line bg-surface p-6"><Construction className="shrink-0 text-primary" size={26} /><p className="leading-7 text-muted">{description}</p></div></section>;
}
