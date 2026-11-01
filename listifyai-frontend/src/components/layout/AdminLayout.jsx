import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  Bars3Icon,
  BellAlertIcon,
  ChatBubbleLeftRightIcon,
  ClipboardDocumentListIcon,
  HomeIcon,
  SparklesIcon,
  UsersIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import api from "../../lib/api";
import { useAuth } from "../../context/AuthContext";
import LogoMark from "../ui/LogoMark";

const ADMIN_NAV = [
  { label: "Dashboard", to: "/admin/dashboard", icon: HomeIcon },
  { label: "Users", to: "/admin/users", icon: UsersIcon },
  { label: "Tickets", to: "/admin/tickets", icon: ChatBubbleLeftRightIcon },
  { label: "Monitoring", to: "/admin/monitoring", icon: BellAlertIcon },
  { label: "Claude Usage", to: "/admin/claude", icon: SparklesIcon },
  { label: "Audit Logs", to: "/admin/audit", icon: ClipboardDocumentListIcon },
];

export default function AdminLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [openTickets, setOpenTickets] = useState(0);

  useEffect(() => {
    let active = true;
    const fetchCount = async () => {
      try {
        const { data } = await api.get("/api/admin/tickets/count");
        if (active) setOpenTickets(data.open ?? 0);
      } catch {
        if (active) setOpenTickets(0);
      }
    };
    fetchCount();
    const timer = setInterval(fetchCount, 60000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, []);

  const handleSignOut = async () => {
    await signOut();
    navigate("/login");
  };

  const sidebar = (
    <aside className="flex h-full w-64 flex-shrink-0 flex-col border-r border-white/10 bg-[#131A2A]">
      <div className="border-b border-white/10 px-4 py-5">
        <div className="flex items-center gap-3">
          <LogoMark size={28} />
          <div>
            <p className="text-base font-black text-[#F3F6FF]">TagWing Admin</p>
            <p className="text-xs font-bold text-[#7C5CFF]">Admin Panel</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {ADMIN_NAV.map(({ label, to, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-r-xl py-2.5 text-sm font-bold transition-colors ${
                isActive
                  ? "border-l-2 border-[#7C5CFF] bg-[#7C5CFF]/20 pl-[10px] pr-3 text-[#7C5CFF]"
                  : "px-3 text-[#94A3B8] hover:bg-white/5 hover:text-[#F3F6FF]"
              }`
            }
          >
            <Icon className="h-5 w-5 flex-shrink-0" />
            <span className="flex-1">{label}</span>
            {label === "Tickets" && openTickets > 0 && (
              <span className="rounded-full bg-rose-500 px-2 py-0.5 text-[10px] font-black text-white">
                {openTickets}
              </span>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="space-y-3 border-t border-white/10 px-4 py-4">
        <p className="truncate text-xs font-semibold text-[#94A3B8]">
          {user?.email}
        </p>
        <Link
          to="/app/generate"
          className="block text-sm font-bold text-[#F3F6FF] hover:text-[#7C5CFF]"
        >
          Back to App
        </Link>
        <button
          onClick={handleSignOut}
          className="text-sm font-bold text-rose-300 hover:text-rose-200"
        >
          Sign out
        </button>
      </div>
    </aside>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-[#0B1020] text-[#F3F6FF]">
      <div className="hidden md:flex">{sidebar}</div>
      {mobileOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <button
            className="absolute inset-0 bg-black/50"
            onClick={() => setMobileOpen(false)}
            aria-label="Close menu"
          />
          <div className="relative z-50">{sidebar}</div>
        </div>
      )}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center gap-3 border-b border-white/10 bg-[#131A2A] px-4 py-3 md:hidden">
          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="rounded-lg p-2 text-[#94A3B8] hover:bg-white/5"
          >
            {mobileOpen ? (
              <XMarkIcon className="h-5 w-5" />
            ) : (
              <Bars3Icon className="h-5 w-5" />
            )}
          </button>
          <span className="text-sm font-black">TagWing Admin</span>
        </header>
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
