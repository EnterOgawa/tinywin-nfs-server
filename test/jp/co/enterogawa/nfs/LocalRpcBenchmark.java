package jp.co.enterogawa.nfs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.program.NfsStatus;
import jp.co.enterogawa.nfs.program.NfsV2Program;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcRequestContext;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * ローカルRPCベンチマーククラスです。<br><br>
 *
 * <p>クラス名称： ローカルRPCベンチマーク</p>
 *
 * <p>NFSv2手続きをプロセス内で呼び出して測定します。
 * Windowsサービスの起動やネットワークポートへのバインドは行いません。</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class LocalRpcBenchmark {
	//	Constants	------------------------------------------------------------
	/** NFSプログラム番号 */
	private static final int				PROGRAM_NFS = RpcConstants.PROGRAM_NFS ;

	/** NFSバージョン */
	private static final int				NFS_VERSION = 2 ;

	/** ローカルRPC要求のトランザクションID */
	private static final int				XID = 0x11223344 ;

	/** レポートファイル名の日時書式 */
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern( "yyyyMMdd-HHmmss") ;

	//	Internal fields	--------------------------------------------------------
	/** 測定オプション */
	private final Options				options ;

	/** 測定対象のNFSv2プログラム */
	private NfsV2Program					program ;

	/** 測定用ファイルハンドル表 */
	private FileHandleTable				handleTable ;

	/** 測定用の共有ルート */
	private Path						root ;

	/** 操作ごとの測定結果 */
	private final List<Result>			results = new ArrayList<Result>() ;

	//--------------------------------------------------------------------------
	/**
	 * Starts the benchmark.
	 *
	 * @param args arguments
	 * @throws Exception benchmark failure
	 */
	//--------------------------------------------------------------------------
	public static void main(String[] args) throws Exception {
		Options options = Options.parse( args) ;
		new LocalRpcBenchmark( options).run() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Creates a new benchmark runner.
	 *
	 * @param options options
	 */
	//--------------------------------------------------------------------------
	private LocalRpcBenchmark(Options options) {
		this.options = options ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Runs the benchmark.
	 *
	 * @throws Exception benchmark failure
	 */
	//--------------------------------------------------------------------------
	private void run() throws Exception {
		long started = System.nanoTime() ;
		Path workRoot = Path.of( "work", "tmp", "local-rpc-benchmark").toAbsolutePath().normalize() ;
		root = workRoot.resolve( "export") ;
		Path data = workRoot.resolve( "data") ;
		Path configPath = workRoot.resolve( "nfs-server.properties") ;
		deleteDirectory( workRoot) ;
		Files.createDirectories( root) ;
		Files.createDirectories( data) ;
		System.setProperty( "tinywin.nfs.data", data.toString()) ;

		try {
			Files.writeString( configPath, createConfigText( root), StandardCharsets.UTF_8) ;
			NfsServerConfig config = NfsServerConfig.load( configPath) ;
			handleTable = new FileHandleTable( config.getExports()) ;
			program = new NfsV2Program( config, handleTable) ;
			record( "初期化", started, System.nanoTime(), 0) ;

			long deadline = options.durationSeconds <= 0
					? 0L
					: System.nanoTime() + options.durationSeconds * 1_000_000_000L ;
			int loop = 0 ;

			while( true) {
				loop++ ;
				runOneLoop( loop) ;

				// 回数指定の測定で繰り返し回数に達した場合
				if( !options.longRun && loop >= options.loops) {
					break ;
				}

				// 長時間測定の終了時刻に達した場合
				if( options.longRun && System.nanoTime() >= deadline) {
					break ;
				}
			}
		} finally {
			// NFS プログラムの初期化が完了している場合
			if( program != null) {
				program.close() ;
			}
		}

		writeReports() ;
		System.out.println( "Benchmark report: " + options.outputDirectory.toAbsolutePath().normalize()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Runs one benchmark loop.
	 *
	 * @param loop loop number
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private void runOneLoop(int loop) throws IOException {
		FileHandle rootHandle = handleTable.getRootHandle() ;
		List<DirectoryNode> directories = createDirectories( rootHandle, loop) ;
		List<FileEntry> files = new ArrayList<FileEntry>() ;
		long started ;

		started = System.nanoTime() ;
		// 指定した数のファイルを作成し、データを書き込む
		for( int i = 0; i < options.files; i++) {
			DirectoryNode directory = directories.get( i % directories.size()) ;
			String name = String.format( "file-%05d.bin", i) ;
			FileHandle handle = createFile( directory.handle, name) ;
			writeFile( handle, createData( i)) ;
			files.add( new FileEntry( directory, name)) ;
		}
		record( "作成と書込 loop=" + loop, started, System.nanoTime(), files.size()) ;

		started = System.nanoTime() ;
		// 作成した全ファイルの LOOKUP を測定する
		for( FileEntry file : files) {
			lookup( file.directory.handle, file.name) ;
		}
		record( "LOOKUP loop=" + loop, started, System.nanoTime(), files.size()) ;

		started = System.nanoTime() ;
		int readdirCount = 0 ;
		// 作成した全ディレクトリの READDIR を測定する
		for( DirectoryNode directory : directories) {
			readdirCount += readDirectory( directory.handle) ;
		}
		record( "READDIR loop=" + loop, started, System.nanoTime(), readdirCount) ;

		started = System.nanoTime() ;
		// 作成した全ファイルの RENAME を測定する
		for( FileEntry file : files) {
			String targetName = file.name + ".renamed" ;
			rename( file.directory.handle, file.name, file.directory.handle, targetName) ;
			file.name = targetName ;
		}
		record( "RENAME loop=" + loop, started, System.nanoTime(), files.size()) ;

		started = System.nanoTime() ;
		// 作成した全ファイルを削除する
		for( FileEntry file : files) {
			remove( file.directory.handle, file.name) ;
		}
		// 子ディレクトリから親ディレクトリの順に削除する
		for( int i = directories.size() - 1; i >= 0; i--) {
			DirectoryNode directory = directories.get( i) ;
			// 測定用のルート以外のディレクトリである場合
			if( directory.parent != null) {
				rmdir( directory.parent.handle, directory.name) ;
			}
		}
		record( "削除 loop=" + loop, started, System.nanoTime(), files.size() + directories.size()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Creates benchmark directories.
	 *
	 * @param rootHandle root handle
	 * @param loop loop number
	 * @return leaf directories
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private List<DirectoryNode> createDirectories(FileHandle rootHandle, int loop) throws IOException {
		int directoryCount = Math.max( 1, options.directories) ;
		int depth = Math.max( 1, options.depth) ;
		List<DirectoryNode> leaves = new ArrayList<DirectoryNode>() ;

		// 指定した数のディレクトリツリーを作成する
		for( int i = 0; i < directoryCount; i++) {
			DirectoryNode parent = new DirectoryNode( null, "root", rootHandle) ;

			// 指定した深さまで子ディレクトリを作成する
			for( int level = 0; level < depth; level++) {
				String name = String.format( "loop-%03d-dir-%03d-level-%02d", loop, i, level) ;
				FileHandle handle = mkdir( parent.handle, name) ;
				parent = new DirectoryNode( parent, name, handle) ;
			}

			addAncestors( leaves, parent) ;
		}

		return leaves ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Adds directory ancestors in parent-first order.
	 *
	 * @param directories directories
	 * @param leaf leaf directory
	 */
	//--------------------------------------------------------------------------
	private void addAncestors(List<DirectoryNode> directories, DirectoryNode leaf) {
		List<DirectoryNode> stack = new ArrayList<DirectoryNode>() ;
		DirectoryNode current = leaf ;

		while( current != null && current.parent != null) {
			stack.add( 0, current) ;
			current = current.parent ;
		}

		// 祖先ディレクトリを親から子の順に測定対象へ追加する
		for( DirectoryNode node : stack) {
			// 測定対象にまだ登録されていないディレクトリの場合
			if( !directories.contains( node)) {
				directories.add( node) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * Creates file data.
	 *
	 * @param index file index
	 * @return file data
	 */
	//--------------------------------------------------------------------------
	private byte[] createData(int index) {
		int range = Math.max( 1, options.maxSize - options.minSize + 1) ;
		int size = options.minSize + (index % range) ;
		byte[] data = new byte[size] ;

		// ファイル番号と位置から再現可能な書き込みデータを生成する
		for( int i = 0; i < data.length; i++) {
			data[i] = (byte)('A' + ((index + i) % 26)) ;
		}

		return data ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls MKDIR.
	 *
	 * @param parent parent handle
	 * @param name directory name
	 * @return directory handle
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private FileHandle mkdir(FileHandle parent, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, parent, name) ;
		writeSattrUnset( arguments) ;
		XdrReader reader = call( 14, arguments) ;
		expectStatus( "MKDIR", reader, NfsStatus.OK) ;
		return new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls CREATE.
	 *
	 * @param parent parent handle
	 * @param name file name
	 * @return file handle
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private FileHandle createFile(FileHandle parent, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, parent, name) ;
		writeSattrUnset( arguments) ;
		XdrReader reader = call( 9, arguments) ;
		expectStatus( "CREATE", reader, NfsStatus.OK) ;
		return new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls WRITE.
	 *
	 * @param handle file handle
	 * @param data data
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private void writeFile(FileHandle handle, byte[] data) throws IOException {
		int offset = 0 ;

		while( offset < data.length) {
			int length = Math.min( 8192, data.length - offset) ;
			byte[] chunk = new byte[length] ;
			System.arraycopy( data, offset, chunk, 0, length) ;
			XdrWriter arguments = new XdrWriter() ;
			arguments.writeFixedOpaque( handle.getValue()) ;
			arguments.writeUnsignedInt( offset) ;
			arguments.writeUnsignedInt( offset) ;
			arguments.writeUnsignedInt( length) ;
			arguments.writeOpaque( chunk) ;
			XdrReader reader = call( 8, arguments) ;
			expectStatus( "WRITE", reader, NfsStatus.OK) ;
			offset += length ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls LOOKUP.
	 *
	 * @param parent parent handle
	 * @param name file name
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private void lookup(FileHandle parent, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, parent, name) ;
		XdrReader reader = call( 4, arguments) ;
		expectStatus( "LOOKUP", reader, NfsStatus.OK) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls READDIR until EOF.
	 *
	 * @param directory directory handle
	 * @return entry count
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private int readDirectory(FileHandle directory) throws IOException {
		int cookie = 0 ;
		int entries = 0 ;
		boolean eof = false ;

		while( !eof) {
			XdrWriter arguments = new XdrWriter() ;
			arguments.writeFixedOpaque( directory.getValue()) ;
			arguments.writeInt( cookie) ;
			arguments.writeInt( 4096) ;
			XdrReader reader = call( 16, arguments) ;
			expectStatus( "READDIR", reader, NfsStatus.OK) ;
			boolean any = false ;

			while( reader.readBoolean()) {
				reader.readUnsignedInt() ;
				reader.readString() ;
				cookie = (int)reader.readUnsignedInt() ;
				entries++ ;
				any = true ;
			}

			eof = reader.readBoolean() ;

			// エントリがなく EOF でもない不正な READDIR 応答の場合
			if( !any && !eof) {
				throw new IOException( "READDIR returned no entries without EOF." ) ;
			}
		}

		return entries ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls RENAME.
	 *
	 * @param fromDirectory source directory
	 * @param fromName source name
	 * @param toDirectory target directory
	 * @param toName target name
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private void rename(FileHandle fromDirectory, String fromName, FileHandle toDirectory, String toName) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, fromDirectory, fromName) ;
		writeDiropargs( arguments, toDirectory, toName) ;
		XdrReader reader = call( 11, arguments) ;
		expectStatus( "RENAME", reader, NfsStatus.OK) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls REMOVE.
	 *
	 * @param parent parent handle
	 * @param name file name
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private void remove(FileHandle parent, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, parent, name) ;
		XdrReader reader = call( 10, arguments) ;
		expectStatus( "REMOVE", reader, NfsStatus.OK) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls RMDIR.
	 *
	 * @param parent parent handle
	 * @param name directory name
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private void rmdir(FileHandle parent, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, parent, name) ;
		XdrReader reader = call( 15, arguments) ;
		expectStatus( "RMDIR", reader, NfsStatus.OK) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Calls the local NFS program.
	 *
	 * @param procedure procedure number
	 * @param arguments arguments
	 * @return response reader
	 * @throws IOException I/O failure
	 */
	//--------------------------------------------------------------------------
	private XdrReader call(int procedure, XdrWriter arguments) throws IOException {
		byte[] request = createCallBytes( procedure, arguments) ;
		RpcCall call = RpcCall.read( request, request.length) ;
		XdrWriter response = new XdrWriter() ;
		RpcRequestContext context = new RpcRequestContext(
				"127.0.0.1",
				12345,
				"local-rpc-benchmark",
				XID,
				PROGRAM_NFS,
				NFS_VERSION,
				procedure,
				call.getCredential()) ;
		int status = program.handle( call, context, response) ;

		// RPC 要求が受け付けられなかった場合
		if( status != RpcConstants.ACCEPT_SUCCESS) {
			throw new IOException( "RPC accept status=" + status + " procedure=" + procedure) ;
		}

		return new XdrReader( response.toByteArray()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Creates RPC call bytes.
	 *
	 * @param procedure procedure number
	 * @param arguments arguments
	 * @return call bytes
	 */
	//--------------------------------------------------------------------------
	private byte[] createCallBytes(int procedure, XdrWriter arguments) {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( XID) ;
		writer.writeInt( RpcConstants.MSG_CALL) ;
		writer.writeInt( RpcConstants.RPC_VERSION) ;
		writer.writeInt( PROGRAM_NFS) ;
		writer.writeInt( NFS_VERSION) ;
		writer.writeInt( procedure) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeFixedOpaque( arguments.toByteArray()) ;
		return writer.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Writes diropargs.
	 *
	 * @param writer writer
	 * @param directory directory handle
	 * @param name name
	 */
	//--------------------------------------------------------------------------
	private void writeDiropargs(XdrWriter writer, FileHandle directory, String name) {
		writer.writeFixedOpaque( directory.getValue()) ;
		writer.writeString( name) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Writes unset sattr.
	 *
	 * @param writer writer
	 */
	//--------------------------------------------------------------------------
	private void writeSattrUnset(XdrWriter writer) {
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Expects a NFS status.
	 *
	 * @param operation operation name
	 * @param reader response reader
	 * @param expected expected status
	 * @throws IOException status mismatch
	 */
	//--------------------------------------------------------------------------
	private void expectStatus(String operation, XdrReader reader, int expected) throws IOException {
		int actual = reader.readInt() ;

		// NFS ステータスが期待値と異なる場合
		if( actual != expected) {
			throw new IOException( operation + " failed. status=" + actual + " expected=" + expected) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * Records elapsed time.
	 *
	 * @param name name
	 * @param started started time
	 * @param finished finished time
	 * @param operations operation count
	 */
	//--------------------------------------------------------------------------
	private void record(String name, long started, long finished, int operations) {
		results.add( new Result( name, operations, finished - started, usedMemoryBytes())) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Writes report files.
	 *
	 * @throws IOException write failure
	 */
	//--------------------------------------------------------------------------
	private void writeReports() throws IOException {
		Files.createDirectories( options.outputDirectory) ;
		String timestamp = TIMESTAMP_FORMAT.format( LocalDateTime.now()) ;
		Path csvPath = options.outputDirectory.resolve( "local-rpc-benchmark-" + timestamp + ".csv") ;
		Path markdownPath = options.outputDirectory.resolve( "local-rpc-benchmark-" + timestamp + ".md") ;
		StringBuilder csv = new StringBuilder() ;
		StringBuilder markdown = new StringBuilder() ;

		csv.append( "name,operations,elapsedMillis,opsPerSecond,usedMemoryBytes\n" ) ;
		markdown.append( "# ローカルRPCベンチマーク結果\n\n" ) ;
		markdown.append( "| 項目 | 値 |\n" ) ;
		markdown.append( "| --- | --- |\n" ) ;
		markdown.append( "| files | " ).append( options.files).append( " |\n" ) ;
		markdown.append( "| directories | " ).append( options.directories).append( " |\n" ) ;
		markdown.append( "| depth | " ).append( options.depth).append( " |\n" ) ;
		markdown.append( "| size | " ).append( options.minSize).append( "-" ).append( options.maxSize).append( " bytes |\n" ) ;
		markdown.append( "| longRun | " ).append( options.longRun).append( " |\n\n" ) ;
		markdown.append( "| 処理 | 件数 | 経過ms | 件/秒 | 使用メモリbytes |\n" ) ;
		markdown.append( "| --- | ---: | ---: | ---: | ---: |\n" ) ;

		// 各測定結果を Markdown と CSV の行に変換する
		for( Result result : results) {
			double millis = result.elapsedNanos / 1_000_000.0 ;
			double opsPerSecond = result.operations == 0 || result.elapsedNanos == 0
					? 0.0
					: result.operations * 1_000_000_000.0 / result.elapsedNanos ;
			csv.append( escapeCsv( result.name)).append( "," )
					.append( result.operations).append( "," )
					.append( String.format( "%.3f", millis)).append( "," )
					.append( String.format( "%.3f", opsPerSecond)).append( "," )
					.append( result.usedMemoryBytes).append( "\n" ) ;
			markdown.append( "| " ).append( result.name).append( " | " )
					.append( result.operations).append( " | " )
					.append( String.format( "%.3f", millis)).append( " | " )
					.append( String.format( "%.3f", opsPerSecond)).append( " | " )
					.append( result.usedMemoryBytes).append( " |\n" ) ;
		}

		Files.writeString( csvPath, csv.toString(), StandardCharsets.UTF_8) ;
		Files.writeString( markdownPath, markdown.toString(), StandardCharsets.UTF_8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Creates config text.
	 *
	 * @param exportPath export path
	 * @return config text
	 */
	//--------------------------------------------------------------------------
	private String createConfigText(Path exportPath) {
		return ""
				+ "portmap.port=11111\n"
				+ "nfs.port=12049\n"
				+ "mount.port=12048\n"
				+ "rpc.udp.workers=" + options.udpWorkers + "\n"
				+ "rpc.udp.queue.size=" + options.udpQueueSize + "\n"
				+ "rpc.tcp.timeout.millis=" + options.tcpTimeoutMillis + "\n"
				+ "export.name=/export\n"
				+ "export.path=" + exportPath.toString().replace( "\\", "\\\\" ) + "\n"
				+ "export.writable=true\n"
				+ "exports.count=1\n"
				+ "exports.1.name=/export\n"
				+ "exports.1.path=" + exportPath.toString().replace( "\\", "\\\\" ) + "\n"
				+ "exports.1.writable=true\n"
				+ "uid=0\n"
				+ "gid=0\n"
				+ "permission.identity=auto\n"
				+ "file.mode=0644\n"
				+ "directory.mode=0755\n"
				+ "block.size=4096\n"
				+ "read.size=8192\n"
				+ "write.size=8192\n"
				+ "directory.preferred.size=4096\n"
				+ "max.file.size=9223372036854775807\n"
				+ "time.delta.nanos=1000000\n"
				+ "pathconf.link.max=1024\n"
				+ "pathconf.name.max=255\n"
				+ "write.sync=" + options.writeSync + "\n"
				+ "write.cache.enabled=" + options.writeCacheEnabled + "\n"
				+ "write.cache.max.open=" + options.writeCacheMaxOpenFiles + "\n"
				+ "write.cache.idle.millis=" + options.writeCacheIdleMillis + "\n"
				+ "filename.charset=UTF-8\n" ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Escapes CSV text.
	 *
	 * @param value value
	 * @return escaped text
	 */
	//--------------------------------------------------------------------------
	private String escapeCsv(String value) {
		return "\"" + value.replace( "\"", "\"\"" ) + "\"" ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Returns used memory.
	 *
	 * @return used memory bytes
	 */
	//--------------------------------------------------------------------------
	private long usedMemoryBytes() {
		Runtime runtime = Runtime.getRuntime() ;
		return runtime.totalMemory() - runtime.freeMemory() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Deletes a directory tree.
	 *
	 * @param path path
	 * @throws IOException delete failure
	 */
	//--------------------------------------------------------------------------
	private void deleteDirectory(Path path) throws IOException {
		// 削除対象のパスが存在しない場合
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			return ;
		}

		try( var stream = Files.walk( path)) {
			List<Path> paths = stream.sorted( Comparator.reverseOrder()).toList() ;

			// 子から親の順に測定用ファイルとディレクトリを削除する
			for( Path target : paths) {
				Files.deleteIfExists( target) ;
			}
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 測定オプションクラスです。<br><br>
	 *
	 * <p>クラス名称： 測定オプション</p>
	 */
	//------------------------------------------------------------------------------
	private static class Options {
		/** 各測定ループで作成するファイル数 */
		int files = 1000 ;

		/** ディレクトリツリー数 */
		int directories = 10 ;

		/** 各ディレクトリツリーの深さ */
		int depth = 1 ;

		/** 測定用ファイルの最小サイズ */
		int minSize = 1024 ;

		/** 測定用ファイルの最大サイズ */
		int maxSize = 8192 ;

		/** 回数指定の測定における繰り返し回数 */
		int loops = 1 ;

		/** 終了時刻まで測定を繰り返すか */
		boolean longRun ;

		/** 長時間測定の継続秒数 */
		int durationSeconds ;

		/** WRITE応答前の同期有無 */
		boolean writeSync ;

		/** 書込ファイルキャッシュの有効化 */
		boolean writeCacheEnabled = true ;

		/** 書込キャッシュの最大オープンファイル数 */
		int writeCacheMaxOpenFiles = 64 ;

		/** 書込キャッシュのアイドル保持時間 */
		int writeCacheIdleMillis = 3000 ;

		/** 測定用設定に保存するUDPワーカー数 */
		int udpWorkers = 8 ;

		/** 測定用設定に保存するUDPキューサイズ */
		int udpQueueSize = 1024 ;

		/** 測定用設定に保存するTCP読込タイムアウト */
		int tcpTimeoutMillis = 30000 ;

		/** レポートの出力先ディレクトリ */
		Path outputDirectory = Path.of( "work", "analysis", "v1.12.0-benchmark") ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 */
		//----------------------------------------------------------------------
		private Options() {
		}

		//----------------------------------------------------------------------
		/**
		 * コマンドライン引数から測定設定を読み込みます。<br><br>
		 *
		 * <p>メソッド名称： 測定設定読込</p>
		 *
		 * @param args	コマンドライン引数
		 * @return	読み込んだ測定設定
		 */
		//----------------------------------------------------------------------
		static Options parse(String[] args) {
			Options options = new Options() ;
			Map<String, String> values = new HashMap<String, String>() ;

			// コマンドライン引数から測定オプションを読み取る
			for( int i = 0; i < args.length; i++) {
				String arg = args[i] ;

				// 長時間測定モードが指定された場合
				if( "--long-run".equals( arg)) {
					options.longRun = true ;
					continue ;
				}

				// 値を伴うオプション名と後続の値が指定された場合
				if( arg.startsWith( "--" ) && i + 1 < args.length) {
					values.put( arg.substring( 2), args[++i]) ;
				}
			}

			options.files = intValue( values, "files", options.files) ;
			options.directories = intValue( values, "directories", options.directories) ;
			options.depth = intValue( values, "depth", options.depth) ;
			options.minSize = intValue( values, "min-size", options.minSize) ;
			options.maxSize = intValue( values, "max-size", options.maxSize) ;
			options.loops = intValue( values, "loops", options.loops) ;
			options.durationSeconds = intValue( values, "duration-seconds", options.durationSeconds) ;
			options.writeSync = booleanValue( values, "write-sync", options.writeSync) ;
			options.writeCacheEnabled = booleanValue( values, "write-cache-enabled", options.writeCacheEnabled) ;
			options.writeCacheMaxOpenFiles = intValue( values, "write-cache-max-open", options.writeCacheMaxOpenFiles) ;
			options.writeCacheIdleMillis = intValue( values, "write-cache-idle-millis", options.writeCacheIdleMillis) ;
			options.udpWorkers = intValue( values, "udp-workers", options.udpWorkers) ;
			options.udpQueueSize = intValue( values, "udp-queue-size", options.udpQueueSize) ;
			options.tcpTimeoutMillis = intValue( values, "tcp-timeout-millis", options.tcpTimeoutMillis) ;

			// 結果の出力先が指定された場合
			if( values.containsKey( "out" )) {
				options.outputDirectory = Path.of( values.get( "out" )) ;
			}

			// 最大ファイルサイズが最小ファイルサイズより小さい場合
			if( options.maxSize < options.minSize) {
				throw new IllegalArgumentException( "max-size must be greater than or equal to min-size." ) ;
			}

			return options ;
		}

		//----------------------------------------------------------------------
		/**
		 * 整数の測定オプションを取得します。<br><br>
		 *
		 * <p>メソッド名称： 整数オプション取得</p>
		 *
		 * @param values	オプション名と値の対応表
		 * @param key	取得するオプション名
		 * @param defaultValue	値が指定されていない場合の既定値
		 * @return	オプション値または既定値
		 */
		//----------------------------------------------------------------------
		private static int intValue(Map<String, String> values, String key, int defaultValue) {
			String value = values.get( key) ;
			return value == null ? defaultValue : Integer.parseInt( value) ;
		}

		//----------------------------------------------------------------------
		/**
		 * booleanの測定オプションを取得します。<br><br>
		 *
		 * <p>メソッド名称： booleanオプション取得</p>
		 *
		 * @param values	オプション名と値の対応表
		 * @param key	取得するオプション名
		 * @param defaultValue	値が指定されていない場合の既定値
		 * @return	オプション値または既定値
		 * <ul>
		 * <li>false: 無効</li>
		 * <li>true: 有効</li>
		 * </ul>
		 */
		//----------------------------------------------------------------------
		private static boolean booleanValue(Map<String, String> values, String key, boolean defaultValue) {
			String value = values.get( key) ;
			return value == null ? defaultValue : Boolean.parseBoolean( value) ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 測定用ディレクトリクラスです。<br><br>
	 *
	 * <p>クラス名称： 測定用ディレクトリ</p>
	 */
	//------------------------------------------------------------------------------
	private static class DirectoryNode {
		/** 親ディレクトリ。測定用ルートの場合は null */
		final DirectoryNode				parent ;

		/** ディレクトリ名 */
		final String					name ;

		/** ディレクトリのファイルハンドル */
		final FileHandle				handle ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param parent	親ディレクトリ。測定用ルートの場合は null
		 * @param name	ディレクトリ名
		 * @param handle	ディレクトリのファイルハンドル
		 */
		//----------------------------------------------------------------------
		DirectoryNode(DirectoryNode parent, String name, FileHandle handle) {
			this.parent = parent ;
			this.name = name ;
			this.handle = handle ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 測定用ファイルクラスです。<br><br>
	 *
	 * <p>クラス名称： 測定用ファイル</p>
	 */
	//------------------------------------------------------------------------------
	private static class FileEntry {
		/** ファイルを保持するディレクトリ */
		final DirectoryNode				directory ;

		/** 現在のファイル名 */
		String							name ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param directory	ファイルを保持するディレクトリ
		 * @param name	ファイル名
		 */
		//----------------------------------------------------------------------
		FileEntry(DirectoryNode directory, String name) {
			this.directory = directory ;
			this.name = name ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 操作別測定結果クラスです。<br><br>
	 *
	 * <p>クラス名称： 操作別測定結果</p>
	 */
	//------------------------------------------------------------------------------
	private static class Result {
		/** 測定した操作の名前 */
		final String					name ;

		/** 操作回数 */
		final int						operations ;

		/** 経過時間のナノ秒値 */
		final long						elapsedNanos ;

		/** 測定時の使用メモリバイト数 */
		final long						usedMemoryBytes ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param name	測定した操作の名前
		 * @param operations	操作回数
		 * @param elapsedNanos	経過時間のナノ秒値
		 * @param usedMemoryBytes	測定時の使用メモリバイト数
		 */
		//----------------------------------------------------------------------
		Result(String name, int operations, long elapsedNanos, long usedMemoryBytes) {
			this.name = name ;
			this.operations = operations ;
			this.elapsedNanos = elapsedNanos ;
			this.usedMemoryBytes = usedMemoryBytes ;
		}
	}
}
