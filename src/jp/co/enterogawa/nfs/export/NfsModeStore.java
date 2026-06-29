package jp.co.enterogawa.nfs.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jp.co.enterogawa.nfs.config.NfsExport;

//------------------------------------------------------------------------------
/**
 * NFS属性メタデータ管理クラスです。<br><br>
 *
 * <p>クラス名称： NFS属性メタデータ管理</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsModeStore {
	//	定数定義	------------------------------------------------------------
	/** モードビットマスク */
	private static final int				MODE_MASK = 07777 ;

	//	内部定義	------------------------------------------------------------
	/** 公開定義 */
	private final List<NfsExport>		exports ;

	/** 永続ファイル */
	private final Path					storePath ;

	/** パス別モード */
	private final Map<String, Integer>	modeMap = new HashMap<String, Integer>() ;

	/** パス別UID */
	private final Map<String, Integer>	uidMap = new HashMap<String, Integer>() ;

	/** パス別GID */
	private final Map<String, Integer>	gidMap = new HashMap<String, Integer>() ;

	/** 永続ファイル保存間隔 */
	private final long					storeSaveIntervalMillis ;

	/** 永続ファイル変更有無 */
	private boolean						storeDirty ;

	/** 最終永続ファイル保存時刻 */
	private long						lastStoreSaveMillis ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param exports	公開定義
	 */
	//--------------------------------------------------------------------------
	public NfsModeStore(List<NfsExport> exports) {
		// 公開定義が存在しない場合
		if( exports == null || exports.isEmpty()) {
			throw new IllegalArgumentException( "At least one export is required.") ;
		}

		this.exports = List.copyOf( exports) ;
		storePath = resolveStorePath() ;
		storeSaveIntervalMillis = getStoreSaveIntervalMillis() ;
		lastStoreSaveMillis = System.currentTimeMillis() ;
		loadStore() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * モードを取得します。<br><br>
	 *
	 * <p>メソッド名称： モード取得</p>
	 *
	 * @param path			対象パス
	 * @param defaultMode	既定モード
	 * @return モード
	 */
	//--------------------------------------------------------------------------
	public synchronized int getMode(Path path, int defaultMode) {
		String key = createPathKey( path) ;

		// 管理対象外の場合
		if( key == null) {
			return defaultMode & MODE_MASK ;
		}

		Integer mode = modeMap.get( key) ;

		// 保存済みモードがない場合
		if( mode == null) {
			return defaultMode & MODE_MASK ;
		}

		return mode.intValue() & MODE_MASK ;
	}

	//--------------------------------------------------------------------------
	/**
	 * モードを設定します。<br><br>
	 *
	 * <p>メソッド名称： モード設定</p>
	 *
	 * @param path	対象パス
	 * @param mode	モード
	 */
	//--------------------------------------------------------------------------
	public synchronized void setMode(Path path, int mode) {
		String key = createPathKey( path) ;

		// 管理対象外の場合
		if( key == null) {
			return ;
		}

		modeMap.put( key, Integer.valueOf( mode & MODE_MASK)) ;
		markStoreDirty() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * UIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： UID取得</p>
	 *
	 * @param path			対象パス
	 * @param defaultUid	既定UID
	 * @return UID
	 */
	//--------------------------------------------------------------------------
	public synchronized int getUid(Path path, int defaultUid) {
		String key = createPathKey( path) ;

		// 管理対象外の場合
		if( key == null) {
			return defaultUid ;
		}

		Integer uid = uidMap.get( key) ;

		// 保存済みUIDがない場合
		if( uid == null) {
			return defaultUid ;
		}

		return uid.intValue() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * GIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： GID取得</p>
	 *
	 * @param path			対象パス
	 * @param defaultGid	既定GID
	 * @return GID
	 */
	//--------------------------------------------------------------------------
	public synchronized int getGid(Path path, int defaultGid) {
		String key = createPathKey( path) ;

		// 管理対象外の場合
		if( key == null) {
			return defaultGid ;
		}

		Integer gid = gidMap.get( key) ;

		// 保存済みGIDがない場合
		if( gid == null) {
			return defaultGid ;
		}

		return gid.intValue() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 所有者を設定します。<br><br>
	 *
	 * <p>メソッド名称： 所有者設定</p>
	 *
	 * @param path	対象パス
	 * @param uid	UID
	 * @param gid	GID
	 */
	//--------------------------------------------------------------------------
	public synchronized void setOwner(Path path, Integer uid, Integer gid) {
		String key = createPathKey( path) ;

		// 管理対象外の場合
		if( key == null) {
			return ;
		}

		boolean changed = false ;

		// UID指定がある場合
		if( uid != null) {
			uidMap.put( key, uid) ;
			changed = true ;
		}

		// GID指定がある場合
		if( gid != null) {
			gidMap.put( key, gid) ;
			changed = true ;
		}

		// 変更がある場合
		if( changed) {
			markStoreDirty() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * メタデータを削除します。<br><br>
	 *
	 * <p>メソッド名称： メタデータ削除</p>
	 *
	 * @param path	対象パス
	 */
	//--------------------------------------------------------------------------
	public synchronized void remove(Path path) {
		String key = createPathKey( path) ;

		// 管理対象外の場合
		if( key == null) {
			return ;
		}

		boolean removed = false ;
		removed |= modeMap.remove( key) != null ;
		removed |= uidMap.remove( key) != null ;
		removed |= gidMap.remove( key) != null ;

		// 削除対象が存在する場合
		if( removed) {
			markStoreDirty() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ツリー配下のメタデータを削除します。<br><br>
	 *
	 * <p>メソッド名称： ツリーメタデータ削除</p>
	 *
	 * @param path	対象パス
	 */
	//--------------------------------------------------------------------------
	public synchronized void removeTree(Path path) {
		NfsExport export = findExport( path) ;

		// 公開定義が存在しない場合
		if( export == null) {
			return ;
		}

		String prefix = createPathKey( export, path.toAbsolutePath().normalize()) ;

		// キーが作成できない場合
		if( prefix == null) {
			return ;
		}

		boolean removed = false ;
		removed |= removeTreeValues( modeMap, prefix) ;
		removed |= removeTreeValues( uidMap, prefix) ;
		removed |= removeTreeValues( gidMap, prefix) ;

		// 削除した場合
		if( removed) {
			markStoreDirty() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * メタデータのパスを移動します。<br><br>
	 *
	 * <p>メソッド名称： メタデータパス移動</p>
	 *
	 * @param source	移動元
	 * @param target	移動先
	 */
	//--------------------------------------------------------------------------
	public synchronized void move(Path source, Path target) {
		NfsExport sourceExport = findExport( source) ;
		NfsExport targetExport = findExport( target) ;

		// 公開定義が異なる場合
		if( sourceExport == null || sourceExport != targetExport) {
			return ;
		}

		Path sourcePath = source.toAbsolutePath().normalize() ;
		Path targetPath = target.toAbsolutePath().normalize() ;
		String sourcePrefix = createPathKey( sourceExport, sourcePath) ;
		String targetPrefix = createPathKey( targetExport, targetPath) ;

		// キーが作成できない場合
		if( sourcePrefix == null || targetPrefix == null) {
			return ;
		}

		boolean moved = false ;
		moved |= moveValues( modeMap, sourcePrefix, targetPrefix) ;
		moved |= moveValues( uidMap, sourcePrefix, targetPrefix) ;
		moved |= moveValues( gidMap, sourcePrefix, targetPrefix) ;

		// 移動対象が存在する場合
		if( moved) {
			markStoreDirty() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイルをflushします。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイルflush</p>
	 */
	//--------------------------------------------------------------------------
	public synchronized void flush() {
		// 未保存変更が存在する場合
		if( storeDirty) {
			saveStore() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイルパスを解決します。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイルパス解決</p>
	 *
	 * @return 永続ファイルパス
	 */
	//--------------------------------------------------------------------------
	private Path resolveStorePath() {
		String dataPath = System.getProperty( "tinywin.nfs.data", "data") ;
		return Path.of( dataPath).resolve( "modes.properties").toAbsolutePath().normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイルを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイル読込</p>
	 */
	//--------------------------------------------------------------------------
	private void loadStore() {
		// 永続ファイルが存在しない場合
		if( !Files.exists( storePath)) {
			return ;
		}

		Properties properties = new Properties() ;

		try( InputStream input = Files.newInputStream( storePath)) {
			properties.load( input) ;

			// メタデータ定義を読み込む
			for( String key : properties.stringPropertyNames()) {
				// モード定義の場合
				if( key.startsWith( "mode.")) {
					modeMap.put( key.substring( "mode.".length()), Integer.valueOf( parseOctalMode( properties.getProperty( key), 0644))) ;
					continue ;
				}

				// UID定義の場合
				if( key.startsWith( "uid.")) {
					uidMap.put( key.substring( "uid.".length()), Integer.valueOf( parseInt32( properties.getProperty( key), 0))) ;
					continue ;
				}

				// GID定義の場合
				if( key.startsWith( "gid.")) {
					gidMap.put( key.substring( "gid.".length()), Integer.valueOf( parseInt32( properties.getProperty( key), 0))) ;
				}
			}
		} catch( IOException | RuntimeException ex) {
			System.err.println( "Failed to load NFS mode store: " + ex.getMessage()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイルへ保存します。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイル保存</p>
	 */
	//--------------------------------------------------------------------------
	private void saveStore() {
		Properties properties = new Properties() ;

		// モード定義を保存する
		for( Map.Entry<String, Integer> entry : modeMap.entrySet()) {
			properties.setProperty( "mode." + entry.getKey(), Integer.toOctalString( entry.getValue().intValue() & MODE_MASK)) ;
		}

		// UID定義を保存する
		for( Map.Entry<String, Integer> entry : uidMap.entrySet()) {
			properties.setProperty( "uid." + entry.getKey(), Integer.toString( entry.getValue().intValue())) ;
		}

		// GID定義を保存する
		for( Map.Entry<String, Integer> entry : gidMap.entrySet()) {
			properties.setProperty( "gid." + entry.getKey(), Integer.toString( entry.getValue().intValue())) ;
		}

		try {
			Path parent = storePath.getParent() ;

			// 親ディレクトリが存在する場合
			if( parent != null) {
				Files.createDirectories( parent) ;
			}

			try( OutputStream output = Files.newOutputStream( storePath)) {
				properties.store( output, "TinyWinNFS metadata store") ;
			}

			storeDirty = false ;
			lastStoreSaveMillis = System.currentTimeMillis() ;
		} catch( IOException ex) {
			System.err.println( "Failed to save NFS mode store: " + ex.getMessage()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイルを変更済みにします。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイル変更済設定</p>
	 */
	//--------------------------------------------------------------------------
	private void markStoreDirty() {
		storeDirty = true ;

		// 保存間隔が無効の場合
		if( storeSaveIntervalMillis <= 0) {
			saveStore() ;
			return ;
		}

		long now = System.currentTimeMillis() ;

		// 保存間隔に達した場合
		if( now - lastStoreSaveMillis >= storeSaveIntervalMillis) {
			saveStore() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイル保存間隔を取得します。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイル保存間隔取得</p>
	 *
	 * @return 保存間隔ミリ秒
	 */
	//--------------------------------------------------------------------------
	private long getStoreSaveIntervalMillis() {
		String value = System.getProperty( "tinywin.nfs.modeStoreSaveIntervalMillis", "30000") ;

		// 値がない場合
		if( value == null || value.isBlank()) {
			return 30000L ;
		}

		try {
			return Math.max( 0L, Long.parseLong( value.trim())) ;
		} catch( NumberFormatException nfex) {
			return 30000L ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ツリー配下の値を削除します。<br><br>
	 *
	 * <p>メソッド名称： ツリー値削除</p>
	 *
	 * @param values	値
	 * @param prefix	接頭辞
	 * @return true:削除あり false:削除なし
	 */
	//--------------------------------------------------------------------------
	private boolean removeTreeValues(Map<String, Integer> values, String prefix) {
		return values.keySet().removeIf( key -> key.equals( prefix) || key.startsWith( prefix + "/")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 値のパスを移動します。<br><br>
	 *
	 * <p>メソッド名称： 値パス移動</p>
	 *
	 * @param values		値
	 * @param sourcePrefix	移動元接頭辞
	 * @param targetPrefix	移動先接頭辞
	 * @return true:移動あり false:移動なし
	 */
	//--------------------------------------------------------------------------
	private boolean moveValues(Map<String, Integer> values, String sourcePrefix, String targetPrefix) {
		Map<String, Integer> movedMap = new HashMap<String, Integer>() ;

		// 移動対象を集める
		for( Map.Entry<String, Integer> entry : values.entrySet()) {
			String key = entry.getKey() ;

			// 移動元配下の場合
			if( key.equals( sourcePrefix) || key.startsWith( sourcePrefix + "/")) {
				String movedKey = targetPrefix + key.substring( sourcePrefix.length()) ;
				movedMap.put( movedKey, entry.getValue()) ;
			}
		}

		// 移動対象がない場合
		if( movedMap.isEmpty()) {
			return false ;
		}

		values.keySet().removeIf( key -> key.equals( sourcePrefix) || key.startsWith( sourcePrefix + "/")) ;
		values.keySet().removeIf( key -> key.equals( targetPrefix) || key.startsWith( targetPrefix + "/")) ;
		values.putAll( movedMap) ;
		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスキーを作成します。<br><br>
	 *
	 * <p>メソッド名称： パスキー作成</p>
	 *
	 * @param path	対象パス
	 * @return パスキー
	 */
	//--------------------------------------------------------------------------
	private String createPathKey(Path path) {
		NfsExport export = findExport( path) ;

		// 公開定義が存在しない場合
		if( export == null) {
			return null ;
		}

		return createPathKey( export, path.toAbsolutePath().normalize()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスキーを作成します。<br><br>
	 *
	 * <p>メソッド名称： パスキー作成</p>
	 *
	 * @param export	公開定義
	 * @param path		対象パス
	 * @return パスキー
	 */
	//--------------------------------------------------------------------------
	private String createPathKey(NfsExport export, Path path) {
		// 公開ルート外の場合
		if( export == null || !path.startsWith( export.getPath())) {
			return null ;
		}

		Path relative = export.getPath().relativize( path) ;
		String normalizedRelative = relative.toString().replace( '\\', '/') ;
		return export.getName() + "/" + normalizedRelative ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスに対応する公開定義を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開定義取得</p>
	 *
	 * @param path	パス
	 * @return 公開定義
	 */
	//--------------------------------------------------------------------------
	private NfsExport findExport(Path path) {
		// パスが存在しない場合
		if( path == null) {
			return null ;
		}

		Path normalized = path.toAbsolutePath().normalize() ;

		// 公開定義を検索する
		for( NfsExport export : exports) {
			// 公開ルート内の場合
			if( normalized.startsWith( export.getPath())) {
				return export ;
			}
		}

		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 8進モードを変換します。<br><br>
	 *
	 * <p>メソッド名称： 8進モード変換</p>
	 *
	 * @param value			値
	 * @param defaultMode	既定モード
	 * @return モード
	 */
	//--------------------------------------------------------------------------
	private int parseOctalMode(String value, int defaultMode) {
		// 値がない場合
		if( value == null || value.isBlank()) {
			return defaultMode ;
		}

		try {
			return Integer.parseInt( value.trim(), 8) & MODE_MASK ;
		} catch( NumberFormatException nfex) {
			return defaultMode ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 32bit整数を変換します。<br><br>
	 *
	 * <p>メソッド名称： 32bit整数変換</p>
	 *
	 * @param value			値
	 * @param defaultValue	既定値
	 * @return 32bit整数
	 */
	//--------------------------------------------------------------------------
	private int parseInt32(String value, int defaultValue) {
		// 値がない場合
		if( value == null || value.isBlank()) {
			return defaultValue ;
		}

		try {
			return Integer.parseInt( value.trim()) ;
		} catch( NumberFormatException nfex) {
			try {
				long unsignedValue = Long.parseUnsignedLong( value.trim()) ;

				// 32bit範囲外の場合
				if( (unsignedValue & 0xffffffff00000000L) != 0L) {
					return defaultValue ;
				}

				return (int)unsignedValue ;
			} catch( NumberFormatException unsignedNfex) {
				return defaultValue ;
			}
		}
	}

}
