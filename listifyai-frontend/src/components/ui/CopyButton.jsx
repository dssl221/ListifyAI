import { useState } from "react";
import { CheckIcon, ClipboardIcon } from "@heroicons/react/24/outline";

export default function CopyButton({ text, label = "Copy" }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard API not available - silent fail
    }
  };

  return (
    <button
      onClick={handleCopy}
      title={copied ? "Copied!" : `Copy ${label}`}
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-xs font-medium border transition-colors ${
        copied
          ? "bg-[#1A2235] border-[#4DA3FF]/70 text-[#A998FF]"
          : "bg-[#131A2A] border-slate-600 text-[#94A3B8] hover:border-[#4DA3FF] hover:text-[#7C5CFF]"
      }`}
    >
      {copied ? (
        <>
          <CheckIcon className="w-3.5 h-3.5" />
          Copied
        </>
      ) : (
        <>
          <ClipboardIcon className="w-3.5 h-3.5" />
          {label}
        </>
      )}
    </button>
  );
}
