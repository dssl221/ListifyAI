import { createContext, useContext, useEffect, useState } from "react";
import { supabase } from "../lib/supabaseClient";
import api, { DEV_AUTH_TOKEN_KEY } from "../lib/api";

const defaultAuthContext = {
  user: null,
  profile: null,
  loading: false,
  signOut: async () => {},
  fetchProfile: async () => {},
  updateDisplayName: async () => {},
};

const AuthContext = createContext(defaultAuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // Supabase auth user
  const [profile, setProfile] = useState(null); // /api/me response
  const [role, setRole] = useState("user");
  const [loading, setLoading] = useState(true);

  // Fetch backend profile after Supabase confirms auth
  const fetchProfile = async () => {
    try {
      const { data } = await api.get("/api/me");
      setProfile(data);
      setRole(data.role ?? "user");
      return data;
    } catch {
      setProfile(null);
      setRole("user");
      return null;
    }
  };

  useEffect(() => {
    // Restore session on mount
    supabase.auth
      .getSession()
      .then(({ data: { session } }) => {
        const supabaseUser = session?.user ?? null;
        setUser(supabaseUser);
        if (supabaseUser) {
          setLoading(false);
          fetchProfile();
        } else {
          const devToken = window.localStorage.getItem(DEV_AUTH_TOKEN_KEY);
          if (devToken) {
            fetchProfile().then((profileData) => {
              if (profileData) {
                setUser({
                  id: profileData.id,
                  email: profileData.email,
                  dev: true,
                });
              } else {
                window.localStorage.removeItem(DEV_AUTH_TOKEN_KEY);
              }
              setLoading(false);
            });
          } else {
            setLoading(false);
          }
        }
      })
      .catch(() => {
        setUser(null);
        setProfile(null);
        setLoading(false);
      });

    // Listen for future auth state changes
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange(async (_event, session) => {
      const supabaseUser = session?.user ?? null;
      setUser(supabaseUser);

      if (supabaseUser) {
        fetchProfile();
      } else {
        setProfile(null);
        setRole("user");
      }
    });

    return () => subscription.unsubscribe();
  }, []);

  const signOut = async () => {
    await supabase.auth.signOut();
    window.localStorage.removeItem(DEV_AUTH_TOKEN_KEY);
    setUser(null);
    setProfile(null);
    setRole("user");
  };

  const updateDisplayName = async (displayName) => {
    const { data } = await api.put("/api/me/display-name", { displayName });
    setProfile(data);
    setRole(data.role ?? "user");
    return data;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        role,
        loading,
        signOut,
        fetchProfile,
        updateDisplayName,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
