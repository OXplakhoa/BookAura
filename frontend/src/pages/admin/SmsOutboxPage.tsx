import { useMutation } from "@tanstack/react-query";
import { MessageSquareText, Search } from "lucide-react";
import { useState, type FormEvent } from "react";
import { api } from "../../lib/api";
import { toDisplayError } from "../../lib/api-error";

interface FakeSms { phone: string; code: string; sentAt: string }

export function SmsOutboxPage() {
  const [phone, setPhone] = useState("");
  const lookup = useMutation({ mutationFn: async () => (await api.get<FakeSms>("/admin/dev/sms-outbox/latest", { params: { phone } })).data });
  function submit(event: FormEvent) { event.preventDefault(); lookup.mutate(); }
  return <section><p className="eyebrow">Administration · Local demo only</p><h1 className="mt-3 flex items-center gap-3 font-display text-4xl font-bold"><MessageSquareText className="text-primary" size={34} />Fake SMS outbox</h1><p className="mt-3 max-w-2xl leading-7 text-muted">Reads the latest in-memory OTP for a phone. This backend endpoint exists only in the local profile; codes are never written to logs or PostgreSQL.</p><form onSubmit={submit} className="mt-8 flex max-w-xl flex-col gap-3 border border-line bg-surface p-5 sm:flex-row sm:items-end"><div className="flex-1"><label className="field-label" htmlFor="outbox-phone">Phone</label><input id="outbox-phone" className="field-input" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} required /></div><button type="submit" className="button button-primary"><Search size={17} />Find code</button></form>{lookup.data && <div className="mt-6 max-w-xl border-l-4 border-primary bg-surface p-6" role="status"><p className="text-sm text-muted">Latest code for {lookup.data.phone}</p><p className="mt-3 font-mono text-4xl font-bold tracking-[0.3em] text-ink">{lookup.data.code}</p><p className="mt-3 text-xs text-muted">Captured {new Date(lookup.data.sentAt).toLocaleString()}</p></div>}{lookup.isError && <p className="mt-5 text-sm font-semibold text-danger" role="alert">{toDisplayError(lookup.error).message}</p>}</section>;
}
