# Token Metadata Registry — website

The project website and user documentation, built with [Astro](https://astro.build) +
[Starlight](https://starlight.astro.build) + Tailwind, the same setup as [Mesmo](https://github.com/bloxbean/mesmo).
It is written for **people who mint, register or integrate tokens**, not for contributors to this repository (they
have the ADRs in `../adr`).

## Develop

```bash
cd website
npm install
PUBLIC_REGISTRY_API_URL=http://localhost:8080 npm run dev   # http://localhost:4321/cf-token-metadata-registry/
npm run build                                               # → dist/
```

## Branding

The site follows the Cardano Foundation's style, as on [cardanofoundation.org](https://cardanofoundation.org): the
Switzer typeface (loaded from Fontshare), a greyscale palette with one violet-blue accent (`#4221e2`), regular-weight
headings with tight tracking, pill-shaped buttons, the soft orange-to-blue hero glow and a dark footer (`#131313`).
The colours and font are set in `src/styles/starlight.css` (docs) and `src/styles/landing.css` (landing page). The
Cardano Foundation logo is `public/images/cf-logo-text.svg`, with a white version for the dark theme; the header
(`src/components/SiteTitle.astro`) shows it next to the product name.

## Where content lives

- `src/pages/index.astro`: the landing page.
- `src/content/docs/`: the documentation (Starlight); the sidebar is in `astro.config.mjs`.
- `src/components/Validator.astro`: the interactive validator, a form over `POST /api/v2/validate/{cip26,cip68,cip113}`
  (ADR-023). It calls the registry at `PUBLIC_REGISTRY_API_URL` (set at build time; a visitor can change it on the page).
  That registry must allow the site's origin in `REGISTRY_VALIDATE_CORS_ALLOWED_ORIGINS`.
- `scripts/llms-integration.mjs`: publishes `/llms.txt` and `/llms-full.txt` for AI tools.
- `src/components/Footer.astro`: the footer, with the credit to the BloxBean libraries the registry is built on.

## Deployment

`.github/workflows/website-deploy.yml` builds and publishes `website/dist` to the `gh-pages` branch on a push to `main`
that touches `website/` (or by manual dispatch). The site is a GitHub Pages project site:
**https://cardano-foundation.github.io/cf-token-metadata-registry/**, so `astro.config.mjs` sets
`base: '/cf-token-metadata-registry'`. GitHub Pages must be enabled once in the repository settings, serving from the
`gh-pages` branch. The registry address the validator uses comes from the repository variable `REGISTRY_API_URL`.

For a custom domain later: add `public/CNAME`, set `SITE` to the domain and remove `BASE` in `astro.config.mjs`.
