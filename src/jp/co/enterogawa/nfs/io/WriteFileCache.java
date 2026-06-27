package jp.co.enterogawa.nfs.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jp.co.enterogawa.nfs.config.NfsServerConfig;

//------------------------------------------------------------------------------
/**
 * 書込ファイルキャッシュクラスです。<br><br>
 *
 * <p>クラス名称： 書込ファイルキャッシュ</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class WriteFileCache implements AutoCloseable {
	//	内部定義	------------------------------------------------------------
	/** 有効有無 */
	private final boolean				enabled ;

	/** 最大オープンファイル数 */
	private final int					maxOpenFiles ;

	/** アイドル保持時間 */
	private final long					idleMillis ;

	/** エントリ */
	private final LinkedHashMap<Path, Entry> entries = new LinkedHashMap<Path, Entry>( 16, 0.75f, true) ;

	/** クリーナースレッド */
	private final Thread					cleanerThread ;

	/** 実行状態 */
	private volatile boolean				running ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param config	設定
	 */
	//--------------------------------------------------------------------------
	public WriteFileCache(NfsServerConfig config) {
		enabled = config.isWriteCacheEnabled() ;
		maxOpenFiles = config.getWriteCacheMaxOpenFiles() ;
		idleMillis = config.getWriteCacheIdleMillis() ;

		// キャッシュが有効な場合
		if( enabled) {
			running = true ;
			cleanerThread = new Thread( this::cleanerLoop, "tinywin-nfs-write-cache-cleaner") ;
			cleanerThread.setDaemon( true) ;
			cleanerThread.start() ;
		} else {
			cleanerThread = null ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルへ書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイル書込</p>
	 *
	 * @param path	対象パス
	 * @param offset	オフセット
	 * @param data		データ
	 * @param length	書込長
	 * @param sync		同期有無
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	public synchronized void write(Path path, long offset, byte[] data, int length, boolean sync) throws IOException {
		// キャッシュが無効な場合
		if( !enabled) {
			writeDirect( path, offset, data, length, sync) ;
			return ;
		}

		Path key = toKey( path) ;
		long now = System.currentTimeMillis() ;
		closeExpired( now) ;
		Entry entry = entries.get( key) ;

		// キャッシュに存在しない場合
		if( entry == null) {
			closeEldestUntilOpenable() ;
			entry = new Entry( key) ;
			entries.put( key, entry) ;
		}

		try {
			entry.write( offset, data, length) ;

			// 同期指定の場合
			if( sync) {
				entry.sync() ;
			}

			entry.touch( now) ;
		} catch( IOException ioex) {
			closeEntry( key, entry) ;
			throw ioex ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを同期します。<br><br>
	 *
	 * <p>メソッド名称： ファイル同期</p>
	 *
	 * @param path	対象パス
	 * @throws IOException 同期異常
	 */
	//--------------------------------------------------------------------------
	public synchronized void sync(Path path) throws IOException {
		Path key = toKey( path) ;
		Entry entry = entries.get( key) ;

		// キャッシュ済みの場合
		if( entry != null) {
			entry.sync() ;
			entry.touch( System.currentTimeMillis()) ;
			return ;
		}

		syncDirect( path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを閉じます。<br><br>
	 *
	 * <p>メソッド名称： ファイルクローズ</p>
	 *
	 * @param path	対象パス
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	public synchronized void close(Path path) throws IOException {
		Path key = toKey( path) ;
		Entry entry = entries.remove( key) ;

		// 対象がキャッシュに存在する場合
		if( entry != null) {
			entry.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリツリー配下のファイルを閉じます。<br><br>
	 *
	 * <p>メソッド名称： ツリー配下ファイルクローズ</p>
	 *
	 * @param path	対象パス
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	public synchronized void closeTree(Path path) throws IOException {
		Path root = toKey( path) ;
		Iterator<Map.Entry<Path, Entry>> iterator = entries.entrySet().iterator() ;

		while( iterator.hasNext()) {
			Map.Entry<Path, Entry> current = iterator.next() ;

			// 対象ツリー配下の場合
			if( current.getKey().startsWith( root)) {
				iterator.remove() ;
				current.getValue().close() ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * すべてのファイルを閉じます。<br><br>
	 *
	 * <p>メソッド名称： 全ファイルクローズ</p>
	 *
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	@Override
	public synchronized void close() throws IOException {
		running = false ;

		// クリーナースレッドが存在する場合
		if( cleanerThread != null) {
			cleanerThread.interrupt() ;
		}

		IOException failure = null ;

		for( Entry entry : entries.values()) {
			try {
				entry.close() ;
			} catch( IOException ioex) {
				failure = ioex ;
			}
		}

		entries.clear() ;

		// クローズ異常がある場合
		if( failure != null) {
			throw failure ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * クリーナーループを処理します。<br><br>
	 *
	 * <p>メソッド名称： クリーナーループ処理</p>
	 */
	//--------------------------------------------------------------------------
	private void cleanerLoop() {
		while( running) {
			try {
				Thread.sleep( getCleanupIntervalMillis()) ;

				synchronized( this) {
					closeExpired( System.currentTimeMillis()) ;
				}
			} catch( InterruptedException iex) {
				// 停止要求の場合
				if( !running) {
					return ;
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * クリーンアップ間隔を取得します。<br><br>
	 *
	 * <p>メソッド名称： クリーンアップ間隔取得</p>
	 *
	 * @return クリーンアップ間隔
	 */
	//--------------------------------------------------------------------------
	private long getCleanupIntervalMillis() {
		return Math.min( Math.max( 250L, idleMillis / 2), 1000L) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 直接書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 直接書込</p>
	 *
	 * @param path	対象パス
	 * @param offset	オフセット
	 * @param data		データ
	 * @param length	書込長
	 * @param sync		同期有無
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private void writeDirect(Path path, long offset, byte[] data, int length, boolean sync) throws IOException {
		try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "rw")) {
			file.seek( offset) ;
			file.write( data, 0, length) ;

			// 同期指定の場合
			if( sync) {
				file.getFD().sync() ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 直接同期します。<br><br>
	 *
	 * <p>メソッド名称： 直接同期</p>
	 *
	 * @param path	対象パス
	 * @throws IOException 同期異常
	 */
	//--------------------------------------------------------------------------
	private void syncDirect(Path path) throws IOException {
		// 通常ファイルではない場合
		if( !Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS)) {
			return ;
		}

		try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "r")) {
			file.getFD().sync() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 期限切れエントリを閉じます。<br><br>
	 *
	 * <p>メソッド名称： 期限切れエントリクローズ</p>
	 *
	 * @param now	現在時刻
	 */
	//--------------------------------------------------------------------------
	private void closeExpired(long now) {
		Iterator<Map.Entry<Path, Entry>> iterator = entries.entrySet().iterator() ;

		while( iterator.hasNext()) {
			Map.Entry<Path, Entry> current = iterator.next() ;

			// アイドル時間内の場合
			if( now - current.getValue().getLastAccessMillis() <= idleMillis) {
				continue ;
			}

			iterator.remove() ;
			closeQuietly( current.getValue()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 新規オープンできるまで古いエントリを閉じます。<br><br>
	 *
	 * <p>メソッド名称： 古いエントリクローズ</p>
	 */
	//--------------------------------------------------------------------------
	private void closeEldestUntilOpenable() {
		while( entries.size() >= maxOpenFiles) {
			Iterator<Map.Entry<Path, Entry>> iterator = entries.entrySet().iterator() ;

			// エントリが存在しない場合
			if( !iterator.hasNext()) {
				return ;
			}

			Map.Entry<Path, Entry> eldest = iterator.next() ;
			iterator.remove() ;
			closeQuietly( eldest.getValue()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * エントリを閉じます。<br><br>
	 *
	 * <p>メソッド名称： エントリクローズ</p>
	 *
	 * @param key	キー
	 * @param entry	エントリ
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	private void closeEntry(Path key, Entry entry) throws IOException {
		entries.remove( key) ;
		entry.close() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * エントリを静かに閉じます。<br><br>
	 *
	 * <p>メソッド名称： 静的エントリクローズ</p>
	 *
	 * @param entry	エントリ
	 */
	//--------------------------------------------------------------------------
	private void closeQuietly(Entry entry) {
		try {
			entry.close() ;
		} catch( IOException ioex) {
			// キャッシュ退避時のクローズ異常は次回I/Oで検出する
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * キャッシュキーへ変換します。<br><br>
	 *
	 * <p>メソッド名称： キャッシュキー変換</p>
	 *
	 * @param path	パス
	 * @return キャッシュキー
	 */
	//--------------------------------------------------------------------------
	private Path toKey(Path path) {
		return path.toAbsolutePath().normalize() ;
	}

	//------------------------------------------------------------------------------
	/**
	 * 書込ファイルキャッシュエントリクラスです。<br><br>
	 *
	 * <p>クラス名称： 書込ファイルキャッシュエントリ</p>
	 */
	//------------------------------------------------------------------------------
	private static class Entry {
		/** ファイル */
		private final RandomAccessFile	file ;

		/** 最終アクセス時刻 */
		private long					lastAccessMillis ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param path	パス
		 * @throws IOException オープン異常
		 */
		//----------------------------------------------------------------------
		Entry(Path path) throws IOException {
			file = new RandomAccessFile( path.toFile(), "rw") ;
			lastAccessMillis = System.currentTimeMillis() ;
		}

		//----------------------------------------------------------------------
		/**
		 * 書き込みます。<br><br>
		 *
		 * <p>メソッド名称： 書込</p>
		 *
		 * @param offset	オフセット
		 * @param data		データ
		 * @param length	書込長
		 * @throws IOException 書込異常
		 */
		//----------------------------------------------------------------------
		void write(long offset, byte[] data, int length) throws IOException {
			file.seek( offset) ;
			file.write( data, 0, length) ;
		}

		//----------------------------------------------------------------------
		/**
		 * 同期します。<br><br>
		 *
		 * <p>メソッド名称： 同期</p>
		 *
		 * @throws IOException 同期異常
		 */
		//----------------------------------------------------------------------
		void sync() throws IOException {
			file.getFD().sync() ;
		}

		//----------------------------------------------------------------------
		/**
		 * 閉じます。<br><br>
		 *
		 * <p>メソッド名称： クローズ</p>
		 *
		 * @throws IOException クローズ異常
		 */
		//----------------------------------------------------------------------
		void close() throws IOException {
			file.close() ;
		}

		//----------------------------------------------------------------------
		/**
		 * 最終アクセス時刻を更新します。<br><br>
		 *
		 * <p>メソッド名称： 最終アクセス時刻更新</p>
		 *
		 * @param value	時刻
		 */
		//----------------------------------------------------------------------
		void touch(long value) {
			lastAccessMillis = value ;
		}

		//----------------------------------------------------------------------
		/**
		 * 最終アクセス時刻を取得します。<br><br>
		 *
		 * <p>メソッド名称： 最終アクセス時刻取得</p>
		 *
		 * @return 最終アクセス時刻
		 */
		//----------------------------------------------------------------------
		long getLastAccessMillis() {
			return lastAccessMillis ;
		}
	}
}
