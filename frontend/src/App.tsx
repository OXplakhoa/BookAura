import { Route, Routes } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
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
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { RegisterPage } from "./pages/RegisterPage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage";
import { useSystemStatus } from "./system/use-system-status";

export default function App() {
  const { maintenance } = useSystemStatus();
  if (maintenance) return <MaintenancePage />;

  return (
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
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="app/loans" element={<LoansPage mode="active" />} />
          <Route path="app/history" element={<LoansPage mode="history" />} />
          <Route element={<RequireAuth admin />}>
            <Route path="admin" element={<PlaceholderPage title="Admin workspace" description="Books, members, loans, CSV import and maintenance controls will be assembled here." />} />
          </Route>
        </Route>
      </Route>

      <Route path="forbidden" element={<ForbiddenPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
