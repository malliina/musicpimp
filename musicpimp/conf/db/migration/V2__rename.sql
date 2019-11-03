alter table playlist_tracks
    change `INDEX` IDX int not null;
alter table plays
    change `WHEN` STARTED timestamp(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3);
create fulltext index track_search_2 on tracks (TITLE, ARTIST, ALBUM);
