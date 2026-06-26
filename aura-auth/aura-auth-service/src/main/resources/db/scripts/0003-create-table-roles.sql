create table if not exists roles
(
    id   bigserial   not null,
    name varchar(50) not null,
    code varchar(50) not null,
    constraint pk_roles_id primary key (id),
    constraint uc_roles_code unique (code)
);

insert into roles (name, code)
select 'Пользователь', 'ROLE_USER'
where not exists (
    select 1
    from roles
    where code = 'ROLE_USER'
);

insert into roles (name, code)
select 'Администратор', 'ROLE_ADMIN'
where not exists (
    select 1
    from roles
    where code = 'ROLE_ADMIN'
);
