/**
 * Lumen Reading OS Types
 */

export type MediaType = 'manga' | 'novel' | 'hybrid';

export interface Dialogue {
  character: string;
  text: string;
  positionX: number; // Percentage 0-100 on the panel
  positionY: number; // Percentage 0-100 on the panel
}

export interface MangaPanel {
  id: string;
  visualConcept: string;
  layoutType: 'hero' | 'split-vertical' | 'full-width' | 'double-page' | 'cinematic';
  atmosphericColor: string; // Hex or gradient string for dynamic blur ambient lighting
  dialogues: Dialogue[];
  narrativeText?: string;
}

export interface HybridElement {
  id: string;
  type: 'text' | 'image-spread' | 'overlay-narration' | 'cinematic-splash';
  content: string; // The text content or image description
  assetPrompt?: string; // Prompt for image generation / visualization
  atmosphericColor?: string; // Dynamic highlight bloom color
  focusHighlight?: boolean;
}

export interface Chapter {
  id: string;
  title: string;
  number: number;
  uploadedAt: string;
  content?: string; // Used for Novels (formatted markdown / HTML paragraphs)
  mangaPanels?: MangaPanel[]; // Used for Manga (panel layout list)
  hybridElements?: HybridElement[]; // Used for Hybrid stories
}

export interface MediaItem {
  id: string;
  title: string;
  originalTitle?: string;
  type: MediaType;
  author: string;
  artist?: string;
  coverUrl: string;
  description: string;
  genres: string[];
  status: 'ongoing' | 'completed';
  rating: number;
  chaptersCount: number;
  chapters: Chapter[];
  isCustom?: boolean; // Formed by Lumen Forge
  syncedAt?: string;
  isFavorite?: boolean;
}

export interface HistoryEntry {
  id: string;
  mediaId: string;
  mediaTitle: string;
  mediaCover: string;
  type: MediaType;
  chapterId: string;
  chapterTitle: string;
  chapterNumber: number;
  progressPercent: number; // percentage read
  lastReadTimestamp: number;
  readingDurationMs: number;
}

export interface UpdateRecord {
  id: string;
  mediaId: string;
  mediaTitle: string;
  mediaCover: string;
  type: MediaType;
  chapterId: string;
  chapterTitle: string;
  chapterNumber: number;
  updatedAt: string;
  isRead: boolean;
}

export interface ReaderSettings {
  // Global
  motionReduction: boolean;
  themeMode: 'dark' | 'light' | 'amoled';
  liquidIntensity: number; // 0 (none) to 100 (maximum refraction blur)
  
  // Novel specific
  fontSize: number; // percentage, e.g. 100
  lineHeight: number; // scale, e.g. 1.6
  fontFamily: 'serif' | 'sans' | 'mono';
  columnWidth: 'narrow' | 'medium' | 'wide' | 'fullscreen';
  warmMode: boolean; // warm amber background tint
  
  // Manga specific
  mangaMode: 'vertical-scroll' | 'page-tap' | 'hybrid-masonry';
  panelZoomEnabled: boolean;
  adaptiveAmbientBlur: boolean;
}
