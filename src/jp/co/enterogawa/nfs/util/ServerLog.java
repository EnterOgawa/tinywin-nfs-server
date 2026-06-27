package jp.co.enterogawa.nfs.util;

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
		System.out.println( message) ;
		String line = LocalDateTime.now().format( FORMATTER) + " " + message + System.lineSeparator() ;

		synchronized( LOCK) {
			try {
				Path path = getLogPath() ;
				Path directory = path.getParent() ;

				// 親ディレクトリが存在する場合
				if( directory != null) {
					Files.createDirectories( directory) ;
				}

				Files.writeString(
						path,
						line,
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE,
						StandardOpenOption.APPEND) ;
			} catch( IOException ioex) {
				System.err.println( "Failed to write server log: " + ioex.getMessage()) ;
			}
		}
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
}
