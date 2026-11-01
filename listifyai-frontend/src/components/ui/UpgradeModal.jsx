import { useState } from "react";
import { XMarkIcon, CheckIcon } from "@heroicons/react/24/outline";
import { useUsage } from "../../context/UsageContext";
import api from "../../lib/api";

const PLANS = [
  {
    id: "starter",
    name: "Starter",
    price: "$15",
    period: "/mo",
    limit: "30 listings/mo",
    features: [
      "30 listing workflows each month",
      "10 Etsy listing audits per month",
      "Titles, descriptions, and tags",
      "Etsy or Shopify workflow",
      "Quality score before publishing",
      "Shipping warning for Etsy",
      "Saved history for recent listings",
    ],
    highlight: false,
  },
  {
    id: "growth",
    name: "Growth",
    price: "$20",
    period: "/mo",
    limit: "100 listings/mo",
    features: [
      "100 listing workflows each month",
      "100 Etsy listing audits per month",
      "Generate Etsy and Shopify together",
      "Platform-specific SEO fields",
      "Quality score and prioritized fixes",
      "Deeper saved listing history",
      "Shop health score and improvement signals",
    ],
    highlight: true,
  },
  {
    id: "pro",
    name: "Pro",
    price: "$29",
    period: "/mo",
    limit: "Unlimited listings",
    features: [
      "Unlimited listing generation",
      "500 Etsy listing audits per month",
      "Bulk CSV workflow",
      "Shop health score and improvement signals",
      "Catalog refresh support",
      "Discovery optimization (coming soon)",
      "Multi-language (coming soon)",
    ],
    highlight: false,
  },
];

export default function UpgradeModal({ onClose }) {
  const { plan: currentPlan } = useUsage();
  const [loading, setLoading] = useState(null); // plan id being processed

  const handleUpgrade = async (planId) => {
    setLoading(planId);
    try {
      const { data } = await api.post(`/api/billing/checkout?plan=${planId}`);
      window.location.href = data.checkoutUrl;
    } catch {
      setLoading(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
      <div className="relative bg-[#131A2A] rounded-2xl shadow-xl w-full max-w-3xl overflow-hidden">
        {/* Header */}
        <div className="flex items-start justify-between px-6 pt-6 pb-4 border-b border-white/10">
          <div>
            <h2 className="text-xl font-bold text-[#F3F6FF]">
              Keep your listing workflow moving
            </h2>
            <p className="text-sm text-slate-400 mt-1">
              Upgrade when you are ready for more listings, Etsy listing audits,
              both-platform workflows, or bulk catalog work.
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-500 hover:bg-[#202B42] hover:text-slate-300 transition-colors"
          >
            <XMarkIcon className="w-5 h-5" />
          </button>
        </div>

        {/* Plan cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-6">
          {PLANS.map((plan) => (
            <div
              key={plan.id}
              className={`relative rounded-xl border p-5 flex flex-col gap-4 ${
                plan.highlight
                  ? "border-[#7C5CFF] ring-1 ring-[#7C5CFF] shadow-sm"
                  : "border-slate-700"
              }`}
            >
              {plan.highlight && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-[#7C5CFF] text-[#F3F6FF] text-[10px] font-bold uppercase tracking-wider px-3 py-0.5 rounded-full">
                  Best for growing shops
                </span>
              )}

              <div>
                <p className="text-sm font-semibold text-slate-400">
                  {plan.name}
                </p>
                <p className="text-3xl font-bold text-[#F3F6FF] mt-1">
                  {plan.price}
                  <span className="text-sm font-normal text-slate-400">
                    {plan.period}
                  </span>
                </p>
                <p className="text-xs text-[#A998FF] font-medium mt-1">
                  {plan.limit}
                </p>
              </div>

              <ul className="space-y-1.5 flex-1">
                {plan.features.map((f) => {
                  const isSoon = f.endsWith("(coming soon)");
                  return (
                    <li
                      key={f}
                      className={`flex items-start gap-2 text-sm ${isSoon ? "text-slate-600" : "text-[#94A3B8]"}`}
                    >
                      <CheckIcon
                        className={`w-4 h-4 flex-shrink-0 mt-0.5 ${isSoon ? "text-slate-700" : "text-[#4DA3FF]"}`}
                      />
                      {f}
                    </li>
                  );
                })}
              </ul>

              <button
                onClick={() => handleUpgrade(plan.id)}
                disabled={loading === plan.id || currentPlan === plan.id}
                className={`w-full py-2 rounded-lg text-sm font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${
                  plan.highlight
                    ? "bg-[#7C5CFF] text-[#F3F6FF] hover:bg-[#6848E8]"
                    : "bg-[#202B42] text-slate-200 hover:bg-[#26324A]"
                }`}
              >
                {loading === plan.id
                  ? "Redirecting..."
                  : currentPlan === plan.id
                    ? "Current plan"
                    : `Move to ${plan.name}`}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
