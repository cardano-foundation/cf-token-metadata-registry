// Builds the AI ingestion files from the docs (llmstxt.org convention):
//   llms.txt       an index of the pages, with their descriptions
//   llms-full.txt  every page, concatenated, for tools that read the whole site at once
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const DOCS = path.join(ROOT, 'src', 'content', 'docs');
const SITE_URL = 'https://cardano-foundation.github.io/cf-token-metadata-registry';

async function listPages(dir, prefix = '') {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const pages = [];
  for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      pages.push(...(await listPages(full, `${prefix}${entry.name}/`)));
    } else if (/\.mdx?$/.test(entry.name)) {
      pages.push({ file: full, slug: `${prefix}${entry.name.replace(/\.mdx?$/, '')}` });
    }
  }
  return pages;
}

function frontmatter(text) {
  const match = text.match(/^---\n([\s\S]*?)\n---\n/);
  const fields = {};
  if (match) {
    for (const line of match[1].split('\n')) {
      const kv = line.match(/^(\w+):\s*(.*)$/);
      if (kv) fields[kv[1]] = kv[2].replace(/^['"]|['"]$/g, '');
    }
  }
  return { fields, body: match ? text.slice(match[0].length) : text };
}

export async function generateLlmsFiles({ outDir, logger }) {
  const pages = await listPages(DOCS);
  const index = [
    '# Cardano Token Metadata Registry',
    '',
    '> One API for Cardano token metadata from CIP-26 (off-chain registry), CIP-68 (on-chain, labels 333 and 444) and '
      + 'CIP-113 (programmable tokens), with validation errors that say what departs from each CIP, and an endpoint '
      + 'to validate a token before you mint it.',
    '',
    '## Docs',
    '',
  ];
  const full = [];
  for (const page of pages) {
    const { fields, body } = frontmatter(await fs.readFile(page.file, 'utf8'));
    const url = `${SITE_URL}/${page.slug === 'index' ? '' : `${page.slug}/`}`;
    index.push(`- [${fields.title ?? page.slug}](${url})${fields.description ? `: ${fields.description}` : ''}`);
    full.push(`# ${fields.title ?? page.slug}\n\nSource: ${url}\n\n${body.replace(/^import .*$/gm, '').trim()}\n`);
  }
  await fs.mkdir(outDir, { recursive: true });
  await fs.writeFile(path.join(outDir, 'llms.txt'), `${index.join('\n')}\n`);
  await fs.writeFile(path.join(outDir, 'llms-full.txt'), full.join('\n---\n\n'));
  logger.info(`[llms-txt] wrote llms.txt and llms-full.txt (${pages.length} pages)`);
}
