import { useEffect, useState, useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../../lib/api";

export default function AdminUserDetailPage() {
  const { id } = useParams();
  const [user, setUser] = useState(null);
  const [plan, setPlan] = useState("free");
  const [message, setMessage] = useState("");
  const load = useCallback(async () => {
    const { data } = await api.get(`/api/admin/users/${id}`);
    setUser(data);
    setPlan(data.plan);
  }, [id]);
  useEffect(() => {
    load();
  }, [load]);
  const action = async (fn, text) => {
    try {
      await fn();
      setMessage(text);
      await load();
    } catch {
      setMessage("Action failed.");
    }
  };
  if (!user)
    return <div className="h-64 animate-pulse rounded-2xl bg-[#131A2A]" />;
  return (
    <div className="space-y-6">
      <Link to="/admin/users" className="text-sm text-[#7C5CFF] underline">
        Back to users
      </Link>
      <div>
        <h1 className="text-2xl font-black">{user.email}</h1>
        <p className="text-sm text-slate-400">
          Plan {user.plan} / Role {user.role} / Joined{" "}
          {new Date(user.createdAt).toLocaleDateString()}
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-4">
        {[
          ["Total Listings", user.totalListingsGenerated],
          ["Bulk Jobs", user.totalBulkJobs],
          ["Monitored Listings", user.totalMonitoredListings],
          ["Audits", user.totalAudits],
        ].map(([l, v]) => (
          <div key={l} className="rounded-2xl bg-[#131A2A] p-5">
            <p className="text-sm text-slate-400">{l}</p>
            <p className="mt-2 text-3xl font-black">{v}</p>
          </div>
        ))}
      </div>
      <div className="rounded-2xl bg-[#131A2A] p-5">
        <h2 className="font-black">Actions</h2>
        <div className="mt-4 flex flex-wrap gap-3">
          <select
            value={plan}
            onChange={(e) => setPlan(e.target.value)}
            className="rounded-xl bg-[#0B1020] px-3 py-2"
          >
            <option value="free">Free</option>
            <option value="starter">Starter</option>
            <option value="growth">Growth</option>
            <option value="pro">Pro</option>
          </select>
          <button
            onClick={() =>
              action(
                () => api.put(`/api/admin/users/${id}/plan`, { plan }),
                "Plan updated.",
              )
            }
            className="rounded-xl bg-[#7C5CFF] px-4 py-2 font-bold"
          >
            Save
          </button>
          <button
            onClick={() =>
              window.confirm("Are you sure? This will prevent login.") &&
              action(
                () => api.post(`/api/admin/users/${id}/suspend`),
                "User suspended.",
              )
            }
            className="rounded-xl bg-rose-600 px-4 py-2 font-bold"
          >
            Suspend User
          </button>
          {user.role === "suspended" && (
            <button
              onClick={() =>
                action(
                  () => api.post(`/api/admin/users/${id}/reactivate`),
                  "User reactivated.",
                )
              }
              className="rounded-xl bg-emerald-600 px-4 py-2 font-bold"
            >
              Reactivate User
            </button>
          )}
          <button
            onClick={() =>
              window.confirm("Grant admin access to this user?") &&
              action(
                () => api.post(`/api/admin/users/${id}/make-admin`),
                "User promoted.",
              )
            }
            className="rounded-xl bg-[#7C5CFF] px-4 py-2 font-bold"
          >
            Make Admin
          </button>
        </div>
        {message && <p className="mt-4 text-sm text-slate-300">{message}</p>}
      </div>
    </div>
  );
}
