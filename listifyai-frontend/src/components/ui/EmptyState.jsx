export default function EmptyState({
  icon: Icon = null,
  title,
  description,
  actionLabel = null,
  onAction = null,
}) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      {/* Icon / illustration area */}
      <div className="w-16 h-16 rounded-2xl bg-[#202B42] flex items-center justify-center mb-5">
        {Icon ? (
          <Icon className="w-8 h-8 text-slate-500" />
        ) : (
          <div className="w-8 h-8 rounded-lg bg-[#26324A]" />
        )}
      </div>

      <h3 className="text-base font-semibold text-[#F3F6FF]">{title}</h3>

      {description && (
        <p className="mt-1.5 text-sm text-slate-400 max-w-sm">{description}</p>
      )}

      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="mt-5 inline-flex items-center px-4 py-2 bg-[#7C5CFF] text-[#F3F6FF] text-sm font-medium rounded-lg hover:bg-[#6848E8] transition-colors"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
