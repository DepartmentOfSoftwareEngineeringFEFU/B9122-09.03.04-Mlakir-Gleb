create table if not exists user_roles
(
    id      bigserial not null,
    role_id bigint    not null,
    user_id bigint    not null,
    constraint pk_user_roles_id primary key (id),
    constraint fk_user_roles_role_id foreign key (role_id) references roles (id),
    constraint fk_user_roles_user_id foreign key (user_id) references users (id),
    constraint uc_user_roles_user_id_role_id unique (user_id, role_id)
);
