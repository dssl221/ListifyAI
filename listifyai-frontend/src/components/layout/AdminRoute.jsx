import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import LoadingSpinner from "../ui/LoadingSpinner";

export default function AdminRoute({ children }) {
  const { user, profile, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-[#0B1020]">
        <LoadingSpinner label="Checking access..." />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/tw-secure-admin-access" replace />;
  }
  if (profile?.role !== "admin") return <Navigate to="/app/generate" replace />;
  return children;
}
