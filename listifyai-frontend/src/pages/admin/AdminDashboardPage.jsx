import { useEffect, useState } from "react";
import api from "../../lib/api";

function Stat({ label, value }) {
  return (
    <div className="rounded-2xl bg-[#131A2A] p-5">
      <p className="text-sm text-slate-400">{label}</p>
      <p className="mt-2 text-3xl font-black text-white">{value}</p>
    </div>
  );
}

function Bars({ title, rows = [] }) {
  const max = Math.max(1, ...rows.map((row) => row.count));
  return (
    <div className="rounded-2xl bg-[#131A2A] p-5">
      <h2 className="font-black">{title}</h2>
      <div className="mt-4 flex h-36 items-end gap-1">
        {rows.map((row) => (
          <div
            key={row.date}
            className="flex flex-1 flex-col items-center gap-2"
          >
            <div
              className="w-full rounded-t bg-[#7C5CFF]"
              style={{ height: `${Math.max(4, (row.count / max) * 120)}px` }}
            />
            <span className="hidden text-[10px] text-slate-500 sm:block">
              {row.date.slice(5)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function AdminDashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const load = async () => {
    setError("");
    try {
      const res = await api.get("/api/admin/dashboard");
      setData(res.data);
    } catch {
      setError("Could not load dashboard.");
    }
  };
  useEffect(() => {
    load();
  }, []);
  if (error)
    return (
      <button
        onClick={load}
        className="rounded-xl bg-[#7C5CFF] px-4 py-2 font-bold"
      >
        Retry
      </button>
    );
  if (!data)
    return <div className="h-40 animate-pulse rounded-2xl bg-[#131A2A]" />;
  const planTotal = Math.max(
    1,
    data.freeUsers + data.starterUsers + data.growthUsers + data.proUsers,
  );
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black">Dashboard</h1>
        <p className="text-sm text-slate-400">
          TagWing business health at a glance.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-4">
        <Stat label="Total Users" value={data.totalUsers} />
        <Stat label="Paid Users" value={data.paidUsers} />
        <Stat label="MRR ($)" value={`$${data.mrr?.toFixed?.(0) ?? 0}`} />
        <Stat label="Open Tickets" value={data.openSupportTickets} />
        <Stat label="Total Listings" value={data.totalListingsGenerated} />
        <Stat label="Claude Calls Today" value={data.totalClaudeCallsToday} />
        <Stat
          label="Estimated Cost This Month"
          value={`$${data.estimatedCostThisMonth?.toFixed?.(2) ?? "0.00"}`}
        />
        <Stat label="Active Monitoring" value={data.activeMonitoringJobs} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Bars title="Signups last 30 days" rows={data.last30DaysSignups} />
        <Bars
          title="Listings generated last 30 days"
          rows={data.last30DaysListings}
        />
      </div>
      <div className="rounded-2xl bg-[#131A2A] p-5">
        <h2 className="font-black">Plan Distribution</h2>
        <div className="mt-4 flex h-4 overflow-hidden rounded-full bg-slate-800">
          <div
            className="bg-slate-500"
            style={{ width: `${(data.freeUsers / planTotal) * 100}%` }}
          />
          <div
            className="bg-blue-500"
            style={{ width: `${(data.starterUsers / planTotal) * 100}%` }}
          />
          <div
            className="bg-[#7C5CFF]"
            style={{ width: `${(data.growthUsers / planTotal) * 100}%` }}
          />
          <div
            className="bg-amber-400"
            style={{ width: `${(data.proUsers / planTotal) * 100}%` }}
          />
        </div>
        <p className="mt-3 text-sm text-slate-400">
          Free {data.freeUsers} / Starter {data.starterUsers} / Growth{" "}
          {data.growthUsers} / Pro {data.proUsers}
        </p>
      </div>
    </div>
  );
}
