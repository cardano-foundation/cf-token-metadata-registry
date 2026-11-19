// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightOpenAPI, { openAPISidebarGroups } from 'starlight-openapi';
import tailwindcss from '@tailwindcss/vite';
import markdownExport from './scripts/markdown-export.mjs';

// Served as a GitHub Pages project site, under /cf-token-metadata-registry/. For a custom domain later: add
// public/CNAME, set SITE to the domain and drop BASE (see website/README.md).
const SITE = 'https://cardano-foundation.github.io';
const BASE = '/cf-token-metadata-registry';
const SITE_METADATA = {
  title: 'CF Token Metadata Registry',
  description: 'Query Cardano token metadata from CIP-26 and CIP-68, with additional information for CIP-113 programmable tokens.',
};

export default defineConfig({
  site: SITE,
  base: BASE,
  integrations: [
    starlight({
      ...SITE_METADATA,
      social: [
        { icon: 'github', label: 'GitHub', href: 'https://github.com/cardano-foundation/cf-token-metadata-registry' },
      ],
      editLink: {
        baseUrl: 'https://github.com/cardano-foundation/cf-token-metadata-registry/edit/main/website/',
      },
      customCss: ['./src/styles/starlight.css'],
      plugins: [
        // The API reference pages, generated from openapi/registry.json (refresh it with scripts/update-openapi.mjs).
        starlightOpenAPI([
          {
            base: 'api',
            schema: './openapi/registry.json',
            sidebar: { label: 'API reference', collapsed: false, operations: { badges: true } },
          },
        ]),
      ],
      components: {
        Footer: './src/components/Footer.astro',
        PageTitle: './src/components/PageTitle.astro',
        SiteTitle: './src/components/SiteTitle.astro',
      },
      sidebar: [
        { label: 'Overview', slug: 'overview' },
        { label: 'Getting started', slug: 'getting-started' },
        {
          label: 'Guides',
          items: [
            { label: 'Register a CIP-26 token', slug: 'guides/cip26' },
            { label: 'Mint a CIP-68 token', slug: 'guides/cip68' },
            { label: 'CIP-113 programmable tokens', slug: 'guides/cip113' },
            { label: 'Run your own registry', slug: 'guides/self-hosting' },
          ],
        },
        {
          label: 'Concepts',
          items: [
            { label: 'A permissive registry', slug: 'concepts/permissive' },
            { label: 'Validation errors', slug: 'concepts/validation-errors' },
            { label: 'Merging the standards', slug: 'concepts/merging' },
          ],
        },
        ...openAPISidebarGroups,
      ],
    }),
    markdownExport(SITE_METADATA),
  ],
  vite: {
    plugins: [tailwindcss()],
    // The request client is loaded with dynamic import() (src/scripts/try-it.ts), which Vite's dependency scan misses.
    // Discovered late, it makes Vite re-bundle and answer 504 for the dev toolbar since Astro 6.2
    // (withastro/astro#16630). Pre-bundling it up front keeps `astro dev` working; the production build is unaffected.
    optimizeDeps: {
      include: ['@scalar/api-client/modal', '@scalar/workspace-store/client'],
    },
  },
});
