package jp.co.enterogawa.nfs.config;

import java.nio.file.Path;

//------------------------------------------------------------------------------
/**
 * TinyWinNFSの標準パス定義クラスです。<br><br>
 *
 * <p>クラス名称： TinyWinNFS標準パス定義</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class TinyWinNfsPaths {
	//	定数定義	------------------------------------------------------------
	/** データルート環境変数 */
	public static final String			DATA_ROOT_ENVIRONMENT = "TINYWIN_NFS_DATA" ;

	/** 会社名 */
	private static final String			PUBLISHER_NAME = "EnterOgawa" ;

	/** 製品名 */
	private static final String			PRODUCT_NAME = "TinyWinNFS Server" ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を抑止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private TinyWinNfsPaths() {
	}

	//--------------------------------------------------------------------------
	/**
	 * データルートを取得します。<br><br>
	 *
	 * <p>メソッド名称： データルート取得</p>
	 *
	 * @param applicationRoot	アプリケーションルート
	 * @return データルート
	 */
	//--------------------------------------------------------------------------
	public static Path getDataRoot(Path applicationRoot) {
		String configuredRoot = System.getenv( DATA_ROOT_ENVIRONMENT) ;

		// データルート環境変数が指定されている場合
		if( configuredRoot != null && !configuredRoot.isBlank()) {
			return Path.of( configuredRoot).toAbsolutePath().normalize() ;
		}

		String programData = System.getenv( "ProgramData") ;

		// ProgramDataが取得できる場合
		if( programData != null && !programData.isBlank()) {
			return Path.of( programData, PUBLISHER_NAME, PRODUCT_NAME).toAbsolutePath().normalize() ;
		}

		return applicationRoot.toAbsolutePath().normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 標準設定ファイルパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 標準設定ファイルパス取得</p>
	 *
	 * @param applicationRoot	アプリケーションルート
	 * @return 設定ファイルパス
	 */
	//--------------------------------------------------------------------------
	public static Path getConfigPath(Path applicationRoot) {
		return getDataRoot( applicationRoot).resolve( "conf").resolve( "nfs-server.properties") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 標準ログファイルパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 標準ログファイルパス取得</p>
	 *
	 * @param applicationRoot	アプリケーションルート
	 * @return ログファイルパス
	 */
	//--------------------------------------------------------------------------
	public static Path getLogPath(Path applicationRoot) {
		return getDataRoot( applicationRoot).resolve( "logs").resolve( "nfs-server.log") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 標準公開フォルダパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 標準公開フォルダパス取得</p>
	 *
	 * @param applicationRoot	アプリケーションルート
	 * @return 公開フォルダパス
	 */
	//--------------------------------------------------------------------------
	public static Path getExportPath(Path applicationRoot) {
		return getDataRoot( applicationRoot).resolve( "export") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 旧設定ファイルパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 旧設定ファイルパス取得</p>
	 *
	 * @param applicationRoot	アプリケーションルート
	 * @return 旧設定ファイルパス
	 */
	//--------------------------------------------------------------------------
	public static Path getLegacyConfigPath(Path applicationRoot) {
		return applicationRoot.toAbsolutePath().normalize().resolve( "conf").resolve( "nfs-server.properties") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定ファイル基準パスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 設定ファイル基準パス取得</p>
	 *
	 * @param configPath	設定ファイルパス
	 * @return 基準パス
	 */
	//--------------------------------------------------------------------------
	public static Path getConfigBasePath(Path configPath) {
		Path absoluteConfigPath = configPath.toAbsolutePath().normalize() ;
		Path configDirectory = absoluteConfigPath.getParent() ;

		// 設定ディレクトリが存在しない場合
		if( configDirectory == null) {
			return Path.of( "" ).toAbsolutePath().normalize() ;
		}

		Path directoryName = configDirectory.getFileName() ;

		// confディレクトリ配下の設定ファイルの場合
		if( directoryName != null && "conf".equalsIgnoreCase( directoryName.toString()) && configDirectory.getParent() != null) {
			return configDirectory.getParent().toAbsolutePath().normalize() ;
		}

		return configDirectory.toAbsolutePath().normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定内パスを解決します。<br><br>
	 *
	 * <p>メソッド名称： 設定内パス解決</p>
	 *
	 * @param configBasePath	設定基準パス
	 * @param value			設定値
	 * @return 解決済みパス
	 */
	//--------------------------------------------------------------------------
	public static Path resolveConfiguredPath(Path configBasePath, String value) {
		Path path = Path.of( value.trim()) ;

		// 絶対パスの場合
		if( path.isAbsolute()) {
			return path.toAbsolutePath().normalize() ;
		}

		return configBasePath.resolve( path).toAbsolutePath().normalize() ;
	}
}
