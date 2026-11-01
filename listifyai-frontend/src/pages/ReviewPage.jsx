import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeftIcon, CheckCircleIcon } from "@heroicons/react/24/outline";
import api from "../lib/api";
import LogoMark from "../components/ui/LogoMark";

const INITIAL_FORM = {
  displayName: "",
  sellerRole: "",
  rating: 5,
  body: "",
};

export default function ReviewPage() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState("");

  const update = (key) => (event) => {
    setForm((current) => ({ ...current, [key]: event.target.value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      await api.post("/api/public/reviews", {
        ...form,
        rating: Number(form.rating),
      });
      setSubmitted(true);
      setForm(INITIAL_FORM);
    } catch (err) {
      setError(
        err.response?.data?.message ??
          "Could not submit your review. Please check the form and try again.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_18%_8%,rgba(124,92,255,0.18),transparent_32%),radial-gradient(circle_at_88%_0%,rgba(77,163,255,0.13),transparent_30%),linear-gradient(135deg,#0B1020_0%,#101729_48%,#0B1020_100%)] px-6 py-6 text-[#F3F6FF]">
      <header className="mx-auto flex max-w-5xl items-center justify-between">
        <Link to="/" className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <LogoMark size={22} />
            <span className="text-lg font-black tracking-tight">Tagwing</span>
          </div>
        </Link>
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-sm font-bold text-[#94A3B8] transition-colors hover:text-[#F3F6FF]"
        >
          <ArrowLeftIcon className="h-4 w-4" />
          Back to site
        </Link>
      </header>

      <main className="mx-auto grid min-h-[calc(100vh-88px)] max-w-5xl items-center gap-10 py-12 lg:grid-cols-[.85fr_1.15fr]">
        <section>
          <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
            Share your review
          </p>
          <h1 className="mt-4 text-5xl font-black leading-tight tracking-tight">
            Share how Tagwing supports your listing operation.
          </h1>
          <p className="mt-5 text-lg leading-8 text-[#94A3B8]">
            Tell us how Tagwing improves output quality, publishing efficiency,
            or day-to-day listing management for your team.
          </p>
          <div className="mt-7 space-y-3">
            {[
              "Approved reviews may appear on the public site",
              "You may use your role instead of a shop name",
              "Specific workflow outcomes are the most useful",
            ].map((item) => (
              <div
                key={item}
                className="flex items-start gap-3 text-sm font-semibold text-slate-300"
              >
                <CheckCircleIcon className="mt-0.5 h-5 w-5 flex-shrink-0 text-[#7C5CFF]" />
                {item}
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-3xl border border-white/10 bg-[#131A2A]/92 p-6 shadow-2xl shadow-[#050816]/38 backdrop-blur">
          {submitted ? (
            <div className="rounded-2xl border border-[#7C5CFF]/25 bg-[#1A2235] p-6">
              <p className="text-xl font-black text-[#F3F6FF]">
                Thank you for the review.
              </p>
              <p className="mt-2 text-sm leading-6 text-[#94A3B8]">
                Your feedback has been submitted for review and may appear on
                the public site once approved.
              </p>
              <div className="mt-5 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => setSubmitted(false)}
                  className="rounded-xl bg-[#7C5CFF] px-4 py-2.5 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8]"
                >
                  Add another review
                </button>
                <Link
                  to="/"
                  className="rounded-xl border border-slate-700 px-4 py-2.5 text-sm font-black text-slate-300 transition-colors hover:bg-[#202B42]"
                >
                  View website
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="mb-2 block text-xs font-bold uppercase tracking-wide text-slate-400">
                  Display name
                </label>
                <input
                  required
                  maxLength={120}
                  value={form.displayName}
                  onChange={update("displayName")}
                  placeholder="e.g. Mina R."
                  className="w-full rounded-2xl border border-slate-700 bg-[#131A2A] px-4 py-3 text-sm font-medium outline-none transition focus:border-[#7C5CFF] focus:ring-4 focus:ring-[#7C5CFF]/20"
                />
              </div>

              <div>
                <label className="mb-2 block text-xs font-bold uppercase tracking-wide text-slate-400">
                  Seller type
                </label>
                <input
                  required
                  maxLength={120}
                  value={form.sellerRole}
                  onChange={update("sellerRole")}
                  placeholder="e.g. Handmade jewelry seller"
                  className="w-full rounded-2xl border border-slate-700 bg-[#131A2A] px-4 py-3 text-sm font-medium outline-none transition focus:border-[#7C5CFF] focus:ring-4 focus:ring-[#7C5CFF]/20"
                />
              </div>

              <div>
                <label className="mb-2 block text-xs font-bold uppercase tracking-wide text-slate-400">
                  Rating
                </label>
                <select
                  value={form.rating}
                  onChange={update("rating")}
                  className="w-full rounded-2xl border border-slate-700 bg-[#131A2A] px-4 py-3 text-sm font-medium outline-none transition focus:border-[#7C5CFF] focus:ring-4 focus:ring-[#7C5CFF]/20"
                >
                  {[5, 4, 3, 2, 1].map((rating) => (
                    <option key={rating} value={rating}>
                      {rating} stars
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-2 block text-xs font-bold uppercase tracking-wide text-slate-400">
                  Your review
                </label>
                <textarea
                  required
                  minLength={12}
                  maxLength={1000}
                  rows={6}
                  value={form.body}
                  onChange={update("body")}
                  placeholder="How has Tagwing improved your listing process, output quality, or publishing efficiency?"
                  className="w-full resize-none rounded-2xl border border-slate-700 bg-[#131A2A] px-4 py-3 text-sm font-medium leading-6 outline-none transition focus:border-[#7C5CFF] focus:ring-4 focus:ring-[#7C5CFF]/20"
                />
                <p className="mt-2 text-right text-xs text-slate-500">
                  {form.body.length} / 1000
                </p>
              </div>

              {error && (
                <p className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm font-semibold text-rose-300">
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={submitting}
                className="w-full rounded-2xl bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8] disabled:opacity-60"
              >
                {submitting ? "Submitting review..." : "Submit review"}
              </button>
            </form>
          )}
        </section>
      </main>
    </div>
  );
}
