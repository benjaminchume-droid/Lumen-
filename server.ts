import express from "express";
import path from "path";
import dotenv from "dotenv";
import { GoogleGenAI, Type } from "@google/genai";
import { createServer as createViteServer } from "vite";

// Load environment variables
dotenv.config();

const app = express();
const PORT = 3000;

// Setup JSON body parsing
app.use(express.json({ limit: "25mb" }));

// Lazy initializer for Gemini client to prevent startup crash if API key is missing
let aiClient: GoogleGenAI | null = null;
function getGeminiClient(): GoogleGenAI {
  if (!aiClient) {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      throw new Error("GEMINI_API_KEY is not defined. Please add it to Settings > Secrets of the AI Studio UI.");
    }
    aiClient = new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build",
        },
      },
    });
  }
  return aiClient;
}

// ----------------------------------------------------
// API Layer
// ----------------------------------------------------

// Health Check
app.get("/api/health", (req, res) => {
  res.json({ status: "healthy", system: "Lumen Reading OS", time: new Date().toISOString() });
});

// Lumen Forge: AI Story & Panel Generation Endpoints (Server-side Gemini proxy)
app.post("/api/gemini/generate-series", async (req, res) => {
  const { prompt, type, genre } = req.body;

  if (!prompt || !type) {
    res.status(400).json({ error: "Missing required parameters: prompt and type." });
    return;
  }

  try {
    const ai = getGeminiClient();

    const systemInstruction = `You are a premium, professional graphic novel and literary designer for Lumen Reading OS, an immersive reading dashboard. 
Your goal is to generate exceptionally rich, cohesive, cinematic, and professional reading materials in JSON.
Never use placeholder data, lorem ipsum, generic terms, or half-finished fields. Every response must generate highly imaginative, engaging, copyright-free, or original content of maximum quality.

Format the response strictly using the provided JSON Schema. 
Depending on the TYPE requested ('manga', 'novel', or 'hybrid'), populate the correct corresponding fields:
- If type is 'manga', ensure 'chapters' contain rich 'mangaPanels' with descriptive artwork art direction, layout arrangements, dialogue placements, and atmospheric blur back-lighting hex colors. Each manga chapter should be fully readable, with structured panels.
- If type is 'novel', ensure 'chapters' contain a beautiful literary prose body in 'content' in clean markdown with indented paragraphs.
- If type is 'hybrid', ensure 'chapters' contain 'hybridElements' blending illustrations, text, and narrative overlays.`;

    const userPrompt = `Generate a beautiful original ${type} story under the theme/prompt: "${prompt}".
${genre ? `Make it fit the genre: ${genre}.` : ""}
Provide 1 very high-quality complete chapter. Set 'chaptersCount' to 1.`;

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: userPrompt,
      config: {
        systemInstruction,
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          required: ["id", "title", "originalTitle", "type", "author", "artist", "coverUrl", "description", "genres", "status", "rating", "chaptersCount", "chapters"],
          properties: {
            id: { type: Type.STRING, description: "A unique slug string, e.g., astronaut-chronicles" },
            title: { type: Type.STRING, description: "The beautiful main title of the series." },
            originalTitle: { type: Type.STRING, description: "Secondary language title or alt script (e.g., stylized Japanese or Latin rendering)." },
            type: { type: Type.STRING, description: "Must match standard types: 'manga', 'novel', or 'hybrid'." },
            author: { type: Type.STRING, description: "Author name." },
            artist: { type: Type.STRING, description: "Artist name (can be same as author, or AI)." },
            coverUrl: { type: Type.STRING, description: "A beautiful, premium abstract visual art prompt description or specific abstract style url." },
            description: { type: Type.STRING, description: "A highly cinematic, engaging introduction synopsis of the series." },
            genres: {
              type: Type.ARRAY,
              items: { type: Type.STRING },
              description: "Genres, e.g., 'Sci-Fi', 'Psychological', 'Fantasy', 'Philosophical'."
            },
            status: { type: Type.STRING, description: "Status: 'completed' or 'ongoing'." },
            rating: { type: Type.NUMBER, description: "Rating from 4.5 to 5.0." },
            chaptersCount: { type: Type.INTEGER, description: "Number of chapters provided." },
            chapters: {
              type: Type.ARRAY,
              items: {
                type: Type.OBJECT,
                required: ["id", "title", "number", "uploadedAt"],
                properties: {
                  id: { type: Type.STRING, description: "Chapter ID slug" },
                  title: { type: Type.STRING, description: "Chapter title" },
                  number: { type: Type.INTEGER, description: "Chapter position index (1)" },
                  uploadedAt: { type: Type.STRING, description: "Formatted ISO timestamp or modern date" },
                  content: { type: Type.STRING, description: "For 'novel' stories only. The full literary prose inside the chapter, rich in description and formatting (Markdown paragraphs, clean and descriptive)." },
                  mangaPanels: {
                    type: Type.ARRAY,
                    items: {
                      type: Type.OBJECT,
                      required: ["id", "visualConcept", "layoutType", "atmosphericColor", "dialogues"],
                      properties: {
                        id: { type: Type.STRING },
                        visualConcept: { type: Type.STRING, description: "Art direction guide of what is visually depicted in the panel." },
                        layoutType: { type: Type.STRING, description: "One of 'hero', 'split-vertical', 'full-width', 'double-page', 'cinematic'." },
                        atmosphericColor: { type: Type.STRING, description: "A subtle hex color representing the dominant atmospheric lighting tone, e.g. '#1e2436' or '#382218' which will be back-projected behind the glass page." },
                        dialogues: {
                          type: Type.ARRAY,
                          items: {
                            type: Type.OBJECT,
                            required: ["character", "text", "positionX", "positionY"],
                            properties: {
                              character: { type: Type.STRING, description: "Speaker name" },
                              text: { type: Type.STRING, description: "Dialogue bubble text" },
                              positionX: { type: Type.NUMBER, description: "Layout positioning percentage 0 to 100" },
                              positionY: { type: Type.NUMBER, description: "Layout positioning percentage 0 to 100" }
                            }
                          }
                        },
                        narrativeText: { type: Type.STRING, description: "Boxed narrative captions (if any)" }
                      }
                    }
                  },
                  hybridElements: {
                    type: Type.ARRAY,
                    items: {
                      type: Type.OBJECT,
                      required: ["id", "type", "content"],
                      properties: {
                        id: { type: Type.STRING },
                        type: { type: Type.STRING, description: "One of 'text', 'image-spread', 'overlay-narration', 'cinematic-splash'." },
                        content: { type: Type.STRING, description: "The literary text paragraph OR detailed cinematic image prompt composition depending on type." },
                        assetPrompt: { type: Type.STRING, description: "Art prompt for the image element." },
                        atmosphericColor: { type: Type.STRING, description: "Subtle highlight hex glow for margins, e.g. '#2b4d66'." },
                        focusHighlight: { type: Type.BOOLEAN }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    });

    if (response && response.text) {
      const data = JSON.parse(response.text.trim());
      res.json({ success: true, series: data });
    } else {
      throw new Error("Empty response returned from the model.");
    }

  } catch (error: any) {
    console.error("Gemini Series Generation Error:", error.message);
    res.status(500).json({
      success: false,
      error: error.message || "Failed to generate series via Gemini AI. Check environment configurations."
    });
  }
});

// ----------------------------------------------------
// Mounting Vite Server Middleware & Production Serves
// ----------------------------------------------------
async function initializeServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
    console.log("Vite development middleware integrated successfully.");
  } else {
    // Production Assets Serving
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
    console.log("Production static workspace server active.");
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Lumen Reading OS active at port http://localhost:${PORT}`);
  });
}

initializeServer().catch((err) => {
  console.error("Critical: Failed to launch server:", err);
});
