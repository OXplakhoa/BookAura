import { Navigate, Outlet, useLocation } from "react-router-dom";
import { LoadingScreen } from "../components/LoadingScreen";
import { useAuth } from "./use-auth";

export function RequireAuth({ admin = false }: { admin?: boolean }) {
  const auth = useAuth();
  const location = useLocation();

  if (!auth.ready) {
    return <LoadingScreen label="Restoring your library session" />;
  }
  if (!auth.authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (admin && !auth.isAdmin) {
    return <Navigate to="/forbidden" replace />;
  }
  return <Outlet />;
}
