--liquibase formatted sql

--changeset mlakir:0002-create-core-tables
create table if not exists aura_core.sources
(
    id              bigserial primary key,
    name            varchar(255)  not null,
    type            varchar(50)   not null,
    base_url        varchar(1000) not null,
    is_active       boolean       not null default true,
    collection_mode varchar(50)   not null,
    description     varchar(2000),
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    constraint uq_sources_name_type unique (name, type)
);

create table if not exists aura_core.reviews
(
    id           bigserial primary key,
    source_id    bigint        not null,
    external_id  varchar(255)  not null,
    text         text          not null,
    author_name  varchar(255),
    rating       integer,
    published_at timestamptz   not null,
    original_url varchar(1000),
    collected_at timestamptz   not null,
    status       varchar(50)   not null,
    constraint fk_reviews_source foreign key (source_id) references aura_core.sources (id),
    constraint uq_reviews_source_external unique (source_id, external_id)
);

create table if not exists aura_core.review_analysis
(
    id            bigserial primary key,
    review_id      bigint         not null,
    sentiment      varchar(50)    not null,
    topic          varchar(50)    not null,
    keywords       varchar(1000),
    confidence     numeric(5, 2)  not null,
    model_version  varchar(100)   not null,
    analyzed_at    timestamptz    not null,
    constraint fk_review_analysis_review foreign key (review_id) references aura_core.reviews (id),
    constraint uq_review_analysis_review unique (review_id)
);

create table if not exists aura_core.collection_jobs
(
    id              bigserial primary key,
    source_id        bigint        not null,
    status           varchar(50)   not null,
    started_at       timestamptz   not null,
    finished_at      timestamptz,
    collected_count  integer       not null default 0,
    error_message    varchar(2000),
    triggered_by     varchar(255)  not null,
    constraint fk_collection_jobs_source foreign key (source_id) references aura_core.sources (id)
);

create index if not exists idx_reviews_source_id on aura_core.reviews (source_id);
create index if not exists idx_reviews_published_at on aura_core.reviews (published_at);
create index if not exists idx_review_analysis_sentiment on aura_core.review_analysis (sentiment);
create index if not exists idx_review_analysis_topic on aura_core.review_analysis (topic);
create index if not exists idx_collection_jobs_started_at on aura_core.collection_jobs (started_at);
