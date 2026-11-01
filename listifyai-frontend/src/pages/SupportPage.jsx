import { useState } from "react";
import { PaperAirplaneIcon } from "@heroicons/react/24/outline";
import api from "../lib/api";

const TICKET_TYPES = [
  { label: "Bug Report", value: "bug" },
  { label: "Billing Issue", value: "billing" },
  { label: "Monitoring Issue", value: "monitoring" },
  { label: "Feature Request", value: "feature" },
  { label: "General Question", value: "general" },
];

const INITIAL_FORM = {
  type: "bug",
  subject: "",
  description: "",
};

export default function SupportPage() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
  };

  const resetForm = () => {
    setForm(INITIAL_FORM);
    setSuccess(false);
    setError("");
    setLoading(false);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      await api.post("/api/support/tickets", {
        type: form.type,
        subject: form.subject.trim(),
        description: form.description.trim(),
      });
      setSuccess(true);
    } catch (err) {
      setError(
        err.response?.data?.message ??
          "Could not send your message. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-full rounded-2xl bg-[#0B1020] p-4 text-[#F3F6FF] md:p-6">
        <div className="mx-auto max-w-2xl">
          <div className="rounded-2xl border border-emerald-400/30 bg-emerald-500/10 p-6 shadow-xl shadow-black/20">
            <p className="text-base font-bold text-emerald-200">
              Message sent! We reply within 24 hours.
            </p>
            <button
              type="button"
              onClick={resetForm}
              className="mt-5 rounded-xl bg-[#7C5CFF] px-4 py-2.5 text-sm font-bold text-[#F3F6FF] transition-colors hover:bg-[#6848E8] focus:outline-none focus:ring-2 focus:ring-[#7C5CFF] focus:ring-offset-2 focus:ring-offset-[#0B1020]"
            >
              Send another message
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-full rounded-2xl bg-[#0B1020] p-4 text-[#F3F6FF] md:p-6">
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="text-2xl font-black text-[#F3F6FF]">Support</h1>
          <p className="mt-1 text-sm text-slate-400">
            Send us a message and we will help you get unstuck.
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-2xl border border-white/10 bg-[#131A2A] p-6 shadow-xl shadow-black/20"
        >
          <div className="space-y-5">
            <label className="block">
              <span className="text-xs font-bold uppercase tracking-wide text-slate-400">
                Type
              </span>
              <select
                name="type"
                value={form.type}
                onChange={handleChange}
                className="mt-2 w-full rounded-xl border border-white/10 bg-[#0B1020] px-4 py-3 text-sm font-semibold text-[#F3F6FF] outline-none transition-colors focus:border-[#7C5CFF]"
              >
                {TICKET_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="block">
              <span className="text-xs font-bold uppercase tracking-wide text-slate-400">
                Subject
              </span>
              <input
                name="subject"
                type="text"
                value={form.subject}
                onChange={handleChange}
                maxLength={500}
                required
                className="mt-2 w-full rounded-xl border border-white/10 bg-[#0B1020] px-4 py-3 text-sm font-semibold text-[#F3F6FF] outline-none transition-colors placeholder:text-slate-600 focus:border-[#7C5CFF]"
                placeholder="What can we help with?"
              />
            </label>

            <label className="block">
              <div className="flex items-center justify-between gap-3">
                <span className="text-xs font-bold uppercase tracking-wide text-slate-400">
                  Description
                </span>
                <span className="text-xs font-semibold text-slate-500">
                  {form.description.length} / 2000
                </span>
              </div>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                minLength={20}
                maxLength={2000}
                rows={6}
                required
                className="mt-2 w-full resize-y rounded-xl border border-white/10 bg-[#0B1020] px-4 py-3 text-sm font-medium leading-6 text-[#F3F6FF] outline-none transition-colors placeholder:text-slate-600 focus:border-[#7C5CFF]"
                placeholder="Include what happened, what you expected, and any details that might help us reproduce or understand it."
              />
            </label>
          </div>

          {error && (
            <p className="mt-4 text-sm font-semibold text-rose-300">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#7C5CFF] px-4 py-3 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8] focus:outline-none focus:ring-2 focus:ring-[#7C5CFF] focus:ring-offset-2 focus:ring-offset-[#131A2A] disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
          >
            <PaperAirplaneIcon className="h-5 w-5" />
            {loading ? "Sending..." : "Send Message"}
          </button>
        </form>
      </div>
    </div>
  );
}
