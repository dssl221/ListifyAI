import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import * as Sentry from "@sentry/react";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { UsageProvider } from "./context/UsageContext";
import AppRouter from "./router/AppRouter";
import ErrorBoundary, { ErrorFallback } from "./components/ui/ErrorBoundary";
import { initSentry } from "./lib/sentry";
import "./index.css";

initSentry();

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <UsageProvider>
          <Sentry.ErrorBoundary
            fallback={({ error, componentStack }) => (
              <ErrorFallback error={error} componentStack={componentStack} />
            )}
          >
            <ErrorBoundary>
              <AppRouter />
            </ErrorBoundary>
          </Sentry.ErrorBoundary>
        </UsageProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
