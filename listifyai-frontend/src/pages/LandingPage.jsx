import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRightIcon,
  ArrowUpTrayIcon,
  BellAlertIcon,
  BoltIcon,
  CheckCircleIcon,
  ChevronRightIcon,
  ClockIcon,
  HeartIcon,
  MagnifyingGlassIcon,
  SparklesIcon,
} from "@heroicons/react/24/outline";
import api from "../lib/api";
import LogoMark from "../components/ui/LogoMark";

const NAV_LINKS = [
  ["Features", "#features"],
  ["Workflow", "#workflow"],
  ["Audit", "#audit-demo"],
  ["Reviews", "#reviews"],
  ["FAQ", "#faq"],
];

const HERO_POINTS = [
  "Generate Etsy and Shopify copy from simple product details",
  "Audit live Etsy listings and see what to improve next",
  "Monitor listing health over time with clearer visibility",
  "Keep drafts, scores, history, and bulk work in one workspace",
];

const PLATFORM_OUTPUTS = [
  {
    title: "Etsy generator",
    icon: SparklesIcon,
    items: [
      "Title options",
      "Tags",
      "Descriptions",
      "Shipping warning signals",
      "Quality score + recommendations",
    ],
  },
  {
    title: "Shopify generator",
    icon: BoltIcon,
    items: [
      "SEO title",
      "Meta description",
      "Bullets",
      "Product description",
      "Platform-specific structure",
    ],
  },
];

const FEATURE_PILLARS = [
  {
    icon: SparklesIcon,
    label: "Generator",
    title: "Turn rough product notes into publish-ready listing drafts",
    body: "Add the details you already know and Tagwing structures the copy for Etsy, Shopify, or both without making you prompt your way through the process.",
    bullets: [
      "Titles, tags, descriptions, bullets, and SEO fields",
      "Quality score with improvement direction",
      "Designed for actual marketplace constraints",
    ],
  },
  {
    icon: MagnifyingGlassIcon,
    label: "Etsy audit",
    title: "Review live listings before they keep underperforming",
    body: "Paste a public Etsy URL to evaluate title strength, tag quality, description clarity, buyer intent, readability, and the highest-priority improvements.",
    bullets: [
      "Score and summary",
      "Optimized title, tags, and description",
      "Useful feedback instead of vague SEO advice",
    ],
  },
  {
    icon: BellAlertIcon,
    label: "Listing monitor",
    title: "Track SEO drift after a listing is already live",
    body: "Monitoring keeps an eye on score movement, snapshots, and optimization issues so you can catch changes before a good listing quietly gets weaker.",
    bullets: [
      "Pause and resume monitoring",
      "History + recommendation tracking",
      "Built for active Etsy listings",
    ],
  },
  {
    icon: HeartIcon,
    label: "Shop health",
    title: "Understand patterns across your catalog, not just one product",
    body: "See how your generated listing history stacks up across score, tag quality, title patterns, platform spread, and repeat weak spots.",
    bullets: [
      "Portfolio-level signals",
      "Improvement themes across listings",
      "Useful for growing shops and refresh cycles",
    ],
  },
  {
    icon: ArrowUpTrayIcon,
    label: "Bulk workflow",
    title: "Prepare larger product batches without rebuilding the process",
    body: "Upload catalog rows in CSV format and turn one-off writing sessions into a repeatable system for launches, seasonal work, and catalog refreshes.",
    bullets: [
      "Batch product processing",
      "CSV-based workflow",
      "Built for high-volume catalog work",
    ],
  },
  {
    icon: ClockIcon,
    label: "Saved history",
    title: "Return to old drafts, compare work, and keep momentum",
    body: "Generated listings stay accessible in your workspace, so product prep does not disappear after one session or force you to restart from scratch.",
    bullets: [
      "Saved listing history",
      "Copy and review later",
      "Supports iterative improvement",
    ],
  },
];

const WORKFLOW = [
  [
    "01",
    "Capture the product context once",
    "Start with the product name, then add buyer, material, use case, sizing, occasion, tone, and any details you already have.",
  ],
  [
    "02",
    "Generate copy for the right marketplace",
    "Create Etsy output, Shopify output, or both together depending on plan and publishing workflow.",
  ],
  [
    "03",
    "Review the score and guided fixes",
    "Use warnings, weak-spot signals, and prioritized recommendations to tighten the listing before it goes live.",
  ],
  [
    "04",
    "Audit, monitor, and improve over time",
    "Keep live listings under review, revisit saved work, and track where your catalog still needs attention.",
  ],
];

const OPERATING_CARDS = [
  {
    title: "Built around commercial listing operations",
    body: "Tagwing is structured around the work sellers and operators actually manage: drafting, evaluating, monitoring, refining, and scaling listing output.",
  },
  {
    title: "Clear next actions after every result",
    body: "Each result is organized for action, with quality signals, prioritized recommendations, and platform-aware output that is ready for review.",
  },
  {
    title: "Effective for focused teams and larger catalogs",
    body: "Whether you publish a handful of products or manage larger release cycles, the workflow stays consistent as volume grows.",
  },
];

const FAQS = [
  {
    question: "How is Tagwing different from a general AI writer?",
    answer:
      "General AI can write, but it does not naturally manage marketplace structure. Tagwing is built around Etsy and Shopify listing workflows, with platform-specific outputs, scores, audits, monitoring, and saved workspace history.",
  },
  {
    question: "Do I need SEO experience to get useful results?",
    answer:
      "No. You can start with normal product details, and Tagwing turns them into stronger copy while explaining weak spots in plain language.",
  },
  {
    question: "What do I need to enter to generate a listing?",
    answer:
      "A product name is enough to begin. More detail such as materials, sizing, buyer, personalization, occasion, or use case usually produces stronger and more specific output.",
  },
  {
    question: "Can I create Etsy and Shopify listings in the same workflow?",
    answer:
      "Yes. Growth and Pro support both-platform generation in one run. Etsy output includes title options, tags, and description guidance. Shopify output includes SEO title, meta description, bullets, and product description.",
  },
  {
    question: "What does the listing score help me decide?",
    answer:
      "It gives you a quick readiness signal. The score helps you judge whether the draft is close to publish-ready, still generic, or missing important details before you ship it.",
  },
  {
    question: "What happens after a listing is already live?",
    answer:
      "That is where Etsy Audit and Listing Monitor help. Audit reviews the current page, and monitoring keeps watching for changes, score movement, and new recommendation signals over time.",
  },
  {
    question: "Who is Tagwing best for?",
    answer:
      "It is a strong fit for Etsy sellers, Shopify operators, handmade brands, POD shops, digital product creators, and catalog-heavy ecommerce teams that want a repeatable listing system.",
  },
  {
    question: "Can I try the workflow before upgrading?",
    answer:
      "Yes. The free plan gives you room to evaluate the product in a live workflow. Paid plans expand monthly output, audits, both-platform generation, and bulk workflows.",
  },
];

const AUDIT_VIDEO_SRC = "/videos/etsy-audit-demo.webm";
const AUDIT_VIDEO_POSTER = "/videos/etsy-audit-poster.png";

const AUDIT_CARDS = [
  [
    "See what hurts the listing most",
    "Find weak tags, vague phrasing, and buyer-intent gaps before you spend another hour rewriting.",
  ],
  [
    "Get practical fixes, not abstract theory",
    "The audit responds with concrete improvements you can use immediately on title, tags, and description.",
  ],
  [
    "Use the process across active listings",
    "Move from one live URL to the next instead of treating optimization as a one-time task.",
  ],
];

const LISTING_EXAMPLES = [
  {
    name: "Lavender candle",
    input: "Lavender soy candle gift",
    before: {
      score: 52,
      title: "Handmade Candle",
      tags: ["candle", "gift", "home"],
      note: "Too broad to communicate scent, material, or gifting intent clearly enough for a competitive marketplace search result.",
    },
    after: {
      score: 91,
      title:
        "Lavender Soy Wax Candle, Handmade Aromatherapy Gift for Relaxation and Home Decor",
      tags: [
        "lavender candle",
        "soy wax candle",
        "aromatherapy gift",
        "self care gift",
      ],
      note: "Adds specificity around scent, material, use case, and buyer language without turning the copy into keyword clutter.",
    },
    stats: [
      ["Search fit", "More specific"],
      ["Buyer clarity", "Clearer value"],
      ["Next step", "Ready to refine"],
    ],
  },
  {
    name: "Dog collar",
    input: "Personalized leather dog collar",
    before: {
      score: 49,
      title: "Custom Dog Collar",
      tags: ["dog collar", "pet", "custom"],
      note: "The draft misses material, personalization, and sizing context, so it reads like many other listings in the same category.",
    },
    after: {
      score: 89,
      title:
        "Personalized Leather Dog Collar, Custom Name Pet Collar, Small to Large Puppy Gift",
      tags: [
        "personalized dog collar",
        "leather pet collar",
        "custom puppy gift",
        "name dog collar",
      ],
      note: "The stronger version uses real comparison details that help a shopper filter and decide faster.",
    },
    stats: [
      ["Search fit", "Specific terms"],
      ["Buyer clarity", "Material + size"],
      ["Next step", "Fewer gaps"],
    ],
  },
  {
    name: "Wedding template",
    input: "Editable wedding invitation template",
    before: {
      score: 55,
      title: "Wedding Invitation Template",
      tags: ["wedding", "template", "invite"],
      note: "Accurate, but too generic to communicate style, delivery format, or why this template is the right fit for a buyer.",
    },
    after: {
      score: 94,
      title:
        "Editable Wedding Invitation Template, Minimalist Printable Invite, Instant Download",
      tags: [
        "editable wedding invite",
        "printable invitation",
        "instant download template",
        "minimalist wedding",
      ],
      note: "The improved version adds the style, format, and instant-download intent that actually help shoppers decide.",
    },
    stats: [
      ["Search fit", "Higher intent"],
      ["Buyer clarity", "Style + format"],
      ["Next step", "Clear edits"],
    ],
  },
];

function formatCount(value) {
  const count = Number(value) || 0;
  if (count >= 1000) {
    return `${Math.floor(count / 100) / 10}k+`;
  }
  return count.toLocaleString();
}

function ListingExample() {
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % LISTING_EXAMPLES.length);
    }, 4200);

    return () => window.clearInterval(intervalId);
  }, []);

  const activeExample = LISTING_EXAMPLES[activeIndex];

  return (
    <div id="workflow-preview" className="relative mx-auto w-full max-w-3xl">
      <div className="absolute -inset-4 rounded-[2rem] bg-[#7C5CFF]/18 blur-3xl" />
      <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#131A2A]/92 shadow-2xl shadow-[#050816]/30 backdrop-blur">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 px-5 py-4">
          <div>
            <p className="text-xs font-black uppercase tracking-wide text-[#A998FF]">
              Live before-and-after direction
            </p>
            <p className="mt-1 text-sm font-semibold text-slate-400">
              Simple input: {activeExample.input}
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-[#1A2235] px-3 py-1 text-xs font-bold text-[#A998FF]">
            <span className="example-pulse-dot h-2 w-2 rounded-full bg-[#4DA3FF]" />
            Marketplace-ready output
          </div>
        </div>

        <div className="border-b border-white/10 px-4 py-3">
          <div className="flex flex-wrap gap-2">
            {LISTING_EXAMPLES.map((example, index) => (
              <button
                key={example.name}
                type="button"
                onClick={() => setActiveIndex(index)}
                className={`rounded-full px-3 py-1.5 text-xs font-bold transition-all ${
                  activeIndex === index
                    ? "bg-[#0B1020] text-[#F3F6FF] shadow-lg shadow-[#050816]/30"
                    : "bg-[#131A2A] text-[#94A3B8] ring-1 ring-white/10 hover:bg-[#1A2235]"
                }`}
              >
                {example.name}
              </button>
            ))}
          </div>
          <div className="mt-3 h-1 overflow-hidden rounded-full bg-[#202B42]">
            <div
              key={activeExample.name}
              className="example-progress h-full rounded-full bg-gradient-to-r from-[#7C5CFF] via-[#4DA3FF] to-[#F6C76B]"
            />
          </div>
        </div>

        <div key={activeExample.name} className="example-stage p-4">
          <div className="grid gap-4 lg:grid-cols-[1fr_auto_1fr] lg:items-stretch">
            <div className="spreadsheet-row rounded-[1.6rem] border border-rose-500/25 bg-rose-500/10 p-5">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-black uppercase tracking-wide text-rose-300">
                  Generic draft
                </p>
                <span className="rounded-full bg-[#131A2A] px-3 py-1 text-xs font-bold text-rose-300">
                  Score: {activeExample.before.score}
                </span>
              </div>
              <p className="mt-4 text-lg font-black text-[#F3F6FF]">
                {activeExample.before.title}
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                {activeExample.before.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full border border-rose-500/25 bg-[#131A2A] px-2.5 py-1 text-xs font-semibold text-[#94A3B8]"
                  >
                    {tag}
                  </span>
                ))}
              </div>
              <p className="mt-5 text-sm leading-6 text-slate-400">
                {activeExample.before.note}
              </p>
            </div>

            <div className="flex items-center justify-center">
              <div className="example-arrow flex h-12 w-12 items-center justify-center rounded-full bg-[#131A2A] text-[#7C5CFF] shadow-lg shadow-[#050816]/30 ring-1 ring-[#7C5CFF]/25">
                <ChevronRightIcon className="h-6 w-6 rotate-90 lg:rotate-0" />
              </div>
            </div>

            <div
              className="spreadsheet-row rounded-[1.6rem] border border-[#7C5CFF]/25 bg-[#1A2235]/82 p-5"
              style={{ animationDelay: "160ms" }}
            >
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
                  Optimized direction
                </p>
                <span className="rounded-full bg-[#131A2A] px-3 py-1 text-xs font-bold text-[#A998FF]">
                  Score: {activeExample.after.score}
                </span>
              </div>
              <p className="mt-4 text-lg font-black leading-7 text-[#F3F6FF]">
                {activeExample.after.title}
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                {activeExample.after.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full border border-[#7C5CFF]/25 bg-[#131A2A] px-2.5 py-1 text-xs font-semibold text-[#A998FF]"
                  >
                    {tag}
                  </span>
                ))}
              </div>
              <p className="mt-5 text-sm leading-6 text-[#94A3B8]">
                {activeExample.after.note}
              </p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 border-t border-white/10 bg-[#131A2A]/88 p-4 text-xs sm:grid-cols-3">
          {activeExample.stats.map(([label, value], index) => (
            <div
              key={label}
              className={`rounded-2xl p-3 ${
                index === 0
                  ? "border border-[#7C5CFF]/25 bg-[#1A2235]"
                  : index === 1
                    ? "border border-amber-500/25 bg-amber-500/10"
                    : "border border-emerald-500/25 bg-emerald-500/10"
              }`}
            >
              <p
                className={`font-bold ${
                  index === 0
                    ? "text-[#A998FF]"
                    : index === 1
                      ? "text-amber-300"
                      : "text-emerald-300"
                }`}
              >
                {label}
              </p>
              <p className="mt-1 text-slate-400">{value}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function DynamicReviewSection() {
  const [stats, setStats] = useState({ signedInUsers: 0, approvedReviews: 0 });
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const loadReviews = async () => {
      setLoading(true);
      try {
        const [{ data: statsData }, { data: reviewData }] = await Promise.all([
          api.get("/api/public/stats"),
          api.get("/api/public/reviews", { params: { limit: 6 } }),
        ]);

        if (!cancelled) {
          setStats(statsData ?? { signedInUsers: 0, approvedReviews: 0 });
          setReviews(Array.isArray(reviewData) ? reviewData : []);
        }
      } catch {
        if (!cancelled) {
          setStats({ signedInUsers: 0, approvedReviews: 0 });
          setReviews([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadReviews();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section id="reviews" className="px-6 py-20">
      <div className="mx-auto grid max-w-6xl gap-10 lg:grid-cols-[.85fr_1.15fr] lg:items-start">
        <div>
          <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
            Seller reviews
          </p>
          <h2 className="mt-3 text-4xl font-black tracking-tight text-[#F3F6FF]">
            Feedback from sellers using Tagwing in live workflows.
          </h2>
          <p className="mt-4 max-w-xl text-sm leading-6 text-[#94A3B8]">
            Reviews shown here are submitted through the product experience so
            prospective customers can evaluate Tagwing through direct seller
            feedback.
          </p>

          <div className="mt-6 grid max-w-md grid-cols-2 gap-3">
            <div className="rounded-2xl border border-white/10 bg-[#131A2A]/90 p-4">
              <p className="text-2xl font-black text-[#F3F6FF]">
                {formatCount(stats.signedInUsers)}
              </p>
              <p className="mt-1 text-xs font-semibold text-slate-400">
                signed-in users
              </p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-[#131A2A]/90 p-4">
              <p className="text-2xl font-black text-[#F3F6FF]">
                {formatCount(stats.approvedReviews)}
              </p>
              <p className="mt-1 text-xs font-semibold text-slate-400">
                approved reviews
              </p>
            </div>
          </div>

          <Link
            to="/review"
            className="mt-7 inline-flex items-center gap-2 rounded-full bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8]"
          >
            Leave a review
            <ArrowRightIcon className="h-4 w-4" />
          </Link>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {loading ? (
            <div className="rounded-3xl border border-white/10 bg-[#131A2A]/90 p-6 text-sm font-semibold text-slate-400 md:col-span-2">
              Loading seller reviews...
            </div>
          ) : reviews.length === 0 ? (
            <div className="rounded-3xl border border-white/10 bg-[#131A2A]/90 p-6 md:col-span-2">
              <p className="text-lg font-black text-[#F3F6FF]">
                No reviews yet
              </p>
              <p className="mt-2 text-sm leading-6 text-[#94A3B8]">
                Approved review submissions will appear here as they become
                available.
              </p>
            </div>
          ) : (
            reviews.map((review, index) => (
              <article
                key={review.id ?? `${review.displayName}-${index}`}
                className="feature-card rounded-3xl border border-white/10 bg-[#131A2A]/90 p-5 shadow-xl shadow-[#050816]/32"
                style={{ animationDelay: `${index * 90}ms` }}
              >
                <div className="flex items-center gap-1 text-sm font-black text-[#F6C76B]">
                  {"*".repeat(Math.max(1, Math.min(5, review.rating ?? 5)))}
                </div>
                <p className="mt-4 text-sm font-bold leading-6 text-[#F3F6FF]">
                  "{review.body}"
                </p>
                <p className="mt-4 text-xs font-semibold text-slate-400">
                  {review.displayName} - {review.sellerRole}
                </p>
              </article>
            ))
          )}
        </div>
      </div>
    </section>
  );
}

function AuditVideoSection() {
  return (
    <section
      id="audit-demo"
      className="border-t border-white/10 bg-[#101729]/82 px-6 py-20 backdrop-blur"
    >
      <div className="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[.85fr_1.15fr] lg:items-center">
        <div>
          <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
            Etsy audit demo
          </p>
          <h2 className="mt-3 text-4xl font-black tracking-tight text-[#F3F6FF]">
            Review live listings before you rewrite blindly.
          </h2>
          <p className="mt-4 max-w-xl text-sm leading-6 text-[#94A3B8]">
            Paste a live Etsy listing to evaluate title quality, tag coverage,
            description clarity, buyer intent, and readability with a clear
            score and prioritized recommendations.
          </p>

          <div className="mt-6 grid gap-3">
            {AUDIT_CARDS.map(([title, body]) => (
              <div
                key={title}
                className="feature-card rounded-2xl border border-white/10 bg-[#131A2A]/88 p-4 shadow-sm"
              >
                <div className="flex items-start gap-3">
                  <CheckCircleIcon className="mt-0.5 h-5 w-5 shrink-0 text-[#6EE7B7]" />
                  <div>
                    <h3 className="text-sm font-black text-[#F3F6FF]">
                      {title}
                    </h3>
                    <p className="mt-1 text-sm leading-6 text-[#94A3B8]">
                      {body}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <Link
            to="/audit"
            className="mt-7 inline-flex items-center gap-2 rounded-full bg-[#7C5CFF] px-5 py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8]"
          >
            Try Etsy Audit
            <ArrowRightIcon className="h-4 w-4" />
          </Link>
        </div>

        <div className="relative">
          <div className="absolute -inset-4 rounded-[2rem] bg-[#4DA3FF]/12 blur-3xl" />
          <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#0B1020] shadow-2xl shadow-[#050816]/40">
            <div className="flex items-center justify-between border-b border-white/10 bg-[#131A2A]/90 px-4 py-3">
              <div className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-rose-400" />
                <span className="h-2.5 w-2.5 rounded-full bg-amber-300" />
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-300" />
              </div>
              <span className="text-xs font-bold text-slate-500">
                tagwing-audit-demo.webm
              </span>
            </div>
            <video
              className="aspect-video w-full bg-[#0B1020] object-cover"
              autoPlay
              controls
              loop
              muted
              playsInline
              preload="metadata"
              poster={AUDIT_VIDEO_POSTER}
              aria-label="Etsy audit workflow demo"
            >
              <source src={AUDIT_VIDEO_SRC} type="video/webm" />
              Your browser does not support embedded video.
            </video>
          </div>
        </div>
      </div>
    </section>
  );
}

export default function LandingPage() {
  return (
    <div className="min-h-screen overflow-hidden bg-[radial-gradient(circle_at_18%_8%,rgba(124,92,255,0.18),transparent_32%),radial-gradient(circle_at_88%_0%,rgba(77,163,255,0.13),transparent_30%),linear-gradient(135deg,#0B1020_0%,#101729_48%,#0B1020_100%)] text-[#F3F6FF]">
      <header className="sticky top-0 z-30 border-b border-white/10 bg-transparent">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-6 py-4">
          <Link to="/" className="flex items-center gap-2">
            <LogoMark size={22} />
            <span className="text-lg font-black tracking-tight">Tagwing</span>
          </Link>

          <nav className="hidden items-center gap-5 lg:flex">
            {NAV_LINKS.map(([label, href]) => (
              <a
                key={label}
                href={href}
                className="text-sm font-semibold text-[#94A3B8] transition-colors hover:text-[#F3F6FF]"
              >
                {label}
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-3">
            <Link
              to="/pricing"
              className="hidden text-sm font-semibold text-[#94A3B8] transition-colors hover:text-[#F3F6FF] sm:block"
            >
              Pricing
            </Link>
            <Link
              to="/login"
              className="rounded-full bg-[#7C5CFF] px-4 py-2 text-sm font-bold text-[#F3F6FF] shadow-lg shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8]"
            >
              Open workspace
            </Link>
          </div>
        </div>
      </header>

      <main>
        <section className="mx-auto grid w-full max-w-7xl gap-14 px-6 pb-18 pt-12 lg:grid-cols-[.95fr_1.05fr] lg:items-center">
          <div>
            <h1 className="mt-6 max-w-4xl text-5xl font-black leading-[1.02] tracking-tight sm:text-6xl lg:text-7xl">
              One place to generate, audit, monitor, and improve your product
              listings.
            </h1>

            <p className="mt-6 max-w-2xl text-lg leading-8 text-[#94A3B8]">
              Tagwing turns product details into stronger marketplace copy, then
              helps you review what is weak, track what changed, and keep
              listing work organized as your catalog grows.
            </p>

            <div className="mt-7 grid max-w-2xl gap-3 sm:grid-cols-2">
              {HERO_POINTS.map((point, index) => (
                <div
                  key={point}
                  className="feature-card flex items-start gap-3 rounded-2xl border border-white/10 bg-[#131A2A]/88 px-4 py-3 text-sm font-semibold text-slate-300 shadow-sm backdrop-blur"
                  style={{ animationDelay: `${index * 90}ms` }}
                >
                  <CheckCircleIcon className="mt-0.5 h-5 w-5 shrink-0 text-[#7C5CFF]" />
                  <span>{point}</span>
                </div>
              ))}
            </div>

            <div className="mt-9 flex flex-wrap items-center gap-3">
              <Link
                to="/login"
                className="group inline-flex items-center gap-2 rounded-full bg-[#7C5CFF] px-6 py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#7C5CFF]/25 transition-all hover:-translate-y-0.5 hover:bg-[#6848E8]"
              >
                Start free
                <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Link>
              <a
                href="#features"
                className="rounded-full border border-slate-700 bg-[#131A2A]/88 px-6 py-3 text-sm font-bold text-slate-300 shadow-sm backdrop-blur transition-all hover:-translate-y-0.5 hover:border-slate-600"
              >
                Explore features
              </a>
            </div>

            <div className="mt-10 grid gap-4 sm:grid-cols-2">
              {PLATFORM_OUTPUTS.map(({ title, icon: Icon, items }) => (
                <div
                  key={title}
                  className="rounded-3xl border border-white/10 bg-[#101729]/82 p-5 shadow-xl shadow-[#050816]/20"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#131A2A] text-[#A998FF]">
                      <Icon className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-sm font-black text-[#F3F6FF]">
                        {title}
                      </p>
                      <p className="text-xs font-semibold text-slate-500">
                        Structured output, not generic copy
                      </p>
                    </div>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {items.map((item) => (
                      <span
                        key={item}
                        className="rounded-full border border-white/10 bg-[#131A2A] px-2.5 py-1 text-xs font-semibold text-slate-300"
                      >
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <ListingExample />
        </section>

        <section className="border-y border-white/10 bg-[#101729]/72 px-6 py-6 backdrop-blur">
          <div className="mx-auto grid max-w-7xl gap-4 md:grid-cols-3">
            {OPERATING_CARDS.map((card, index) => (
              <div
                key={card.title}
                className="feature-card rounded-3xl border border-white/10 bg-[#131A2A]/90 p-5 shadow-xl shadow-[#050816]/24"
                style={{ animationDelay: `${index * 100}ms` }}
              >
                <p className="text-base font-black text-[#F3F6FF]">
                  {card.title}
                </p>
                <p className="mt-2 text-sm leading-6 text-[#94A3B8]">
                  {card.body}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section id="features" className="px-6 py-20">
          <div className="mx-auto max-w-3xl text-center">
            <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
              Feature stack
            </p>
            <h2 className="mt-3 text-4xl font-black tracking-tight text-[#F3F6FF]">
              A complete optimization workspace for independent sellers and
              listing teams.
            </h2>
            <p className="mx-auto mt-4 max-w-2xl text-sm leading-6 text-[#94A3B8]">
              Tagwing brings together generation, live Etsy audits, monitoring,
              portfolio-level insight, saved history, and bulk catalog workflows
              in one operational system.
            </p>
          </div>

          <div className="mx-auto mt-10 grid max-w-7xl gap-5 md:grid-cols-2 xl:grid-cols-3">
            {FEATURE_PILLARS.map(
              ({ icon: Icon, label, title, body, bullets }, index) => (
                <div
                  key={title}
                  className="feature-card rounded-[1.8rem] border border-white/10 bg-[#131A2A]/88 p-6 shadow-xl shadow-[#050816]/32 backdrop-blur"
                  style={{ animationDelay: `${index * 90}ms` }}
                >
                  <div className="flex items-center justify-between gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#0B1020] text-[#F3F6FF]">
                      <Icon className="h-5 w-5" />
                    </div>
                    <span className="rounded-full border border-[#7C5CFF]/25 bg-[#0B1020] px-3 py-1 text-[11px] font-black uppercase tracking-wide text-[#A998FF]">
                      {label}
                    </span>
                  </div>
                  <h3 className="mt-5 text-xl font-black leading-7 text-[#F3F6FF]">
                    {title}
                  </h3>
                  <p className="mt-3 text-sm leading-6 text-[#94A3B8]">
                    {body}
                  </p>
                  <div className="mt-5 space-y-2">
                    {bullets.map((bullet) => (
                      <div
                        key={bullet}
                        className="flex items-start gap-2 text-sm font-semibold text-slate-300"
                      >
                        <CheckCircleIcon className="mt-0.5 h-4 w-4 shrink-0 text-[#7C5CFF]" />
                        <span>{bullet}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ),
            )}
          </div>
        </section>

        <section
          id="workflow"
          className="border-y border-white/10 bg-[#101729]/82 px-6 py-20 backdrop-blur"
        >
          <div className="mx-auto max-w-6xl">
            <div className="max-w-2xl">
              <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
                Workflow
              </p>
              <h2 className="mt-3 text-4xl font-black tracking-tight text-[#F3F6FF]">
                A structured path from product input to publication-ready
                output.
              </h2>
              <p className="mt-4 text-sm leading-6 text-[#94A3B8]">
                The workflow is designed to keep listing work consistent,
                efficient, and repeatable across individual products, active
                listings, and larger catalog updates.
              </p>
            </div>

            <div className="mt-10 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {WORKFLOW.map(([step, title, body], index) => (
                <div
                  key={step}
                  className="feature-card rounded-3xl border border-white/10 bg-[#131A2A]/88 p-6 shadow-xl shadow-[#050816]/32"
                  style={{ animationDelay: `${index * 90}ms` }}
                >
                  <span className="text-sm font-black text-[#A998FF]">
                    {step}
                  </span>
                  <h3 className="mt-5 text-xl font-black text-[#F3F6FF]">
                    {title}
                  </h3>
                  <p className="mt-3 text-sm leading-6 text-[#94A3B8]">
                    {body}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <AuditVideoSection />

        <DynamicReviewSection />

        <section
          id="faq"
          className="border-t border-white/10 bg-[#101729]/82 px-6 py-20 backdrop-blur"
        >
          <div className="mx-auto max-w-5xl">
            <div className="text-center">
              <p className="text-sm font-black uppercase tracking-wide text-[#A998FF]">
                FAQ
              </p>
              <h2 className="mt-3 text-4xl font-black tracking-tight text-[#F3F6FF]">
                Clear answers before you commit.
              </h2>
              <p className="mx-auto mt-4 max-w-3xl text-sm leading-6 text-[#94A3B8]">
                The basics around fit, workflow, platform coverage, audits,
                monitoring, and when upgrading starts to make sense.
              </p>
            </div>
            <div className="mt-10 grid grid-cols-1 gap-4 md:grid-cols-2">
              {FAQS.map(({ question, answer }) => (
                <div
                  key={question}
                  className="feature-card rounded-3xl border border-white/10 bg-[#131A2A]/90 p-6 shadow-xl shadow-[#050816]/32"
                >
                  <h3 className="font-black leading-6 text-[#F3F6FF]">
                    {question}
                  </h3>
                  <p className="mt-3 text-sm leading-7 text-[#94A3B8]">
                    {answer}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="px-6 py-20">
          <div className="mx-auto max-w-5xl rounded-[2rem] border border-white/10 bg-[#0B1020] p-8 text-center shadow-2xl shadow-[#050816]/45 sm:p-12">
            <p className="text-sm font-black uppercase tracking-wide text-[#BFB5FF]">
              Ready when the next product is ready
            </p>
            <h2 className="mx-auto mt-4 max-w-3xl text-4xl font-black tracking-tight text-[#F3F6FF]">
              Bring generation, evaluation, and ongoing optimization into one
              professional listing workflow.
            </h2>
            <p className="mx-auto mt-4 max-w-2xl text-sm leading-6 text-[#94A3B8]">
              Start with a single product and scale into audits, monitoring,
              historical review, portfolio insight, and bulk catalog execution
              as your business grows.
            </p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Link
                to="/login"
                className="group inline-flex items-center gap-2 rounded-full bg-[#4DA3FF] px-6 py-3 text-sm font-black text-[#F3F6FF] shadow-xl shadow-[#4DA3FF]/20 transition-all hover:-translate-y-0.5 hover:bg-[#72B7FF]"
              >
                Open Tagwing
                <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Link>
              <Link
                to="/pricing"
                className="rounded-full border border-white/15 bg-[#131A2A]/10 px-6 py-3 text-sm font-black text-[#F3F6FF] transition-all hover:-translate-y-0.5 hover:bg-[#131A2A]/15"
              >
                Compare plans
              </Link>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
