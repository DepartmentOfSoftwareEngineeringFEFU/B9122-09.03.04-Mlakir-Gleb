--liquibase formatted sql

--changeset mlakir:0004-add-dashboard-indexes
create index if not exists idx_reviews_published_at on aura_core.reviews (published_at);
create index if not exists idx_reviews_source_id_published_at on aura_core.reviews (source_id, published_at);
