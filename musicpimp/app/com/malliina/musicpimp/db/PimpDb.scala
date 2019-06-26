package com.malliina.musicpimp.db

import java.nio.file.{Files, Path, Paths}

import com.malliina.concurrent.ExecutionContexts
import com.malliina.file.StorageFile
import com.malliina.musicpimp.app.PimpConf
import com.malliina.musicpimp.audio.PimpEnc
import com.malliina.musicpimp.db.PimpDb.{GetDummy, log}
import com.malliina.musicpimp.library.{FileStreams, Library}
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.{ErrorMessage, UnixPath}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import org.h2.jdbcx.JdbcConnectionPool
import play.api.Logger
import slick.jdbc._
import slick.util.AsyncExecutor

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.higherKinds

object PimpDb {
  private val log = Logger(getClass)
  val H2UrlSettings = "h2.url.settings"
  val H2Home = "h2.home"
  val maxConn = 20

  def default(ec: ExecutionContext) = {
    DatabaseConf.prod().toOption.map(conf => mysql(conf, ec)).getOrElse(defaultH2(ec))
  }

  def defaultH2(ec: ExecutionContext) = {
    val dirByConf: Option[Path] = sys.props.get(H2Home).map(p => Paths.get(p))
    val dataHome: Path = dirByConf getOrElse (FileUtil.pimpHomeDir / "db")
    file(dataHome / "pimp291", ec)
  }

  /**
    * @param path path to database file
    * @return a file-based database stored at `path`
    */
  def file(path: Path, ec: ExecutionContext) = {
    Option(path.getParent).foreach(p => Files.createDirectories(p))
    h2(path.toString, ec)
  }

  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  def test()(implicit ec: ExecutionContext) = h2("mem:test", ec)

  def h2(conn: String, ec: ExecutionContext) = {
    val databaseUrlSettings = sys.props
      .get(H2UrlSettings)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(ss => s";$ss")
      .getOrElse("")
    val url = s"jdbc:h2:$conn;DB_CLOSE_DELAY=-1$databaseUrlSettings"
    log info s"Connecting to: $url"
    val pool = JdbcConnectionPool.create(url, "", "")
    apply(H2Profile, pool, ec)
  }

  def mysql(conf: DatabaseConf): PimpDb = mysql(conf, ExecutionContexts.cached)

  def mysql(conf: DatabaseConf, ec: ExecutionContext): PimpDb = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(conf.url)
    hikariConfig.setUsername(conf.user)
    hikariConfig.setPassword(conf.pass)
    hikariConfig.setDriverClassName(conf.driver)
    log.info(s"Connecting to '${conf.url}'...")
    apply(MySQLProfile, new HikariDataSource(hikariConfig), ec)
  }

  def executor(threads: Int) = AsyncExecutor(
    name = "AsyncExecutor.musicpimp",
    minThreads = threads,
    maxThreads = threads,
    queueSize = 2000,
    maxConnections = threads
  )

  def apply(profile: JdbcProfile, ds: DataSource, ec: ExecutionContext): PimpDb =
    new PimpDb(profile, profile.api.Database.forDataSource(ds, Option(maxConn), executor(maxConn)))(
      ec)

  case class DatabaseConf(url: String, user: String, pass: String, driver: String)

  object DatabaseConf {
    val H2Driver = "org.h2.Driver"
    val MySQLDriver = "com.mysql.jdbc.Driver"

    def read(key: String) = PimpConf.read(key)

    def prod(): Either[ErrorMessage, DatabaseConf] = for {
      url <- read("db_url")
      user <- read("db_user")
      pass <- read("db_pass")
    } yield apply(url, user, pass, read("db_driver").getOrElse(MySQLDriver))
  }

  object GetDummy extends GetResult[Int] {
    override def apply(v1: PositionedResult) = 0
  }
}

class PimpDb(val p: JdbcProfile, val database: JdbcProfile#Backend#Database)(
    implicit val ec: ExecutionContext)
    extends DatabaseLike(p)
    with AutoCloseable {

  val schema = PimpSchema(profile)
  val api = schema.api

  import schema._
  import api._

  val tracksName = schema.tracks.baseTableRow.tableName
  val tableQueries = schema.tableQueries

  def init(): Unit = {
    log info s"Ensuring all tables exist..."
    createIfNotExists(tableQueries: _*)
  }

  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*): Unit =
    tables.reverse.filter(t => !exists(t)).foreach(t => initTable(t))

  def fullText(searchTerm: String,
               limit: Int = 1000,
               tableName: String = tracksName): Future[Seq[DataTrack]] = {
    if (p == MySQLProfile) fullTextMySQL(searchTerm, limit, tableName)
    else fullTextH2(searchTerm, limit, tableName)
  }

  def fullTextH2(searchTerm: String,
                 limit: Int = 1000,
                 tableName: String = tracksName): Future[Seq[DataTrack]] = {
    log info s"Querying: $searchTerm"
    //    val conn = database.source.createConnection()
    //    try {
    //      val rs = FullText.searchData(conn, searchTerm, 0, 0)
    //      val columnCount = rs.getMetaData.getColumnCount
    //      log.info(s"Got $columnCount columns")
    //    } finally conn.close()
    val action =
      sql"""SELECT T.* FROM FT_SEARCH_DATA($searchTerm,0,0) FT, #$tableName T WHERE FT.TABLE='#$tableName' AND T.ID=FT.KEYS[0] LIMIT $limit;"""
        .as[DataTrack]
    run(action)
  }

  def fullTextMySQL(searchTerm: String,
                    limit: Int = 1000,
                    tableName: String = tracksName): Future[Seq[DataTrack]] = {
    log debug s"Querying: $searchTerm"
    val words = searchTerm.split(" ")
    val commaSeparated = words.mkString(",")
    val exactAction =
      sql"""SELECT T.* FROM #$tableName T WHERE MATCH(title, artist, album) AGAINST($commaSeparated) LIMIT #$limit;"""
        .as[DataTrack]
    run(exactAction)
  }

  /** Fails if the index is already created or if the table does not exist.
    *
    * TODO check when this throws and whether I'm calling it correctly
    */
  def initIndexH2(tableName: String): Future[Unit] = {
    val clazz = "\"org.h2.fulltext.FullText.init\""
    for {
      _ <- executePlain(sqlu"CREATE ALIAS IF NOT EXISTS FT_INIT FOR #$clazz;")
      _ <- database.run(sql"CALL FT_INIT();".as[Int](GetDummy))
      _ <- database.run(sql"CALL FT_CREATE_INDEX('PUBLIC', '#$tableName', NULL);".as[Int](GetDummy))
    } yield {
      log info s"Initialized index for '$tableName'."
    }
  }

  def initIndexMySQL(tableName: String): Future[Unit] = {
    for {
      _ <- executePlain(sqlu"create fulltext index track_search on TRACKS(title, artist, album);")
    } yield {
      log info s"Initialized index for table '$tableName'."
    }
  }

  def recreateIndex() = database.run(sql"SELECT FT_REINDEX()".as[Int](GetDummy)).map { _ =>
    log.info(s"Recreated index.")
  }

  def dropIndex(tableName: String): Future[Unit] = {
    val q = sql"CALL FT_DROP_INDEX('PUBLIC','#${tableName}')".as[Int](GetDummy)
    database
      .run(q)
      .map { _ =>
        log.info(s"Dropped index for '$tableName'.")
      }
      .recover {
        case e: Exception =>
          log.error(s"Unable to drop index for '$tableName'.", e)
      }
  }

  def folder(id: FolderID): Future[(Seq[DataTrack], Seq[DataFolder])] = {
    val tracksQuery = tracks.join(foldersFor(id)).on(_.folder === _.id).map(_._1)
    // '=!=' in slick-lang is the same as '!='
    val foldersQuery = folders
      .filter(_.id =!= Library.RootId)
      .join(foldersFor(id))
      .on(_.parent === _.id)
      .map(_._1)
      .sortBy(_.title)
//    val foldersQuery = folders.filter(f => f.parent === id && f.id =!= Library.RootId).sortBy(_.title)
    //    println(tracksQuery.selectStatement + "\n" + foldersQuery.selectStatement)
    val action = for {
      ts <- tracksQuery.result
      fs <- foldersQuery.result
    } yield (ts, fs)
    run(action)
  }

  def folderOnly(id: FolderID): Future[Option[DataFolder]] = {
    run(foldersFor(id).result.headOption)
  }

  private def foldersFor(id: FolderID) =
    folders.filter(folder =>
      folder.id === id || folder.path === UnixPath.fromRaw(PimpEnc.decodeId(id)))

  def trackFor(id: TrackID): Future[Option[DataTrack]] =
    tracksFor(Seq(id)).map(_.headOption)

  // the path predicate is legacy
  def tracksFor(ids: Seq[TrackID]): Future[Seq[DataTrack]] = {
    val action = DBIO
      .sequence(
        ids.map { id =>
          tracks
            .filter(t => t.id === id || t.path === UnixPath.fromRaw(PimpEnc.decodeId(id)))
            .result
        }
      )
      .map(_.flatten)
    run(action)
  }

  def insertFolders(fs: Seq[DataFolder]) = run(folders ++= fs)

  def insertTracks(ts: Seq[DataTrack]) = run(tracks ++= ts)

  def initTable[T <: Table[_]](table: TableQuery[T]): Unit = {
    val name = table.baseTableRow.tableName
    log.info(s"Creating table: $name")
    await(database.run(table.schema.create))
    log.info(s"Created table: $name")
    if (name == tracksName) {
      await {
        initIndex(tracksName) recover {
          case t: Throwable =>
            log.warn(s"Initialization of index of table $tracksName failed", t)
            throw t
        }
      }
    }
  }

  def initIndex(tableName: String): Future[Unit] =
    if (p == MySQLProfile) initIndexMySQL(tableName)
    else initIndexH2(tableName)

  def dropAll() = {
    tableQueries foreach { t =>
      if (exists(t)) {
        await(executePlain(sqlu"DROP TABLE ${t.baseTableRow.tableName};"))
        // ddl.drop fails if the table has a constraint to something nonexistent
        //        t.ddl.drop
        log info s"Dropped table: ${t.baseTableRow.tableName}"
      }
    }
    dropIndex(tracksName)
  }

  def runIndexer(library: FileStreams)(
      onFileCountUpdate: Long => Future[Unit]): Future[IndexResult] = {
    log info "Indexing..."
    // deletes any old rows from previous indexings
    val firstIdsDeletion = tempFoldersTable.delete
    val musicFolders = library.folderStream
    // upserts every folder in the library to the database
    val updateActions = musicFolders.map(folder => folders.insertOrUpdate(folder))
    val folderUpdates = DBIO.sequence(updateActions)
    // adds every existing folder to the temp table
    val idInsertion = tempFoldersTable ++= musicFolders.map(f => TempFolder(f.id))
    // deletes non-existing folders
    val foldersDeletion = folders.filterNot(f => f.id.in(tempFoldersTable.map(i => i.id))).delete
    val secondIdsDeletion = tempFoldersTable.delete
    val foldersInit = DBIO.seq(
      firstIdsDeletion,
      folderUpdates,
      idInsertion
    )

    def updateFolders() = for {
      _ <- run(foldersInit)
      foldersDeleted <- run(foldersDeletion)
      _ <- run(secondIdsDeletion)
    } yield foldersDeleted

    // repeat above, but for tracks
    val oldTrackDeletion = tempTracksTable.delete
    var fileCount = 0L

    def upsertAllTracks(): Unit = {
      library.dataTrackStream.grouped(100).foreach { chunk =>
        val trackUpdates = DBIO.sequence(chunk.map(track => tracks.insertOrUpdate(track)))
        val chunkInsertion = run(
          DBIO.seq(
            trackUpdates,
            tempTracksTable ++= chunk.map(t => TempTrack(t.id))
          ))
        await(chunkInsertion, 1.hour)
        fileCount += chunk.size
        onFileCountUpdate(fileCount)
      }
    }

    val tracksDeletion = tracks.filterNot(t => t.id.in(tempTracksTable.map(_.id))).delete
    val thirdIdsDeletion = tempFoldersTable.delete

    def deleteNonExistentTracks() = for {
      tracksDeleted <- run(tracksDeletion)
      _ <- run(thirdIdsDeletion)
    } yield tracksDeleted

    def updateTracks() = for {
      _ <- run(oldTrackDeletion)
      _ = upsertAllTracks()
      ts <- deleteNonExistentTracks()
    } yield ts

    for {
      fs <- updateFolders()
      ts <- updateTracks()
    } yield IndexResult(fileCount, fs, ts)
  }

  def runQuery[A, B, C[_]](query: Query[A, B, C]): Future[C[B]] = run(query.result)

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = database.run(a)

  def await[T](f: Future[T], dur: FiniteDuration = 5.seconds): T =
    Await.result(f, dur)

  def close(): Unit = database.close()
}

case class IndexResult(totalFiles: Long, foldersPurged: Int, tracksPurged: Int)
