[![Build Status](https://github.com/malliina/musicpimp/workflows/Test/badge.svg)](https://github.com/malliina/musicpimp/actions)

This is the MusicPimp server software for Windows/Linux desktops. Check [www.musicpimp.org](https://www.musicpimp.org) for details.

# Modules

- [musicpimp](musicpimp) as a standalone app
- [pimpbeam](pimpbeam) deployed to [beam.musicpimp.org](https://beam.musicpimp.org)
- [musicmeta](musicmeta) deployed to [api.musicpimp.org](https://api.musicpimp.org)
- [pimpcloud](pimpcloud) deployed to [cloud.musicpimp.org](https://cloud.musicpimp.org)

## Development

To run tests:

    sbt test

If tests fail on Ubuntu, run

    sudo apt install libncurses5
