package com.malliina.musicpimp.models

import com.malliina.musicpimp.library.FolderInfo

case class ItemCollection(dirs: Seq[FolderInfo], songs: Seq[FolderInfo])
