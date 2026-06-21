create table if not exists USERS
(
    USER      varchar(100) not null primary key,
    PASS_HASH text         not null
);

create table if not exists FOLDERS
(
    ID     varchar(191) not null primary key,
    TITLE  text         not null,
    PATH   text         not null,
    PARENT varchar(191) not null,
    constraint FOLDERS_PARENT_FK foreign key (PARENT) references FOLDERS (ID) on update cascade on delete cascade
);

create table if not exists TRACKS
(
    ID       varchar(191)            not null primary key,
    TITLE    text                    not null,
    ARTIST   text                    not null,
    ALBUM    text                    not null,
    DURATION int                     not null,
    SIZE     bigint                  not null,
    PATH     varchar(254) default '' not null,
    FOLDER   varchar(191)            not null,
    constraint TRACKS_FOLDER_FK foreign key (FOLDER) references FOLDERS (ID) on update cascade on delete cascade
);

create table if not exists TOKENS
(
    USER   varchar(100) not null,
    SERIES bigint       not null,
    TOKEN  bigint       not null,
    constraint TOKENS_USER_FK foreign key (USER) references USERS (USER) on update cascade on delete cascade
);

create table if not exists TEMP_TRACKS
(
    ID varchar(191) not null primary key
);

create table if not exists TEMP_FOLDERS
(
    ID varchar(191) not null primary key
);

create table if not exists PLAYS
(
    TRACK  varchar(191)                              not null,
    `WHEN` timestamp(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    WHO    varchar(100)                              not null,
    constraint PLAYS_TRACK_FK foreign key (TRACK) references TRACKS (ID) on update cascade,
    constraint PLAYS_WHO_FK foreign key (WHO) references USERS (USER) on update cascade on delete cascade
);

create table if not exists PLAYLISTS
(
    ID   bigint auto_increment primary key,
    NAME text         not null,
    USER varchar(100) not null,
    constraint PLAYLISTS_USER_FK foreign key (USER) references USERS (USER) on update cascade on delete cascade
);

create table if not exists PLAYLIST_TRACKS
(
    PLAYLIST bigint       not null,
    TRACK    varchar(191) not null,
    `INDEX`  int          not null,
    primary key (PLAYLIST, `INDEX`),
    constraint PLAYLIST_TRACKS_PLAYLIST_FK foreign key (PLAYLIST) references PLAYLISTS (ID) on update cascade on delete cascade,
    constraint PLAYLIST_TRACKS_TRACK_FK foreign key (TRACK) references TRACKS (ID) on update cascade on delete cascade
);
