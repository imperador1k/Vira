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
    server_version bigint not null,
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
    server_version bigint not null,
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
    server_version bigint not null,
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
    server_version bigint not null,
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
    server_version bigint not null,
    deleted_at timestamptz default null
);

-- 8. Safe migration to drop identities and drop column defaults (preventing double nextval)
alter table public.user_profiles alter column server_version drop identity if exists;
alter table public.user_profiles alter column server_version drop default;

alter table public.collection_spots alter column server_version drop identity if exists;
alter table public.collection_spots alter column server_version drop default;

alter table public.collection_entries alter column server_version drop identity if exists;
alter table public.collection_entries alter column server_version drop default;

alter table public.redemption_entries alter column server_version drop identity if exists;
alter table public.redemption_entries alter column server_version drop default;

alter table public.goals alter column server_version drop identity if exists;
alter table public.goals alter column server_version drop default;

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

-- 10. Enable Row Level Security (RLS) on all private sync tables
alter table public.user_profiles enable row level security;
alter table public.collection_spots enable row level security;
alter table public.collection_entries enable row level security;
alter table public.redemption_entries enable row level security;
alter table public.goals enable row level security;

-- Drop legacy permissive development policies
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.user_profiles;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_spots;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.redemption_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.goals;

-- Granular RLS Policies for user_profiles
create policy "Users can select own profile"
    on public.user_profiles for select
    to authenticated
    using (user_id = (select auth.uid()));

create policy "Users can insert own profile"
    on public.user_profiles for insert
    to authenticated
    with check (user_id = (select auth.uid()));

create policy "Users can update own profile"
    on public.user_profiles for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "Users can delete own profile"
    on public.user_profiles for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- Granular RLS Policies for collection_spots
create policy "Users can select own spots"
    on public.collection_spots for select
    to authenticated
    using (user_id = (select auth.uid()));

create policy "Users can insert own spots"
    on public.collection_spots for insert
    to authenticated
    with check (user_id = (select auth.uid()));

create policy "Users can update own spots"
    on public.collection_spots for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "Users can delete own spots"
    on public.collection_spots for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- Granular RLS Policies for collection_entries
create policy "Users can select own collections"
    on public.collection_entries for select
    to authenticated
    using (user_id = (select auth.uid()));

create policy "Users can insert own collections"
    on public.collection_entries for insert
    to authenticated
    with check (user_id = (select auth.uid()));

create policy "Users can update own collections"
    on public.collection_entries for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "Users can delete own collections"
    on public.collection_entries for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- Granular RLS Policies for redemption_entries
create policy "Users can select own redemptions"
    on public.redemption_entries for select
    to authenticated
    using (user_id = (select auth.uid()));

create policy "Users can insert own redemptions"
    on public.redemption_entries for insert
    to authenticated
    with check (user_id = (select auth.uid()));

create policy "Users can update own redemptions"
    on public.redemption_entries for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "Users can delete own redemptions"
    on public.redemption_entries for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- Granular RLS Policies for goals
create policy "Users can select own goals"
    on public.goals for select
    to authenticated
    using (user_id = (select auth.uid()));

create policy "Users can insert own goals"
    on public.goals for insert
    to authenticated
    with check (user_id = (select auth.uid()));

create policy "Users can update own goals"
    on public.goals for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

create policy "Users can delete own goals"
    on public.goals for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- 11. Table & Sequence Grants (Revoke all from anon/public; grant to authenticated)
revoke all on all tables in schema public from anon, public;
revoke all on all sequences in schema public from anon, public;
revoke all on all functions in schema public from anon, public;

grant select, insert, update, delete on table
    public.user_profiles,
    public.collection_spots,
    public.collection_entries,
    public.redemption_entries,
    public.goals
to authenticated, service_role;

grant usage on sequence public.global_sync_version_seq to authenticated, service_role;

-- 12. Performance Composite Indexes for (user_id, server_version)
create index if not exists idx_collection_entries_user_version on public.collection_entries (user_id, server_version);
create index if not exists idx_collection_spots_user_version on public.collection_spots (user_id, server_version);
create index if not exists idx_redemption_entries_user_version on public.redemption_entries (user_id, server_version);
create index if not exists idx_goals_user_version on public.goals (user_id, server_version);
create index if not exists idx_user_profiles_user_version on public.user_profiles (user_id, server_version);

-- ==============================================================================
-- 13. Corrective Sequence Initialization
-- Sets global_sync_version_seq to at least the highest server_version existing
-- across all tables, never moving the sequence backwards.
-- ==============================================================================
do $$
declare
    v_max_existing bigint;
    v_current_seq bigint;
    v_target bigint;
begin
    select coalesce(max(mv), 0) into v_max_existing from (
        select coalesce(max(server_version), 0) as mv from public.user_profiles
        union all
        select coalesce(max(server_version), 0) as mv from public.collection_spots
        union all
        select coalesce(max(server_version), 0) as mv from public.collection_entries
        union all
        select coalesce(max(server_version), 0) as mv from public.redemption_entries
        union all
        select coalesce(max(server_version), 0) as mv from public.goals
    ) as sub;

    select last_value into v_current_seq from public.global_sync_version_seq;

    -- Guarantee sequence is initialized above existing data and never moves backwards
    v_target := greatest(v_max_existing, v_current_seq, 1);

    perform setval('public.global_sync_version_seq', v_target, true);
end $$;

-- ==============================================================================
-- 14. Atomic Snapshot-Consistent Pull RPC (SECURITY INVOKER + User Isolated)
-- Requires authentication, filters strictly by caller auth.uid(), and returns
-- consistent new_cursor evaluated inside caller permissions.
-- ==============================================================================
create or replace function public.pull_sync_changes(p_since_cursor bigint default 0)
returns jsonb as $$
declare
    v_caller uuid;
    v_collections jsonb;
    v_spots jsonb;
    v_redemptions jsonb;
    v_goals jsonb;
    v_profiles jsonb;
    v_max_collection bigint;
    v_max_spot bigint;
    v_max_redemption bigint;
    v_max_goal bigint;
    v_max_profile bigint;
    v_new_cursor bigint;
begin
    -- 1. Strict Authentication Requirement
    v_caller := auth.uid();
    if v_caller is null then
        raise exception 'Unauthorized: authentication required' using errcode = '42501';
    end if;

    -- 2. Read caller's collections
    select coalesce(jsonb_agg(to_jsonb(c)), '[]'::jsonb), coalesce(max(c.server_version), p_since_cursor)
    into v_collections, v_max_collection
    from public.collection_entries c
    where c.user_id = v_caller
      and c.server_version > p_since_cursor;

    -- 3. Read caller's spots
    select coalesce(jsonb_agg(to_jsonb(s)), '[]'::jsonb), coalesce(max(s.server_version), p_since_cursor)
    into v_spots, v_max_spot
    from public.collection_spots s
    where s.user_id = v_caller
      and s.server_version > p_since_cursor;

    -- 4. Read caller's redemptions
    select coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb), coalesce(max(r.server_version), p_since_cursor)
    into v_redemptions, v_max_redemption
    from public.redemption_entries r
    where r.user_id = v_caller
      and r.server_version > p_since_cursor;

    -- 5. Read caller's goals
    select coalesce(jsonb_agg(to_jsonb(g)), '[]'::jsonb), coalesce(max(g.server_version), p_since_cursor)
    into v_goals, v_max_goal
    from public.goals g
    where g.user_id = v_caller
      and g.server_version > p_since_cursor;

    -- 6. Read caller's profile
    select coalesce(jsonb_agg(to_jsonb(p)), '[]'::jsonb), coalesce(max(p.server_version), p_since_cursor)
    into v_profiles, v_max_profile
    from public.user_profiles p
    where p.user_id = v_caller
      and p.server_version > p_since_cursor;

    -- Compute atomic new cursor strictly from max version of caller records returned in snapshot
    v_new_cursor := greatest(
        p_since_cursor,
        v_max_collection,
        v_max_spot,
        v_max_redemption,
        v_max_goal,
        v_max_profile
    );

    return jsonb_build_object(
        'collections', v_collections,
        'spots', v_spots,
        'redemptions', v_redemptions,
        'goals', v_goals,
        'profile', (case when jsonb_array_length(v_profiles) > 0 then v_profiles->0 else null end),
        'new_cursor', v_new_cursor,
        'has_more', false
    );
end;
$$ language plpgsql security invoker set search_path = public;

-- Restrict Execution to Authenticated Callers (Revoke from public & anon)
revoke execute on function public.pull_sync_changes(bigint) from public, anon;
grant execute on function public.pull_sync_changes(bigint) to authenticated, service_role;
