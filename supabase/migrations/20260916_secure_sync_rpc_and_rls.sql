-- ==============================================================================
-- VIRA — Migration: Explicit Security Hardening, Granular RLS, and Safe Defaults
-- Migration File: supabase/migrations/20260916_secure_sync_rpc_and_rls.sql
-- ==============================================================================

-- 1. Ensure Global Sequence exists
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

-- 6. Explicit targeted revokes on Vira private tables and sequences (avoiding blind blanket revokes)
revoke all on table public.user_profiles from anon, public;
revoke all on table public.collection_spots from anon, public;
revoke all on table public.collection_entries from anon, public;
revoke all on table public.redemption_entries from anon, public;
revoke all on table public.goals from anon, public;

revoke all on sequence public.global_sync_version_seq from anon, public;

-- 7. Grant explicit minimal privileges only to authenticated role
grant select, insert, update, delete on table
    public.user_profiles,
    public.collection_spots,
    public.collection_entries,
    public.redemption_entries,
    public.goals
to authenticated;

grant usage on sequence public.global_sync_version_seq to authenticated;

-- 8. Configure secure default privileges for future objects created in schema public
-- Prevents newly created tables/functions from automatically exposing themselves to anon/public
alter default privileges in schema public revoke execute on functions from public, anon;
alter default privileges in schema public revoke all on tables from anon, public;
alter default privileges in schema public revoke all on sequences from anon, public;

-- 4. Drop legacy permissive development policies
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.user_profiles;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_spots;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.redemption_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.goals;

-- 5. Guarantee Row Level Security (RLS) enabled on all private sync tables
alter table public.user_profiles enable row level security;
alter table public.collection_spots enable row level security;
alter table public.collection_entries enable row level security;
alter table public.redemption_entries enable row level security;
alter table public.goals enable row level security;

-- 6. Granular RLS Policies for user_profiles
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

drop policy if exists "Users can delete own profile" on public.user_profiles;
create policy "Users can delete own profile"
    on public.user_profiles for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- 7. Granular RLS Policies for collection_spots
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

drop policy if exists "Users can delete own spots" on public.collection_spots;
create policy "Users can delete own spots"
    on public.collection_spots for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- 8. Granular RLS Policies for collection_entries
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

drop policy if exists "Users can delete own collections" on public.collection_entries;
create policy "Users can delete own collections"
    on public.collection_entries for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- 9. Granular RLS Policies for redemption_entries
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

drop policy if exists "Users can delete own redemptions" on public.redemption_entries;
create policy "Users can delete own redemptions"
    on public.redemption_entries for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- 10. Granular RLS Policies for goals
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

drop policy if exists "Users can delete own goals" on public.goals;
create policy "Users can delete own goals"
    on public.goals for delete
    to authenticated
    using (user_id = (select auth.uid()));

-- 11. Performance Composite Indexes for (user_id, server_version)
create index if not exists idx_collection_entries_user_version on public.collection_entries (user_id, server_version);
create index if not exists idx_collection_spots_user_version on public.collection_spots (user_id, server_version);
create index if not exists idx_redemption_entries_user_version on public.redemption_entries (user_id, server_version);
create index if not exists idx_goals_user_version on public.goals (user_id, server_version);
create index if not exists idx_user_profiles_user_version on public.user_profiles (user_id, server_version);

-- 12. Atomic Snapshot-Consistent Pull RPC (SECURITY INVOKER + Caller Ownership Isolation)
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
    -- 1. Authoritative Authentication Check
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

-- 13. Restrict Execution Strictly to Authenticated Callers
revoke execute on function public.pull_sync_changes(bigint) from public, anon, service_role;
grant execute on function public.pull_sync_changes(bigint) to authenticated;
