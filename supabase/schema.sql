-- ==============================================================================
-- VIRA — Supabase PostgreSQL Schema & Global Versioning Protocol
-- ==============================================================================

create extension if not exists "uuid-ossp";

-- 1. Global Monotonic Sync Sequence shared across all synchronizable tables
create sequence if not exists public.global_sync_version_seq as bigint start with 1 increment by 1;

-- 2. Trigger function to assign strictly monotonic server_version and authoritative timestamp on INSERT & UPDATE
create or replace function public.assign_server_sync_metadata()
returns trigger as $$
begin
    new.server_version = nextval('public.global_sync_version_seq');
    new.server_updated_at = now();
    return new;
end;
$$ language plpgsql;

-- 3. User Profiles
create table if not exists public.user_profiles (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    name text not null default '',
    theme_preference text not null default 'SYSTEM',
    client_updated_at bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    server_version bigint not null default nextval('public.global_sync_version_seq'),
    deleted_at timestamptz default null
);

-- 4. Collection Spots (Private collection points)
create table if not exists public.collection_spots (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    name text not null,
    latitude double precision not null,
    longitude double precision not null,
    address text,
    client_created_at bigint not null default 0,
    client_updated_at bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    server_version bigint not null default nextval('public.global_sync_version_seq'),
    deleted_at timestamptz default null
);

-- 5. Collection Entries
create table if not exists public.collection_entries (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    container_count integer not null check (container_count > 0),
    timestamp bigint not null,
    estimated_value_cents bigint not null check (estimated_value_cents >= 0),
    spot_id uuid references public.collection_spots(id) on delete set null,
    note text,
    latitude double precision,
    longitude double precision,
    client_updated_at bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    server_version bigint not null default nextval('public.global_sync_version_seq'),
    deleted_at timestamptz default null
);

-- 6. Redemption Entries
create table if not exists public.redemption_entries (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    presented_containers integer not null check (presented_containers >= 0),
    accepted_containers integer not null check (accepted_containers >= 0),
    rejected_containers integer not null default 0,
    actual_recovered_cents bigint not null check (actual_recovered_cents >= 0),
    timestamp bigint not null,
    return_point_id text,
    note text,
    client_updated_at bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    server_version bigint not null default nextval('public.global_sync_version_seq'),
    deleted_at timestamptz default null
);

-- 7. Goals
create table if not exists public.goals (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade default auth.uid(),
    type text not null,
    target_value integer not null,
    period text not null,
    is_active boolean not null default true,
    client_updated_at bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    server_version bigint not null default nextval('public.global_sync_version_seq'),
    deleted_at timestamptz default null
);

-- 8. Safe migration in case tables were previously created with identity columns
alter table public.user_profiles alter column server_version drop identity if exists;
alter table public.user_profiles alter column server_version set default nextval('public.global_sync_version_seq');

alter table public.collection_spots alter column server_version drop identity if exists;
alter table public.collection_spots alter column server_version set default nextval('public.global_sync_version_seq');

alter table public.collection_entries alter column server_version drop identity if exists;
alter table public.collection_entries alter column server_version set default nextval('public.global_sync_version_seq');

alter table public.redemption_entries alter column server_version drop identity if exists;
alter table public.redemption_entries alter column server_version set default nextval('public.global_sync_version_seq');

alter table public.goals alter column server_version drop identity if exists;
alter table public.goals alter column server_version set default nextval('public.global_sync_version_seq');

-- 9. Register BEFORE INSERT OR UPDATE triggers to ensure every mutation advances the global version
drop trigger if exists trg_user_profiles_updated on public.user_profiles;
drop trigger if exists trg_user_profiles_sync_metadata on public.user_profiles;
create or replace trigger trg_user_profiles_sync_metadata
before insert or update on public.user_profiles
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_collection_spots_updated on public.collection_spots;
drop trigger if exists trg_collection_spots_sync_metadata on public.collection_spots;
create or replace trigger trg_collection_spots_sync_metadata
before insert or update on public.collection_spots
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_collection_entries_updated on public.collection_entries;
drop trigger if exists trg_collection_entries_sync_metadata on public.collection_entries;
create or replace trigger trg_collection_entries_sync_metadata
before insert or update on public.collection_entries
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_redemption_entries_updated on public.redemption_entries;
drop trigger if exists trg_redemption_entries_sync_metadata on public.redemption_entries;
create or replace trigger trg_redemption_entries_sync_metadata
before insert or update on public.redemption_entries
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_goals_updated on public.goals;
drop trigger if exists trg_goals_sync_metadata on public.goals;
create or replace trigger trg_goals_sync_metadata
before insert or update on public.goals
for each row execute function public.assign_server_sync_metadata();

-- 10. Enable Row Level Security (RLS) with permissive fallback for guest/anon dev sync
alter table public.user_profiles enable row level security;
alter table public.collection_spots enable row level security;
alter table public.collection_entries enable row level security;
alter table public.redemption_entries enable row level security;
alter table public.goals enable row level security;

drop policy if exists "Allow all operations for anon/authenticated in dev" on public.user_profiles;
create policy "Allow all operations for anon/authenticated in dev" on public.user_profiles for all using (true) with check (true);

drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_spots;
create policy "Allow all operations for anon/authenticated in dev" on public.collection_spots for all using (true) with check (true);

drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_entries;
create policy "Allow all operations for anon/authenticated in dev" on public.collection_entries for all using (true) with check (true);

drop policy if exists "Allow all operations for anon/authenticated in dev" on public.redemption_entries;
create policy "Allow all operations for anon/authenticated in dev" on public.redemption_entries for all using (true) with check (true);

drop policy if exists "Allow all operations for anon/authenticated in dev" on public.goals;
create policy "Allow all operations for anon/authenticated in dev" on public.goals for all using (true) with check (true);
