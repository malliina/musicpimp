alter table playlist_tracks
    change `INDEX` idx int not null;
alter table plays
    change `WHEN` started timestamp(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3);
