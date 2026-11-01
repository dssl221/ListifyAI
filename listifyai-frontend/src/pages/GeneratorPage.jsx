import { useState } from "react";
import { SparklesIcon } from "@heroicons/react/24/outline";
import api from "../lib/api";
import { useUsage } from "../context/UsageContext";
import CharCounter from "../components/ui/CharCounter";
import { EtsyCard, ShopifyCard } from "../components/ui/ListingCards";
import LoadingSpinner from "../components/ui/LoadingSpinner";
import QualityScoreCard from "../components/ui/QualityScoreCard";
import UpgradeModal from "../components/ui/UpgradeModal";

const PLATFORMS = ["etsy", "shopify", "both"];
const TONES = [
  {
    value: "friendly",
    title: "Warm, approachable, like a conversation with a friend",
  },
  {
    value: "professional",
    title: "Clear, polished, builds trust with serious buyers",
  },
  {
    value: "playful",
    title: "Fun, upbeat, great for gifts and personality-led products",
  },
  { value: "luxury", title: "Aspirational, refined, premium feel throughout" },
  {
    value: "minimal",
    title: "Clean, simple, lets the product speak for itself",
  },
];
const OPTIONAL_FIELD_MAX = {
  material: 100,
  size: 80,
  targetBuyer: 100,
  useCase: 120,
  occasion: 80,
};
const EMPTY_FORM = {
  productName: "",
  material: "",
  size: "",
  targetBuyer: "",
  useCase: "",
  occasion: "",
  platform: "etsy",
  tone: "friendly",
  shippingPrice: "",
};

function hasQualityFeedback(output) {
  return (
    typeof output?.qualityScore === "number" &&
    Array.isArray(output?.tips) &&
    output.tips.length === 3
  );
}

function ShippingWarningBanner({ result, dismissed, onDismiss }) {
  if (dismissed || result?.shippingWarning !== true) {
    return null;
  }

  return (
    <div className="flex items-start gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-amber-300 shadow-sm">
      <svg
        className="mt-0.5 h-5 w-5 flex-shrink-0"
        viewBox="0 0 20 20"
        fill="currentColor"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.515 2.625H3.72c-1.345 0-2.188-1.458-1.515-2.625L8.485 2.495ZM10 5.25a.75.75 0 0 1 .75.75v4a.75.75 0 0 1-1.5 0V6a.75.75 0 0 1 .75-.75Zm0 8a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"
          clipRule="evenodd"
        />
      </svg>
      <p className="flex-1 text-sm font-medium leading-6">
        {result.shippingWarningMessage ||
          "This shipping price may reduce buyer confidence on Etsy. Consider testing a lower visible shipping cost before publishing."}
      </p>
      <button
        type="button"
        onClick={onDismiss}
        className="rounded-lg p-1 text-amber-300 transition-colors hover:bg-amber-500/15"
        aria-label="Dismiss shipping warning"
      >
        <svg
          className="h-4 w-4"
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z" />
        </svg>
      </button>
    </div>
  );
}

export default function GeneratorPage() {
  const { refreshUsage, plan } = useUsage();
  const [form, setForm] = useState(EMPTY_FORM);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showUpgrade, setShowUpgrade] = useState(false);
  const [selectedEtsyTitleIndex, setSelectedEtsyTitleIndex] = useState(0);
  const [shippingWarningDismissed, setShippingWarningDismissed] =
    useState(false);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
  const showShippingPrice =
    form.platform === "etsy" || form.platform === "both";
  const bothPlatformsBlocked =
    form.platform === "both" && (plan === "free" || plan === "starter");

  const handleGenerate = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);
    setSelectedEtsyTitleIndex(0);
    setShippingWarningDismissed(false);

    try {
      const payload = {
        ...form,
        shippingPrice: form.shippingPrice === "" ? null : form.shippingPrice,
      };
      const { data } = await api.post("/api/generate", payload);
      setResult(data);
      await refreshUsage();
    } catch (err) {
      if (err.response?.status === 402) {
        setShowUpgrade(true);
      } else {
        setError(
          err.response?.data?.message ??
            "Something went wrong. Please try again.",
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="feature-card max-w-4xl mx-auto space-y-6 pb-20 md:pb-0">
      <div className="rounded-3xl border border-white/10 bg-[#131A2A]/88 p-6 shadow-xl shadow-[#050816]/32 backdrop-blur">
        <h1 className="text-3xl font-black text-[#F3F6FF] flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#7C5CFF] text-[#F3F6FF] shadow-lg shadow-[#7C5CFF]/22">
            <SparklesIcon className="w-5 h-5" />
          </span>
          <span>
            Generate a{" "}
            <span className="animated-gradient-text">publish-ready</span>{" "}
            listing
          </span>
        </h1>
        <p className="text-sm text-slate-400 mt-3">
          Add the details you already know. Tagwing will structure the copy,
          surface weak spots, and help you publish with more confidence.
        </p>
      </div>

      <form
        id="generate-form"
        onSubmit={handleGenerate}
        className="bg-[#131A2A]/92 backdrop-blur rounded-3xl border border-white/10 shadow-xl shadow-[#050816]/32 p-6 space-y-5"
      >
        <div className="rounded-2xl border border-[#7C5CFF]/25 bg-[#1A2235]/78 p-3">
          <label className="block text-sm font-medium text-slate-300 mb-1">
            Product name <span className="text-rose-300">*</span>
          </label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              value={form.productName}
              onChange={set("productName")}
              required
              placeholder="e.g. Hand-stamped copper ring"
              className="min-w-0 flex-1 px-3 py-3 border border-[#4DA3FF]/35 rounded-xl bg-[#131A2A] text-sm font-medium focus:outline-none focus:ring-2 focus:ring-[#7C5CFF]"
            />
            <button
              type="submit"
              disabled={loading || bothPlatformsBlocked}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF] shadow-lg shadow-[#7C5CFF]/22 transition-colors hover:bg-[#6848E8] disabled:opacity-60"
            >
              {loading ? (
                <>
                  <LoadingSpinner size="sm" /> Building draft...
                </>
              ) : (
                <>
                  <SparklesIcon className="w-4 h-4" /> Generate
                </>
              )}
            </button>
          </div>
          <div className="mt-1 flex justify-end">
            <CharCounter text={form.productName} max={500} />
          </div>
        </div>

        {showShippingPrice && (
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">
              Shipping price (USD) - optional
            </label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.shippingPrice}
              onChange={set("shippingPrice")}
              placeholder="e.g. 4.99"
              className="w-full px-3 py-2.5 border border-slate-600 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#7C5CFF]"
            />
            <p className="mt-1 text-xs text-slate-400">
              We will flag shipping prices that may reduce buyer confidence on
              Etsy.
            </p>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {[
            {
              key: "material",
              label: "Material",
              placeholder: "e.g. Sterling silver",
            },
            {
              key: "size",
              label: "Size",
              placeholder: "e.g. Adjustable, 6mm wide",
            },
            {
              key: "targetBuyer",
              label: "Ideal buyer",
              placeholder: "e.g. Boho brides",
            },
            {
              key: "useCase",
              label: "Why they buy it",
              placeholder: "e.g. Everyday wear",
            },
            {
              key: "occasion",
              label: "Occasion or moment",
              placeholder: "e.g. Wedding gift",
            },
          ].map(({ key, label, placeholder }) => (
            <div key={key}>
              <label className="block text-sm font-medium text-slate-300 mb-1">
                {label}
              </label>
              <input
                value={form[key]}
                onChange={set(key)}
                placeholder={placeholder}
                className="w-full px-3 py-2.5 border border-slate-600 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#7C5CFF]"
              />
              <div className="mt-1 flex justify-end">
                <CharCounter text={form[key]} max={OPTIONAL_FIELD_MAX[key]} />
              </div>
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Platform
            </label>
            <div className="flex gap-2 flex-wrap">
              {PLATFORMS.map((platform) => (
                <button
                  type="button"
                  key={platform}
                  onClick={() => setForm((f) => ({ ...f, platform }))}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors capitalize ${
                    form.platform === platform
                      ? "bg-[#7C5CFF] text-[#F3F6FF] border-[#7C5CFF]"
                      : "bg-[#131A2A] text-[#94A3B8] border-slate-600 hover:border-[#4DA3FF]"
                  }`}
                >
                  {platform}
                </button>
              ))}
            </div>
            {form.platform === "both" &&
              (plan === "free" || plan === "starter") && (
                <div className="mt-2 flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-300">
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
                    <strong>Both-platform generation</strong> is available on
                    Growth and Pro when you want Etsy and Shopify copy in one
                    workflow.{" "}
                    <button
                      type="button"
                      onClick={() => setShowUpgrade(true)}
                      className="font-semibold underline hover:text-amber-200"
                    >
                      See plans
                    </button>
                  </p>
                </div>
              )}
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Tone
            </label>
            <div className="flex gap-2 flex-wrap">
              {TONES.map((tone) => (
                <button
                  type="button"
                  key={tone.value}
                  title={tone.title}
                  onClick={() => setForm((f) => ({ ...f, tone: tone.value }))}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors capitalize ${
                    form.tone === tone.value
                      ? "bg-[#7C5CFF] text-[#F3F6FF] border-[#7C5CFF]"
                      : "bg-[#131A2A] text-[#94A3B8] border-slate-600 hover:border-[#4DA3FF]"
                  }`}
                >
                  {tone.value}
                </button>
              ))}
            </div>
          </div>
        </div>

        {error && (
          <p className="text-sm text-rose-400 bg-rose-500/10 border border-rose-500/30 rounded-lg px-3 py-2">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading || bothPlatformsBlocked}
          className="w-full py-3 bg-[#7C5CFF] text-[#F3F6FF] font-semibold rounded-xl hover:bg-[#6848E8] transition-colors disabled:opacity-60 flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <LoadingSpinner size="sm" /> Building draft...
            </>
          ) : (
            <>
              <SparklesIcon className="w-4 h-4" /> Generate listing
            </>
          )}
        </button>
      </form>

      <ShippingWarningBanner
        result={result}
        dismissed={shippingWarningDismissed}
        onDismiss={() => setShippingWarningDismissed(true)}
      />

      {!result && !loading && (
        <div className="rounded-2xl border border-dashed border-white/10 bg-[#131A2A]/40 px-6 py-12 text-center">
          <SparklesIcon className="mx-auto h-10 w-10 text-slate-700 mb-3" />
          <p className="text-sm text-slate-500">
            Your generated listing will appear here
          </p>
        </div>
      )}

      {result && (
        <div className="space-y-4 animate-[fadeSlideUp_400ms_ease_both]">
          {result.etsy && (
            <>
              <EtsyCard
                data={result.etsy}
                selectedTitleIndex={selectedEtsyTitleIndex}
                onSelectTitle={setSelectedEtsyTitleIndex}
                selectableTitles
              />
              {form.platform !== "both" && hasQualityFeedback(result.etsy) && (
                <QualityScoreCard
                  score={result.etsy.qualityScore}
                  tips={result.etsy.tips}
                  platform="etsy"
                />
              )}
            </>
          )}
          {result.shopify && (
            <>
              <ShopifyCard data={result.shopify} />
              {form.platform !== "both" &&
                hasQualityFeedback(result.shopify) && (
                  <QualityScoreCard
                    score={result.shopify.qualityScore}
                    tips={result.shopify.tips}
                    platform="shopify"
                  />
                )}
            </>
          )}
          {form.platform === "both" &&
            (hasQualityFeedback(result.etsy) ||
              hasQualityFeedback(result.shopify)) && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {hasQualityFeedback(result.etsy) && (
                  <QualityScoreCard
                    score={result.etsy.qualityScore}
                    tips={result.etsy.tips}
                    platform="etsy"
                  />
                )}
                {hasQualityFeedback(result.shopify) && (
                  <QualityScoreCard
                    score={result.shopify.qualityScore}
                    tips={result.shopify.tips}
                    platform="shopify"
                  />
                )}
              </div>
            )}
          <p
            className={`text-xs text-right ${
              !result.unlimited && Number(result.creditsRemaining) <= 1
                ? "text-rose-400"
                : "text-slate-500"
            }`}
          >
            {result.unlimited
              ? "Unlimited listings remaining this month"
              : Number(result.creditsRemaining) === 0
                ? "⚠ No listings remaining — upgrade to continue"
                : Number(result.creditsRemaining) === 1
                  ? "⚠ 1 listing remaining this month"
                  : `${result.creditsRemaining} listings remaining this month`}
          </p>
        </div>
      )}

      {form.productName && !result && (
        <div className="fixed bottom-0 left-0 right-0 z-30 md:hidden border-t border-white/10 bg-[#0B1020]/95 backdrop-blur px-4 py-3">
          <button
            type="submit"
            form="generate-form"
            disabled={loading || bothPlatformsBlocked}
            className="w-full py-3 bg-[#7C5CFF] text-[#F3F6FF] font-semibold rounded-xl hover:bg-[#6848E8] disabled:opacity-60 flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <LoadingSpinner size="sm" /> Building draft...
              </>
            ) : (
              <>
                <SparklesIcon className="w-4 h-4" /> Generate listing
              </>
            )}
          </button>
        </div>
      )}

      {showUpgrade && <UpgradeModal onClose={() => setShowUpgrade(false)} />}
    </div>
  );
}
