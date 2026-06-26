create table if not exists tokens
(
    id          bigserial                not null,
    user_id     bigint                   not null,
    access_jti  uuid                     not null,
    refresh_jti uuid                     not null,
    issued_at   timestamp with time zone not null,
    expired_at  timestamp with time zone not null,
    constraint pk_tokens_id primary key (id),
    constraint uc_tokens_access_jti unique (access_jti),
    constraint uc_tokens_refresh_jti unique (refresh_jti),
    constraint fk_tokens_user_id foreign key (user_id) references users (id) on delete cascade
);
