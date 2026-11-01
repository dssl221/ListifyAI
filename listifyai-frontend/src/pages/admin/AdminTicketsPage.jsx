import { useEffect, useState, useCallback } from "react";
import api from "../../lib/api";

export default function AdminTicketsPage() {
  const [status, setStatus] = useState("");
  const [data, setData] = useState(null);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    status: "open",
    priority: "normal",
    adminNotes: "",
  });
  const load = useCallback(async () => {
    const res = await api.get("/api/admin/tickets", {
      params: { status, page: 0, size: 20 },
    });
    setData(res.data);
  }, [status]);
  useEffect(() => {
    load();
  }, [load]);
  const save = async () => {
    await api.put(`/api/admin/tickets/${editing.id}`, form);
    setEditing(null);
    await load();
  };
  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-black">Tickets</h1>
        <p className="text-sm text-slate-400">
          Review and update customer support requests.
        </p>
      </div>
      <div className="flex flex-wrap gap-2">
        {["", "open", "in_progress", "resolved", "closed"].map((s) => (
          <button
            key={s}
            onClick={() => setStatus(s)}
            className={`rounded-xl px-3 py-2 text-sm font-bold ${status === s ? "bg-[#7C5CFF]" : "bg-[#131A2A] text-slate-400"}`}
          >
            {s || "All"}
          </button>
        ))}
      </div>
      {!data ? (
        <div className="h-64 animate-pulse rounded-2xl bg-[#131A2A]" />
      ) : (
        <div className="overflow-hidden rounded-2xl bg-[#131A2A]">
          <table className="w-full text-left text-sm">
            <thead className="text-slate-400">
              <tr>
                <th className="p-4">User Email</th>
                <th>Type</th>
                <th>Subject</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((t) => (
                <tr key={t.id} className="border-t border-white/10">
                  <td className="p-4">{t.userEmail}</td>
                  <td>{t.type}</td>
                  <td>{t.subject}</td>
                  <td>{t.priority}</td>
                  <td>{t.status}</td>
                  <td>{new Date(t.createdAt).toLocaleDateString()}</td>
                  <td>
                    <button
                      onClick={() => {
                        setEditing(t);
                        setForm({
                          status: t.status,
                          priority: t.priority,
                          adminNotes: t.adminNotes ?? "",
                        });
                      }}
                      className="text-[#7C5CFF] underline"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {editing && (
        <div className="rounded-2xl bg-[#131A2A] p-5">
          <h2 className="font-black">Edit Ticket</h2>
          <div className="mt-4 grid gap-3 md:grid-cols-3">
            <select
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value })}
              className="rounded-xl bg-[#0B1020] px-3 py-2"
            >
              <option value="open">Open</option>
              <option value="in_progress">In Progress</option>
              <option value="resolved">Resolved</option>
              <option value="closed">Closed</option>
            </select>
            <select
              value={form.priority}
              onChange={(e) => setForm({ ...form, priority: e.target.value })}
              className="rounded-xl bg-[#0B1020] px-3 py-2"
            >
              <option value="low">Low</option>
              <option value="normal">Normal</option>
              <option value="high">High</option>
              <option value="urgent">Urgent</option>
            </select>
            <button
              onClick={save}
              className="rounded-xl bg-[#7C5CFF] px-4 py-2 font-bold"
            >
              Save
            </button>
          </div>
          <textarea
            value={form.adminNotes}
            onChange={(e) => setForm({ ...form, adminNotes: e.target.value })}
            rows={4}
            className="mt-3 w-full rounded-xl bg-[#0B1020] p-3"
          />
        </div>
      )}
    </div>
  );
}
