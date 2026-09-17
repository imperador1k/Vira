-- ==============================================================================
-- VIRA — Hotfix Migration: Hardened Security, Single-Statement Atomic Snapshot,
-- Tombstone-Only Architecture, and Internal Function Privilege Protection
-- File: supabase/migrations/20260916_secure_sync_rpc_and_rls.sql
-- ==============================================================================

begin;

-- 1. Ensure Global Monotonic Sequence exists
create sequence if not exists public.global_sync_version_seq as bigint start with 1 increment by 1;

-- 2. Drop legacy identities and column defaults to eliminate double nextval invocations
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

-- 3. Register server metadata trigger function
create or replace function public.assign_server_sync_metadata()
returns trigger as $$
begin
    new.server_version = nextval('public.global_sync_version_seq');
    new.server_updated_at = now();
    return new;
end;
$$ language plpgsql;

-- 4. Register BEFORE INSERT OR UPDATE triggers on all 5 private sync tables
drop trigger if exists trg_user_profiles_sync_metadata on public.user_profiles;
create trigger trg_user_profiles_sync_metadata
before insert or update on public.user_profiles
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_collection_spots_sync_metadata on public.collection_spots;
create trigger trg_collection_spots_sync_metadata
before insert or update on public.collection_spots
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_collection_entries_sync_metadata on public.collection_entries;
create trigger trg_collection_entries_sync_metadata
before insert or update on public.collection_entries
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_redemption_entries_sync_metadata on public.redemption_entries;
create trigger trg_redemption_entries_sync_metadata
before insert or update on public.redemption_entries
for each row execute function public.assign_server_sync_metadata();

drop trigger if exists trg_goals_sync_metadata on public.goals;
create trigger trg_goals_sync_metadata
before insert or update on public.goals
for each row execute function public.assign_server_sync_metadata();

-- 5. Safe Monotonic Sequence Initialization (never moves sequence backwards)
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

    v_target := greatest(v_max_existing, v_current_seq, 1);

    perform setval('public.global_sync_version_seq', v_target, true);
end $$;

-- 6. Explicit targeted revokes on Vira private tables, sequences, and internal trigger function
revoke all on table public.user_profiles from anon, public;
revoke all on table public.collection_spots from anon, public;
revoke all on table public.collection_entries from anon, public;
revoke all on table public.redemption_entries from anon, public;
revoke all on table public.goals from anon, public;

revoke all on sequence public.global_sync_version_seq from anon, public;

-- Revoke physical DELETE from all client roles (Enforces Tombstone-only architecture)
revoke delete on table
    public.user_profiles,
    public.collection_spots,
    public.collection_entries,
    public.redemption_entries,
    public.goals
from authenticated, anon, public;

-- Harden internal trigger function: revoke direct EXECUTE from client roles
revoke execute on function public.assign_server_sync_metadata() from public, anon, authenticated;

-- 7. Grant explicit minimal privileges only to authenticated role (No DELETE granted)
grant select, insert, update on table
    public.user_profiles,
    public.collection_spots,
    public.collection_entries,
    public.redemption_entries,
    public.goals
to authenticated;

grant usage on sequence public.global_sync_version_seq to authenticated;

-- 8. Configure secure default privileges for role postgres and schema public
alter default privileges for role postgres in schema public revoke execute on functions from public, anon;
alter default privileges for role postgres in schema public revoke all on tables from anon, public;
alter default privileges for role postgres in schema public revoke all on sequences from anon, public;

alter default privileges in schema public revoke execute on functions from public, anon;
alter default privileges in schema public revoke all on tables from anon, public;
alter default privileges in schema public revoke all on sequences from anon, public;

-- 9. Clean up any legacy development or previous DELETE policies
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.user_profiles;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_spots;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.redemption_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.goals;

drop policy if exists "Users can delete own profile" on public.user_profiles;
drop policy if exists "Users can delete own spots" on public.collection_spots;
drop policy if exists "Users can delete own collections" on public.collection_entries;
drop policy if exists "Users can delete own redemptions" on public.redemption_entries;
drop policy if exists "Users can delete own goals" on public.goals;

-- 10. Guarantee Row Level Security (RLS) enabled on all private sync tables
alter table public.user_profiles enable row level security;
alter table public.collection_spots enable row level security;
alter table public.collection_entries enable row level security;
alter table public.redemption_entries enable row level security;
alter table public.goals enable row level security;

-- 11. Granular RLS Policies for user_profiles (SELECT, INSERT, UPDATE only)
drop policy if exists "Users can select own profile" on public.user_profiles;
create policy "Users can select own profile"
    on public.user_profiles for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists "Users can insert own profile" on public.user_profiles;
create policy "Users can insert own profile"
    on public.user_profiles for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists "Users can update own profile" on public.user_profiles;
create policy "Users can update own profile"
    on public.user_profiles for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- 12. Granular RLS Policies for collection_spots (SELECT, INSERT, UPDATE only)
drop policy if exists "Users can select own spots" on public.collection_spots;
create policy "Users can select own spots"
    on public.collection_spots for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists "Users can insert own spots" on public.collection_spots;
create policy "Users can insert own spots"
    on public.collection_spots for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists "Users can update own spots" on public.collection_spots;
create policy "Users can update own spots"
    on public.collection_spots for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- 13. Granular RLS Policies for collection_entries (SELECT, INSERT, UPDATE only)
drop policy if exists "Users can select own collections" on public.collection_entries;
create policy "Users can select own collections"
    on public.collection_entries for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists "Users can insert own collections" on public.collection_entries;
create policy "Users can insert own collections"
    on public.collection_entries for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists "Users can update own collections" on public.collection_entries;
create policy "Users can update own collections"
    on public.collection_entries for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- 14. Granular RLS Policies for redemption_entries (SELECT, INSERT, UPDATE only)
drop policy if exists "Users can select own redemptions" on public.redemption_entries;
create policy "Users can select own redemptions"
    on public.redemption_entries for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists "Users can insert own redemptions" on public.redemption_entries;
create policy "Users can insert own redemptions"
    on public.redemption_entries for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists "Users can update own redemptions" on public.redemption_entries;
create policy "Users can update own redemptions"
    on public.redemption_entries for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- 15. Granular RLS Policies for goals (SELECT, INSERT, UPDATE only)
drop policy if exists "Users can select own goals" on public.goals;
create policy "Users can select own goals"
    on public.goals for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists "Users can insert own goals" on public.goals;
create policy "Users can insert own goals"
    on public.goals for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists "Users can update own goals" on public.goals;
create policy "Users can update own goals"
    on public.goals for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- 16. Performance Composite Indexes for (user_id, server_version)
create index if not exists idx_collection_entries_user_version on public.collection_entries (user_id, server_version);
create index if not exists idx_collection_spots_user_version on public.collection_spots (user_id, server_version);
create index if not exists idx_redemption_entries_user_version on public.redemption_entries (user_id, server_version);
create index if not exists idx_goals_user_version on public.goals (user_id, server_version);
create index if not exists idx_user_profiles_user_version on public.user_profiles (user_id, server_version);

-- 17. Single-Statement Atomic Snapshot Pull RPC
-- Declared STABLE: read-only, side-effect free, optimizer friendly.
-- Executes in a SINGLE SQL statement using CTEs:
-- Guarantees that all 5 tables and the new cursor are evaluated against the EXACT SAME database snapshot at query start.
create or replace function public.pull_sync_changes(p_since_cursor bigint default 0)
returns jsonb
stable
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_caller uuid;
    v_result jsonb;
begin
    -- 1. Authoritative Authentication Check
    v_caller := auth.uid();
    if v_caller is null then
        raise exception 'Unauthorized: authentication required' using errcode = '42501';
    end if;

    -- 2. Execute a SINGLE ATOMIC STATEMENT across all synchronizable tables.
    -- In PostgreSQL READ COMMITTED, all CTEs within a single statement evaluate
    -- against the EXACT SAME single database snapshot taken at statement start.
    with
    ce as (
        select coalesce(jsonb_agg(to_jsonb(c)), '[]'::jsonb) as data,
               coalesce(max(c.server_version), p_since_cursor) as max_v
        from public.collection_entries c
        where c.user_id = v_caller
          and c.server_version > p_since_cursor
    ),
    cs as (
        select coalesce(jsonb_agg(to_jsonb(s)), '[]'::jsonb) as data,
               coalesce(max(s.server_version), p_since_cursor) as max_v
        from public.collection_spots s
        where s.user_id = v_caller
          and s.server_version > p_since_cursor
    ),
    re as (
        select coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb) as data,
               coalesce(max(r.server_version), p_since_cursor) as max_v
        from public.redemption_entries r
        where r.user_id = v_caller
          and r.server_version > p_since_cursor
    ),
    g as (
        select coalesce(jsonb_agg(to_jsonb(gl)), '[]'::jsonb) as data,
               coalesce(max(gl.server_version), p_since_cursor) as max_v
        from public.goals gl
        where gl.user_id = v_caller
          and gl.server_version > p_since_cursor
    ),
    up as (
        select coalesce(jsonb_agg(to_jsonb(p)), '[]'::jsonb) as data,
               coalesce(max(p.server_version), p_since_cursor) as max_v
        from public.user_profiles p
        where p.user_id = v_caller
          and p.server_version > p_since_cursor
    )
    select jsonb_build_object(
        'collections', ce.data,
        'spots', cs.data,
        'redemptions', re.data,
        'goals', g.data,
        'profile', (case when jsonb_array_length(up.data) > 0 then up.data->0 else null end),
        'new_cursor', greatest(p_since_cursor, ce.max_v, cs.max_v, re.max_v, g.max_v, up.max_v),
        'has_more', false
    )
    into v_result
    from ce cross join cs cross join re cross join g cross join up;

    return v_result;
end;
$$;

-- 18. Restrict Execution Strictly to Authenticated Callers
revoke execute on function public.pull_sync_changes(bigint) from public, anon, service_role;
grant execute on function public.pull_sync_changes(bigint) to authenticated;

commit;
