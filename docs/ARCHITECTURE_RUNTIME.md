# Lumen dual-runtime architecture (v1.8)

## Host
- SourceRegistry binds IndexEntry → ContentSource
- Order: MihonSourceAdapter → LnReaderSourceAdapter → HostFallbackSource
- UI/downloads call only: popular / latest / search / details / chapters / content

## Mihon runtime
- PathClassLoader + tachiyomi.extension.* metadata
- Host stubs: NetworkHelper (CookieJar, CF retry), Injekt, HttpSource, ParsedHttpSource, SManga/SChapter/Page/MangasPage, Filter, GET/POST, rx.Observable bridge
- MihonSourceAdapter reflects get/fetch Popular/Search/Latest/Details/Chapters/PageList

## LNReader runtime
- Rhino JS engine + host bindings (fetch, absoluteUrl, storageGet/Set)
- LnReaderSourceAdapter evaluates plugin methods and maps to ContentPayload.Text

## Fallback
- HostFallbackSource wraps CatalogService (Jsoup + AJAX) — last resort only

## Product edge
- Aggregator multi-source chapter merge
- SourceHealth success/failure tracking
- Encrypted downloads, library persistence, Creator Studio, Supabase auth
