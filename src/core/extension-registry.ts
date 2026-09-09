/**
 * Extension Registry — load, enable, disable, discover sources
 * Compatible with Mihon-style extension repos (index.min.json) and Shosetsu plugin lists
 */

import type { ExtensionManifest, ExtensionPackage, Source } from "./extension-core";

export interface ExtensionRepo {
  id: string;
  name: string;
  indexUrl: string;
  kind: "manga" | "novel" | "both";
}

export class ExtensionRegistry {
  private packages = new Map<string, ExtensionPackage>();
  private enabled = new Set<string>();
  private repos: ExtensionRepo[] = [];

  register(pkg: ExtensionPackage): void {
    this.packages.set(pkg.manifest.id, pkg);
  }

  unregister(id: string): void {
    this.packages.delete(id);
    this.enabled.delete(id);
  }

  enable(id: string): void {
    if (this.packages.has(id)) this.enabled.add(id);
  }

  disable(id: string): void {
    this.enabled.delete(id);
  }

  isEnabled(id: string): boolean {
    return this.enabled.has(id);
  }

  listManifests(): ExtensionManifest[] {
    return Array.from(this.packages.values()).map((p) => p.manifest);
  }

  listEnabledSources(): Source[] {
    return Array.from(this.enabled)
      .map((id) => this.packages.get(id))
      .filter(Boolean)
      .map((p) => p!.createSource());
  }

  getSource(id: string): Source | null {
    if (!this.enabled.has(id)) return null;
    return this.packages.get(id)?.createSource() ?? null;
  }

  addRepo(repo: ExtensionRepo): void {
    if (!this.repos.find((r) => r.id === repo.id)) this.repos.push(repo);
  }

  listRepos(): ExtensionRepo[] {
    return [...this.repos];
  }

  async syncRepo(
    repoId: string,
    fetchJson: (url: string) => Promise<unknown>
  ): Promise<ExtensionManifest[]> {
    const repo = this.repos.find((r) => r.id === repoId);
    if (!repo) throw new Error(`Unknown repo: ${repoId}`);

    const data = (await fetchJson(repo.indexUrl)) as {
      extensions?: Array<{
        pkg?: string;
        name?: string;
        version?: string;
        lang?: string;
        nsfw?: number | boolean;
        sources?: Array<{ id: string; name: string; lang: string; baseUrl?: string }>;
      }>;
      plugins?: Array<{ id: string; name: string; version: string; lang?: string }>;
    };

    const manifests: ExtensionManifest[] = [];

    if (data.extensions) {
      for (const ext of data.extensions) {
        const sources = ext.sources ?? [];
        for (const s of sources) {
          const id = s.id || `${ext.pkg}:${s.name}`;
          const manifest: ExtensionManifest = {
            id,
            name: s.name || ext.name || id,
            version: ext.version || "0.0.0",
            lang: s.lang || ext.lang || "en",
            kind: repo.kind === "novel" ? "novel" : "manga",
            baseUrl: s.baseUrl || "",
            nsfw: Boolean(ext.nsfw),
          };
          manifests.push(manifest);
          if (!this.packages.has(id)) {
            this.register({
              manifest,
              createSource: () => {
                throw new Error(`Extension ${id} not fully loaded.`);
              },
            });
          }
        }
      }
    }

    if (data.plugins) {
      for (const p of data.plugins) {
        const manifest: ExtensionManifest = {
          id: p.id,
          name: p.name,
          version: p.version,
          lang: p.lang || "en",
          kind: "novel",
          baseUrl: "",
          nsfw: false,
        };
        manifests.push(manifest);
        if (!this.packages.has(p.id)) {
          this.register({
            manifest,
            createSource: () => {
              throw new Error(`Plugin ${p.id} not fully loaded.`);
            },
          });
        }
      }
    }

    return manifests;
  }
}

export const globalRegistry = new ExtensionRegistry();
