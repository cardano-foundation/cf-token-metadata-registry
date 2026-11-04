// Astro integration publishing /llms.txt and /llms-full.txt with the build (and on demand in `astro dev`).
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import os from 'node:os';
import fs from 'node:fs/promises';
import { generateLlmsFiles } from './generate-llms-txt.mjs';

const SERVED_FILES = ['/llms.txt', '/llms-full.txt'];

export default function llmsIntegration() {
  let base = '/';
  return {
    name: 'token-registry-llms-txt',
    hooks: {
      'astro:config:done': ({ config }) => {
        base = config.base ?? '/';
      },
      'astro:build:done': async ({ dir, logger }) => {
        await generateLlmsFiles({ outDir: fileURLToPath(dir), logger });
      },
      'astro:server:setup': async ({ server, logger }) => {
        const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'token-registry-llms-'));
        const served = new Set(SERVED_FILES.map((p) => path.posix.join(base, p)));
        server.middlewares.use(async (req, res, next) => {
          const url = (req.url || '').split('?')[0];
          if (!served.has(url)) return next();
          try {
            await generateLlmsFiles({ outDir: tmpDir, logger: { info: () => {}, error: (m) => logger.error(m) } });
            res.setHeader('Content-Type', 'text/plain; charset=utf-8');
            res.end(await fs.readFile(path.join(tmpDir, path.posix.basename(url))));
          } catch (err) {
            next(err);
          }
        });
      },
    },
  };
}
