import { useEffect, useRef, useState } from "react";
import {
  ArrowUpTrayIcon,
  ArrowDownTrayIcon,
} from "@heroicons/react/24/outline";
import api from "../lib/api";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import EmptyState from "../components/ui/EmptyState";
import UpgradeModal from "../components/ui/UpgradeModal";
import { useUsage } from "../context/UsageContext";

const STATUS_COLORS = {
  queued: "bg-[#202B42] text-[#94A3B8]",
  processing: "bg-amber-500/10 text-amber-300",
  done: "bg-emerald-500/10 text-emerald-300",
  failed: "bg-rose-500/10 text-rose-300",
};

function timeAgo(dateString) {
  const diff = Date.now() - new Date(dateString).getTime();
  const mins = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  if (mins < 60) return mins <= 1 ? "Just now" : `${mins}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  return new Date(dateString).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export default function BulkPage() {
  const { plan } = useUsage();
  const [jobs, setJobs] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [showUpgrade, setShowUpgrade] = useState(false);
  const fileRef = useRef(null);
  const bulkLocked = plan !== "pro";

  const fetchJobs = async () => {
    try {
      const { data } = await api.get("/api/bulk/jobs");
      setJobs(data);
    } catch {
      // Auth failures are handled in the interceptor.
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  useEffect(() => {
    const hasActive = jobs.some((job) =>
      ["queued", "processing"].includes(job.status),
    );
    if (!hasActive) return;
    const timer = setInterval(fetchJobs, 4000);
    return () => clearInterval(timer);
  }, [jobs]);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (bulkLocked) {
      setShowUpgrade(true);
      if (fileRef.current) fileRef.current.value = "";
      return;
    }

    setUploading(true);
    setError("");

    const form = new FormData();
    form.append("file", file);

    try {
      await api.post("/api/bulk/upload", form, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      await fetchJobs();
    } catch (err) {
      if (err.response?.status === 402) {
        setShowUpgrade(true);
      } else {
        setError(
          err.response?.data?.message ??
            "Upload failed. Check your CSV and try again.",
        );
      }
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  };

  const downloadResult = (job) => {
    const a = document.createElement("a");
    a.href = job.resultCsvUrl;
    a.download = `tagwing-bulk-${job.jobId}.csv`;
    a.click();
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#F3F6FF] flex items-center gap-2">
          <ArrowUpTrayIcon className="w-6 h-6 text-[#7C5CFF]" /> Bulk Upload
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Prepare up to 50 products at once and keep catalog work moving in the
          background.
        </p>
      </div>

      <div className="bg-[#131A2A] rounded-2xl border border-slate-700 shadow-sm p-6">
        {bulkLocked && (
          <div className="mb-4 flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-300">
            <svg
              className="mt-0.5 h-4 w-4 flex-shrink-0"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Zm-7-4a1 1 0 1 1-2 0 1 1 0 0 1 2 0ZM9 9a.75.75 0 0 0 0 1.5h.253a.25.25 0 0 1 .244.304l-.459 2.066A1.75 1.75 0 0 0 10.747 15H11a.75.75 0 0 0 0-1.5h-.253a.25.25 0 0 1-.244-.304l.459-2.066A1.75 1.75 0 0 0 9.253 9H9Z"
                clipRule="evenodd"
              />
            </svg>
            <p>
              <strong>Bulk Upload</strong> is a Pro workflow for sellers
              preparing larger catalogs or launch batches.{" "}
              <button
                type="button"
                onClick={() => setShowUpgrade(true)}
                className="font-semibold underline hover:text-amber-200"
              >
                See Pro
              </button>
            </p>
          </div>
        )}
        <label
          htmlFor="bulk-file-input"
          className={`flex flex-col items-center justify-center gap-3 border-2 border-dashed rounded-xl py-10 px-4 transition-colors group ${
            bulkLocked
              ? "cursor-not-allowed border-slate-700 bg-[#1A2235] opacity-70"
              : "cursor-pointer border-slate-600 hover:border-[#4DA3FF]"
          }`}
        >
          <ArrowUpTrayIcon className="w-8 h-8 text-slate-600 group-hover:text-[#4DA3FF] transition-colors" />
          <div className="text-center">
            <p className="text-sm font-medium text-slate-300">
              Drop a product CSV here or click to browse
            </p>
            <p className="text-xs text-slate-500 mt-1">
              Include product details, buyer, use case, platform, and tone.
              Tagwing handles the listing structure.
            </p>
          </div>
          {uploading && (
            <LoadingSpinner size="sm" label="Reading your product rows..." />
          )}
        </label>
        <input
          id="bulk-file-input"
          ref={fileRef}
          type="file"
          accept=".csv,text/csv"
          className="sr-only"
          onChange={handleUpload}
          disabled={uploading || bulkLocked}
        />
        {error && (
          <p className="mt-3 text-sm text-rose-400 bg-rose-500/10 border border-rose-500/30 rounded-lg px-3 py-2">
            {error}
          </p>
        )}
      </div>

      {jobs.length === 0 ? (
        <EmptyState
          icon={ArrowUpTrayIcon}
          title="No catalog batches yet"
          description="Upload a CSV when you are ready to turn a product batch into organized listing drafts."
        />
      ) : (
        <div className="space-y-3">
          {jobs.map((job) => (
            <div
              key={job.jobId}
              className="bg-[#131A2A] rounded-xl border border-slate-700 shadow-sm p-4 flex items-center gap-4"
            >
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span
                    className={`text-xs font-semibold px-2 py-0.5 rounded-full capitalize ${STATUS_COLORS[job.status] ?? "bg-[#202B42] text-[#94A3B8]"}`}
                  >
                    {job.status}
                  </span>
                  <span className="text-xs text-slate-500">
                    {job.processedRows}/{job.totalRows} rows
                    {job.failedRows > 0 && ` - ${job.failedRows} failed`}
                  </span>
                </div>
                {job.status === "failed" && job.errorLog && (
                  <p className="mt-1 text-xs text-rose-300 truncate">
                    {job.errorLog.length > 80
                      ? `${job.errorLog.slice(0, 80)}...`
                      : job.errorLog}
                  </p>
                )}
                <div className="w-full bg-[#202B42] rounded-full h-1.5 mt-2">
                  <div
                    className="bg-[#4DA3FF] h-1.5 rounded-full transition-all duration-500"
                    style={{ width: `${job.progressPercent}%` }}
                  />
                </div>
                {job.createdAt && (
                  <p className="mt-1.5 text-xs text-slate-500">
                    Started {timeAgo(job.createdAt)}
                  </p>
                )}
              </div>
              {job.status === "done" && job.resultCsvUrl && (
                <button
                  onClick={() => downloadResult(job)}
                  className="flex items-center gap-1.5 text-xs font-medium text-[#A998FF] border border-[#4DA3FF]/70 bg-[#1A2235] hover:bg-[#24304A] px-3 py-1.5 rounded-lg transition-colors whitespace-nowrap"
                >
                  <ArrowDownTrayIcon className="w-4 h-4" />
                  Download results
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {showUpgrade && <UpgradeModal onClose={() => setShowUpgrade(false)} />}
    </div>
  );
}
