import type { APIRoute } from 'astro';
import spec from '../../openapi/registry.json';

// The spec the API reference pages are generated from, published so the Scalar page tries out the same endpoints.
export const GET: APIRoute = () =>
  new Response(JSON.stringify(spec), { headers: { 'Content-Type': 'application/json' } });
