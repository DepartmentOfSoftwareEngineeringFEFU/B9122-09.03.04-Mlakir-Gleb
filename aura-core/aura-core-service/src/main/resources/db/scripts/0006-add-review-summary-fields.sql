--liquibase formatted sql

--changeset mlakir:0006-add-review-summary-fields
alter table aura_core.reviews
    add column if not exists summary text,
    add column if not exists summary_generated_at timestamptz,
    add column if not exists summary_model_version varchar(100);
