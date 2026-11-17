// Refreshes openapi/registry.json, the spec the API reference pages are generated from.
//
//   node scripts/update-openapi.mjs [url-or-file]
//
// The source defaults to https://tokens.cardano.org/apidocs. Point it at an instance running the branch you document
// (for example http://localhost:8081/apidocs) when the public registry does not serve the endpoints yet.
//
// The registry's own spec also lists operator endpoints (Yaci Store admin, actuator, the deprecated /health), names the
// server it was fetched from and tags the V2 controller by its class name. Only the public API is kept, with the public
// registries as servers and readable tags.
import { readFile, writeFile } from 'node:fs/promises';

const source = process.argv[2] ?? 'https://tokens.cardano.org/apidocs';
const target = new URL('../openapi/registry.json', import.meta.url);

const spec = JSON.parse(
  /^https?:\/\//.test(source) ? await (await fetch(source)).text() : await readFile(source, 'utf8'),
);

const PUBLIC_PATHS = [/^\/api\/v2\/subjects(?:\/|$)/, /^\/metadata\//];
// Readable operation ids, as they are the page URLs (api/operations/<id>/) and springdoc suffixes clashes
// (getSubjects_1), and short summaries, as they are the sidebar labels and page titles.
const OPERATIONS = {
  getSubject: { id: 'v2-get-subject', summary: 'Get a subject' },
  getSubjects_1: { id: 'v2-query-subjects', summary: 'Query subjects' },
  getAllPropertiesForSubject: { id: 'v1-get-subject', summary: 'Get a subject' },
  getPropertyForSubject: { id: 'v1-get-property', summary: 'Get a property of a subject' },
  getSubjects: { id: 'v1-query-subjects', summary: 'Query subjects' },
};
const TAGS = {
  'v-2-api-controller': 'V2 subjects',
  'v1-api': 'V1 (deprecated)',
};

spec.info = {
  title: 'CF Token Metadata Registry API',
  version: 'v2',
  description:
    'Query Cardano token metadata merged from CIP-26 and CIP-68, with additional script information for CIP-113 programmable tokens. V1 serves CIP-26 only and is retained here for existing integrations.',
};
spec.servers = [
  { url: 'https://tokens.cardano.org', description: 'Mainnet' },
  { url: 'https://preprod.tokens.cardano.org', description: 'Preprod' },
];

spec.paths = Object.fromEntries(
  Object.entries(spec.paths).filter(([path]) => PUBLIC_PATHS.some((pattern) => pattern.test(path))),
);
for (const [path, operations] of Object.entries(spec.paths)) {
  for (const operation of Object.values(operations)) {
    operation.tags = (operation.tags ?? []).map((tag) => TAGS[tag] ?? tag);
    const renamed = OPERATIONS[operation.operationId];
    if (renamed) {
      operation.description = operation.description ? `${operation.summary}.\n\n${operation.description}` : operation.summary;
      operation.operationId = renamed.id;
      operation.summary = renamed.summary;
    }
    if (path.startsWith('/metadata/')) operation.deprecated = true;
  }
}

spec.tags = [
  { name: 'V2 subjects', description: 'Token metadata merged from CIP-26 and CIP-68, with separate CIP-113 extensions and validation errors.' },
  { name: 'V1 (deprecated)', description: 'CIP-26 metadata only, as served before V2. Use the V2 endpoints.' },
];

// Keep only the schemas the public paths reach, directly or through other schemas.
const schemas = spec.components?.schemas ?? {};
const reached = new Set();
const visit = (node) => {
  if (Array.isArray(node)) return node.forEach(visit);
  if (!node || typeof node !== 'object') return;
  const ref = node.$ref?.match(/^#\/components\/schemas\/(.+)$/)?.[1];
  if (ref && !reached.has(ref)) {
    reached.add(ref);
    visit(schemas[ref]);
  }
  Object.values(node).forEach(visit);
};
visit(spec.paths);
spec.components.schemas = Object.fromEntries(Object.entries(schemas).filter(([name]) => reached.has(name)));

await writeFile(target, `${JSON.stringify(spec, null, 2)}\n`);
console.log(`${source} -> openapi/registry.json: ${Object.keys(spec.paths).length} paths, ${reached.size} schemas`);
