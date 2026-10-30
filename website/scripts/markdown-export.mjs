// Writes a Markdown copy beside every page of the build, so a reader (or a tool) can take the text of the page
// instead of scraping the HTML. Docs pages keep their source text; the API operations, which only exist as pages
// generated from openapi/registry.json, are rendered from the spec.
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const DOCS = path.join(ROOT, 'src', 'content', 'docs');
const SPEC_PATH = path.join(ROOT, 'openapi', 'registry.json');

const METHODS = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options'];

async function listDocs(dir, prefix = '') {
	const entries = await fs.readdir(dir, { withFileTypes: true });
	const pages = [];
	for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
		const full = path.join(dir, entry.name);
		if (entry.isDirectory()) {
			pages.push(...(await listDocs(full, `${prefix}${entry.name}/`)));
		} else if (/\.mdx?$/.test(entry.name)) {
			pages.push({ file: full, slug: `${prefix}${entry.name.replace(/\.mdx?$/, '')}` });
		}
	}
	return pages;
}

const splitFrontmatter = (text) => {
	const match = text.match(/^---\n([\s\S]*?)\n---\n/);
	const fields = {};
	if (match) {
		for (const line of match[1].split('\n')) {
			const kv = line.match(/^(\w+):\s*(.*)$/);
			if (kv) fields[kv[1]] = kv[2].replace(/^['"]|['"]$/g, '');
		}
	}
	return { fields, body: match ? text.slice(match[0].length) : text };
};

async function docsMarkdown(file) {
	const { fields, body } = splitFrontmatter(await fs.readFile(file, 'utf8'));
	const heading = fields.title ? `# ${fields.title}\n\n` : '';
	return `${heading}${body.trim()}\n`;
}

const cell = (value) => String(value ?? '').replace(/\|/g, '\\|').replace(/\n+/g, ' ').trim();

function parametersTable(parameters = []) {
	if (!parameters.length) return '';
	const rows = parameters.map((parameter) => {
		const schema = parameter.schema ?? {};
		const type = schema.type ?? (schema.$ref ? schema.$ref.split('/').pop() : '');
		const allowed = schema.enum ? ` (${schema.enum.join(', ')})` : '';
		return `| \`${parameter.name}\` | ${parameter.in} | ${cell(type)}${allowed} | ${parameter.required ? 'yes' : 'no'} | ${cell(parameter.description)} |`;
	});
	return [
		'| Parameter | In | Type | Required | Description |',
		'|---|---|---|---|---|',
		...rows,
		'',
	].join('\n');
}

function operationMarkdown(pathname, method, operation) {
	const lines = [`# ${operation.summary ?? `${method.toUpperCase()} ${pathname}`}`, ''];
	lines.push(`\`${method.toUpperCase()} ${pathname}\``, '');
	if (operation.description) lines.push(operation.description, '');
	if (operation.parameters?.length) {
		lines.push('## Parameters', '', parametersTable(operation.parameters));
	}
	const body = operation.requestBody?.content?.['application/json'];
	if (body?.schema) {
		lines.push('## Request body', '');
		lines.push('```json', JSON.stringify(body.schema, null, 2), '```', '');
	}
	if (body?.example) {
		lines.push('### Example', '', '```json', JSON.stringify(body.example, null, 2), '```', '');
	}
	if (operation.responses) {
		lines.push('## Responses', '');
		for (const [status, response] of Object.entries(operation.responses)) {
			lines.push(`- \`${status}\` ${cell(response.description)}`);
		}
		lines.push('');
	}
	lines.push('```bash', `curl -X ${method.toUpperCase()} "https://tokens.cardano.org${pathname}"`, '```', '');
	return lines.join('\n');
}

export default function markdownExport() {
	return {
		name: 'token-registry-markdown-export',
		hooks: {
			'astro:build:done': async ({ dir, logger }) => {
				const outDir = fileURLToPath(dir);
				let written = 0;
				const write = async (target, content) => {
					await fs.mkdir(path.dirname(target), { recursive: true });
					await fs.writeFile(target, content);
					written += 1;
				};

				for (const page of await listDocs(DOCS)) {
					await write(path.join(outDir, page.slug, 'index.md'), await docsMarkdown(page.file));
				}

				const spec = JSON.parse(await fs.readFile(SPEC_PATH, 'utf8'));
				for (const [pathname, operations] of Object.entries(spec.paths ?? {})) {
					for (const method of METHODS) {
						const operation = operations[method];
						if (!operation) continue;
						const name = operation.operationId ?? `${method}-${pathname.replace(/\W+/g, '-')}`;
						await write(
							path.join(outDir, 'api', 'operations', name, 'index.md'),
							operationMarkdown(pathname, method, operation),
						);
					}
				}

				logger.info(`Markdown copies written: ${written}`);
			},
		},
	};
}
