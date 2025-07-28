create table mod
(
    mod_id                 TEXT not null
        primary key,
    friendly_name          TEXT not null,
    published_service_name TEXT not null,
    description            text,
    download_status        text
)
    strict;

create table mod_category
(
    mod_id   text not null
        constraint mod_category_mod_mod_id_fk
            references mod
            on delete cascade,
    category text not null,
    constraint mod_category_pk
        primary key (mod_id, category)
)
    strict;

create table mod_list_profile
(
    mod_list_profile_id     text not null
        constraint mod_list_profile_pk
            primary key,
    profile_name            text not null,
    space_engineers_version text not null
)
    strict;

create index mod_list_profile_profile_name
    on mod_list_profile (profile_name);

create table mod_list_profile_mod
(
    mod_list_profile_id text                  not null
        constraint mod_list_profile_mod_mod_list_profile_mod_list_profile_id_fk
            references mod_list_profile
            on delete cascade,
    mod_id              text                  not null
        constraint mod_list_profile_mod_mod_mod_id_fk
            references mod
            on delete cascade,
    load_priority       integer               not null,
    active              integer default false not null,
    constraint mod_list_profile_mod_pk
        primary key (mod_id, mod_list_profile_id)
)
    strict;

create index mod_list_profile_mod_mod_id_index
    on mod_list_profile_mod (mod_id);

create index mod_list_profile_mod_mod_list_profile_id_index
    on mod_list_profile_mod (mod_list_profile_id);

create table mod_modified_path
(
    mod_id        text not null
        constraint mod_modified_path_mod_mod_id_fk
            references mod
            on delete cascade,
    modified_path text not null,
    constraint mod_modified_path_pk
        primary key (mod_id, modified_path)
)
    strict;

create index mod_modified_path_mod_id_index
    on mod_modified_path (mod_id);

create table modio_mod
(
    mod_id                 text not null
        constraint modio_mod_pk
            primary key
        constraint modio_mod_mod_mod_id_fk
            references mod
            on delete cascade,
    last_updated_year      text,
    last_updated_month_day text,
    last_updated_hour      text
)
    strict;

create table save_profile
(
    save_profile_id               text                  not null
        constraint save_profile_pk
            primary key,
    profile_name                  text                  not null,
    save_name                     text                  not null,
    save_path                     text,
    last_used_mod_list_profile_id text
        constraint save_profile_mod_list_profile_mod_list_profile_id_fk
            references mod_list_profile
            on delete set null,
    last_save_status              text                  not null,
    last_saved                    text,
    save_exists                   integer default false not null,
    space_engineers_version       text                  not null,
    save_type       text                  not null
)
    strict;

create table steam_mod
(
    mod_id       text not null
        constraint steam_mod_pk
            primary key
        constraint steam_mod_mod_mod_id_fk
            references mod
            on delete cascade,
    last_updated text
)
    strict;

create table user_configuration
(
    id                            integer
        primary key autoincrement,
    user_theme                    TEXT    default 'PrimerLight' not null,
    last_modified_save_profile_id text
        constraint user_configuration_last_modified_save_save_profile_save_profile_id_fk
            references save_profile
            on delete set null,
    last_active_mod_profile_id    text
        constraint user_configuration_mod_list_profile_mod_list_profile_id_fk
            references mod_list_profile
            on delete set null,
    last_active_save_profile_id   text
        constraint user_configuration_last_active_save_save_profile_save_profile_id_fk
            references save_profile
            on delete set null,
    run_first_time_setup          INTEGER default false         not null
)
    strict;

create index user_configuration_id_index
    on user_configuration (id);

create index user_configuration_last_active_mod_profile_id_index
    on user_configuration (last_active_mod_profile_id);

create index user_configuration_last_active_save_profile_id_index
    on user_configuration (last_active_save_profile_id);

create index user_configuration_last_modified_save_profile_id_index
    on user_configuration (last_modified_save_profile_id);

create table user_configuration_mod_list_profile
(
    user_configuration_id integer not null
        constraint user_configuration_mod_list_profile_user_configuration_id_fk
            references user_configuration
            on delete cascade,
    mod_list_profile_id   text    not null
        constraint user_configuration_mod_list_profile_mod_list_profile_mod_list_profile_id_fk
            references mod_list_profile
            on delete cascade,
    constraint user_configuration_mod_list_profile_pk
        primary key (mod_list_profile_id, user_configuration_id)
)
    strict;

create index user_configuration_mod_list_profile_mod_list_profile_id_index
    on user_configuration_mod_list_profile (mod_list_profile_id);

create index user_configuration_mod_list_profile_user_configuration_id_index
    on user_configuration_mod_list_profile (user_configuration_id);

create index user_configuration_mod_list_profile_user_configuration_id_mod_list_profile_id_index
    on user_configuration_mod_list_profile (user_configuration_id, mod_list_profile_id);

create table user_configuration_save_profile
(
    user_configuration_id integer not null
        constraint user_configuration_save_profile_user_configuration_id_fk
            references user_configuration
            on delete cascade,
    save_profile_id       text    not null
        constraint user_configuration_save_profile_save_profile_save_profile_id_fk
            references save_profile
            on delete cascade,
    constraint user_configuration_save_profile_pk
        primary key (user_configuration_id, save_profile_id)
)
    strict;

create index user_configuration_save_profile_save_profile_id_index
    on user_configuration_save_profile (save_profile_id);

create index user_configuration_save_profile_user_configuration_id_index
    on user_configuration_save_profile (user_configuration_id);

create index user_configuration_save_profile_user_configuration_id_save_profile_id_index
    on user_configuration_save_profile (user_configuration_id, save_profile_id);

-- Create a trigger to delete mods no longer used in any mod list
CREATE TRIGGER delete_orphan_mods
    AFTER DELETE
    ON mod_list_profile_mod
    FOR EACH ROW
    WHEN NOT EXISTS (SELECT 1
                     FROM mod_list_profile_mod
                     WHERE mod_id = OLD.mod_id)
BEGIN
    DELETE FROM mod WHERE mod_id = OLD.mod_id;
END;

-- Prevent a mod from being a modio_mod if it's already a steam_mod
CREATE TRIGGER enforce_modio_exclusivity
    BEFORE INSERT
    ON modio_mod
    FOR EACH ROW
    WHEN EXISTS (SELECT 1
                 FROM steam_mod
                 WHERE mod_id = NEW.mod_id)
BEGIN
    SELECT RAISE(ABORT, 'Mod cannot be both modio and steam');
END;

-- Prevent a mod from being a steam_mod if it's already a modio_mod
CREATE TRIGGER enforce_steam_exclusivity
    BEFORE INSERT
    ON steam_mod
    FOR EACH ROW
    WHEN EXISTS (SELECT 1
                 FROM modio_mod
                 WHERE mod_id = NEW.mod_id)
BEGIN
    SELECT RAISE(ABORT, 'Mod cannot be both modio and steam');
END;