--liquibase formatted sql

--changeset mlakir:0008-add-source-schedule-fields
alter table aura_core.sources
    add column if not exists schedule_enabled boolean not null default false,
    add column if not exists schedule_interval_minutes integer,
    add column if not exists last_collected_at timestamptz,
    add column if not exists next_collection_at timestamptz;

update aura_core.sources
set schedule_enabled = true,
    schedule_interval_minutes = coalesce(schedule_interval_minutes, 1440),
    next_collection_at = coalesce(next_collection_at, now() + interval '1440 minutes')
where collection_mode = 'SCHEDULED'
  and schedule_enabled = false;

update aura_core.sources
set schedule_interval_minutes = coalesce(schedule_interval_minutes, 1440),
    next_collection_at = coalesce(next_collection_at, now() + (coalesce(schedule_interval_minutes, 1440) || ' minutes')::interval)
where schedule_enabled = true;

update aura_core.sources
set schedule_interval_minutes = null,
    next_collection_at = null
where schedule_enabled = false;

create index if not exists idx_sources_schedule_due
    on aura_core.sources (schedule_enabled, next_collection_at);
