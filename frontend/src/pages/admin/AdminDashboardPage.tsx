import { BookOpen, ChevronRight, Library, Settings2, Users } from "lucide-react";
import { Link } from "react-router-dom";

const sections = [
  { to: "/admin/books", icon: BookOpen, title: "Books", text: "Create, edit, archive, search and import the collection." },
  { to: "/admin/members", icon: Users, title: "Members", text: "Search profiles, onboard members and disable accounts safely." },
  { to: "/admin/loans", icon: Library, title: "Loans", text: "Review current activity and return a loan on a member's behalf." },
  { to: "/admin/maintenance", icon: Settings2, title: "Operations", text: "Control maintenance mode without locking out health checks." },
  ...(import.meta.env.DEV ? [{ to: "/admin/sms-outbox", icon: Settings2, title: "Fake SMS outbox", text: "Read local in-memory phone OTPs without writing secrets to logs." }] : []),
];

export function AdminDashboardPage() {
  return <section><p className="eyebrow">Administration</p><h1 className="mt-3 font-display text-4xl font-bold">Library operations</h1><p className="mt-3 max-w-2xl leading-7 text-muted">Four focused workspaces for the tasks needed in a reliable BookAura demo.</p><div className="mt-9 grid gap-5 md:grid-cols-2">{sections.map(({ to, icon: Icon, title, text }) => <Link key={to} to={to} className="group border border-line bg-surface p-6 transition-[transform,box-shadow] hover:-translate-y-1 hover:shadow-card"><div className="flex items-start justify-between gap-5"><span className="grid size-12 place-items-center rounded-xl bg-primary/10 text-primary"><Icon size={23} /></span><ChevronRight className="text-muted transition-transform group-hover:translate-x-1" size={20} /></div><h2 className="mt-7 font-display text-2xl font-bold">{title}</h2><p className="mt-2 leading-7 text-muted">{text}</p></Link>)}</div></section>;
}
