# GitHub Actions Manager — Go-to-Market Plan

*Last updated: 2026-06-11. Sensitive figures (revenue, customer names, prospect lists) live outside the
public repo; this document covers strategy and execution.*

## Positioning

**The missing GitHub Actions UI for JetBrains IDEs.**

- GitHub ships an official Actions extension for VS Code; JetBrains users have no official equivalent.
  This plugin fills that gap.
- The built-in JetBrains GitHub plugin covers PRs and gists but has no Actions support.
- GitHub Enterprise Server support is the enterprise wedge: GHES web UIs are slow, behind VPNs, and
  painful for log reading and deployment approvals. "Approve deployments and read logs without leaving
  IntelliJ" lands hardest there.

## Ideal customer profile

Companies with 50–500 engineers, JVM/Kotlin or Python backends (JetBrains IDE shops), on GitHub Actions,
with a platform/DevEx team that owns tooling budget. Big enough for 20+ seats, small enough that a team
lead can expense the license without procurement.

Priority geographies (by observed trial→paid conversion): US, UK, Canada, DACH, Nordics, Ireland.

## What the funnel data showed

- The biggest leak is **downloads → trials**: the free tier satisfies users because paid features are
  invisible until licensed. Fix: locked-but-visible paid actions with a one-click trial affordance.
- Most buyers convert **well after day 7** of first trying the plugin — the trial was extended from
  7 to 30 days to match.
- Monthly subscriptions churn fast ("subscribe for the week I need it"); annual retention is healthy.
  Packaging presents annual-first.

## Phases

### P1 — Funnel fixes ✅ (done)

1. Trial extended 7 → 30 days.
2. Paid-feature discoverability in the free product.
3. Annual-first packaging; perpetual/fallback option.
4. Win-back outreach to churned customers via discount codes.

### P2 — Top of funnel (in progress)

1. **Marketplace listing refresh** — positioning line, outcome-led copy, free-vs-paid clarity
   (the listing description is generated from `README.md` between the
   `<!-- Plugin description -->` markers; it updates on the next plugin release).
2. **Comparison & use-case content** — "browser vs `gh` CLI vs VS Code extension vs JetBrains plugin",
   "approve deployments from the IDE", GHES-focused piece. Published on the company site, submitted to
   Kotlin Weekly, JVM Weekly, and Console.dev.
3. **Community launches** — Show HN, r/Kotlin, r/Jetbrains, Kotlin Slack.
4. Pitch a JetBrains Platform guest blog post (they feature marketplace vendors).
5. Skip paid conference sponsorship (not ROI-positive at this price point); CFP talks only.

Draft assets live in `notes/marketing/` (untracked) until approved and published.

### P3 — B2B outbound (next)

1. Build a ~100-org prospect list from owned signals (GitHub stargazers, issue filers, Marketplace
   reviewers, discount-form responses) merged with GitHub-Enterprise × JetBrains company datasets.
2. Batched outreach (15–20/week) to platform-eng/DevEx leads: engineer-to-engineer email, 30-day team
   trial code, volume discount offer. Org purchases go through the JetBrains shop/quote flow
   (volume quotes via marketplace@jetbrains.com).
3. Dedicated GHES landing page and outreach track for regulated JVM shops.
4. Ask the JetBrains marketplace team about promo programs and the reseller channel.

## Success metrics (90-day)

- Trials per month back above 100 and growing.
- Trial → paid conversion ≥ 6%.
- Double ARR; 10 multi-seat organization customers.
