/**
 * Universal Chapter Aggregator
 * Merges chapters from multiple extensions into one always-up-to-date series.
 * When one source is clearly highest quality, it becomes the sole primary.
 */

import type {
  ChapterMeta,
  AggregatedChapter,
  AggregatedSeries,
  SeriesMeta,
  SourcePriority,
} from "./extension-core";

function chapterKey(n: number): string {
  return Number(n.toFixed(4)).toString();
}

export function mergeChapters(
  lists: ChapterMeta[][],
  priorities: SourcePriority[]
): AggregatedChapter[] {
  const prioMap = new Map(priorities.map((p) => [p.sourceId, p]));
  const groups = new Map<string, ChapterMeta[]>();

  for (const list of lists) {
    for (const ch of list) {
      const key = chapterKey(ch.number);
      const arr = groups.get(key) ?? [];
      arr.push(ch);
      groups.set(key, arr);
    }
  }

  const result: AggregatedChapter[] = [];

  for (const [, mirrors] of groups) {
    const scored = mirrors.map((m) => {
      const p = prioMap.get(m.sourceId);
      const priority = p?.priority ?? 0;
      const quality = p?.qualityScore ?? 50;
      const recency = m.uploadedAt ?? 0;
      const score = priority * 1000 + quality + recency / 1e12;
      return { meta: m, score, quality };
    });
    scored.sort((a, b) => b.score - a.score);
    const best = scored[0];

    result.push({
      number: best.meta.number,
      name: best.meta.name,
      primary: best.meta,
      mirrors: scored.map((s) => s.meta),
      qualityScore: best.quality,
    });
  }

  result.sort((a, b) => a.number - b.number);
  return result;
}

export function shouldUseSingleSource(
  chapters: AggregatedChapter[],
  priorities: SourcePriority[]
): string | null {
  if (chapters.length === 0) return null;

  const coverage = new Map<string, number>();
  for (const ch of chapters) {
    for (const m of ch.mirrors) {
      coverage.set(m.sourceId, (coverage.get(m.sourceId) ?? 0) + 1);
    }
  }

  const total = chapters.length;
  let bestId: string | null = null;
  let bestQuality = -1;

  for (const [id, count] of coverage) {
    const ratio = count / total;
    const q = priorities.find((p) => p.sourceId === id)?.qualityScore ?? 50;
    if (ratio >= 0.95 && q >= bestQuality) {
      bestId = id;
      bestQuality = q;
    }
  }

  if (bestId && bestQuality >= 0) {
    const othersHigher = priorities.some(
      (p) => p.sourceId !== bestId && p.qualityScore > bestQuality
    );
    if (!othersHigher) return bestId;
  }
  return null;
}

export function buildAggregatedSeries(
  id: string,
  title: string,
  kind: AggregatedSeries["kind"],
  linked: AggregatedSeries["linkedSources"],
  chapterLists: ChapterMeta[][],
  priorities: SourcePriority[],
  meta?: Partial<SeriesMeta>
): AggregatedSeries {
  const chapters = mergeChapters(chapterLists, priorities);
  const sole = shouldUseSingleSource(chapters, priorities);

  const finalChapters = sole
    ? chapters.map((ch) => {
        const primary = ch.mirrors.find((m) => m.sourceId === sole) ?? ch.primary;
        return {
          ...ch,
          primary,
          mirrors: [primary],
          qualityScore:
            priorities.find((p) => p.sourceId === sole)?.qualityScore ?? ch.qualityScore,
        };
      })
    : chapters;

  return {
    id,
    title,
    kind,
    coverUrl: meta?.coverUrl,
    description: meta?.description,
    genres: meta?.genres ?? [],
    status: meta?.status ?? "unknown",
    linkedSources: linked,
    chapters: finalChapters,
    lastSyncedAt: Date.now(),
  };
}
