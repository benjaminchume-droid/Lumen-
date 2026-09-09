/**
 * Lumen Extension Core
 * Mihon / Tachiyomi / Shosetsu inspired source + extension system
 */

export type MediaKind = "manga" | "novel";

export interface ExtensionManifest {
  id: string;
  name: string;
  version: string;
  lang: string;
  kind: MediaKind;
  baseUrl: string;
  nsfw: boolean;
  iconUrl?: string;
}

export interface SeriesMeta {
  url: string;
  title: string;
  altTitles?: string[];
  author?: string;
  artist?: string;
  description?: string;
  coverUrl?: string;
  genres?: string[];
  status: "ongoing" | "completed" | "hiatus" | "cancelled" | "unknown";
  kind: MediaKind;
}

export interface ChapterMeta {
  url: string;
  name: string;
  number: number;
  scanlator?: string;
  uploadedAt?: number;
  sourceId: string;
}

export interface Page {
  index: number;
  url: string;
  headers?: Record<string, string>;
}

export interface NovelPage {
  index: number;
  content: string;
}

export interface Source {
  readonly id: string;
  readonly name: string;
  readonly lang: string;
  readonly kind: MediaKind;
  readonly baseUrl: string;

  getPopular(page: number): Promise<{ series: SeriesMeta[]; hasNext: boolean }>;
  getLatest(page: number): Promise<{ series: SeriesMeta[]; hasNext: boolean }>;
  search(query: string, page: number): Promise<{ series: SeriesMeta[]; hasNext: boolean }>;
  getSeriesDetails(seriesUrl: string): Promise<SeriesMeta>;
  getChapterList(seriesUrl: string): Promise<ChapterMeta[]>;
  getPageList(chapterUrl: string): Promise<Page[] | NovelPage[]>;
}

export interface ExtensionPackage {
  manifest: ExtensionManifest;
  createSource(): Source;
}

export interface SourcePriority {
  sourceId: string;
  priority: number;
  qualityScore: number;
}

export interface AggregatedChapter {
  number: number;
  name: string;
  primary: ChapterMeta;
  mirrors: ChapterMeta[];
  qualityScore: number;
}

export interface AggregatedSeries {
  id: string;
  title: string;
  kind: MediaKind;
  coverUrl?: string;
  description?: string;
  genres: string[];
  status: SeriesMeta["status"];
  linkedSources: Array<{ sourceId: string; seriesUrl: string; priority: number }>;
  chapters: AggregatedChapter[];
  lastSyncedAt: number;
}
