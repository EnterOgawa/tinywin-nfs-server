package jp.co.enterogawa.nfs.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//------------------------------------------------------------------------------
/**
 * サーバーログ出力クラスです。<br><br>
 *
 * <p>クラス名称： サーバーログ出力</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class ServerLog {
	//	定数定義	------------------------------------------------------------
	/** 時刻書式 */
	private static final DateTimeFormatter	FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.SSS") ;

	/** ロック */
	private static final Object				LOCK = new Object() ;

	/** ログWriter */
	private static BufferedWriter			writer ;

	/** ログWriterパス */
	private static Path						writerPath ;

	static {
		Runtime.getRuntime().addShutdownHook( new Thread( ServerLog::closeQuietly, "tinywin-nfs-log-close")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を抑止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private ServerLog() {
	}

	//--------------------------------------------------------------------------
	/**
	 * 情報ログを出力します。<br><br>
	 *
	 * <p>メソッド名称： 情報ログ出力</p>
	 *
	 * @param message	メッセージ
	 */
	//--------------------------------------------------------------------------
	public static void info(String message) {
		// コンソールログが有効な場合
		if( isConsoleEnabled()) {
			System.out.println( message) ;
		}

		String line = LocalDateTime.now().format( FORMATTER) + " " + message + System.lineSeparator() ;

		synchronized( LOCK) {
			try {
				Path path = getLogPath() ;

				// ログパスが変わった場合
				if( writer == null || writerPath == null || !writerPath.equals( path)) {
					openWriter( path) ;
				}

				writer.write( line) ;
				writer.flush() ;
			} catch( IOException ioex) {
				System.err.println( "Failed to write server log: " + ioex.getMessage()) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 要求ログの有効可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 要求ログ有効可否取得</p>
	 *
	 * @return true:有効 false:無効
	 */
	//--------------------------------------------------------------------------
	public static boolean isRequestLogEnabled() {
		return Boolean.parseBoolean( System.getProperty( "tinywin.nfs.requestLog", "false")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 変更操作ログ出力要否を判定します。<br><br>
	 *
	 * <p>メソッド名称： 変更操作ログ出力要否判定</p>
	 *
	 * @param operation	操作名
	 * @param status	ステータス
	 * @return true:出力 false:抑制
	 */
	//--------------------------------------------------------------------------
	public static boolean shouldLogOperation(String operation, int status) {
		// デバッグログまたは詳細変更ログが有効な場合
		if( isDebugEnabled() || isVerboseMutationLogEnabled()) {
			return true ;
		}

		// エラーの場合
		if( status != 0) {
			return true ;
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * デバッグログの有効可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： デバッグログ有効可否取得</p>
	 *
	 * @return true:有効 false:無効
	 */
	//--------------------------------------------------------------------------
	public static boolean isDebugEnabled() {
		return Boolean.parseBoolean( System.getProperty( "tinywin.nfs.debug", "false")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 詳細変更ログの有効可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 詳細変更ログ有効可否取得</p>
	 *
	 * @return true:有効 false:無効
	 */
	//--------------------------------------------------------------------------
	private static boolean isVerboseMutationLogEnabled() {
		return Boolean.parseBoolean( System.getProperty( "tinywin.nfs.verboseMutationLog", "false")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * コンソールログの有効可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： コンソールログ有効可否取得</p>
	 *
	 * @return true:有効 false:無効
	 */
	//--------------------------------------------------------------------------
	private static boolean isConsoleEnabled() {
		return Boolean.parseBoolean( System.getProperty( "tinywin.nfs.consoleLog", "false")) || isDebugEnabled() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： ログパス取得</p>
	 *
	 * @return ログパス
	 */
	//--------------------------------------------------------------------------
	private static Path getLogPath() {
		return Path.of( System.getProperty( "tinywin.nfs.log", "logs/nfs-server.log")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログWriterを開きます。<br><br>
	 *
	 * <p>メソッド名称： ログWriterオープン</p>
	 *
	 * @param path	ログパス
	 * @throws IOException オープン異常
	 */
	//--------------------------------------------------------------------------
	private static void openWriter(Path path) throws IOException {
		closeWriter() ;
		Path directory = path.getParent() ;

		// 親ディレクトリが存在する場合
		if( directory != null) {
			Files.createDirectories( directory) ;
		}

		writer = Files.newBufferedWriter(
				path,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND) ;
		writerPath = path ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログWriterを閉じます。<br><br>
	 *
	 * <p>メソッド名称： ログWriterクローズ</p>
	 *
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	private static void closeWriter() throws IOException {
		// Writerが存在しない場合
		if( writer == null) {
			return ;
		}

		writer.close() ;
		writer = null ;
		writerPath = null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログWriterを静かに閉じます。<br><br>
	 *
	 * <p>メソッド名称： ログWriter静的クローズ</p>
	 */
	//--------------------------------------------------------------------------
	private static void closeQuietly() {
		synchronized( LOCK) {
			try {
				closeWriter() ;
			} catch( IOException ignored) {
				// 終了時ログクローズ異常は無視する
			}
		}
	}
}
