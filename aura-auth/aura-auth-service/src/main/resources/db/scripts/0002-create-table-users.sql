create table if not exists users
(
    id    bigserial    not null,
    login varchar(255) not null,
    constraint pk_users_id primary key (id),
    constraint uc_users_login unique (login)
);
