package jp.co.enterogawa.nfs.diagnostic;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jp.co.enterogawa.nfs.config.NfsExport;
import jp.co.enterogawa.nfs.config.NfsServerConfig;

//------------------------------------------------------------------------------
/**
 * NFS運用診断クラスです。<br><br>
 *
 * <p>クラス名称： NFS運用診断</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class NfsDiagnostics {
	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を抑止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private NfsDiagnostics() {
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断レポートを作成します。<br><br>
	 *
	 * <p>メソッド名称： 診断レポート作成</p>
	 *
	 * @param config	設定
	 * @return 診断レポート
	 */
	//--------------------------------------------------------------------------
	public static DiagnosticReport collect(NfsServerConfig config) {
		List<DiagnosticMessage> configurationMessages = inspectConfiguration( config) ;
		List<ExportReport> exportReports = new ArrayList<ExportReport>() ;

		// 公開定義を診断する
		for( NfsExport export : config.getExports()) {
			exportReports.add( inspectExport( export)) ;
		}

		return new DiagnosticReport( configurationMessages, exportReports) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定を診断します。<br><br>
	 *
	 * <p>メソッド名称： 設定診断</p>
	 *
	 * @param config	設定
	 * @return 診断メッセージ
	 */
	//--------------------------------------------------------------------------
	public static List<DiagnosticMessage> inspectConfiguration(NfsServerConfig config) {
		List<DiagnosticMessage> messages = new ArrayList<DiagnosticMessage>() ;
		int portmapPort = config.getPortmapPort() ;
		int nfsPort = config.getNfsPort() ;
		int mountPort = config.getMountPort() ;

		// ポート番号が重複している場合
		if( portmapPort == nfsPort || portmapPort == mountPort || nfsPort == mountPort) {
			messages.add( DiagnosticMessage.warning( "CONFIG_PORT_DUPLICATED", "Portmap, NFS, and MOUNT ports should be different.")) ;
		}

		// 管理者権限が必要な既定ポートを利用する場合
		if( portmapPort <= 1024 || nfsPort <= 1024 || mountPort <= 1024) {
			messages.add( DiagnosticMessage.info( "CONFIG_PRIVILEGED_PORT", "One or more service ports require Administrator privileges on Windows.")) ;
		}

		// 書込サイズが読込サイズと異なる場合
		if( config.getWriteSize() != config.getReadSize()) {
			messages.add( DiagnosticMessage.info( "CONFIG_TRANSFER_SIZE_DIFFERENT", "read.size and write.size are different.")) ;
		}

		// UDPで扱いづらい転送サイズの場合
		if( config.getReadSize() > 65536 || config.getWriteSize() > 65536) {
			messages.add( DiagnosticMessage.warning( "CONFIG_TRANSFER_SIZE_LARGE", "Large read/write sizes may not be suitable for UDP clients.")) ;
		}

		// ファイル名文字コードがUTF-8以外の場合
		if( !Charset.forName( "UTF-8").equals( config.getFilenameCharset())) {
			messages.add( DiagnosticMessage.info( "CONFIG_FILENAME_CHARSET", "filename.charset is not UTF-8. Confirm that all clients use the same encoding.")) ;
		}

		// 許可クライアントが未設定の公開を確認する
		for( NfsExport export : config.getExports()) {
			// すべてのクライアントを許可する場合
			if( export.getAllowedClients().isEmpty()) {
				messages.add( DiagnosticMessage.info( "CONFIG_ALLOWED_CLIENTS_ANY", export.getName() + " allows all client addresses.")) ;
			}
		}

		return List.copyOf( messages) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 大文字小文字を無視した衝突を検出します。<br><br>
	 *
	 * <p>メソッド名称： 大文字小文字衝突検出</p>
	 *
	 * @param relativePaths	相対パス
	 * @return 衝突一覧
	 */
	//--------------------------------------------------------------------------
	public static List<CaseCollision> detectCaseCollisions(List<String> relativePaths) {
		Map<String, List<String>> groups = new TreeMap<String, List<String>>() ;

		// 相対パスを大文字小文字非区別キーへ分類する
		for( String relativePath : relativePaths) {
			String normalizedPath = normalizeRelativePath( relativePath) ;
			String key = normalizedPath.toLowerCase( Locale.ROOT) ;
			groups.computeIfAbsent( key, ignored -> new ArrayList<String>()).add( normalizedPath) ;
		}

		List<CaseCollision> collisions = new ArrayList<CaseCollision>() ;

		// 分類結果から衝突を抽出する
		for( Map.Entry<String, List<String>> entry : groups.entrySet()) {
			LinkedHashSet<String> distinctPaths = new LinkedHashSet<String>( entry.getValue()) ;

			// 同一キーに複数表記が存在する場合
			if( distinctPaths.size() > 1) {
				collisions.add( new CaseCollision( entry.getKey(), new ArrayList<String>( distinctPaths)) ) ;
			}
		}

		return List.copyOf( collisions) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開定義を診断します。<br><br>
	 *
	 * <p>メソッド名称： 公開定義診断</p>
	 *
	 * @param export	公開定義
	 * @return 公開診断
	 */
	//--------------------------------------------------------------------------
	private static ExportReport inspectExport(NfsExport export) {
		Path path = export.getPath() ;
		List<DiagnosticMessage> messages = new ArrayList<DiagnosticMessage>() ;
		List<String> relativePaths = new ArrayList<String>() ;
		long fileCount = 0L ;
		long directoryCount = 0L ;
		long totalBytes = 0L ;
		boolean exists = Files.exists( path, LinkOption.NOFOLLOW_LINKS) ;
		boolean directory = Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS) ;
		boolean readable = Files.isReadable( path) ;
		boolean writable = Files.isWritable( path) ;

		// 公開パスが存在しない場合
		if( !exists) {
			messages.add( DiagnosticMessage.error( "EXPORT_MISSING", "Export path does not exist: " + path)) ;
			return new ExportReport( export, exists, directory, readable, writable, 0L, 0L, 0L, List.of(), messages) ;
		}

		// 公開パスがディレクトリではない場合
		if( !directory) {
			messages.add( DiagnosticMessage.error( "EXPORT_NOT_DIRECTORY", "Export path is not a directory: " + path)) ;
			return new ExportReport( export, exists, directory, readable, writable, 0L, 0L, 0L, List.of(), messages) ;
		}

		// 公開パスが読込不可の場合
		if( !readable) {
			messages.add( DiagnosticMessage.error( "EXPORT_NOT_READABLE", "Export path is not readable: " + path)) ;
		}

		// 書込共有だが公開パスが書込不可の場合
		if( export.isWritable() && !writable) {
			messages.add( DiagnosticMessage.error( "EXPORT_NOT_WRITABLE", "Writable export path is not writable: " + path)) ;
		}

		try( var stream = Files.walk( path)) {
			List<Path> entries = stream.toList() ;

			// 公開配下のファイルとディレクトリを集計する
			for( Path entry : entries) {
				// 公開ルート自身の場合
				if( path.equals( entry)) {
					continue ;
				}

				String relativePath = path.relativize( entry).toString() ;
				relativePaths.add( relativePath) ;

				// ディレクトリの場合
				if( Files.isDirectory( entry, LinkOption.NOFOLLOW_LINKS)) {
					directoryCount++ ;
				}
				// 通常ファイルの場合
				else if( Files.isRegularFile( entry, LinkOption.NOFOLLOW_LINKS)) {
					fileCount++ ;
					totalBytes += readSizeQuietly( entry) ;
				}
			}
		} catch( IOException ioex) {
			messages.add( DiagnosticMessage.warning( "EXPORT_SCAN_FAILED", "Export scan failed: " + ioex.getMessage())) ;
		}

		List<CaseCollision> collisions = detectCaseCollisions( relativePaths) ;

		// 大文字小文字衝突が存在する場合
		if( !collisions.isEmpty()) {
			messages.add( DiagnosticMessage.warning( "EXPORT_CASE_COLLISION", "Case-insensitive path collisions were found: " + collisions.size())) ;
		}

		return new ExportReport( export, exists, directory, readable, writable, fileCount, directoryCount, totalBytes, collisions, messages) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルサイズを取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイルサイズ取得</p>
	 *
	 * @param path	パス
	 * @return ファイルサイズ
	 */
	//--------------------------------------------------------------------------
	private static long readSizeQuietly(Path path) {
		try {
			return Files.size( path) ;
		} catch( IOException ioex) {
			return 0L ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 相対パス表記を正規化します。<br><br>
	 *
	 * <p>メソッド名称： 相対パス表記正規化</p>
	 *
	 * @param value	値
	 * @return 正規化値
	 */
	//--------------------------------------------------------------------------
	private static String normalizeRelativePath(String value) {
		return value.replace( '\\', '/' ) ;
	}

	//------------------------------------------------------------------------------
	/**
	 * 診断レポートクラスです。<br><br>
	 *
	 * <p>クラス名称： 診断レポート</p>
	 */
	//------------------------------------------------------------------------------
	public static class DiagnosticReport {
		/** 設定メッセージ */
		private final List<DiagnosticMessage>	configurationMessages ;

		/** 公開診断 */
		private final List<ExportReport>			exportReports ;

		//--------------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param configurationMessages	設定メッセージ
		 * @param exportReports			公開診断
		 */
		//--------------------------------------------------------------------------
		private DiagnosticReport(List<DiagnosticMessage> configurationMessages, List<ExportReport> exportReports) {
			this.configurationMessages = List.copyOf( configurationMessages) ;
			this.exportReports = List.copyOf( exportReports) ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 設定メッセージを取得します。<br><br>
		 *
		 * <p>メソッド名称： 設定メッセージ取得</p>
		 *
		 * @return 設定メッセージ
		 */
		//--------------------------------------------------------------------------
		public List<DiagnosticMessage> getConfigurationMessages() {
			return configurationMessages ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 公開診断を取得します。<br><br>
		 *
		 * <p>メソッド名称： 公開診断取得</p>
		 *
		 * @return 公開診断
		 */
		//--------------------------------------------------------------------------
		public List<ExportReport> getExportReports() {
			return exportReports ;
		}

		//--------------------------------------------------------------------------
		/**
		 * テキストへ整形します。<br><br>
		 *
		 * <p>メソッド名称： テキスト整形</p>
		 *
		 * @return テキスト
		 */
		//--------------------------------------------------------------------------
		public String formatText() {
			StringBuilder builder = new StringBuilder() ;
			builder.append( "Configuration diagnostics" ).append( System.lineSeparator()) ;
			appendMessages( builder, configurationMessages) ;
			builder.append( System.lineSeparator()) ;
			builder.append( "Export diagnostics" ).append( System.lineSeparator()) ;

			// 公開診断を整形する
			for( ExportReport report : exportReports) {
				builder.append( report.formatText()).append( System.lineSeparator()) ;
			}

			return builder.toString() ;
		}

		//--------------------------------------------------------------------------
		/**
		 * メッセージを追記します。<br><br>
		 *
		 * <p>メソッド名称： メッセージ追記</p>
		 *
		 * @param builder	出力先
		 * @param messages	メッセージ
		 */
		//--------------------------------------------------------------------------
		private void appendMessages(StringBuilder builder, List<DiagnosticMessage> messages) {
			// メッセージが存在しない場合
			if( messages.isEmpty()) {
				builder.append( "- OK" ).append( System.lineSeparator()) ;
				return ;
			}

			// メッセージを出力する
			for( DiagnosticMessage message : messages) {
				builder.append( "- " ).append( message.formatText()).append( System.lineSeparator()) ;
			}
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 公開診断クラスです。<br><br>
	 *
	 * <p>クラス名称： 公開診断</p>
	 */
	//------------------------------------------------------------------------------
	public static class ExportReport {
		/** 公開定義 */
		private final NfsExport					export ;

		/** 存在有無 */
		private final boolean					exists ;

		/** ディレクトリ有無 */
		private final boolean					directory ;

		/** 読込可否 */
		private final boolean					readable ;

		/** 書込可否 */
		private final boolean					writable ;

		/** ファイル数 */
		private final long						fileCount ;

		/** ディレクトリ数 */
		private final long						directoryCount ;

		/** 総バイト数 */
		private final long						totalBytes ;

		/** 大文字小文字衝突 */
		private final List<CaseCollision>		caseCollisions ;

		/** 診断メッセージ */
		private final List<DiagnosticMessage>	messages ;

		//--------------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 */
		//--------------------------------------------------------------------------
		private ExportReport(NfsExport export, boolean exists, boolean directory, boolean readable, boolean writable, long fileCount, long directoryCount, long totalBytes, List<CaseCollision> caseCollisions, List<DiagnosticMessage> messages) {
			this.export = export ;
			this.exists = exists ;
			this.directory = directory ;
			this.readable = readable ;
			this.writable = writable ;
			this.fileCount = fileCount ;
			this.directoryCount = directoryCount ;
			this.totalBytes = totalBytes ;
			this.caseCollisions = List.copyOf( caseCollisions) ;
			this.messages = List.copyOf( messages) ;
		}

		//--------------------------------------------------------------------------
		/**
		 * ファイル数を取得します。<br><br>
		 *
		 * <p>メソッド名称： ファイル数取得</p>
		 *
		 * @return ファイル数
		 */
		//--------------------------------------------------------------------------
		public long getFileCount() {
			return fileCount ;
		}

		//--------------------------------------------------------------------------
		/**
		 * ディレクトリ数を取得します。<br><br>
		 *
		 * <p>メソッド名称： ディレクトリ数取得</p>
		 *
		 * @return ディレクトリ数
		 */
		//--------------------------------------------------------------------------
		public long getDirectoryCount() {
			return directoryCount ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 総バイト数を取得します。<br><br>
		 *
		 * <p>メソッド名称： 総バイト数取得</p>
		 *
		 * @return 総バイト数
		 */
		//--------------------------------------------------------------------------
		public long getTotalBytes() {
			return totalBytes ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 大文字小文字衝突を取得します。<br><br>
		 *
		 * <p>メソッド名称： 大文字小文字衝突取得</p>
		 *
		 * @return 大文字小文字衝突
		 */
		//--------------------------------------------------------------------------
		public List<CaseCollision> getCaseCollisions() {
			return caseCollisions ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 診断メッセージを取得します。<br><br>
		 *
		 * <p>メソッド名称： 診断メッセージ取得</p>
		 *
		 * @return 診断メッセージ
		 */
		//--------------------------------------------------------------------------
		public List<DiagnosticMessage> getMessages() {
			return messages ;
		}

		//--------------------------------------------------------------------------
		/**
		 * テキストへ整形します。<br><br>
		 *
		 * <p>メソッド名称： テキスト整形</p>
		 *
		 * @return テキスト
		 */
		//--------------------------------------------------------------------------
		public String formatText() {
			StringBuilder builder = new StringBuilder() ;
			builder.append( export.getName()).append( System.lineSeparator()) ;
			builder.append( "  path=" ).append( export.getPath()).append( System.lineSeparator()) ;
			builder.append( "  configuredWritable=" ).append( export.isWritable()).append( System.lineSeparator()) ;
			builder.append( "  exists=" ).append( exists).append( System.lineSeparator()) ;
			builder.append( "  directory=" ).append( directory).append( System.lineSeparator()) ;
			builder.append( "  readable=" ).append( readable).append( System.lineSeparator()) ;
			builder.append( "  writable=" ).append( writable).append( System.lineSeparator()) ;
			builder.append( "  fileCount=" ).append( fileCount).append( System.lineSeparator()) ;
			builder.append( "  directoryCount=" ).append( directoryCount).append( System.lineSeparator()) ;
			builder.append( "  totalBytes=" ).append( totalBytes).append( System.lineSeparator()) ;
			builder.append( "  caseCollisionCount=" ).append( caseCollisions.size()).append( System.lineSeparator()) ;

			// メッセージを出力する
			for( DiagnosticMessage message : messages) {
				builder.append( "  " ).append( message.formatText()).append( System.lineSeparator()) ;
			}

			// 大文字小文字衝突を出力する
			for( CaseCollision collision : caseCollisions) {
				builder.append( "  collision " ).append( collision.getNormalizedPath()).append( ": " ).append( collision.getPaths()).append( System.lineSeparator()) ;
			}

			return builder.toString() ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 診断メッセージクラスです。<br><br>
	 *
	 * <p>クラス名称： 診断メッセージ</p>
	 */
	//------------------------------------------------------------------------------
	public static class DiagnosticMessage {
		/** 重大度 */
		private final String	severity ;

		/** コード */
		private final String	code ;

		/** メッセージ */
		private final String	message ;

		//--------------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param severity	重大度
		 * @param code		コード
		 * @param message	メッセージ
		 */
		//--------------------------------------------------------------------------
		private DiagnosticMessage(String severity, String code, String message) {
			this.severity = severity ;
			this.code = code ;
			this.message = message ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 情報メッセージを作成します。<br><br>
		 *
		 * <p>メソッド名称： 情報メッセージ作成</p>
		 *
		 * @param code		コード
		 * @param message	メッセージ
		 * @return メッセージ
		 */
		//--------------------------------------------------------------------------
		public static DiagnosticMessage info(String code, String message) {
			return new DiagnosticMessage( "INFO", code, message) ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 警告メッセージを作成します。<br><br>
		 *
		 * <p>メソッド名称： 警告メッセージ作成</p>
		 *
		 * @param code		コード
		 * @param message	メッセージ
		 * @return メッセージ
		 */
		//--------------------------------------------------------------------------
		public static DiagnosticMessage warning(String code, String message) {
			return new DiagnosticMessage( "WARNING", code, message) ;
		}

		//--------------------------------------------------------------------------
		/**
		 * エラーメッセージを作成します。<br><br>
		 *
		 * <p>メソッド名称： エラーメッセージ作成</p>
		 *
		 * @param code		コード
		 * @param message	メッセージ
		 * @return メッセージ
		 */
		//--------------------------------------------------------------------------
		public static DiagnosticMessage error(String code, String message) {
			return new DiagnosticMessage( "ERROR", code, message) ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 重大度を取得します。<br><br>
		 *
		 * <p>メソッド名称： 重大度取得</p>
		 *
		 * @return 重大度
		 */
		//--------------------------------------------------------------------------
		public String getSeverity() {
			return severity ;
		}

		//--------------------------------------------------------------------------
		/**
		 * コードを取得します。<br><br>
		 *
		 * <p>メソッド名称： コード取得</p>
		 *
		 * @return コード
		 */
		//--------------------------------------------------------------------------
		public String getCode() {
			return code ;
		}

		//--------------------------------------------------------------------------
		/**
		 * テキストへ整形します。<br><br>
		 *
		 * <p>メソッド名称： テキスト整形</p>
		 *
		 * @return テキスト
		 */
		//--------------------------------------------------------------------------
		public String formatText() {
			return severity + " " + code + ": " + message ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 大文字小文字衝突クラスです。<br><br>
	 *
	 * <p>クラス名称： 大文字小文字衝突</p>
	 */
	//------------------------------------------------------------------------------
	public static class CaseCollision {
		/** 正規化パス */
		private final String		normalizedPath ;

		/** 実パス表記 */
		private final List<String>	paths ;

		//--------------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param normalizedPath	正規化パス
		 * @param paths				実パス表記
		 */
		//--------------------------------------------------------------------------
		private CaseCollision(String normalizedPath, List<String> paths) {
			this.normalizedPath = normalizedPath ;
			this.paths = List.copyOf( paths) ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 正規化パスを取得します。<br><br>
		 *
		 * <p>メソッド名称： 正規化パス取得</p>
		 *
		 * @return 正規化パス
		 */
		//--------------------------------------------------------------------------
		public String getNormalizedPath() {
			return normalizedPath ;
		}

		//--------------------------------------------------------------------------
		/**
		 * 実パス表記を取得します。<br><br>
		 *
		 * <p>メソッド名称： 実パス表記取得</p>
		 *
		 * @return 実パス表記
		 */
		//--------------------------------------------------------------------------
		public List<String> getPaths() {
			return paths ;
		}
	}
}
