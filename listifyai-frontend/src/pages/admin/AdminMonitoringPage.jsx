import { useEffect, useState } from "react";
import api from "../../lib/api";

function Stat({ label, value }) {
  return (
    <div className="rounded-2xl bg-[#131A2A] p-5">
      <p className="text-sm text-slate-400">{label}</p>
      <p className="mt-2 text-3xl font-black">{value}</p>
    </div>
  );
}

export default function AdminMonitoringPage() {
  const [data, setData] = useState(null);
  const load = async () => {
    const res = await api.get("/api/admin/monitoring/overview");
    setData(res.data);
  };
  useEffect(() => {
    load();
  }, []);
  if (!data)
    return <div className="h-64 animate-pulse rounded-2xl bg-[#131A2A]" />;
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black">Monitoring</h1>
        <p className="text-sm text-slate-400">
          Operational view for listing monitoring.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-4">
        <Stat label="Total Monitored" value={data.totalMonitoredListings} />
        <Stat label="Active" value={data.activeListings} />
        <Stat label="Pending Jobs" value={data.pendingJobs} />
        <Stat label="Failed Today" value={data.failedJobsToday} />
      </div>
      <div className="rounded-2xl bg-[#131A2A] p-5">
        <p className="text-sm text-slate-400">Success Rate Today</p>
        <p
          className={`mt-2 text-4xl font-black ${data.successRateToday >= 95 ? "text-emerald-300" : data.successRateToday >= 80 ? "text-amber-300" : "text-rose-300"}`}
        >
          {data.successRateToday.toFixed(1)}%
        </p>
      </div>
      <div className="rounded-2xl bg-[#131A2A] p-5 text-sm text-slate-400">
        Failed jobs and active listing controls are available through retry,
        pause, and resume endpoints as rows are exposed by the monitoring queue.
      </div>
    </div>
  );
}
