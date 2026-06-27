package jp.co.enterogawa.nfs.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jp.co.enterogawa.nfs.config.NfsExport;

//------------------------------------------------------------------------------
/**
 * NFSファイルハンドル管理クラスです。<br><br>
 *
 * <p>クラス名称： NFSファイルハンドル管理</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class FileHandleTable {
	//	定数定義	------------------------------------------------------------
	/** ファイルハンドル識別子 */
	private static final byte[]			MAGIC = "JNFS".getBytes( StandardCharsets.US_ASCII) ;

	/** ルートID */
	private static final int				ROOT_ID = 1 ;

	/** 複数公開ルートID基底値 */
	private static final int				ROOT_ID_BASE = 0x40000000 ;

	//	内部定義	------------------------------------------------------------
	/** 公開定義 */
	private final List<NfsExport>		exports ;

	/** 永続ファイル */
	private final Path					storePath ;

	/** 次ID */
	private final AtomicInteger			nextId = new AtomicInteger( 2) ;

	/** パス別ファイルハンドル */
	private final Map<Path, FileHandle>	pathMap = new HashMap<Path, FileHandle>() ;

	/** ファイルハンドル別パス */
	private final Map<String, Path>		handleMap = new HashMap<String, Path>() ;

	/** 公開名別ルートファイルハンドル */
	private final Map<String, FileHandle> rootHandleMap = new HashMap<String, FileHandle>() ;

	/** ルートID */
	private final Set<Integer>			rootIds = new HashSet<Integer>() ;

	/** ルートファイルハンドル */
	private final FileHandle				rootHandle ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param rootPath	公開ルート
	 */
	//--------------------------------------------------------------------------
	public FileHandleTable(Path rootPath) {
		this( List.of( new NfsExport( "/export", rootPath, true))) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param exports	公開定義
	 */
	//--------------------------------------------------------------------------
	public FileHandleTable(List<NfsExport> exports) {
		// 公開定義が存在しない場合
		if( exports == null || exports.isEmpty()) {
			throw new IllegalArgumentException( "At least one export is required.") ;
		}

		this.exports = List.copyOf( exports) ;
		storePath = resolveStorePath( this.exports) ;
		FileHandle firstHandle = null ;
		int index = 0 ;

		// 公開ルートのファイルハンドルを登録する
		for( NfsExport export : this.exports) {
			int id = index == 0 ? ROOT_ID : ROOT_ID_BASE + index ;
			FileHandle handle = createHandle( id) ;
			rootIds.add( id) ;
			rootHandleMap.put( export.getName(), handle) ;
			pathMap.put( export.getPath(), handle) ;
			handleMap.put( handle.key(), export.getPath()) ;

			// 先頭公開ルートの場合
			if( firstHandle == null) {
				firstHandle = handle ;
			}

			index++ ;
		}

		rootHandle = firstHandle ;
		loadStore() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ルートファイルハンドルを取得します。<br><br>
	 *
	 * <p>メソッド名称： ルートファイルハンドル取得</p>
	 *
	 * @return ルートファイルハンドル
	 */
	//--------------------------------------------------------------------------
	public FileHandle getRootHandle() {
		return rootHandle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開名に対応するルートファイルハンドルを取得します。<br><br>
	 *
	 * <p>メソッド名称： ルートファイルハンドル取得</p>
	 *
	 * @param exportName	公開名
	 * @return ルートファイルハンドル
	 */
	//--------------------------------------------------------------------------
	public FileHandle getRootHandle(String exportName) {
		return rootHandleMap.get( exportName) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開ルートを取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開ルート取得</p>
	 *
	 * @return 公開ルート
	 */
	//--------------------------------------------------------------------------
	public Path getRootPath() {
		return exports.get( 0).getPath() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスに対応する公開ルートを取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開ルート取得</p>
	 *
	 * @param path	パス
	 * @return 公開ルート
	 */
	//--------------------------------------------------------------------------
	public Path getRootPath(Path path) {
		NfsExport export = findExport( path) ;

		// 公開定義が存在しない場合
		if( export == null) {
			return getRootPath() ;
		}

		return export.getPath() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスが公開ルート内かを確認します。<br><br>
	 *
	 * <p>メソッド名称： 公開ルート内確認</p>
	 *
	 * @param path	パス
	 * @return true:公開ルート内 false:公開ルート外
	 */
	//--------------------------------------------------------------------------
	public boolean isInExportRoot(Path path) {
		return findExport( path) != null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 2つのパスが同じ公開ルート内かを確認します。<br><br>
	 *
	 * <p>メソッド名称： 同一公開ルート確認</p>
	 *
	 * @param first		パス1
	 * @param second	パス2
	 * @return true:同一公開ルート false:異なる公開ルート
	 */
	//--------------------------------------------------------------------------
	public boolean isSameExport(Path first, Path second) {
		NfsExport firstExport = findExport( first) ;
		NfsExport secondExport = findExport( second) ;
		return firstExport != null && firstExport == secondExport ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスの書込可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 書込可否取得</p>
	 *
	 * @param path	パス
	 * @return true:書込可 false:読込専用
	 */
	//--------------------------------------------------------------------------
	public boolean isWritable(Path path) {
		NfsExport export = findExport( path) ;
		return export != null && export.isWritable() ;
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
	public NfsExport getExport(Path path) {
		return findExport( path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルに対応するパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： パス取得</p>
	 *
	 * @param handle	ファイルハンドル
	 * @return パス
	 */
	//--------------------------------------------------------------------------
	public synchronized Path getPath(FileHandle handle) {
		return handleMap.get( handle.key()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルを取得または作成します。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル取得作成</p>
	 *
	 * @param path	パス
	 * @return ファイルハンドル
	 */
	//--------------------------------------------------------------------------
	public synchronized FileHandle getOrCreate(Path path) {
		Path normalized = path.toAbsolutePath().normalize() ;
		NfsExport export = findExport( normalized) ;

		// 公開ルート外の場合
		if( export == null) {
			throw new IllegalArgumentException( "Path is outside export root.") ;
		}

		FileHandle handle = pathMap.get( normalized) ;

		// 既存ファイルハンドルが存在する場合
		if( handle != null) {
			return handle ;
		}

		handle = createHandle( nextFileId()) ;
		pathMap.put( normalized, handle) ;
		handleMap.put( handle.key(), normalized) ;
		saveStore() ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルを忘却します。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル忘却</p>
	 *
	 * @param path	対象パス
	 */
	//--------------------------------------------------------------------------
	public synchronized void forget(Path path) {
		Path normalized = path.toAbsolutePath().normalize() ;
		FileHandle handle = pathMap.remove( normalized) ;

		// ハンドルが存在する場合
		if( handle != null) {
			handleMap.remove( handle.key()) ;
			saveStore() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルのパスを移動します。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドルパス移動</p>
	 *
	 * @param source	移動元
	 * @param target	移動先
	 */
	//--------------------------------------------------------------------------
	public synchronized void move(Path source, Path target) {
		Path sourcePath = source.toAbsolutePath().normalize() ;
		Path targetPath = target.toAbsolutePath().normalize() ;
		Map<Path, Path> targetMap = new HashMap<Path, Path>() ;
		Map<Path, FileHandle> handleMoveMap = new HashMap<Path, FileHandle>() ;

		// 公開ルート外の場合
		if( !isSameExport( sourcePath, targetPath)) {
			throw new IllegalArgumentException( "Path is outside export root.") ;
		}

		// 移動対象のファイルハンドルを集める
		for( Map.Entry<Path, FileHandle> entry : pathMap.entrySet()) {
			Path entryPath = entry.getKey() ;

			// 移動元配下の場合
			if( entryPath.startsWith( sourcePath)) {
				Path relativePath = sourcePath.relativize( entryPath) ;
				Path movedPath = targetPath.resolve( relativePath).normalize() ;
				targetMap.put( entryPath, movedPath) ;
				handleMoveMap.put( movedPath, entry.getValue()) ;
			}
		}

		// 既存の移動対象ファイルハンドルを削除する
		for( Path movedSource : targetMap.keySet()) {
			FileHandle oldHandle = pathMap.remove( movedSource) ;

			// ハンドルが存在する場合
			if( oldHandle != null) {
				handleMap.remove( oldHandle.key()) ;
			}
		}

		// 上書き先の既存ファイルハンドルを削除する
		for( Path movedTarget : handleMoveMap.keySet()) {
			FileHandle overwrittenHandle = pathMap.remove( movedTarget) ;

			// ハンドルが存在する場合
			if( overwrittenHandle != null) {
				handleMap.remove( overwrittenHandle.key()) ;
			}
		}

		// 移動後のファイルハンドルを登録する
		for( Map.Entry<Path, FileHandle> entry : handleMoveMap.entrySet()) {
			pathMap.put( entry.getKey(), entry.getValue()) ;
			handleMap.put( entry.getValue().key(), entry.getKey()) ;
		}

		saveStore() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイルID取得</p>
	 *
	 * @param handle	ファイルハンドル
	 * @return ファイルID
	 */
	//--------------------------------------------------------------------------
	public int getFileId(FileHandle handle) {
		ByteBuffer buffer = ByteBuffer.wrap( handle.getValue()) ;
		buffer.position( 4) ;
		return buffer.getInt() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルを作成します。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル作成</p>
	 *
	 * @param id	ID
	 * @return ファイルハンドル
	 */
	//--------------------------------------------------------------------------
	private FileHandle createHandle(int id) {
		byte[] value = new byte[FileHandle.LENGTH] ;
		System.arraycopy( MAGIC, 0, value, 0, MAGIC.length) ;
		ByteBuffer.wrap( value).putInt( 4, id) ;
		return new FileHandle( value) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 永続ファイルパスを解決します。<br><br>
	 *
	 * <p>メソッド名称： 永続ファイルパス解決</p>
	 *
	 * @param rootPath	公開ルート
	 * @return 永続ファイルパス
	 */
	//--------------------------------------------------------------------------
	private Path resolveStorePath(List<NfsExport> exports) {
		String dataPath = System.getProperty( "tinywin.nfs.data", "data") ;
		StringBuilder key = new StringBuilder() ;

		// 単一公開定義の場合
		if( exports.size() == 1) {
			key.append( exports.get( 0).getPath()) ;
		}
		// 複数公開定義の場合
		else {
			// 公開定義から永続化キーを作成する
			for( NfsExport export : exports) {
				key.append( export.getName()).append( "=" ).append( export.getPath()).append( "|" ) ;
			}
		}

		return Path.of( dataPath).resolve( "handles-" + hashText( key.toString()) + ".properties").toAbsolutePath().normalize() ;
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
			int maxId = ROOT_ID ;

			for( String key : properties.stringPropertyNames()) {
				// パス定義以外の場合
				if( !key.startsWith( "path.")) {
					continue ;
				}

				int id = Integer.parseInt( key.substring( "path.".length())) ;

				// ルートの場合
				if( rootIds.contains( id)) {
					continue ;
				}

				NfsExport export = findExportByName( properties.getProperty( "root." + id, exports.get( 0).getName())) ;

				// 公開定義が存在しない場合
				if( export == null) {
					continue ;
				}

				Path path = export.getPath().resolve( properties.getProperty( key)).toAbsolutePath().normalize() ;

				// 公開ルート外の場合
				if( !path.startsWith( export.getPath())) {
					continue ;
				}

				FileHandle handle = createHandle( id) ;
				pathMap.put( path, handle) ;
				handleMap.put( handle.key(), path) ;
				maxId = Math.max( maxId, id) ;
			}

			int storedNextId = parseInt( properties.getProperty( "nextId"), maxId + 1) ;
			nextId.set( Math.max( storedNextId, maxId + 1)) ;
		} catch( IOException | RuntimeException ex) {
			System.err.println( "Failed to load file handle store: " + ex.getMessage()) ;
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
		properties.setProperty( "nextId", Integer.toString( nextId.get())) ;

		for( Map.Entry<Path, FileHandle> entry : pathMap.entrySet()) {
			int id = getFileId( entry.getValue()) ;

			// ルートの場合
			if( rootIds.contains( id)) {
				continue ;
			}

			NfsExport export = findExport( entry.getKey()) ;

			// 公開定義が存在しない場合
			if( export == null) {
				continue ;
			}

			Path relativePath = export.getPath().relativize( entry.getKey()) ;
			properties.setProperty( "root." + id, export.getName()) ;
			properties.setProperty( "path." + id, relativePath.toString().replace( '\\', '/')) ;
		}

		try {
			Path parent = storePath.getParent() ;

			// 親ディレクトリが存在する場合
			if( parent != null) {
				Files.createDirectories( parent) ;
			}

			try( OutputStream output = Files.newOutputStream( storePath)) {
				properties.store( output, "TinyWinNFS file handles") ;
			}
		} catch( IOException ex) {
			System.err.println( "Failed to save file handle store: " + ex.getMessage()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 数値を変換します。<br><br>
	 *
	 * <p>メソッド名称： 数値変換</p>
	 *
	 * @param value			値
	 * @param defaultValue	デフォルト値
	 * @return 数値
	 */
	//--------------------------------------------------------------------------
	private int parseInt(String value, int defaultValue) {
		// 値がない場合
		if( value == null || value.isBlank()) {
			return defaultValue ;
		}

		try {
			return Integer.parseInt( value.trim()) ;
		} catch( NumberFormatException nfex) {
			return defaultValue ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 次ファイルIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： 次ファイルID取得</p>
	 *
	 * @return ファイルID
	 */
	//--------------------------------------------------------------------------
	private int nextFileId() {
		int id = nextId.getAndIncrement() ;

		// ルートID帯と衝突する場合
		while( rootIds.contains( id)) {
			id = nextId.getAndIncrement() ;
		}

		return id ;
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
	 * 公開名に対応する公開定義を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開定義取得</p>
	 *
	 * @param name	公開名
	 * @return 公開定義
	 */
	//--------------------------------------------------------------------------
	private NfsExport findExportByName(String name) {
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
	 * テキストをハッシュ化します。<br><br>
	 *
	 * <p>メソッド名称： テキストハッシュ化</p>
	 *
	 * @param value	値
	 * @return ハッシュ文字列
	 */
	//--------------------------------------------------------------------------
	private String hashText(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance( "SHA-256") ;
			byte[] hash = digest.digest( value.getBytes( StandardCharsets.UTF_8)) ;
			StringBuilder builder = new StringBuilder() ;

			for( int i = 0; i < 8; i++) {
				builder.append( String.format( "%02x", hash[i] & 0xff)) ;
			}

			return builder.toString() ;
		} catch( NoSuchAlgorithmException nsaex) {
			return Integer.toHexString( value.hashCode()) ;
		}
	}
}
