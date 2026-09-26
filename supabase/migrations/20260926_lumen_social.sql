-- Lumen social (Supabase2 project ryoewtikgwmyejrpjgnw)
-- Reactions + comments keyed by external_series_id + chapter_id

create table if not exists public.lumen_reactions (
  id uuid primary key default gen_random_uuid(),
  external_series_id text not null,
  chapter_id text not null default '',
  user_key text not null default 'anon',
  vote text not null check (vote in ('like','dislike')),
  created_at timestamptz not null default now(),
  unique (external_series_id, chapter_id, user_key)
);

create table if not exists public.lumen_comments (
  id uuid primary key default gen_random_uuid(),
  external_series_id text not null,
  chapter_id text not null default '',
  author text not null default 'Reader',
  body text not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_lumen_reactions_target on public.lumen_reactions (external_series_id, chapter_id);
create index if not exists idx_lumen_comments_target on public.lumen_comments (external_series_id, chapter_id);
