import { BookOpen, ChevronRight, Library, Settings2, Users } from "lucide-react";
import { Link } from "react-router-dom";
import { useLanguage } from "../../i18n/language";

const sections = [
  { to: "/admin/books", icon: BookOpen, titleKey: "admin.books", textKey: "admin.booksText" },
  { to: "/admin/members", icon: Users, titleKey: "admin.members", textKey: "admin.membersText" },
  { to: "/admin/loans", icon: Library, titleKey: "admin.loans", textKey: "admin.loansText" },
  { to: "/admin/maintenance", icon: Settings2, titleKey: "admin.operations", textKey: "admin.operationsText" },
  ...(import.meta.env.DEV ? [{ to: "/admin/sms-outbox", icon: Settings2, titleKey: "admin.fakeOutbox", textKey: "admin.fakeOutboxText" }] : []),
];

export function AdminDashboardPage() {
  const { t } = useLanguage();
  return <section><p className="eyebrow">{t("admin.eyebrow")}</p><h1 className="mt-3 font-display text-4xl font-bold">{t("admin.dashboardTitle")}</h1><p className="mt-3 max-w-2xl leading-7 text-muted">{t("admin.dashboardText")}</p><div className="mt-9 grid gap-5 md:grid-cols-2">{sections.map(({ to, icon: Icon, titleKey, textKey }) => <Link key={to} to={to} className="group border border-line bg-surface p-6 transition-[transform,box-shadow] hover:-translate-y-1 hover:shadow-card"><div className="flex items-start justify-between gap-5"><span className="grid size-12 place-items-center rounded-xl bg-primary/10 text-primary"><Icon size={23} /></span><ChevronRight className="text-muted transition-transform group-hover:translate-x-1" size={20} /></div><h2 className="mt-7 font-display text-2xl font-bold">{t(titleKey)}</h2><p className="mt-2 leading-7 text-muted">{t(textKey)}</p></Link>)}</div></section>;
}
