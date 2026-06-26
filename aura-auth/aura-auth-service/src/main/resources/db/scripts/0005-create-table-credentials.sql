create table if not exists credentials
(
    id      bigserial    not null,
    user_id bigint       not null,
    salt    varchar(255) not null,
    hash    varchar(255) not null,
    constraint pk_credentials_id primary key (id),
    constraint fk_credentials_user_id foreign key (user_id) references users (id) on delete cascade,
    constraint uc_credentials_user_id unique (user_id)
);