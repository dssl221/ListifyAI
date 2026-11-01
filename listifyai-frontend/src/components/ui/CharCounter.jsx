export default function CharCounter({ text = "", min = null, max }) {
  const len = text.length;
  const withinMax = len <= max;
  const withinMin = min === null || len >= min;
  const valid = withinMax && withinMin;

  const colorClass =
    len === 0 ? "text-slate-500" : valid ? "text-emerald-300" : "text-rose-400";

  return (
    <span className={`text-xs tabular-nums ${colorClass}`}>
      {len}
      {max != null && ` / ${max}`}
      {min != null && len > 0 && !withinMin && (
        <span className="ml-1 font-medium">(min {min})</span>
      )}
    </span>
  );
}
