import { useEffect, useState } from "react";

const SCORE_STYLES = {
  poor: {
    label: "Poor",
    text: "text-rose-300",
    bg: "bg-rose-500/10",
    ring: "#e11d48",
  },
  good: {
    label: "Good",
    text: "text-amber-500",
    bg: "bg-amber-500/10",
    ring: "#f59e0b",
  },
  excellent: {
    label: "Excellent",
    text: "text-[#7C5CFF]",
    bg: "bg-[#1A2235]",
    ring: "#0d9488",
  },
};

function getScoreStyle(score) {
  if (score >= 75) return SCORE_STYLES.excellent;
  if (score >= 50) return SCORE_STYLES.good;
  return SCORE_STYLES.poor;
}

export default function QualityScoreCard({
  score = 0,
  tips = [],
  platform = "",
}) {
  const normalizedScore = Math.max(0, Math.min(100, Number(score) || 0));
  const [displayScore, setDisplayScore] = useState(0);
  const style = getScoreStyle(normalizedScore);
  const displayPlatform = platform
    ? platform.charAt(0).toUpperCase() + platform.slice(1)
    : "Listing";

  useEffect(() => {
    const timer = setTimeout(() => {
      setDisplayScore(normalizedScore);
    }, 150);
    return () => clearTimeout(timer);
  }, [normalizedScore]);

  return (
    <div className="bg-[#131A2A] rounded-2xl border border-slate-700 shadow-sm p-6">
      <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide">
        {displayPlatform} score
      </p>

      <div className="mt-4 flex flex-col items-center">
        <div
          className="h-28 w-28 rounded-full p-1"
          style={{
            background: `conic-gradient(${style.ring} ${displayScore * 3.6}deg, #e5e7eb 0deg)`,
            transition: "background 1s ease",
          }}
        >
          <div
            className={`flex h-full w-full flex-col items-center justify-center rounded-full ${style.bg}`}
          >
            <span className={`text-3xl font-bold leading-none ${style.text}`}>
              {normalizedScore}
            </span>
            <span className="mt-1 text-xs font-medium text-slate-500">
              /100
            </span>
          </div>
        </div>
        <p className={`mt-3 text-sm font-semibold ${style.text}`}>
          {style.label}
        </p>
      </div>

      <div className="mt-5">
        <h3 className="text-sm font-semibold text-slate-200">How to improve</h3>
        <div className="mt-3 space-y-2">
          {tips.slice(0, 3).map((tip, index) => (
            <div
              key={`${index}-${tip}`}
              className="flex items-start gap-3 rounded-lg border border-white/10 bg-[#1A2235] p-3"
            >
              <span
                className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full text-xs font-bold ${style.bg} ${style.text}`}
              >
                {index + 1}
              </span>
              <p className="text-sm leading-5 text-slate-300">{tip}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
