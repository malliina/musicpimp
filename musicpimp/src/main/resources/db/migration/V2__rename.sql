alter table PLAYLIST_TRACKS
    change `INDEX` IDX int not null;
alter table PLAYS
    change `WHEN` STARTED timestamp(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3);
create fulltext index track_search_2 on TRACKS (TITLE, ARTIST, ALBUM);
