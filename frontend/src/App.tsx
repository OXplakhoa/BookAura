import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
import { LoadingScreen } from "./components/LoadingScreen";
import { AppShell } from "./layouts/AppShell";
import { AuthLayout } from "./layouts/AuthLayout";
import { PublicLayout } from "./layouts/PublicLayout";
import { BookDetailPage } from "./pages/BookDetailPage";
import { CatalogPage } from "./pages/CatalogPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { LoansPage } from "./pages/LoansPage";
import { MaintenancePage } from "./pages/MaintenancePage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { OAuthCallbackPage } from "./pages/OAuthCallbackPage";

const AdminBooksPage = lazy(() => import("./pages/admin/AdminBooksPage").then((module) => ({ default: module.AdminBooksPage })));
const AdminDashboardPage = lazy(() => import("./pages/admin/AdminDashboardPage").then((module) => ({ default: module.AdminDashboardPage })));
const AdminLoansPage = lazy(() => import("./pages/admin/AdminLoansPage").then((module) => ({ default: module.AdminLoansPage })));
const AdminMembersPage = lazy(() => import("./pages/admin/AdminMembersPage").then((module) => ({ default: module.AdminMembersPage })));
const BookFormPage = lazy(() => import("./pages/admin/BookFormPage").then((module) => ({ default: module.BookFormPage })));
const MaintenanceControlPage = lazy(() => import("./pages/admin/MaintenanceControlPage").then((module) => ({ default: module.MaintenanceControlPage })));
const MemberFormPage = lazy(() => import("./pages/admin/MemberFormPage").then((module) => ({ default: module.MemberFormPage })));
import { RegisterPage } from "./pages/RegisterPage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage";
import { useSystemStatus } from "./system/use-system-status";

export default function App() {
  const { maintenance } = useSystemStatus();
  if (maintenance) return <MaintenancePage />;

  return (
    <Suspense fallback={<LoadingScreen label="Loading workspace" />}>
    <Routes>
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route path="books/:bookId" element={<BookDetailPage />} />
      </Route>

      <Route element={<AuthLayout />}>
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="oauth/callback" element={<OAuthCallbackPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="app/loans" element={<LoansPage mode="active" />} />
          <Route path="app/history" element={<LoansPage mode="history" />} />
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
          </Route>
        </Route>
      </Route>

      <Route path="forbidden" element={<ForbiddenPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
    </Suspense>
  );
}
