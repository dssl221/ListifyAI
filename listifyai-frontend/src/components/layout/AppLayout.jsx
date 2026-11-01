import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  SparklesIcon,
  HomeIcon,
  MagnifyingGlassIcon,
  HeartIcon,
  BellAlertIcon,
  ArrowUpTrayIcon,
  ClockIcon,
  Cog6ToothIcon,
  ChatBubbleLeftRightIcon,
  Bars3Icon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { useUsage } from "../../context/UsageContext";
import UsageBadge from "./UsageBadge";
import LogoMark from "../ui/LogoMark";

const NAV_ITEMS = [
  { label: "Dashboard", to: "/app/dashboard", icon: HomeIcon },
  { label: "Etsy Audit", to: "/app/audit", icon: MagnifyingGlassIcon },
  { label: "Shop Health", to: "/app/shop-health", icon: HeartIcon },
  { label: "Listing Monitor", to: "/app/monitoring", icon: BellAlertIcon },
  { label: "Generator", to: "/app/generate", icon: SparklesIcon },
  { label: "Bulk Upload", to: "/app/bulk", icon: ArrowUpTrayIcon },
  { label: "History", to: "/app/history", icon: ClockIcon },
  { label: "Settings", to: "/app/settings", icon: Cog6ToothIcon },
  { label: "Support", to: "/app/support", icon: ChatBubbleLeftRightIcon },
];

function preferredName(user, profile) {
  if (profile?.displayName?.trim()) return profile.displayName.trim();

  const metadataName =
    user?.user_metadata?.full_name ||
    user?.user_metadata?.name ||
    user?.user_metadata?.display_name;

  if (metadataName?.trim()) return metadataName.trim();

  const emailPrefix = user?.email?.split("@")[0];
  if (!emailPrefix) return "";

  return emailPrefix
    .split(/[._-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function NavItem({ to, label, Icon, plan, onClick }) {
  const showGrowthBadge =
    to === "/app/shop-health" && (plan === "free" || plan === "starter");

  return (
    <NavLink
      to={to}
      end={to === "/app"}
      onClick={onClick}
      className={({ isActive }) =>
        `flex items-center gap-3 py-2.5 rounded-2xl text-sm font-bold transition-all ${
          isActive
            ? "bg-[#0B1020] text-[#F3F6FF] shadow-lg shadow-[#050816]/35 border-l-2 border-[#7C5CFF] pl-[10px] pr-3"
            : "px-3 text-[#94A3B8] hover:-translate-y-0.5 hover:bg-[#131A2A] hover:text-[#F3F6FF] hover:shadow-sm"
        }`
      }
    >
      <Icon className="w-5 h-5 flex-shrink-0" />
      {label}
      {showGrowthBadge && (
        <span className="ml-auto text-[10px] text-slate-600 font-bold">
          Growth+
        </span>
      )}
    </NavLink>
  );
}

export default function AppLayout() {
  const { user, profile, signOut, updateDisplayName } = useAuth();
  const { plan } = useUsage();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [displayName, setDisplayName] = useState(() =>
    preferredName(user, profile),
  );
  const [prefilledName, setPrefilledName] = useState(false);
  const [nameError, setNameError] = useState("");
  const [savingName, setSavingName] = useState(false);

  const savedDisplayName = preferredName(user, profile);
  const shouldAskForName = profile?.namePromptCompleted === false;

  useEffect(() => {
    setPrefilledName(false);
    setNameError("");
    setSavingName(false);
  }, [user?.id, profile?.id]);

  useEffect(() => {
    if (shouldAskForName && !prefilledName) {
      setDisplayName(preferredName(user, profile));
      setPrefilledName(true);
    }
  }, [prefilledName, profile, shouldAskForName, user]);

  const handleSignOut = async () => {
    await signOut();
    navigate("/login");
  };

  const handleDisplayNameSubmit = async (event) => {
    event.preventDefault();
    const trimmedName = displayName.trim().replace(/\s+/g, " ");

    if (!trimmedName) {
      setNameError("Enter the name you want us to use.");
      return;
    }

    if (trimmedName.length > 50) {
      setNameError("Use 50 characters or fewer.");
      return;
    }

    setSavingName(true);
    setNameError("");
    try {
      await updateDisplayName(trimmedName);
    } catch {
      setNameError("Could not save your name right now.");
    } finally {
      setSavingName(false);
    }
  };

  const sidebar = (
    <aside className="flex flex-col h-full bg-[#131A2A]/90 backdrop-blur-xl border-r border-white/10 w-64 flex-shrink-0 shadow-2xl shadow-[#050816]/32">
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-white/10">
        <div>
          <div className="flex items-center gap-2">
            <LogoMark size={24} />
            <span className="text-lg font-black text-[#F3F6FF] tracking-tight">
              Tagwing
            </span>
          </div>
          <p className="text-xs font-medium text-slate-500">seller workspace</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
        {NAV_ITEMS.map(({ label, to, icon: Icon }) => (
          <NavItem
            key={to}
            to={to}
            label={label}
            Icon={Icon}
            plan={plan}
            onClick={() => setMobileOpen(false)}
          />
        ))}
      </nav>

      {/* Footer - user info */}
      <div className="px-4 py-4 border-t border-white/10 space-y-3">
        <div className="rounded-2xl border border-[#7C5CFF]/25 bg-[#1A2235]/82 p-3">
          <p className="text-xs font-semibold text-[#A998FF] truncate">
            {savedDisplayName || user?.email}
          </p>
          {savedDisplayName && (
            <p className="mt-0.5 text-[11px] font-medium text-slate-500 truncate">
              {user?.email}
            </p>
          )}
          <p
            className={`mt-1 text-xs font-bold capitalize ${
              plan === "pro"
                ? "text-[#D8D0FF]"
                : plan === "growth"
                  ? "text-[#A998FF]"
                  : plan === "starter"
                    ? "text-[#9DCCFF]"
                    : "text-[#94A3B8]"
            }`}
          >
            {plan ?? "free"} plan
          </p>
        </div>
        <button
          onClick={handleSignOut}
          className="w-full text-left text-xs text-slate-400 hover:text-rose-400 transition-colors"
        >
          Sign out
        </button>
      </div>
    </aside>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-[radial-gradient(circle_at_15%_0%,rgba(124,92,255,0.16),transparent_30%),radial-gradient(circle_at_85%_0%,rgba(77,163,255,0.10),transparent_32%),linear-gradient(135deg,#0B1020_0%,#101729_52%,#0B1020_100%)]">
      {/* Desktop sidebar */}
      <div className="hidden md:flex">{sidebar}</div>

      {/* Mobile sidebar overlay */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <div
            className="fixed inset-0 bg-black/40"
            onClick={() => setMobileOpen(false)}
          />
          <div className="relative z-50 flex h-full">{sidebar}</div>
        </div>
      )}

      {/* Main area */}
      <div className="flex flex-col flex-1 overflow-hidden">
        {/* Top bar */}
        <header className="flex items-center justify-between px-4 py-3 bg-[#131A2A]/88 backdrop-blur-xl border-b border-white/10 gap-4 shadow-sm shadow-[#050816]/32">
          {/* Mobile menu toggle */}
          <button
            className="md:hidden p-1 rounded text-slate-400 hover:bg-[#202B42]"
            onClick={() => setMobileOpen(!mobileOpen)}
          >
            {mobileOpen ? (
              <XMarkIcon className="w-5 h-5" />
            ) : (
              <Bars3Icon className="w-5 h-5" />
            )}
          </button>

          {/* Desktop logo placeholder to keep usage badge right-aligned */}
          <div className="hidden md:flex items-center gap-2 text-sm font-bold text-slate-400">
            <span className="h-2 w-2 rounded-full bg-[#4DA3FF] animate-pulse" />
            Live listing workspace
          </div>

          {/* Right side */}
          <div className="flex items-center gap-3 ml-auto">
            <UsageBadge />
            <span className="hidden sm:block text-sm font-medium text-[#94A3B8] truncate max-w-[200px]">
              {savedDisplayName || user?.email}
            </span>
            <button
              onClick={handleSignOut}
              className="text-xs text-slate-400 hover:text-rose-400 transition-colors whitespace-nowrap"
            >
              Sign out
            </button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <Outlet />
        </main>
      </div>

      {shouldAskForName && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#050816]/88 px-4 backdrop-blur-md">
          <form
            onSubmit={handleDisplayNameSubmit}
            className="w-full max-w-md rounded-2xl border border-white/10 bg-[#131A2A] p-6 shadow-2xl shadow-black/40"
          >
            <p className="text-xs font-black uppercase tracking-wide text-[#A998FF]">
              Welcome to Tagwing
            </p>
            <h2 className="mt-2 text-2xl font-black text-[#F3F6FF]">
              What should we call you?
            </h2>
            <p className="mt-2 text-sm text-slate-400">
              We will remember this for your workspace greeting whenever you
              sign in.
            </p>
            <label className="mt-5 block">
              <span className="text-xs font-bold uppercase tracking-wide text-slate-500">
                Your name
              </span>
              <input
                autoFocus
                value={displayName}
                onChange={(event) => {
                  setDisplayName(event.target.value);
                  setNameError("");
                }}
                maxLength={50}
                className="mt-2 w-full rounded-xl border border-white/10 bg-[#0B1020] px-4 py-3 text-base font-semibold text-[#F3F6FF] outline-none transition-colors placeholder:text-slate-600 focus:border-[#7C5CFF]"
                placeholder="Name"
              />
            </label>
            {nameError && (
              <p className="mt-3 text-sm font-semibold text-rose-300">
                {nameError}
              </p>
            )}
            <button
              type="submit"
              disabled={savingName}
              className="mt-5 w-full rounded-xl bg-[#7C5CFF] px-4 py-3 text-sm font-black text-[#F3F6FF] transition-colors hover:bg-[#6848E8] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {savingName ? "Saving..." : "Continue"}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
