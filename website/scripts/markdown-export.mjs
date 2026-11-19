// One collection of docs and API pages feeds per-page Markdown, llms.txt and llms-full.txt in dev and builds.
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { slug } from 'github-slugger';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const DOCS = path.join(ROOT, 'src', 'content', 'docs');
const SPEC_PATH = path.join(ROOT, 'openapi', 'registry.json');
const METHODS = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options'];

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

function operationMarkdown(pathname, method, operation, schemas) {
	const lines = [`\`${method.toUpperCase()} ${pathname}\``, ''];
	if (operation.description) lines.push(operation.description, '');
	if (operation.parameters?.length) {
		lines.push('## Parameters', '', parametersTable(operation.parameters));
	}
	const body = Object.entries(operation.requestBody?.content ?? {})
		.find(([type]) => type.split(';')[0].trim().toLowerCase() === 'application/json')?.[1];
	if (body?.schema) {
		const reference = body.schema.$ref?.match(/^#\/components\/schemas\/(.+)$/)?.[1];
		lines.push('## Request body', '');
		lines.push('```json', JSON.stringify(schemas[reference] ?? body.schema, null, 2), '```', '');
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
	return lines.join('\n');
}

const apiIndexMarkdown = (description, operations, linkPrefix) => [
	description ?? '', '',
	...operations.map(({ pathname, method, operation }) =>
		`- [${operation.summary ?? operation.operationId}](${linkPrefix}${slug(operation.operationId)}/): \`${method.toUpperCase()} ${pathname}\``),
	'',
].join('\n');

async function collectPages(siteUrl) {
	const pages = [];
	const add = (route, title, description, body) => pages.push({
		route: `${route}/index.md`, title, description, body: body.trim(), url: new URL(`${route}/`, siteUrl).href,
	});
	const docs = await Array.fromAsync(fs.glob('**/*.{md,mdx}', { cwd: DOCS }));
	for (const file of docs.sort()) {
		const route = file.replace(/\.mdx?$/, '');
		const { fields, body } = splitFrontmatter(await fs.readFile(path.join(DOCS, file), 'utf8'));
		add(route, fields.title ?? route, fields.description, body);
	}
	const spec = JSON.parse(await fs.readFile(SPEC_PATH, 'utf8'));
	const entries = [];
	for (const [pathname, operations] of Object.entries(spec.paths ?? {})) {
		for (const method of METHODS) {
			const operation = operations[method];
			if (!operation) continue;
			const name = slug(operation.operationId ?? `${method}-${pathname.replace(/\W+/g, '-')}`);
			entries.push({ pathname, method, operation });
			add(`api/operations/${name}`, operation.summary ?? `${method.toUpperCase()} ${pathname}`,
				`${method.toUpperCase()} ${pathname}`, operationMarkdown(pathname, method, operation, spec.components?.schemas ?? {}));
		}
	}
	add('api', spec.info.title, spec.info.description, apiIndexMarkdown(spec.info.description, entries, 'operations/'));
	const tags = new Set(entries.flatMap(({ operation }) => operation.tags ?? []));
	for (const name of tags) {
		const tag = spec.tags?.find((entry) => entry.name === name);
		add(`api/operations/tags/${slug(name)}`, name, tag?.description,
			apiIndexMarkdown(tag?.description, entries.filter(({ operation }) => operation.tags?.includes(name)), '../../'));
	}
	return pages;
}

const renderMarkdown = ({ title, url, body }, withSource = false) =>
	`# ${title}\n\n${withSource ? `Source: ${url}\n\n` : ''}${body}\n`;

const renderLlmsIndex = (pages, { title, description }) => [
	`# ${title}`, '', `> ${description}`, '', '## Docs', '',
	...pages.map(({ title, url, description }) => `- [${title}](${url})${description ? `: ${description}` : ''}`),
	'',
].join('\n');

const renderLlmsFull = (pages) => pages.map((page) => renderMarkdown(page, true)).join('\n---\n\n');

async function collectExports(siteUrl, siteMetadata) {
	const pages = await collectPages(siteUrl);
	const files = new Map(pages.map((page) => [page.route, renderMarkdown(page)]));
	files.set('llms.txt', renderLlmsIndex(pages, siteMetadata));
	files.set('llms-full.txt', renderLlmsFull(pages));
	return files;
}

export default function markdownExport(siteMetadata) {
	let siteUrl;
	return {
		name: 'token-registry-markdown-export',
		hooks: {
			'astro:config:done': ({ config }) => {
				siteUrl = new URL(config.base.replace(/\/?$/, '/'), config.site);
			},
			'astro:server:setup': ({ server }) => {
				server.middlewares.use(async (request, response, next) => {
					// Vite has already stripped Astro's configured base from request.url.
					const route = new URL(request.url ?? '/', 'http://localhost').pathname.slice(1);
					const isLlms = route === 'llms.txt' || route === 'llms-full.txt';
					if (!['GET', 'HEAD'].includes(request.method) || (!isLlms && !route.endsWith('/index.md'))) return next();
					try {
						// Look up known routes, never filesystem paths supplied by the request.
						const content = (await collectExports(siteUrl, siteMetadata)).get(route);
						if (content === undefined) return next();
						response.setHeader('Content-Type', `${isLlms ? 'text/plain' : 'text/markdown'}; charset=utf-8`);
						response.end(request.method === 'HEAD' ? '' : content);
					} catch (error) {
						next(error);
					}
				});
			},
			'astro:build:done': async ({ dir, logger }) => {
				const outDir = fileURLToPath(dir);
				const files = await collectExports(siteUrl, siteMetadata);
				for (const [route, content] of files) {
					const target = path.join(outDir, route);
					await fs.mkdir(path.dirname(target), { recursive: true });
					await fs.writeFile(target, content);
				}
				logger.info(`Exported ${files.size} files: per-page Markdown, llms.txt and llms-full.txt`);
			},
		},
	};
}
