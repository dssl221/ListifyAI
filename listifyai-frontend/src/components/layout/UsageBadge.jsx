import { useUsage } from "../../context/UsageContext";

export default function UsageBadge() {
  const { listingsUsed, monthlyLimit, unlimited } = useUsage();

  const pct =
    unlimited || monthlyLimit === 0 ? 0 : (listingsUsed / monthlyLimit) * 100;

  const colorClass = unlimited
    ? "bg-[#1A2235] text-[#A998FF] border-[#4DA3FF]/35"
    : pct >= 90
      ? "bg-rose-500/10 text-rose-300 border-rose-500/30"
      : pct >= 70
        ? "bg-amber-500/10 text-amber-300 border-amber-500/30"
        : "bg-emerald-500/10 text-emerald-300 border-emerald-500/30";

  const label = unlimited
    ? `${listingsUsed} used - Unlimited`
    : `${listingsUsed} / ${monthlyLimit} listings`;

  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border whitespace-nowrap ${colorClass}`}
    >
      {label}
    </span>
  );
}
