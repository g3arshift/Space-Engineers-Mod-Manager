create table main.mod
(
    mod_id                 TEXT                  not null
        primary key,
    friendly_name          TEXT                  not null,
    published_service_name TEXT                  not null,
    active                 integer default false not null,
    description            text
)
    strict;

create table main.mod_category
(
    mod_id   text not null
        constraint mod_category_mod_mod_id_fk
            references main.mod
            on delete cascade,
    category text not null,
    constraint mod_category_pk
        primary key (mod_id, category)
)
    strict;

create table main.mod_list_profile
(
    mod_list_profile_id     text not null
        constraint mod_list_profile_pk
            primary key,
    profile_name            text not null,
    space_engineers_version text not null
)
    strict;

create table main.mod_list_profile_mod
(
    mod_list_profile_id text    not null
        constraint mod_list_profile_mod_mod_list_profile_mod_list_profile_id_fk
            references main.mod_list_profile
            on delete cascade,
    mod_id              text    not null
        constraint mod_list_profile_mod_mod_mod_id_fk
            references main.mod
            on delete cascade,
    load_priority       integer not null,
    constraint mod_list_profile_mod_pk
        primary key (mod_id, mod_list_profile_id)
)
    strict;

create index main.mod_list_profile_mod_mod_id_index
    on main.mod_list_profile_mod (mod_id);

create index main.mod_list_profile_mod_mod_list_profile_id_index
    on main.mod_list_profile_mod (mod_list_profile_id);

create table main.mod_modified_path
(
    mod_id        text not null
        constraint mod_modified_path_mod_mod_id_fk
            references main.mod
            on delete cascade,
    modified_path text not null,
    constraint mod_modified_path_pk
        primary key (mod_id, modified_path)
)
    strict;

create index main.mod_modified_path_mod_id_index
    on main.mod_modified_path (mod_id);

create table main.modio_mod
(
    mod_id                 text not null
        constraint modio_mod_pk
            primary key
        constraint modio_mod_mod_mod_id_fk
            references main.mod
            on delete cascade,
    last_updated_year      integer,
    last_updated_month_day text,
    last_updated_hour      text
)
    strict;

create table main.save_profile
(
    save_profile_id               text                  not null
        constraint save_profile_pk
            primary key,
    profile_name                  text                  not null,
    save_name                     text                  not null,
    save_path                     text                  not null,
    last_used_mod_list_profile_id text
        constraint save_profile_mod_list_profile_mod_list_profile_id_fk
            references main.mod_list_profile
            on delete set null,
    last_save_status              text                  not null,
    last_saved                    text,
    save_exists                   integer default false not null,
    space_engineers_version       text                  not null
)
    strict;

create table main.sqlite_master
(
    type     TEXT,
    name     TEXT,
    tbl_name TEXT,
    rootpage INT,
    sql      TEXT
);

create table main.sqlite_sequence
(
    name,
    seq
);

create table main.steam_mod
(
    mod_id       text not null
        constraint steam_mod_pk
            primary key
        constraint steam_mod_mod_mod_id_fk
            references main.mod
            on delete cascade,
    last_updated text
)
    strict;

create table main.user_configuration
(
    id                            integer
        primary key autoincrement,
    user_theme                    TEXT    default 'PrimerLight' not null,
    last_modified_save_profile_id text
        constraint user_configuration_last_modified_save_save_profile_save_profile_id_fk
            references main.save_profile
            on delete set null,
    last_active_mod_profile_id    text
        constraint user_configuration_mod_list_profile_mod_list_profile_id_fk
            references main.mod_list_profile
            on delete set null,
    last_active_save_profile_id   text
        constraint user_configuration_last_active_save_save_profile_save_profile_id_fk
            references main.save_profile
            on delete set null,
    run_first_time_setup          INTEGER default false         not null
)
    strict;

create index main.user_configuration_id_index
    on main.user_configuration (id);

create index main.user_configuration_last_active_mod_profile_id_index
    on main.user_configuration (last_active_mod_profile_id);

create index main.user_configuration_last_active_save_profile_id_index
    on main.user_configuration (last_active_save_profile_id);

create index main.user_configuration_last_modified_save_profile_id_index
    on main.user_configuration (last_modified_save_profile_id);

create table main.user_configuration_mod_list_profile
(
    user_configuration_id integer not null
        constraint user_configuration_mod_list_profile_user_configuration_id_fk
            references main.user_configuration
            on delete cascade,
    mod_list_profile_id   text    not null
        constraint user_configuration_mod_list_profile_mod_list_profile_mod_list_profile_id_fk
            references main.mod_list_profile
            on delete cascade,
    constraint user_configuration_mod_list_profile_pk
        primary key (mod_list_profile_id, user_configuration_id)
)
    strict;

create index main.user_configuration_mod_list_profile_mod_list_profile_id_index
    on main.user_configuration_mod_list_profile (mod_list_profile_id);

create index main.user_configuration_mod_list_profile_user_configuration_id_index
    on main.user_configuration_mod_list_profile (user_configuration_id);

create index main.user_configuration_mod_list_profile_user_configuration_id_mod_list_profile_id_index
    on main.user_configuration_mod_list_profile (user_configuration_id, mod_list_profile_id);

create table main.user_configuration_save_profile
(
    user_configuration_id integer not null
        constraint user_configuration_save_profile_user_configuration_id_fk
            references main.user_configuration
            on delete cascade,
    save_profile_id       text    not null
        constraint user_configuration_save_profile_save_profile_save_profile_id_fk
            references main.save_profile
            on delete cascade,
    constraint user_configuration_save_profile_pk
        primary key (user_configuration_id, save_profile_id)
)
    strict;

create index main.user_configuration_save_profile_save_profile_id_index
    on main.user_configuration_save_profile (save_profile_id);

create index main.user_configuration_save_profile_user_configuration_id_index
    on main.user_configuration_save_profile (user_configuration_id);

create index main.user_configuration_save_profile_user_configuration_id_save_profile_id_index
    on main.user_configuration_save_profile (user_configuration_id, save_profile_id);

