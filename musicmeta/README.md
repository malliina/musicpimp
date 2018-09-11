# musicmeta

This is an album cover HTTP service.

## API

To get an album cover image, send an HTTP request to

    /covers?artist=$artist&album=$album
    
If the cover is found, a 200 OK is returned along with the cover file. If the cover cannot be found, a 404 NOT FOUND is
returned. If the request is erroneous, a 400 BAD REQUEST is returned.

## Implementation

DiscoGs is used as the cover backend. Covers are cached on the local filesystem in a directory specified by system 
property ```cover.dir```. DiscoGs requires OAuth authentication, so you need to specify OAuth credentials in a file 
specified by system property ```discogs.oauth```.
