// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightOpenAPI, { openAPISidebarGroups } from 'starlight-openapi';
import tailwindcss from '@tailwindcss/vite';
import llmsIntegration from './scripts/llms-integration.mjs';

// Served as a GitHub Pages project site, under /cf-token-metadata-registry/. For a custom domain later: add
// public/CNAME, set SITE to the domain and drop BASE (see website/README.md).
const SITE = 'https://cardano-foundation.github.io';
const BASE = '/cf-token-metadata-registry';

export default defineConfig({
  site: SITE,
  base: BASE,
  integrations: [
    starlight({
      title: 'CF Token Metadata Registry',
      description: 'The Cardano Foundation token metadata registry: Cardano token metadata from CIP-26, CIP-68 and CIP-113 in one API, with a validator to check your token before you mint it.',
      social: [
        { icon: 'github', label: 'GitHub', href: 'https://github.com/cardano-foundation/cf-token-metadata-registry' },
      ],
      editLink: {
        baseUrl: 'https://github.com/cardano-foundation/cf-token-metadata-registry/edit/develop/website/',
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
        { label: 'Validator', slug: 'validator' },
        {
          label: 'Guides',
          items: [
            { label: 'Register a CIP-26 token', slug: 'guides/cip26' },
            { label: 'Mint a CIP-68 token', slug: 'guides/cip68' },
            { label: 'CIP-113 programmable tokens', slug: 'guides/cip113' },
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
        {
          label: 'Reference',
          items: [
            { label: 'API', slug: 'reference/api' },
            { label: 'Try the API (Scalar)', link: '/api-reference/', attrs: { target: '_self' } },
            { label: 'Run your own registry', slug: 'reference/self-hosting' },
          ],
        },
        ...openAPISidebarGroups,
      ],
    }),
    llmsIntegration(),
  ],
  vite: {
    plugins: [tailwindcss()],
  },
});
