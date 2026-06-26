--liquibase formatted sql

--changeset mlakir:0007-create-organization-insights
create table if not exists aura_core.organization_insights
(
    id               bigserial primary key,
    organization_id  bigint         not null,
    summary          text           not null,
    strengths        text           not null,
    weaknesses       text           not null,
    recommendations  text           not null,
    reviews_used     integer        not null,
    model_version    varchar(100)   not null,
    generated_at     timestamptz    not null,
    created_at       timestamptz    not null,
    updated_at       timestamptz    not null,
    constraint fk_organization_insights_organization
        foreign key (organization_id) references aura_core.organizations (id)
);

create index if not exists idx_organization_insights_organization_id
    on aura_core.organization_insights (organization_id);

create index if not exists idx_organization_insights_generated_at
    on aura_core.organization_insights (generated_at);
