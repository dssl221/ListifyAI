import { useEffect, useState } from "react";
import api from "../../lib/api";

export default function AdminAuditPage() {
  const [data, setData] = useState(null);
  const [open, setOpen] = useState(null);
  useEffect(() => {
    api
      .get("/api/admin/audit-logs", { params: { page: 0, size: 50 } })
      .then((res) => setData(res.data));
  }, []);
  if (!data)
    return <div className="h-64 animate-pulse rounded-2xl bg-[#131A2A]" />;
  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-black">Audit Logs</h1>
        <p className="text-sm text-slate-400">
          Admin actions and target records.
        </p>
      </div>
      <div className="overflow-hidden rounded-2xl bg-[#131A2A]">
        <table className="w-full text-left text-sm">
          <thead className="text-slate-400">
            <tr>
              <th className="p-4">Admin Email</th>
              <th>Action</th>
              <th>Target</th>
              <th>IP Address</th>
              <th>Time</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map((log) => (
              <>
                <tr
                  key={log.id}
                  onClick={() => setOpen(open === log.id ? null : log.id)}
                  className="cursor-pointer border-t border-white/10"
                >
                  <td className="p-4">{log.adminEmail}</td>
                  <td>{log.action}</td>
                  <td>{log.targetType}</td>
                  <td>{log.ipAddress}</td>
                  <td>{new Date(log.createdAt).toLocaleString()}</td>
                </tr>
                {open === log.id && (
                  <tr>
                    <td colSpan="5" className="bg-[#0B1020] p-4">
                      <pre className="overflow-auto text-xs text-slate-300">
                        {JSON.stringify(
                          JSON.parse(log.details || "{}"),
                          null,
                          2,
                        )}
                      </pre>
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
