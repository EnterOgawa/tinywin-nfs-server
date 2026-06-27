package jp.co.enterogawa.nfs.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

//------------------------------------------------------------------------------
/**
 * NFSサーバー設定クラスです。<br><br>
 *
 * <p>クラス名称： NFSサーバー設定</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsServerConfig {
	//	定数定義	------------------------------------------------------------
	/** デフォルトPortmapポート */
	private static final int				DEFAULT_PORTMAP_PORT = 111 ;

	/** デフォルトNFSポート */
	private static final int				DEFAULT_NFS_PORT = 2049 ;

	/** デフォルトMOUNTポート */
	private static final int				DEFAULT_MOUNT_PORT = 20048 ;

	//	内部定義	------------------------------------------------------------
	/** Portmapポート */
	private final int					portmapPort ;

	/** NFSポート */
	private final int					nfsPort ;

	/** MOUNTポート */
	private final int					mountPort ;

	/** 公開定義 */
	private final List<NfsExport>		exports ;

	/** UID */
	private final int					uid ;

	/** GID */
	private final int					gid ;

	/** ファイルモード */
	private final int					fileMode ;

	/** ディレクトリモード */
	private final int					directoryMode ;

	/** ブロックサイズ */
	private final int					blockSize ;

	/** 読込サイズ */
	private final int					readSize ;

	/** ファイル名文字コード */
	private final Charset				filenameCharset ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param properties	設定値
	 */
	//--------------------------------------------------------------------------
	private NfsServerConfig(Properties properties) {
		portmapPort = getInt( properties, "portmap.port", DEFAULT_PORTMAP_PORT) ;
		nfsPort = getInt( properties, "nfs.port", DEFAULT_NFS_PORT) ;
		mountPort = getInt( properties, "mount.port", DEFAULT_MOUNT_PORT) ;
		exports = loadExports( properties) ;
		uid = getInt( properties, "uid", 0) ;
		gid = getInt( properties, "gid", 0) ;
		fileMode = getOctalInt( properties, "file.mode", 0644) ;
		directoryMode = getOctalInt( properties, "directory.mode", 0755) ;
		blockSize = getInt( properties, "block.size", 4096) ;
		readSize = getInt( properties, "read.size", 8192) ;
		filenameCharset = Charset.forName( getString( properties, "filename.charset", "UTF-8")) ;
		validate() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開定義を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 公開定義読込</p>
	 *
	 * @param properties	設定値
	 * @return 公開定義
	 */
	//--------------------------------------------------------------------------
	private List<NfsExport> loadExports(Properties properties) {
		int count = getInt( properties, "exports.count", 0) ;
		List<NfsExport> result = new ArrayList<NfsExport>() ;

		// 複数公開定義がある場合
		if( count > 0) {
			for( int i = 1; i <= count; i++) {
				String prefix = "exports." + i + "." ;
				String name = properties.getProperty( prefix + "name") ;
				String path = properties.getProperty( prefix + "path") ;
				boolean writable = Boolean.parseBoolean( properties.getProperty( prefix + "writable", "true")) ;
				List<String> allowedClients = getAllowedClients( properties, prefix + "allowed.clients") ;

				// 公開定義が不足している場合
				if( name == null || name.isBlank() || path == null || path.isBlank()) {
					throw new IllegalArgumentException( prefix + "name and " + prefix + "path are required.") ;
				}

				result.add( new NfsExport( name, Path.of( path.trim()), writable, allowedClients)) ;
			}

			return List.copyOf( result) ;
		}

		String name = properties.getProperty( "export.name", "/export") ;
		String path = properties.getProperty( "export.path", "export") ;
		boolean writable = Boolean.parseBoolean( properties.getProperty( "export.writable", "true")) ;
		List<String> allowedClients = getAllowedClients( properties, "export.allowed.clients") ;
		result.add( new NfsExport( name, Path.of( path.trim()), writable, allowedClients)) ;
		return List.copyOf( result) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定ファイルを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 設定読込</p>
	 *
	 * @param configPath	設定ファイルパス
	 * @return 設定
	 * @throws IOException 読込異常
	 */
	//--------------------------------------------------------------------------
	public static NfsServerConfig load(Path configPath) throws IOException {
		Properties properties = new Properties() ;

		// 設定ファイルが存在する場合
		if( Files.exists( configPath)) {
			try( InputStream input = Files.newInputStream( configPath)) {
				properties.load( input) ;
			}
		}

		return new NfsServerConfig( properties) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 数値設定を取得します。<br><br>
	 *
	 * <p>メソッド名称： 数値設定取得</p>
	 *
	 * @param properties	設定値
	 * @param key		キー
	 * @param defaultValue	デフォルト値
	 * @return 数値設定
	 */
	//--------------------------------------------------------------------------
	private static int getInt(Properties properties, String key, int defaultValue) {
		String value = properties.getProperty( key) ;

		// 設定がない場合
		if( value == null || value.isBlank()) {
			return defaultValue ;
		}

		return Integer.parseInt( value.trim()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 8進数設定を取得します。<br><br>
	 *
	 * <p>メソッド名称： 8進数設定取得</p>
	 *
	 * @param properties	設定値
	 * @param key		キー
	 * @param defaultValue	デフォルト値
	 * @return 数値設定
	 */
	//--------------------------------------------------------------------------
	private static int getOctalInt(Properties properties, String key, int defaultValue) {
		String value = properties.getProperty( key) ;

		// 設定がない場合
		if( value == null || value.isBlank()) {
			return defaultValue ;
		}

		return Integer.parseInt( value.trim(), 8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列設定を取得します。<br><br>
	 *
	 * <p>メソッド名称： 文字列設定取得</p>
	 *
	 * @param properties	設定値
	 * @param key		キー
	 * @param defaultValue	デフォルト値
	 * @return 文字列設定
	 */
	//--------------------------------------------------------------------------
	private static String getString(Properties properties, String key, String defaultValue) {
		String value = properties.getProperty( key) ;

		// 設定がない場合
		if( value == null || value.isBlank()) {
			return defaultValue ;
		}

		return value.trim() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 許可クライアントを取得します。<br><br>
	 *
	 * <p>メソッド名称： 許可クライアント取得</p>
	 *
	 * @param properties	設定値
	 * @param key		キー
	 * @return 許可クライアント
	 */
	//--------------------------------------------------------------------------
	private static List<String> getAllowedClients(Properties properties, String key) {
		String value = properties.getProperty( key) ;

		// 設定がない場合
		if( value == null || value.isBlank()) {
			return List.of() ;
		}

		List<String> result = new ArrayList<String>() ;
		String[] entries = value.split( "," ) ;

		// 許可クライアントを分割して検証する
		for( String entry : entries) {
			String address = entry.trim() ;

			// 空要素の場合
			if( address.isEmpty()) {
				continue ;
			}

			// IPv4アドレスではない場合
			if( !isExactIpv4Address( address)) {
				throw new IllegalArgumentException( key + " contains invalid IPv4 address: " + address) ;
			}

			result.add( address) ;
		}

		return List.copyOf( result) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定値を検証します。<br><br>
	 *
	 * <p>メソッド名称： 設定値検証</p>
	 */
	//--------------------------------------------------------------------------
	private void validate() {
		validatePort( "portmap.port", portmapPort) ;
		validatePort( "nfs.port", nfsPort) ;
		validatePort( "mount.port", mountPort) ;

		// 公開定義が存在しない場合
		if( exports.isEmpty()) {
			throw new IllegalArgumentException( "at least one export is required.") ;
		}

		Set<String> names = new HashSet<String>() ;
		Set<Path> paths = new HashSet<Path>() ;

		// 公開定義を検証する
		for( NfsExport export : exports) {
			validateExportName( export.getName()) ;
			validateExportPath( export) ;

			// 公開名が重複している場合
			if( !names.add( export.getName())) {
				throw new IllegalArgumentException( "export name is duplicated: " + export.getName()) ;
			}

			// 公開パスが重複している場合
			if( !paths.add( export.getPath())) {
				throw new IllegalArgumentException( "export path is duplicated: " + export.getPath()) ;
			}
		}

		// 公開パスの入れ子を検証する
		for( NfsExport first : exports) {
			for( NfsExport second : exports) {
				// 同一公開定義の場合
				if( first == second) {
					continue ;
				}

				// 公開パスが入れ子の場合
				if( first.getPath().startsWith( second.getPath())) {
					throw new IllegalArgumentException( "export paths must not be nested: " + first.getPath() + " / " + second.getPath()) ;
				}
			}
		}

		// ブロックサイズが不正な場合
		if( blockSize <= 0) {
			throw new IllegalArgumentException( "block.size must be greater than zero.") ;
		}

		// 読込サイズが不正な場合
		if( readSize <= 0) {
			throw new IllegalArgumentException( "read.size must be greater than zero.") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開パスを検証します。<br><br>
	 *
	 * <p>メソッド名称： 公開パス検証</p>
	 *
	 * @param export	公開定義
	 */
	//--------------------------------------------------------------------------
	private void validateExportPath(NfsExport export) {
		Path path = export.getPath() ;

		// 公開パスが存在しない場合
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			throw new IllegalArgumentException( "export path does not exist: " + path) ;
		}

		// 公開パスがディレクトリではない場合
		if( !Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS)) {
			throw new IllegalArgumentException( "export path is not a directory: " + path) ;
		}

		// 公開パスが読込不可の場合
		if( !Files.isReadable( path)) {
			throw new IllegalArgumentException( "export path is not readable: " + path) ;
		}

		// 書込共有だが公開パスが書込不可の場合
		if( export.isWritable() && !Files.isWritable( path)) {
			throw new IllegalArgumentException( "export path is not writable: " + path) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開名を検証します。<br><br>
	 *
	 * <p>メソッド名称： 公開名検証</p>
	 *
	 * @param name	公開名
	 */
	//--------------------------------------------------------------------------
	private void validateExportName(String name) {
		// 公開名が絶対パス形式ではない場合
		if( name == null || name.isBlank() || !name.startsWith( "/")) {
			throw new IllegalArgumentException( "export name must start with '/': " + name) ;
		}

		// 公開名がルートのみの場合
		if( "/".equals( name)) {
			throw new IllegalArgumentException( "export name must not be root only.") ;
		}

		// 公開名が不正な文字を含む場合
		if( name.contains( "\\" ) || name.contains( "//") || containsWhitespace( name) || hasParentSegment( name)) {
			throw new IllegalArgumentException( "export name is invalid: " + name) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 空白文字の有無を確認します。<br><br>
	 *
	 * <p>メソッド名称： 空白文字有無確認</p>
	 *
	 * @param value	値
	 * @return true:あり false:なし
	 */
	//--------------------------------------------------------------------------
	private boolean containsWhitespace(String value) {
		// 文字列の空白文字を確認する
		for( int i = 0; i < value.length(); i++) {
			// 空白文字の場合
			if( Character.isWhitespace( value.charAt( i))) {
				return true ;
			}
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 親ディレクトリセグメント有無を確認します。<br><br>
	 *
	 * <p>メソッド名称： 親ディレクトリセグメント有無確認</p>
	 *
	 * @param value	値
	 * @return true:あり false:なし
	 */
	//--------------------------------------------------------------------------
	private boolean hasParentSegment(String value) {
		String[] segments = value.split( "/" ) ;

		// パスセグメントを検証する
		for( String segment : segments) {
			// 親ディレクトリセグメントの場合
			if( "..".equals( segment)) {
				return true ;
			}
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * IPv4アドレスを確認します。<br><br>
	 *
	 * <p>メソッド名称： IPv4アドレス確認</p>
	 *
	 * @param value	値
	 * @return true:IPv4 false:IPv4以外
	 */
	//--------------------------------------------------------------------------
	private static boolean isExactIpv4Address(String value) {
		String[] parts = value.split( "\\.", -1) ;

		// 4オクテットではない場合
		if( parts.length != 4) {
			return false ;
		}

		// 各オクテットを確認する
		for( String part : parts) {
			// 空オクテットの場合
			if( part.isEmpty()) {
				return false ;
			}

			try {
				int octet = Integer.parseInt( part) ;

				// 範囲外の場合
				if( octet < 0 || octet > 255) {
					return false ;
				}
			} catch( NumberFormatException nfex) {
				return false ;
			}
		}

		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ポート番号を検証します。<br><br>
	 *
	 * <p>メソッド名称： ポート番号検証</p>
	 *
	 * @param key	キー
	 * @param port	ポート
	 */
	//--------------------------------------------------------------------------
	private void validatePort(String key, int port) {
		// ポート番号が範囲外の場合
		if( port <= 0 || port > 65535) {
			throw new IllegalArgumentException( key + " is out of range.") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * Portmapポートを取得します。<br><br>
	 *
	 * <p>メソッド名称： Portmapポート取得</p>
	 *
	 * @return Portmapポート
	 */
	//--------------------------------------------------------------------------
	public int getPortmapPort() {
		return portmapPort ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSポートを取得します。<br><br>
	 *
	 * <p>メソッド名称： NFSポート取得</p>
	 *
	 * @return NFSポート
	 */
	//--------------------------------------------------------------------------
	public int getNfsPort() {
		return nfsPort ;
	}

	//--------------------------------------------------------------------------
	/**
	 * MOUNTポートを取得します。<br><br>
	 *
	 * <p>メソッド名称： MOUNTポート取得</p>
	 *
	 * @return MOUNTポート
	 */
	//--------------------------------------------------------------------------
	public int getMountPort() {
		return mountPort ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開定義一覧を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開定義一覧取得</p>
	 *
	 * @return 公開定義一覧
	 */
	//--------------------------------------------------------------------------
	public List<NfsExport> getExports() {
		return exports ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開名から公開定義を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開定義取得</p>
	 *
	 * @param name	公開名
	 * @return 公開定義
	 */
	//--------------------------------------------------------------------------
	public NfsExport findExport(String name) {
		// 公開定義を検索する
		for( NfsExport export : exports) {
			// 公開名が一致する場合
			if( export.getName().equals( name)) {
				return export ;
			}
		}

		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開名を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開名取得</p>
	 *
	 * @return 公開名
	 */
	//--------------------------------------------------------------------------
	public String getExportName() {
		return exports.get( 0).getName() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開パスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開パス取得</p>
	 *
	 * @return 公開パス
	 */
	//--------------------------------------------------------------------------
	public Path getExportPath() {
		return exports.get( 0).getPath() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開書込可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開書込可否取得</p>
	 *
	 * @return true:書込可 false:読込専用
	 */
	//--------------------------------------------------------------------------
	public boolean isExportWritable() {
		return exports.get( 0).isWritable() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * UIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： UID取得</p>
	 *
	 * @return UID
	 */
	//--------------------------------------------------------------------------
	public int getUid() {
		return uid ;
	}

	//--------------------------------------------------------------------------
	/**
	 * GIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： GID取得</p>
	 *
	 * @return GID
	 */
	//--------------------------------------------------------------------------
	public int getGid() {
		return gid ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルモードを取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイルモード取得</p>
	 *
	 * @return ファイルモード
	 */
	//--------------------------------------------------------------------------
	public int getFileMode() {
		return fileMode ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリモードを取得します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリモード取得</p>
	 *
	 * @return ディレクトリモード
	 */
	//--------------------------------------------------------------------------
	public int getDirectoryMode() {
		return directoryMode ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ブロックサイズを取得します。<br><br>
	 *
	 * <p>メソッド名称： ブロックサイズ取得</p>
	 *
	 * @return ブロックサイズ
	 */
	//--------------------------------------------------------------------------
	public int getBlockSize() {
		return blockSize ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 読込サイズを取得します。<br><br>
	 *
	 * <p>メソッド名称： 読込サイズ取得</p>
	 *
	 * @return 読込サイズ
	 */
	//--------------------------------------------------------------------------
	public int getReadSize() {
		return readSize ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイル名文字コードを取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイル名文字コード取得</p>
	 *
	 * @return ファイル名文字コード
	 */
	//--------------------------------------------------------------------------
	public Charset getFilenameCharset() {
		return filenameCharset ;
	}
}
