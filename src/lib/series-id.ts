/**
 * Every external book/series gets a stable Lumen ID.
 * Format: lumen:{sourceId}:{externalKey}
 * Hash form used for compact storage: lumen_x_{sha1-12}
 */

export function stableSeriesId(sourceId: string, externalKey: string): string {
  const raw = `${sourceId.trim()}::${externalKey.trim()}`.toLowerCase();
  return `lumen:${sourceId}:${slug(externalKey)}`;
}

export function stableChapterId(seriesId: string, chapterKey: string): string {
  return `${seriesId}:ch:${slug(chapterKey)}`;
}

function slug(s: string): string {
  return s
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 64) || "item";
}

/** Simple non-crypto hash for compact local keys */
export function compactId(input: string): string {
  let h = 2166136261;
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return `lx_${(h >>> 0).toString(16)}`;
}
