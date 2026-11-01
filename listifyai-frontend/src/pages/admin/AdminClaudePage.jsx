import { useEffect, useState } from "react";
import api from "../../lib/api";

export default function AdminClaudePage() {
  const [data, setData] = useState(null);
  useEffect(() => {
    api.get("/api/admin/claude-usage").then((res) => setData(res.data));
  }, []);
  if (!data)
    return <div className="h-64 animate-pulse rounded-2xl bg-[#131A2A]" />;
  const total = Math.max(
    1,
    Object.values(data.callsByType ?? {}).reduce((a, b) => a + b, 0),
  );
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black">Claude Usage</h1>
        <p className="text-sm text-slate-400">
          Token consumption and estimated cost.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {[
          ["Calls Today", data.totalCallsToday],
          ["Calls This Month", data.totalCallsThisMonth],
          [
            "Estimated Cost This Month",
            `$${data.estimatedCostThisMonth.toFixed(2)}`,
          ],
        ].map(([l, v]) => (
          <div key={l} className="rounded-2xl bg-[#131A2A] p-5">
            <p className="text-sm text-slate-400">{l}</p>
            <p className="mt-2 text-3xl font-black">{v}</p>
          </div>
        ))}
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl bg-[#131A2A] p-5">
          <p className="text-sm text-slate-400">Today Tokens</p>
          <p className="mt-2 text-3xl font-black">{data.totalTokensToday}</p>
        </div>
        <div className="rounded-2xl bg-[#131A2A] p-5">
          <p className="text-sm text-slate-400">Month Tokens</p>
          <p className="mt-2 text-3xl font-black">
            {data.totalTokensThisMonth}
          </p>
        </div>
      </div>
      <div className="rounded-2xl bg-[#131A2A] p-5">
        <h2 className="font-black">Call Type Breakdown</h2>
        {Object.entries(data.callsByType ?? {}).map(([type, count]) => (
          <div key={type} className="mt-3">
            <div className="mb-1 flex justify-between text-sm">
              <span>{type}</span>
              <span>{count}</span>
            </div>
            <div className="h-2 rounded-full bg-slate-800">
              <div
                className="h-2 rounded-full bg-[#7C5CFF]"
                style={{ width: `${(count / total) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>
      <div className="rounded-2xl bg-[#131A2A] p-5">
        <h2 className="font-black">Top Users by Tokens</h2>
        <table className="mt-3 w-full text-left text-sm">
          <tbody>
            {data.topUsersByTokens.map((u) => (
              <tr key={u.userId} className="border-t border-white/10">
                <td className="py-3">{u.email}</td>
                <td>{u.totalTokens}</td>
                <td>${((u.totalTokens * 9) / 1000000).toFixed(4)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
