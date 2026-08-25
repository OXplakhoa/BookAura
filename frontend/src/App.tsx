import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
import { LoadingScreen } from "./components/LoadingScreen";
import { AppShell } from "./layouts/AppShell";
import { useLanguage } from "./i18n/language";
import { AuthLayout } from "./layouts/AuthLayout";
import { PublicLayout } from "./layouts/PublicLayout";
import { AccountSettingsPage } from "./pages/AccountSettingsPage";
import { AuraPage } from "./pages/AuraPage";
import { BookDetailPage } from "./pages/BookDetailPage";
import { CatalogPage } from "./pages/CatalogPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { LoansPage } from "./pages/LoansPage";
import { MaintenancePage } from "./pages/MaintenancePage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { OAuthCallbackPage } from "./pages/OAuthCallbackPage";
import { PhoneLoginPage } from "./pages/PhoneLoginPage";

const AdminBooksPage = lazy(() => import("./pages/admin/AdminBooksPage").then((module) => ({ default: module.AdminBooksPage })));
const AdminDashboardPage = lazy(() => import("./pages/admin/AdminDashboardPage").then((module) => ({ default: module.AdminDashboardPage })));
const AdminLoansPage = lazy(() => import("./pages/admin/AdminLoansPage").then((module) => ({ default: module.AdminLoansPage })));
const AdminMembersPage = lazy(() => import("./pages/admin/AdminMembersPage").then((module) => ({ default: module.AdminMembersPage })));
const BookFormPage = lazy(() => import("./pages/admin/BookFormPage").then((module) => ({ default: module.BookFormPage })));
const MaintenanceControlPage = lazy(() => import("./pages/admin/MaintenanceControlPage").then((module) => ({ default: module.MaintenanceControlPage })));
const MemberFormPage = lazy(() => import("./pages/admin/MemberFormPage").then((module) => ({ default: module.MemberFormPage })));
const SmsOutboxPage = lazy(() => import("./pages/admin/SmsOutboxPage").then((module) => ({ default: module.SmsOutboxPage })));
import { RegisterPage } from "./pages/RegisterPage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage";
import { useSystemStatus } from "./system/use-system-status";

export default function App() {
  const { maintenance } = useSystemStatus();
  const { t } = useLanguage();
  if (maintenance) return <MaintenancePage />;

  return (
    <Suspense fallback={<LoadingScreen label={t("loading.workspace")} />}>
    <Routes>
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route path="aura" element={<AuraPage />} />
        <Route path="books/:bookId" element={<BookDetailPage />} />
      </Route>

      <Route element={<AuthLayout />}>
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="oauth/callback" element={<OAuthCallbackPage />} />
        <Route path="phone-login" element={<PhoneLoginPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="app/loans" element={<LoansPage mode="active" />} />
          <Route path="app/history" element={<LoansPage mode="history" />} />
          <Route path="app/account" element={<AccountSettingsPage />} />
          <Route element={<RequireAuth admin />}>
            <Route path="admin" element={<AdminDashboardPage />} />
            <Route path="admin/books" element={<AdminBooksPage />} />
            <Route path="admin/books/new" element={<BookFormPage />} />
            <Route path="admin/books/:bookId/edit" element={<BookFormPage />} />
            <Route path="admin/members" element={<AdminMembersPage />} />
            <Route path="admin/members/new" element={<MemberFormPage />} />
            <Route path="admin/members/:memberId/edit" element={<MemberFormPage />} />
            <Route path="admin/loans" element={<AdminLoansPage />} />
            <Route path="admin/maintenance" element={<MaintenanceControlPage />} />
            <Route path="admin/sms-outbox" element={<SmsOutboxPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="forbidden" element={<ForbiddenPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
    </Suspense>
  );
}
