import { useEffect, useState } from "react";
import {
  BookmarkIcon,
  ChevronDownIcon,
  ChevronRightIcon,
  ClockIcon,
  TrashIcon,
} from "@heroicons/react/24/outline";
import api from "../lib/api";
import EmptyState from "../components/ui/EmptyState";
import { EtsyCard, ShopifyCard } from "../components/ui/ListingCards";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import QualityScoreCard from "../components/ui/QualityScoreCard";
import { useUsage } from "../context/UsageContext";

const PLATFORM_BADGE = {
  etsy: "bg-orange-500/10 text-orange-300 border-orange-500/25",
  shopify: "bg-emerald-500/10 text-emerald-300 border-emerald-500/25",
  both: "bg-[#1A2235] text-[#A998FF] border-[#7C5CFF]/25",
};

function hasQualityFeedback(output) {
  return (
    typeof output?.qualityScore === "number" &&
    Array.isArray(output?.tips) &&
    output.tips.length === 3
  );
}

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

export default function HistoryPage() {
  const { plan } = useUsage();
  const [listings, setListings] = useState([]);
  const [filter, setFilter] = useState("all");
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [toggleSaveLoading, setToggleSaveLoading] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(null);

  const fetchPage = async (p = 0, replace = true) => {
    setLoading(true);
    try {
      const { data } = await api.get("/api/listings", {
        params: { page: p, size: 20 },
      });
      setListings((prev) =>
        replace ? data.content : [...prev, ...data.content],
      );
      setHasMore(!data.last);
      setPage(p);
    } catch {
      // handled globally
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPage(0);
  }, []);

  const toggleSave = async (id, saved) => {
    setToggleSaveLoading(id);
    try {
      await api.put(`/api/listings/${id}/save`);
      setListings((prev) =>
        prev.map((listing) =>
          listing.id === id ? { ...listing, isSaved: !saved } : listing,
        ),
      );
      setDetail((current) =>
        current?.id === id ? { ...current, isSaved: !saved } : current,
      );
    } catch {
      // handled globally
    } finally {
      setToggleSaveLoading(null);
    }
  };

  const deleteListing = async (id) => {
    if (
      !window.confirm("Delete this saved listing draft? This cannot be undone.")
    )
      return;
    setDeleteLoading(id);
    try {
      await api.delete(`/api/listings/${id}`);
      setListings((prev) => prev.filter((listing) => listing.id !== id));
      if (expandedId === id) {
        setExpandedId(null);
        setDetail(null);
      }
    } catch {
      // handled globally
    } finally {
      setDeleteLoading(null);
    }
  };

  const toggleExpand = async (id) => {
    if (expandedId === id) {
      setExpandedId(null);
      setDetail(null);
      setDetailError("");
      return;
    }

    setExpandedId(id);
    setDetail(null);
    setDetailError("");
    setDetailLoading(true);

    try {
      const { data } = await api.get(`/api/listings/${id}`);
      setDetail(data);
    } catch {
      setDetailError("Could not load this saved listing. Please try again.");
    } finally {
      setDetailLoading(false);
    }
  };

  if (loading && listings.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner label="Loading your saved listing work..." />
      </div>
    );
  }

  const visibleListings =
    filter === "saved"
      ? listings.filter((listing) => listing.isSaved)
      : listings;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#F3F6FF] flex items-center gap-2">
          <ClockIcon className="w-6 h-6 text-[#7C5CFF]" /> Listing History
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Saved drafts, optimization work, and generated copy for products you
          have prepared.
        </p>
      </div>

      {listings.length === 0 ? (
        <EmptyState
          icon={ClockIcon}
          title="No saved listing work yet"
          description="Generate your first product listing and it will appear here for review, copying, and future edits."
        />
      ) : (
        <>
          <div className="flex gap-2 border-b border-white/10 pb-3">
            {["all", "saved"].map((tab) => (
              <button
                key={tab}
                onClick={() => setFilter(tab)}
                className={`px-4 py-1.5 rounded-lg text-sm font-semibold capitalize transition-colors ${
                  filter === tab
                    ? "bg-[#1A2235] text-[#F3F6FF]"
                    : "text-slate-400 hover:text-[#F3F6FF]"
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          {filter === "saved" && visibleListings.length === 0 ? (
            <EmptyState
              icon={BookmarkIcon}
              title="No saved listings yet"
              description="Bookmark listings you want to keep by clicking the bookmark icon on any row."
            />
          ) : (
            <div className="space-y-3">
              {visibleListings.map((listing) => {
                const isExpanded = expandedId === listing.id;

                return (
                  <div
                    key={listing.id}
                    className="bg-[#131A2A] rounded-xl border border-slate-700 shadow-sm overflow-hidden"
                  >
                    <div className="p-4 flex items-start gap-3">
                      <button
                        onClick={() => toggleExpand(listing.id)}
                        title={
                          isExpanded
                            ? "Collapse"
                            : "Expand to view full listing"
                        }
                        className="p-1.5 rounded-lg text-slate-500 hover:bg-[#1A2235] hover:text-[#7C5CFF] transition-colors flex-shrink-0"
                      >
                        {isExpanded ? (
                          <ChevronDownIcon className="w-4 h-4" />
                        ) : (
                          <ChevronRightIcon className="w-4 h-4" />
                        )}
                      </button>

                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-[#F3F6FF] text-sm truncate">
                          {listing.productName}
                        </p>
                        <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                          <span
                            className={`text-xs px-2 py-0.5 rounded-full border capitalize ${PLATFORM_BADGE[listing.platform] ?? "bg-[#1A2235] text-[#94A3B8] border-slate-700"}`}
                          >
                            {listing.platform}
                          </span>
                          <span className="text-xs text-slate-500 capitalize">
                            {listing.tone}
                          </span>
                          <span className="text-xs text-slate-600">-</span>
                          <span className="text-xs text-slate-500">
                            {timeAgo(listing.createdAt)}
                          </span>
                        </div>
                      </div>

                      <button
                        onClick={() => toggleSave(listing.id, listing.isSaved)}
                        disabled={toggleSaveLoading === listing.id}
                        title={listing.isSaved ? "Unsave" : "Save"}
                        className={`p-1.5 rounded-lg transition-colors flex-shrink-0 ${
                          toggleSaveLoading === listing.id
                            ? "opacity-50 cursor-not-allowed"
                            : "text-slate-500 hover:bg-[#1A2235]"
                        }`}
                      >
                        <BookmarkIcon
                          className={`w-4 h-4 ${listing.isSaved ? "text-[#7C5CFF] fill-[#7C5CFF]" : ""}`}
                        />
                      </button>

                      <button
                        onClick={() => deleteListing(listing.id)}
                        disabled={deleteLoading === listing.id}
                        title="Delete"
                        className={`p-1.5 rounded-lg transition-colors flex-shrink-0 ${
                          deleteLoading === listing.id
                            ? "opacity-50 cursor-not-allowed"
                            : "text-slate-500 hover:bg-rose-500/10 hover:text-rose-300"
                        }`}
                      >
                        <TrashIcon className="w-4 h-4" />
                      </button>
                    </div>

                    {isExpanded && (
                      <div className="border-t border-white/10 bg-[#1A2235] px-4 py-4">
                        {detailLoading && (
                          <div className="flex justify-center py-8">
                            <LoadingSpinner label="Opening saved listing details..." />
                          </div>
                        )}

                        {detailError && (
                          <p className="text-sm text-rose-400 bg-rose-500/10 border border-rose-500/30 rounded-lg px-3 py-2">
                            {detailError}
                          </p>
                        )}

                        {detail && (
                          <div className="space-y-4">
                            {detail.etsyOutput && (
                              <EtsyCard data={detail.etsyOutput} />
                            )}
                            {detail.shopifyOutput && (
                              <ShopifyCard data={detail.shopifyOutput} />
                            )}
                            {(hasQualityFeedback(detail.etsyOutput) ||
                              hasQualityFeedback(detail.shopifyOutput)) && (
                              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                                {hasQualityFeedback(detail.etsyOutput) && (
                                  <QualityScoreCard
                                    score={detail.etsyOutput.qualityScore}
                                    tips={detail.etsyOutput.tips}
                                    platform="etsy"
                                  />
                                )}
                                {hasQualityFeedback(detail.shopifyOutput) && (
                                  <QualityScoreCard
                                    score={detail.shopifyOutput.qualityScore}
                                    tips={detail.shopifyOutput.tips}
                                    platform="shopify"
                                  />
                                )}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {hasMore && filter !== "saved" && (
            <div className="flex justify-center pt-2">
              <button
                onClick={() => fetchPage(page + 1, false)}
                disabled={loading}
                className="px-5 py-2 text-sm font-medium text-[#A998FF] border border-[#4DA3FF]/70 rounded-lg hover:bg-[#1A2235] transition-colors disabled:opacity-50"
              >
                {loading ? "Loading..." : "Load more saved listings"}
              </button>
            </div>
          )}

          {!hasMore &&
            listings.length > 0 &&
            (plan === "free" || plan === "starter" || plan === "growth") && (
              <p className="text-center text-xs text-slate-400 mt-2">
                {plan === "free" || plan === "starter"
                  ? "Showing your most recent 10 listings. Growth keeps more history available."
                  : "Showing your most recent 50 listings. Pro is built for unlimited catalog work."}
              </p>
            )}
        </>
      )}
    </div>
  );
}
