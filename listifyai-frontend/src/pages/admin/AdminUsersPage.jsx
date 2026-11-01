import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../lib/api";

export default function AdminUsersPage() {
  const [search, setSearch] = useState("");
  const [plan, setPlan] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    const timer = setTimeout(async () => {
      setError("");
      try {
        const res = await api.get("/api/admin/users", {
          params: { search, plan, page, size: 20 },
        });
        setData(res.data);
      } catch {
        setError("Could not load users.");
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [search, plan, page]);
  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-black">Users</h1>
        <p className="text-sm text-slate-400">
          Search accounts and manage plans.
        </p>
      </div>
      <div className="flex flex-col gap-3 md:flex-row">
        <input
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          placeholder="Search email or name"
          className="rounded-xl border border-white/10 bg-[#131A2A] px-4 py-2"
        />
        <select
          value={plan}
          onChange={(e) => {
            setPlan(e.target.value);
            setPage(0);
          }}
          className="rounded-xl border border-white/10 bg-[#131A2A] px-4 py-2"
        >
          <option value="">All plans</option>
          <option value="free">Free</option>
          <option value="starter">Starter</option>
          <option value="growth">Growth</option>
          <option value="pro">Pro</option>
        </select>
      </div>
      {error && (
        <button
          onClick={() => setPage(0)}
          className="rounded-xl bg-[#7C5CFF] px-4 py-2 font-bold"
        >
          Retry
        </button>
      )}
      {!data ? (
        <div className="h-64 animate-pulse rounded-2xl bg-[#131A2A]" />
      ) : (
        <div className="overflow-hidden rounded-2xl bg-[#131A2A]">
          <div className="p-4 text-sm text-slate-400">
            {data.totalElements} users
          </div>
          <table className="w-full text-left text-sm">
            <thead className="text-slate-400">
              <tr>
                <th className="p-4">Email</th>
                <th>Plan</th>
                <th>Role</th>
                <th>Listings Used</th>
                <th>Joined</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((u) => (
                <tr key={u.id} className="border-t border-white/10">
                  <td className="p-4">{u.email}</td>
                  <td className="capitalize">{u.plan}</td>
                  <td className="capitalize">{u.role}</td>
                  <td>{u.listingsUsedThisMonth}</td>
                  <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                  <td>
                    <Link
                      to={`/admin/users/${u.id}`}
                      className="text-[#7C5CFF] underline"
                    >
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex gap-2 p-4">
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              className="rounded-lg bg-white/5 px-3 py-1 disabled:opacity-40"
            >
              Previous
            </button>
            <button
              disabled={data.last}
              onClick={() => setPage(page + 1)}
              className="rounded-lg bg-white/5 px-3 py-1 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
