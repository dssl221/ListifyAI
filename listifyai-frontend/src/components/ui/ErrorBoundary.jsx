import { Component } from "react";

const isDev = import.meta.env.DEV;

export function ErrorFallback({ error, componentStack }) {
  return (
    <div className="min-h-screen bg-[#1A2235] flex items-center justify-center px-4">
      <div className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-[#131A2A] p-8 shadow-sm">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#1A2235]">
          <span className="text-2xl text-[#7C5CFF]">!</span>
        </div>
        <div className="text-center">
          <h1 className="text-2xl font-bold text-[#F3F6FF]">
            Something went wrong
          </h1>
          <p className="mt-2 text-sm text-slate-400">
            An unexpected error occurred. Try refreshing the page.
          </p>
        </div>

        <div className="mt-6 flex justify-center">
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="rounded-xl bg-[#7C5CFF] px-4 py-2.5 text-sm font-semibold text-[#F3F6FF] transition-colors hover:bg-[#6848E8]"
          >
            Refresh page
          </button>
        </div>

        {isDev && error && (
          <pre className="mt-6 overflow-x-auto rounded-xl border border-rose-500/25 bg-rose-500/10 p-4 text-left text-xs leading-6 text-rose-300 whitespace-pre-wrap">
            {error.message}
            {componentStack ? `\n${componentStack}` : ""}
            {error.stack ? `\n${error.stack}` : ""}
          </pre>
        )}
      </div>
    </div>
  );
}

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo });
  }

  render() {
    const { hasError, error, errorInfo } = this.state;

    if (!hasError) {
      return this.props.children;
    }

    return (
      <ErrorFallback error={error} componentStack={errorInfo?.componentStack} />
    );
  }
}
