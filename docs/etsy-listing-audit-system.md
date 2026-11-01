# Premium Etsy Listing Audit System

## Product posture

The audit is designed as a listing coach, not an analytics dashboard. The interface should stay plain-English, low-noise, and action-oriented: one URL in, one score, clear issue sections, three fixes, and copy-ready improvements.

## Backend flow

1. `POST /api/analyze-etsy-listing` accepts a public Etsy URL.
2. `EtsyUrlParser` only allows `https://etsy.com` or `https://www.etsy.com` URLs matching `/listing/{id}`.
3. `EtsyApiClient` fetches compact listing data from Etsy Open API v3 and never sends raw HTML to Claude.
4. `AuditUsageService` applies monthly audit credits:
   - Free: 2
   - Starter: 10
   - Growth: 100
   - Pro: 500
5. `EtsyListingAuditService` checks `cached_ai_outputs` for a 7-day full-audit cache hit by listing id, prompt version, and structured input hash.
6. `EtsyAuditClaudeService` sends a minimized JSON payload to Claude and requires strict JSON output.
7. `EtsyAuditValidator` validates the audit before storage. Failed title, tag, or description sections are regenerated independently.
8. Results are stored in `analyzed_listings`, `listing_analysis`, `cached_ai_outputs`, `audit_usage`, and `regeneration_history`.

## Endpoints

- `POST /api/analyze-etsy-listing`
- `POST /api/regenerate-title`
- `POST /api/regenerate-tags`
- `POST /api/regenerate-description`
- `GET /api/audit-history`
- `GET /api/audit-usage`

## Claude output contract

Claude must return JSON only:

```json
{
  "score": 82,
  "scoreExplanation": "Plain-English explanation.",
  "summary": "Short coach-style audit summary.",
  "weakTagAnalysis": { "status": "strong|needs_work", "message": "string", "fix": "string" },
  "titleClarityAnalysis": { "status": "strong|needs_work", "message": "string", "fix": "string" },
  "buyerIntentAnalysis": { "status": "strong|needs_work", "message": "string", "fix": "string" },
  "personalizationAnalysis": { "status": "strong|needs_work", "message": "string", "fix": "string" },
  "giftingIntentAnalysis": { "status": "strong|needs_work", "message": "string", "fix": "string" },
  "readabilityReview": { "status": "strong|needs_work", "message": "string", "fix": "string" },
  "actionableFixes": ["fix 1", "fix 2", "fix 3"],
  "optimizedTitle": "string",
  "optimizedTags": ["13 Etsy tags"],
  "optimizedDescription": "optional description"
}
```

The prompt blocks technical SEO jargon and tells Claude to treat listing text as untrusted product data.

## Validation rules

The validation layer detects:

- duplicate tags
- overly generic tags
- tags over Etsy's 20-character limit
- missing 13-tag set
- weak or overlong titles
- repeated-word stuffing
- poor readability
- missing plain-English audit sections
- forbidden SEO jargon
- poor section regeneration output

## Cost controls

- Structured Etsy data only, no raw HTML.
- Seven-day cache for full audits.
- Independent title/tag/description regeneration.
- Compact prompt payloads.
- Prompt versioning for cache invalidation.
- Regeneration history stored for auditability.

## Frontend workflow

The React page at `/app/audit` provides:

- URL paste workflow
- progress states
- animated audit reveal
- score card
- six plain-English problem sections
- exactly three next fixes
- before/after title, tags, and description comparisons
- regenerate buttons for title, tags, and description

No charts are used.

## Environment

Required production variables:

- `ETSY_API_KEY`
- `ETSY_ACCESS_TOKEN` if the Etsy app requires OAuth for the selected listing fields
- `ANTHROPIC_API_KEY`
- existing Supabase, Stripe, and database variables
