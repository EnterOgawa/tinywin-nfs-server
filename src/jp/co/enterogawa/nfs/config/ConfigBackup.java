package jp.co.enterogawa.nfs.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

//------------------------------------------------------------------------------
/**
 * 設定バックアップクラスです。<br><br>
 *
 * <p>クラス名称： 設定バックアップ</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class ConfigBackup {
	//	定数定義	------------------------------------------------------------
	/** バックアップフォルダ名 */
	private static final String			BACKUP_DIRECTORY_NAME = "backups" ;

	/** バックアップ保持世代 */
	private static final int				MAX_BACKUP_COUNT = 10 ;

	/** バックアップ時刻フォーマット */
	private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern( "yyyyMMdd-HHmmss-SSS") ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を抑止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private ConfigBackup() {
	}

	//--------------------------------------------------------------------------
	/**
	 * 既存設定をバックアップします。<br><br>
	 *
	 * <p>メソッド名称： 既存設定バックアップ</p>
	 *
	 * @param configPath	設定ファイル
	 * @return バックアップファイル。設定ファイルが存在しない場合は null
	 * @throws IOException バックアップ異常
	 */
	//--------------------------------------------------------------------------
	public static Path backupIfExists(Path configPath) throws IOException {
		Path configDirectory = configPath.toAbsolutePath().normalize().getParent() ;
		return backupIfExists( configPath, configDirectory, "nfs-server") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 指定ファイルをバックアップします。<br><br>
	 *
	 * <p>メソッド名称： 指定ファイルバックアップ</p>
	 *
	 * @param sourcePath		バックアップ元
	 * @param configDirectory	設定ディレクトリ
	 * @param prefix			バックアップ名prefix
	 * @return バックアップファイル。バックアップ元が存在しない場合は null
	 * @throws IOException バックアップ異常
	 */
	//--------------------------------------------------------------------------
	public static Path backupIfExists(Path sourcePath, Path configDirectory, String prefix) throws IOException {
		Path normalizedSource = sourcePath.toAbsolutePath().normalize() ;

		// バックアップ元が存在しない場合
		if( !Files.exists( normalizedSource)) {
			return null ;
		}

		Path backupDirectory = configDirectory.toAbsolutePath().normalize().resolve( BACKUP_DIRECTORY_NAME) ;
		Files.createDirectories( backupDirectory) ;
		Path backupPath = createBackupPath( backupDirectory, prefix) ;
		Files.copy( normalizedSource, backupPath, StandardCopyOption.COPY_ATTRIBUTES) ;
		pruneBackups( backupDirectory, prefix) ;
		return backupPath ;
	}

	//--------------------------------------------------------------------------
	/**
	 * バックアップパスを作成します。<br><br>
	 *
	 * <p>メソッド名称： バックアップパス作成</p>
	 *
	 * @param backupDirectory	バックアップディレクトリ
	 * @param prefix			バックアップ名prefix
	 * @return バックアップパス
	 * @throws IOException バックアップパス作成異常
	 */
	//--------------------------------------------------------------------------
	private static Path createBackupPath(Path backupDirectory, String prefix) throws IOException {
		String timestamp = LocalDateTime.now().format( BACKUP_TIME_FORMAT) ;

		// 同一ミリ秒内の重複を避けるため連番を確認する
		for( int i = 0; i < 100; i++) {
			String suffix = i == 0 ? "" : "-" + i ;
			Path candidate = backupDirectory.resolve( prefix + "-" + timestamp + suffix + ".properties") ;

			// バックアップファイルが未使用の場合
			if( !Files.exists( candidate)) {
				return candidate ;
			}
		}

		return Files.createTempFile( backupDirectory, prefix + "-" + timestamp + "-", ".properties") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * バックアップ世代を整理します。<br><br>
	 *
	 * <p>メソッド名称： バックアップ世代整理</p>
	 *
	 * @param backupDirectory	バックアップディレクトリ
	 * @param prefix			バックアップ名prefix
	 * @throws IOException 整理異常
	 */
	//--------------------------------------------------------------------------
	private static void pruneBackups(Path backupDirectory, String prefix) throws IOException {
		try( Stream<Path> stream = Files.list( backupDirectory)) {
			List<Path> backups = stream
					.filter( path -> path.getFileName().toString().startsWith( prefix + "-" ) )
					.sorted( Comparator.comparing( ConfigBackup::lastModifiedMillis).reversed())
					.toList() ;

			// 保持世代を超えたバックアップを削除する
			for( int i = MAX_BACKUP_COUNT; i < backups.size(); i++) {
				Files.deleteIfExists( backups.get( i)) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 最終更新時刻を取得します。<br><br>
	 *
	 * <p>メソッド名称： 最終更新時刻取得</p>
	 *
	 * @param path	パス
	 * @return 最終更新時刻
	 */
	//--------------------------------------------------------------------------
	private static long lastModifiedMillis(Path path) {
		try {
			return Files.getLastModifiedTime( path).toMillis() ;
		} catch( IOException ioex) {
			return 0L ;
		}
	}
}
