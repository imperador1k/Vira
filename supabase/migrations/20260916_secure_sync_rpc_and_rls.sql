-- ==============================================================================
-- VIRA — Migration: Secure Sync RPC, Granular RLS, and Schema Hardening
-- Migration File: supabase/migrations/20260916_secure_sync_rpc_and_rls.sql
-- ==============================================================================

-- 1. Revoke default public/anon table, sequence and function privileges
revoke all on all tables in schema public from anon, public;
revoke all on all sequences in schema public from anon, public;
revoke all on all functions in schema public from anon, public;

-- 2. Grant explicit minimal privileges to authenticated role
grant select, insert, update, delete on table
    public.user_profiles,
    public.collection_spots,
    public.collection_entries,
    public.redemption_entries,
    public.goals
to authenticated, service_role;

grant usage on sequence public.global_sync_version_seq to authenticated, service_role;

-- 3. Drop all legacy permissive development policies
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.user_profiles;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_spots;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.collection_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.redemption_entries;
drop policy if exists "Allow all operations for anon/authenticated in dev" on public.goals;

-- 4. Enable Row Level Security (RLS) on all private sync tables
alter table public.user_profiles enable row level security;
alter table public.collection_spots enable row level security;
alter table public.collection_entries enable row level security;
alter table public.redemption_entries enable row level security;
alter table public.goals enable row level security;

-- 5. Create Granular RLS Policies for user_profiles
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

-- 6. Create Granular RLS Policies for collection_spots
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

-- 7. Create Granular RLS Policies for collection_entries
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

-- 8. Create Granular RLS Policies for redemption_entries
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

-- 9. Create Granular RLS Policies for goals
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

-- 10. Performance Composite Indexes for (user_id, server_version)
create index if not exists idx_collection_entries_user_version on public.collection_entries (user_id, server_version);
create index if not exists idx_collection_spots_user_version on public.collection_spots (user_id, server_version);
create index if not exists idx_redemption_entries_user_version on public.redemption_entries (user_id, server_version);
create index if not exists idx_goals_user_version on public.goals (user_id, server_version);
create index if not exists idx_user_profiles_user_version on public.user_profiles (user_id, server_version);

-- 11. Replace pull_sync_changes with SECURITY INVOKER, auth.uid() check, and strict user scoping
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
    -- Strict Authentication Requirement
    v_caller := auth.uid();
    if v_caller is null then
        raise exception 'Unauthorized: authentication required' using errcode = '42501';
    end if;

    -- 1. Read caller's collections
    select coalesce(jsonb_agg(to_jsonb(c)), '[]'::jsonb), coalesce(max(c.server_version), p_since_cursor)
    into v_collections, v_max_collection
    from public.collection_entries c
    where c.user_id = v_caller
      and c.server_version > p_since_cursor;

    -- 2. Read caller's spots
    select coalesce(jsonb_agg(to_jsonb(s)), '[]'::jsonb), coalesce(max(s.server_version), p_since_cursor)
    into v_spots, v_max_spot
    from public.collection_spots s
    where s.user_id = v_caller
      and s.server_version > p_since_cursor;

    -- 3. Read caller's redemptions
    select coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb), coalesce(max(r.server_version), p_since_cursor)
    into v_redemptions, v_max_redemption
    from public.redemption_entries r
    where r.user_id = v_caller
      and r.server_version > p_since_cursor;

    -- 4. Read caller's goals
    select coalesce(jsonb_agg(to_jsonb(g)), '[]'::jsonb), coalesce(max(g.server_version), p_since_cursor)
    into v_goals, v_max_goal
    from public.goals g
    where g.user_id = v_caller
      and g.server_version > p_since_cursor;

    -- 5. Read caller's profile
    select coalesce(jsonb_agg(to_jsonb(p)), '[]'::jsonb), coalesce(max(p.server_version), p_since_cursor)
    into v_profiles, v_max_profile
    from public.user_profiles p
    where p.user_id = v_caller
      and p.server_version > p_since_cursor;

    -- Calculate atomic new cursor strictly from max version of caller records returned in snapshot
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

-- 12. Restrict Execution to Authenticated Callers (Revoke from public & anon)
revoke execute on function public.pull_sync_changes(bigint) from public, anon;
grant execute on function public.pull_sync_changes(bigint) to authenticated, service_role;
