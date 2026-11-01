export default function LoadingSpinner({ label = null, size = "md" }) {
  const sizeClass =
    {
      sm: "w-5 h-5 border-2",
      md: "w-8 h-8 border-2",
      lg: "w-12 h-12 border-[3px]",
    }[size] ?? "w-8 h-8 border-2";

  return (
    <div className="flex flex-col items-center justify-center gap-3">
      <div
        className={`rounded-full border-slate-700 border-t-[#7C5CFF] animate-spin ${sizeClass}`}
        role="status"
        aria-label={label ?? "Loading"}
      />
      {label && <p className="text-sm text-slate-400 animate-pulse">{label}</p>}
    </div>
  );
}
