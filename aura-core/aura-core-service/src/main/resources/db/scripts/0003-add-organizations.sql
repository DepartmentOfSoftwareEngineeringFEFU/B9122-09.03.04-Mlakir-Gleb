--liquibase formatted sql

--changeset mlakir:0003-add-organizations
create table if not exists aura_core.organizations
(
    id          bigserial primary key,
    name        varchar(255)  not null,
    short_name  varchar(100)  not null,
    description varchar(2000),
    website     varchar(1000),
    is_active   boolean       not null default true,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint uq_organizations_name unique (name),
    constraint uq_organizations_short_name unique (short_name)
);

alter table aura_core.sources
    add column if not exists organization_id bigint;

insert into aura_core.organizations (name, short_name, description, website, is_active, created_at, updated_at)
select 'Default organization', 'DEFAULT', null, null, true, now(), now()
where exists (select 1 from aura_core.sources where organization_id is null)
  and not exists (select 1 from aura_core.organizations where short_name = 'DEFAULT');

update aura_core.sources
set organization_id = (select id from aura_core.organizations where short_name = 'DEFAULT')
where organization_id is null;

alter table aura_core.sources
    alter column organization_id set not null;

alter table aura_core.sources
    drop constraint if exists fk_sources_organization;

alter table aura_core.sources
    add constraint fk_sources_organization
        foreign key (organization_id) references aura_core.organizations (id);

alter table aura_core.sources
    drop constraint if exists uq_sources_name_type;

alter table aura_core.sources
    add constraint uq_sources_organization_name_type unique (organization_id, name, type);

create index if not exists idx_sources_organization_id on aura_core.sources (organization_id);
