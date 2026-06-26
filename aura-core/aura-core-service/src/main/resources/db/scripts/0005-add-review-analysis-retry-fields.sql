--liquibase formatted sql

--changeset mlakir:0005-add-review-analysis-retry-fields
alter table aura_core.reviews
    add column if not exists analysis_retry_count integer not null default 0;

alter table aura_core.reviews
    add column if not exists last_analysis_attempt_at timestamptz;

alter table aura_core.reviews
    add column if not exists analysis_error_message text;

update aura_core.reviews
set analysis_retry_count = 0
where analysis_retry_count is null;
