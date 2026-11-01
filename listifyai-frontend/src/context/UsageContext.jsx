import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { useAuth } from "./AuthContext";
import api from "../lib/api";

const defaultUsageContext = {
  listingsUsed: 0,
  monthlyLimit: 5,
  unlimited: false,
  creditsRemaining: "0",
  plan: "free",
  refreshUsage: async () => {},
};

const UsageContext = createContext(defaultUsageContext);

export function UsageProvider({ children }) {
  const { user } = useAuth();

  const [listingsUsed, setListingsUsed] = useState(0);
  const [monthlyLimit, setMonthlyLimit] = useState(5);
  const [unlimited, setUnlimited] = useState(false);
  const [creditsRemaining, setCreditsRemaining] = useState("0");
  const [plan, setPlan] = useState("free");

  const fetchUsage = useCallback(async () => {
    if (!user) return;
    try {
      const { data } = await api.get("/api/me");
      setListingsUsed(data.listingsUsedThisMonth ?? 0);
      setMonthlyLimit(data.monthlyLimit ?? 5);
      setUnlimited(Boolean(data.unlimited));
      setCreditsRemaining(data.creditsRemaining ?? "0");
      setPlan(data.plan ?? "free");
    } catch {
      // Auth failures are already handled in the interceptor.
    }
  }, [user]);

  useEffect(() => {
    fetchUsage();
  }, [fetchUsage]);

  return (
    <UsageContext.Provider
      value={{
        listingsUsed,
        monthlyLimit,
        unlimited,
        creditsRemaining,
        plan,
        refreshUsage: fetchUsage,
      }}
    >
      {children}
    </UsageContext.Provider>
  );
}

export function useUsage() {
  return useContext(UsageContext);
}
