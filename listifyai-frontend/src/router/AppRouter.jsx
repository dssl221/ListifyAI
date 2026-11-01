import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AdminRoute from "../components/layout/AdminRoute";
import LoadingSpinner from "../components/ui/LoadingSpinner";

const AppLayout = lazy(() => import("../components/layout/AppLayout"));
const AdminLayout = lazy(() => import("../components/layout/AdminLayout"));
const LandingPage = lazy(() => import("../pages/LandingPage"));
const LoginPage = lazy(() => import("../pages/LoginPage"));
const AuthCallbackPage = lazy(() => import("../pages/AuthCallbackPage"));
const AdminLoginPage = lazy(() => import("../pages/admin/AdminLoginPage"));
const PricingPage = lazy(() => import("../pages/PricingPage"));
const ReviewPage = lazy(() => import("../pages/ReviewPage"));
const DashboardPage = lazy(() => import("../pages/DashboardPage"));
const EtsyAuditPage = lazy(() => import("../pages/EtsyAuditPage"));
const MonitoringPage = lazy(() => import("../pages/MonitoringPage"));
const ShopHealthPage = lazy(() => import("../pages/ShopHealthPage"));
const GeneratorPage = lazy(() => import("../pages/GeneratorPage"));
const BulkPage = lazy(() => import("../pages/BulkPage"));
const HistoryPage = lazy(() => import("../pages/HistoryPage"));
const SettingsPage = lazy(() => import("../pages/SettingsPage"));
const SupportPage = lazy(() => import("../pages/SupportPage"));
const AdminDashboardPage = lazy(
  () => import("../pages/admin/AdminDashboardPage"),
);
const AdminUsersPage = lazy(() => import("../pages/admin/AdminUsersPage"));
const AdminUserDetailPage = lazy(
  () => import("../pages/admin/AdminUserDetailPage"),
);
const AdminTicketsPage = lazy(() => import("../pages/admin/AdminTicketsPage"));
const AdminMonitoringPage = lazy(
  () => import("../pages/admin/AdminMonitoringPage"),
);
const AdminClaudePage = lazy(() => import("../pages/admin/AdminClaudePage"));
const AdminAuditPage = lazy(() => import("../pages/admin/AdminAuditPage"));

function RouteFallback() {
  return (
    <div className="flex h-screen items-center justify-center">
      <LoadingSpinner label="Loading..." />
    </div>
  );
}

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <RouteFallback />;
  }

  return user ? children : <Navigate to="/login" replace />;
}

function ProtectedRedirect({ to }) {
  return (
    <ProtectedRoute>
      <Navigate to={to} replace />
    </ProtectedRoute>
  );
}

function FallbackRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <RouteFallback />;
  }

  return <Navigate to={user ? "/app/generate" : "/"} replace />;
}

export default function AppRouter() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        {/* Public */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route path="/tw-secure-admin-access" element={<AdminLoginPage />} />
        <Route path="/pricing" element={<PricingPage />} />
        <Route path="/review" element={<ReviewPage />} />

        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminLayout />
            </AdminRoute>
          }
        >
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboardPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="users/:id" element={<AdminUserDetailPage />} />
          <Route path="tickets" element={<AdminTicketsPage />} />
          <Route path="monitoring" element={<AdminMonitoringPage />} />
          <Route path="claude" element={<AdminClaudePage />} />
          <Route path="audit" element={<AdminAuditPage />} />
        </Route>

        <Route
          path="/dashboard"
          element={<ProtectedRedirect to="/app/dashboard" />}
        />
        <Route
          path="/generate"
          element={<ProtectedRedirect to="/app/generate" />}
        />
        <Route
          path="/generator"
          element={<ProtectedRedirect to="/app/generate" />}
        />
        <Route path="/audit" element={<ProtectedRedirect to="/app/audit" />} />
        <Route
          path="/shop-health"
          element={<ProtectedRedirect to="/app/shop-health" />}
        />
        <Route path="/bulk" element={<ProtectedRedirect to="/app/bulk" />} />
        <Route
          path="/history"
          element={<ProtectedRedirect to="/app/history" />}
        />
        <Route
          path="/settings"
          element={<ProtectedRedirect to="/app/settings" />}
        />

        {/* Protected - nested under AppLayout */}
        <Route
          path="/app"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="generate" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="audit" element={<EtsyAuditPage />} />
          <Route path="monitoring" element={<MonitoringPage />} />
          <Route path="shop-health" element={<ShopHealthPage />} />
          <Route path="generate" element={<GeneratorPage />} />
          <Route path="bulk" element={<BulkPage />} />
          <Route path="history" element={<HistoryPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="support" element={<SupportPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<FallbackRoute />} />
      </Routes>
    </Suspense>
  );
}
