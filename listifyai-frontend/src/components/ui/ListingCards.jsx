import CopyButton from "./CopyButton";
import CharCounter from "./CharCounter";

function ResultCard({ title, children }) {
  return (
    <div className="bg-[#131A2A] rounded-2xl border border-slate-700 shadow-sm p-6 space-y-4">
      <h2 className="font-semibold text-slate-200">{title}</h2>
      {children}
    </div>
  );
}

function Field({ label, value }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs font-medium text-slate-400 uppercase tracking-wide">
          {label}
        </span>
        <CopyButton text={value} label={label} />
      </div>
      <p className="text-sm text-slate-200 whitespace-pre-wrap leading-relaxed">
        {value}
      </p>
    </div>
  );
}

function EtsyTitleSelector({
  titles = [],
  selectedIndex = 0,
  onSelect,
  selectable = false,
}) {
  return (
    <div>
      <span className="text-xs font-medium text-slate-400 uppercase tracking-wide">
        Title Options
      </span>
      <div className="mt-2 grid gap-3">
        {titles.map((title, index) => {
          const selected = selectable && selectedIndex === index;

          return (
            <div
              role={selectable ? "button" : undefined}
              tabIndex={selectable ? 0 : undefined}
              key={`${index}-${title}`}
              onClick={() => selectable && onSelect?.(index)}
              onKeyDown={(event) => {
                if (
                  selectable &&
                  (event.key === "Enter" || event.key === " ")
                ) {
                  event.preventDefault();
                  onSelect?.(index);
                }
              }}
              className={`w-full text-left rounded-lg border p-3 transition-colors ${
                selected
                  ? "border-[#7C5CFF] bg-[#1A2235]"
                  : "border-slate-700 bg-[#131A2A] hover:border-[#4DA3FF]/70"
              } ${selectable ? "cursor-pointer" : "cursor-default"}`}
            >
              <div className="flex items-center justify-between gap-3 mb-2">
                <span className="text-xs font-semibold text-[#94A3B8]">
                  Option {index + 1}
                </span>
                <div className="flex items-center gap-2">
                  <span className="rounded-full border border-slate-700 bg-[#1A2235] px-2 py-0.5 text-xs tabular-nums text-slate-400">
                    {title?.length ?? 0} chars
                  </span>
                  <span onClick={(event) => event.stopPropagation()}>
                    <CopyButton text={title} label={`option ${index + 1}`} />
                  </span>
                </div>
              </div>
              <p className="text-sm text-slate-200 whitespace-pre-wrap leading-relaxed">
                {title}
              </p>
            </div>
          );
        })}
      </div>
    </div>
  );
}

const TAG_COMPETITION_DOT = {
  high: "bg-rose-500",
  medium: "bg-amber-500",
  low: "bg-[#4DA3FF]",
};

function getTagText(tag) {
  return typeof tag === "string" ? tag : tag?.tag;
}

function getTagCompetition(tag) {
  return typeof tag === "string" ? null : tag?.competition;
}

export function EtsyCard({
  data,
  selectedTitleIndex = 0,
  onSelectTitle,
  selectableTitles = false,
}) {
  const tags = data.tags ?? [];
  const tagText = tags.map(getTagText).filter(Boolean).join(", ");

  return (
    <ResultCard title="Etsy Listing">
      <EtsyTitleSelector
        titles={data.titles ?? (data.title ? [data.title] : [])}
        selectedIndex={selectedTitleIndex}
        onSelect={onSelectTitle}
        selectable={selectableTitles}
      />
      <Field label="Description" value={data.description} />
      <div>
        <div className="flex items-center justify-between mb-1">
          <span className="text-xs font-medium text-slate-400 uppercase tracking-wide">
            Tags ({tags.length})
          </span>
          <CopyButton text={tagText} label="tags" />
        </div>
        <div className="flex flex-wrap gap-1.5">
          {tags.map((tag, index) => {
            const text = getTagText(tag);
            const competition = getTagCompetition(tag);
            const dotClass = TAG_COMPETITION_DOT[competition] ?? "bg-slate-500";

            return (
              <span
                key={`${index}-${text}`}
                className="inline-flex items-center gap-1.5 bg-[#1A2235] text-[#A998FF] text-xs px-2 py-0.5 rounded-full border border-[#7C5CFF]/25"
              >
                <span className={`h-1.5 w-1.5 rounded-full ${dotClass}`} />
                {text}
              </span>
            );
          })}
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-slate-400">
          <span className="inline-flex items-center gap-1">
            <span className="h-1.5 w-1.5 rounded-full bg-rose-500" />
            High competition
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
            Medium
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="h-1.5 w-1.5 rounded-full bg-[#4DA3FF]" />
            Low
          </span>
        </div>
      </div>
    </ResultCard>
  );
}

export function ShopifyCard({ data }) {
  return (
    <ResultCard title="Shopify Listing">
      <Field label="SEO Title" value={data.seoTitle} />
      <div className="flex justify-end">
        <CharCounter text={data.seoTitle ?? ""} min={50} max={60} />
      </div>
      <Field label="Meta Description" value={data.metaDescription} />
      <div className="flex justify-end">
        <CharCounter text={data.metaDescription ?? ""} min={150} max={160} />
      </div>
      <Field label="Description" value={data.description} />
      <div>
        <div className="flex items-center justify-between mb-1">
          <span className="text-xs font-medium text-slate-400 uppercase tracking-wide">
            Bullets
          </span>
          <CopyButton text={data.bullets?.join("\n")} label="bullets" />
        </div>
        <ul className="space-y-1">
          {data.bullets?.map((bullet, index) => (
            <li
              key={index}
              className="text-sm text-slate-300 flex items-start gap-2"
            >
              <span className="text-[#4DA3FF] font-bold mt-0.5">&gt;</span>
              {bullet}
            </li>
          ))}
        </ul>
      </div>
    </ResultCard>
  );
}
