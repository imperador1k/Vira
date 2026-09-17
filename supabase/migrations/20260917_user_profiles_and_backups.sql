-- ==============================================================================
-- VIRA — User Profiles Extension & User Backups Snapshot Storage
-- Migration: 20260917_user_profiles_and_backups.sql
-- ==============================================================================

begin;

-- 1. Extend user_profiles with optional username, city, and avatar_url
alter table public.user_profiles
    add column if not exists username text default null,
    add column if not exists city text default null,
    add column if not exists avatar_url text default null;

-- 2. Create user_backups table for immutable point-in-time recovery snapshots
create table if not exists public.user_backups (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    created_at timestamptz not null default now(),
    device_model text,
    collections_count integer not null default 0,
    spots_count integer not null default 0,
    size_bytes bigint not null default 0,
    backup_type text not null default 'MANUAL', -- 'DAILY', 'WEEKLY', 'MANUAL'
    payload jsonb not null
);

-- 3. Row Level Security for user_backups
alter table public.user_backups enable row level security;

drop policy if exists "Users can select own backups" on public.user_backups;
create policy "Users can select own backups"
    on public.user_backups for select
    to authenticated
    using (auth.uid() = user_id);

drop policy if exists "Users can insert own backups" on public.user_backups;
create policy "Users can insert own backups"
    on public.user_backups for insert
    to authenticated
    with check (auth.uid() = user_id);

drop policy if exists "Users can delete own backups" on public.user_backups;
create policy "Users can delete own backups"
    on public.user_backups for delete
    to authenticated
    using (auth.uid() = user_id);

-- 4. Index for efficient snapshot retrieval and retention policy pruning
create index if not exists idx_user_backups_user_created 
    on public.user_backups (user_id, created_at desc);

commit;
