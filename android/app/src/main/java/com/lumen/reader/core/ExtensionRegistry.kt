package com.lumen.reader.core

data class ExtensionRepo(
    val id: String,
    val name: String,
    val indexUrl: String,
    val kind: String
)

class ExtensionRegistry {
    private val packages = mutableMapOf<String, Pair<ExtensionManifest, () -> Source>>()
    private val enabled = mutableSetOf<String>()
    private val repos = mutableListOf<ExtensionRepo>()

    fun register(manifest: ExtensionManifest, factory: () -> Source) {
        packages[manifest.id] = manifest to factory
    }

    fun enable(id: String) { if (packages.containsKey(id)) enabled.add(id) }
    fun disable(id: String) { enabled.remove(id) }
    fun isEnabled(id: String) = enabled.contains(id)

    fun listManifests(): List<ExtensionManifest> = packages.values.map { it.first }

    fun listEnabledSources(): List<Source> =
        enabled.mapNotNull { packages[it]?.second?.invoke() }

    fun getSource(id: String): Source? {
        if (!enabled.contains(id)) return null
        return packages[id]?.second?.invoke()
    }

    fun addRepo(repo: ExtensionRepo) {
        if (repos.none { it.id == repo.id }) repos.add(repo)
    }

    fun listRepos(): List<ExtensionRepo> = repos.toList()
}

val globalRegistry = ExtensionRegistry()
