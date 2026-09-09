/**
 * Lumen Reading OS - Source Integration System & Adaptation Relationship Engine
 */

import { MediaType, MediaItem } from "../types";

export interface SourceExtension {
  id: string;
  name: string;
  version: string;
  type: "manga" | "novel";
  repositoryUrl: string;
  healthStatus: "healthy" | "degraded" | "offline";
  pingMs: number;
}

// Strictly Separated Manga & Novel Repositories Ingestion Manifest
export const MANGA_REPOSITORIES: SourceExtension[] = [
  {
    id: "manga-dex-adapter",
    name: "MangaDex Hub Extension",
    version: "2.8.4",
    type: "manga",
    repositoryUrl: "https://raw.githubusercontent.com/lumen-reader/keiyoushi-manga/main",
    healthStatus: "healthy",
    pingMs: 42,
  },
  {
    id: "keiyoushi-repo",
    name: "Keiyoushi Keitais",
    version: "4.1.2",
    type: "manga",
    repositoryUrl: "https://registry.keiyoushi.org/manga",
    healthStatus: "healthy",
    pingMs: 88,
  },
  {
    id: "tachiyomi-revived",
    name: "Tachiyomi Legacy Revived",
    version: "1.0.9",
    type: "manga",
    repositoryUrl: "https://backports.tachiyomi.org",
    healthStatus: "degraded",
    pingMs: 210,
  }
];

export const NOVEL_REPOSITORIES: SourceExtension[] = [
  {
    id: "lnreader-adapter",
    name: "LNReader Shosetsu Core",
    version: "1.5.0",
    type: "novel",
    repositoryUrl: "https://lnreader.github.io/plugins",
    healthStatus: "healthy",
    pingMs: 50,
  },
  {
    id: "shosetsu-universe",
    name: "Shosetsu Galactic Plugin",
    version: "3.2.1",
    type: "novel",
    repositoryUrl: "https://universe.shosetsu.app/plugins",
    healthStatus: "healthy",
    pingMs: 72,
  }
];

// Adaptation Map linking manga and novels to simulate the hybrid adaptation pipeline
export interface AdaptationLink {
  novelId: string;
  mangaId: string;
  adaptationNotes: string;
}

export const ADAPTATION_LINKS: AdaptationLink[] = [
  {
    novelId: "the-metamorphosis",
    mangaId: "the-metamorphosis-manga",
    adaptationNotes: "Atmospheric psychological line-art manga adaptation of Kafka's classic work."
  },
  {
    novelId: "the-time-machine-novel",
    mangaId: "the-time-machine",
    adaptationNotes: "Steampunk futuristic sci-fi novel adaptation mapping Wells' Time Explorer timeline."
  }
];

// Companion adaptation items to populate when the user binds adaptation views
export const COMPANION_ADAPTATIONS: MediaItem[] = [
  {
    id: "the-metamorphosis-manga",
    title: "The Metamorphosis (Manga Adaptation)",
    originalTitle: "Verwandlung: Lineage",
    type: "manga",
    author: "Franz Kafka",
    artist: "Lumen Studio",
    coverUrl: "https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&q=80&w=600",
    description: "Witness Kafka's terrifying dreamscape of isolation transformed into a visually striking sequence of heavy surrealist gothic manga panels.",
    genres: ["Existential", "Absurdist", "Classic Literature", "Psychological"],
    status: "completed",
    rating: 4.8,
    chaptersCount: 1,
    chapters: [
      {
        id: "manga-chapter-1",
        title: "The Shell and the Awaking",
        number: 1,
        uploadedAt: "2026-05-24",
        mangaPanels: [
          {
            id: "kafk-p-1",
            visualConcept: "Gregor staring at his brand new insect claw struggling to trigger a brass alarm clock.",
            layoutType: "hero",
            atmosphericColor: "#2b2d42",
            dialogues: [
              {
                character: "Gregor Samsa",
                text: "My claws... they fail to reach the pendulum. This is no hallucination.",
                positionX: 40,
                positionY: 20
              }
            ],
            narrativeText: "The heavy morning rainfall splatters against the glass pane."
          }
        ]
      }
    ]
  },
  {
    id: "the-time-machine-novel",
    title: "The Time Machine (Light Novel version)",
    originalTitle: "Chronos Light Chronicle",
    type: "novel",
    author: "H.G. Wells",
    coverUrl: "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=600",
    description: "Read the verbatim classic science fiction masterpiece tracing the Traveler's chronological odyssey in detailed rich literary text prose.",
    genres: ["Sci-Fi", "Dystopian", "Adventure"],
    status: "completed",
    rating: 4.9,
    chaptersCount: 1,
    chapters: [
      {
        id: "novel-chapter-1",
        title: "The Time-Traveller's Exposition",
        number: 1,
        uploadedAt: "2026-05-25",
        content: `### The Golden Dial Settings

The Time Traveller (for so it will be convenient to speak of him) was expounding a recondite matter to us. His grey eyes shone and twinkled, and his usually pale face was flushed and animated. The fire burned brightly, and the soft radiance of the incandescent lights in the lilies of silver caught the bubbles that flashed and passed in our glasses.

"You must follow me carefully. I shall have to controvert one or two ideas that are almost universally accepted. The geometry, for instance, they taught you at school is founded on a misconception."

"Is not that rather a large thing to expect us to begin with?" said Filby, an argumentative person with red hair.

"I do not mean to ask you to accept anything without reasonable ground for it. You will soon hear as much as I want you to admit. You know, of course, that a mathematical line, a line of thickness nil, has no real existence. They taught you that? Neither has a mathematical plane. These things are mere abstractions."`
      }
    ]
  }
];

// Unified content optimization system helper
export function optimizeContentItem(item: MediaItem): MediaItem {
  return {
    ...item,
    coverUrl: item.coverUrl.includes("unsplash") ? `${item.coverUrl}&q=70&w=400` : item.coverUrl,
    syncedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  };
}
