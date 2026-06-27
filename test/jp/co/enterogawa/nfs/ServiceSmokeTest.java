package jp.co.enterogawa.nfs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.program.NfsStatus;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * サービススモークテストクラスです。<br><br>
 *
 * <p>クラス名称： サービススモークテスト</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class ServiceSmokeTest {
	//	定数定義	------------------------------------------------------------
	/** 接続先 */
	private static final String			HOST = "127.0.0.1" ;

	/** Portmapポート */
	private static final int				PORTMAP_PORT = 111 ;

	/** NFSポート */
	private static final int				NFS_PORT = 2049 ;

	/** MOUNTポート */
	private static final int				MOUNT_PORT = 20048 ;

	/** タイムアウト */
	private static final int				TIMEOUT = 3000 ;

	/** 永続化確認ファイル名 */
	private static final String			PERSISTENCE_FILE_NAME = "service-handle-persistence.txt" ;

	/** 永続化確認ファイル内容 */
	private static final String			PERSISTENCE_CONTENT = "service restart handle persistence" ;

	/** XID */
	private int							xid = 0x20000000 ;

	/** TCP transport使用有無 */
	private boolean						tcpTransport ;

	//--------------------------------------------------------------------------
	/**
	 * テストを開始します。<br><br>
	 *
	 * <p>メソッド名称： テスト開始</p>
	 *
	 * @param args	起動引数
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	public static void main(String[] args) throws Exception {
		ServiceSmokeTest test = new ServiceSmokeTest() ;

		if( args.length > 0) {
			if( args.length == 1 && "tcp".equals( args[0])) {
				test.tcpTransport = true ;
				test.run() ;
				return ;
			}

			if( args.length == 2 && "prepare-handle-persistence".equals( args[0])) {
				test.prepareHandlePersistence( Path.of( args[1])) ;
				return ;
			}

			if( args.length == 2 && "verify-handle-persistence".equals( args[0])) {
				test.verifyHandlePersistence( Path.of( args[1])) ;
				return ;
			}

			if( args.length == 2 && "verify-file-integrity".equals( args[0])) {
				test.verifyFileIntegrity( Path.of( args[1])) ;
				return ;
			}

			throw new IllegalArgumentException( "Unknown arguments." ) ;
		}

		test.run() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テストを実行します。<br><br>
	 *
	 * <p>メソッド名称： テスト実行</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void run() throws Exception {
		assertPortmapGetPort() ;
		byte[] rootHandle = mountExport() ;
		assertRootGetAttr( rootHandle) ;
		assertWriteRoundTrip( rootHandle) ;
		System.out.println( "SERVICE SMOKE TEST PASSED") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドル永続化確認を準備します。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル永続化確認準備</p>
	 *
	 * @param statePath	状態ファイル
	 * @throws Exception 準備異常
	 */
	//--------------------------------------------------------------------------
	private void prepareHandlePersistence(Path statePath) throws Exception {
		byte[] rootHandle = mountExport() ;
		removeFileIfExists( rootHandle, PERSISTENCE_FILE_NAME) ;
		byte[] fileHandle = createFile( rootHandle, PERSISTENCE_FILE_NAME) ;
		writeFile( fileHandle, PERSISTENCE_CONTENT) ;
		assertReadFile( fileHandle, PERSISTENCE_CONTENT) ;

		Path absoluteStatePath = statePath.toAbsolutePath().normalize() ;
		Path parent = absoluteStatePath.getParent() ;

		if( parent != null) {
			Files.createDirectories( parent) ;
		}

		Files.writeString( absoluteStatePath, Base64.getEncoder().encodeToString( fileHandle), StandardCharsets.US_ASCII) ;
		System.out.println( "PASS: service handle persistence prepared") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドル永続化を確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル永続化確認</p>
	 *
	 * @param statePath	状態ファイル
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void verifyHandlePersistence(Path statePath) throws Exception {
		byte[] fileHandle = Base64.getDecoder().decode( Files.readString( statePath.toAbsolutePath().normalize(), StandardCharsets.US_ASCII).trim()) ;
		assertReadFile( fileHandle, PERSISTENCE_CONTENT) ;
		removeFileIfExists( mountExport(), PERSISTENCE_FILE_NAME) ;
		System.out.println( "PASS: service handle persistence after restart") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバー側ファイル内容の整合性を確認します。<br><br>
	 *
	 * <p>メソッド名称： サーバー側ファイル内容整合性確認</p>
	 *
	 * @param configPath	設定ファイル
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void verifyFileIntegrity(Path configPath) throws Exception {
		NfsServerConfig config = NfsServerConfig.load( configPath.toAbsolutePath().normalize()) ;
		Path exportRoot = config.getExportPath() ;
		String fileName = "service-integrity-" + Long.toHexString( System.currentTimeMillis()) + ".txt" ;
		String temporaryName = "." + fileName + ".tmp" ;
		Path filePath = exportRoot.resolve( fileName).toAbsolutePath().normalize() ;
		Path temporaryPath = exportRoot.resolve( temporaryName).toAbsolutePath().normalize() ;
		byte[] rootHandle = mountExport() ;

		removeFileIfExists( rootHandle, fileName) ;
		removeFileIfExists( rootHandle, temporaryName) ;
		byte[] fileHandle = createFile( rootHandle, fileName) ;

		writeFileAtOffset( fileHandle, 0, "0123456789ABCDEF") ;
		assertDiskContent( filePath, "0123456789ABCDEF") ;

		writeFileAtOffset( fileHandle, 4, "----") ;
		assertDiskContent( filePath, "0123----89ABCDEF") ;

		setFileSize( fileHandle, 8) ;
		assertDiskContent( filePath, "0123----") ;

		writeFileAtOffset( fileHandle, 8, "TAIL") ;
		assertDiskContent( filePath, "0123----TAIL") ;

		setFileSize( fileHandle, 0) ;
		writeFileAtOffset( fileHandle, 0, "chunk-") ;
		writeFileAtOffset( fileHandle, 6, "alpha") ;
		assertDiskContent( filePath, "chunk-alpha") ;

		byte[] temporaryHandle = createFile( rootHandle, temporaryName) ;
		writeFileAtOffset( temporaryHandle, 0, "rename replacement") ;
		renameFile( rootHandle, temporaryName, fileName) ;
		assertDiskContent( filePath, "rename replacement") ;

		if( Files.exists( temporaryPath)) {
			throw new AssertionError( "Temporary file still exists: " + temporaryPath) ;
		}

		removeFile( rootHandle, fileName) ;
		System.out.println( "PASS: service file integrity") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Portmap GETPORTを確認します。<br><br>
	 *
	 * <p>メソッド名称： Portmap GETPORT確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void assertPortmapGetPort() throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeInt( RpcConstants.PROGRAM_NFS) ;
		arguments.writeInt( 2) ;
		arguments.writeInt( tcpTransport ? RpcConstants.IPPROTO_TCP : RpcConstants.IPPROTO_UDP) ;
		arguments.writeInt( 0) ;

		XdrReader reader = call( PORTMAP_PORT, RpcConstants.PROGRAM_PORTMAP, 2, 3, arguments) ;
		int port = reader.readInt() ;

		// NFSポートが一致しない場合
		if( port != NFS_PORT) {
			throw new AssertionError( "Portmap returned unexpected NFS port: " + port) ;
		}

		System.out.println( "PASS: service portmap GETPORT") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * exportをmountします。<br><br>
	 *
	 * <p>メソッド名称： export mount</p>
	 *
	 * @return ルートファイルハンドル
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private byte[] mountExport() throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( "/export") ;
		XdrReader reader = call( MOUNT_PORT, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments) ;
		int status = reader.readInt() ;

		// mountが失敗した場合
		if( status != 0) {
			throw new AssertionError( "Mount failed: " + status) ;
		}

		byte[] handle = reader.readFixedOpaque( FileHandle.LENGTH) ;
		System.out.println( "PASS: service mount MNT") ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ルートGETATTRを確認します。<br><br>
	 *
	 * <p>メソッド名称： ルートGETATTR確認</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void assertRootGetAttr(byte[] rootHandle) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 1, arguments) ;
		int status = reader.readInt() ;

		// GETATTRが失敗した場合
		if( status != 0) {
			throw new AssertionError( "GETATTR failed: " + status) ;
		}

		int type = reader.readInt() ;

		// ルートがディレクトリではない場合
		if( type != 2) {
			throw new AssertionError( "Root type is not directory: " + type) ;
		}

		System.out.println( "PASS: service nfs GETATTR") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービス越しの書込を確認します。<br><br>
	 *
	 * <p>メソッド名称： サービス書込確認</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void assertWriteRoundTrip(byte[] rootHandle) throws Exception {
		String fileName = "service-smoke-" + Long.toHexString( System.currentTimeMillis()) + ".txt" ;
		byte[] fileHandle = createFile( rootHandle, fileName) ;
		writeFile( fileHandle, "service write" ) ;
		assertReadFile( fileHandle, "service write" ) ;
		setFileSize( fileHandle, 0) ;
		writeFile( fileHandle, "service rewrite" ) ;
		assertReadFile( fileHandle, "service rewrite" ) ;
		assertRenameOverwrite( rootHandle, fileName) ;
		fileHandle = lookupFile( rootHandle, fileName) ;
		assertReadFile( fileHandle, "service rename overwrite" ) ;
		removeFile( rootHandle, fileName) ;
		System.out.println( "PASS: service nfs CREATE/WRITE/SETATTR/READ/RENAME/REMOVE") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを作成します。<br><br>
	 *
	 * <p>メソッド名称： ファイル作成</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param fileName	ファイル名
	 * @return ファイルハンドル
	 * @throws Exception 作成異常
	 */
	//--------------------------------------------------------------------------
	private byte[] createFile(byte[] rootHandle, String fileName) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, fileName) ;
		writeSattrUnset( arguments) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 9, arguments) ;
		int status = reader.readInt() ;

		// CREATEが失敗した場合
		if( status != 0) {
			throw new AssertionError( "CREATE failed: " + status) ;
		}

		byte[] handle = reader.readFixedOpaque( FileHandle.LENGTH) ;
		skipAttributes( reader) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを検索します。<br><br>
	 *
	 * <p>メソッド名称： ファイル検索</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param fileName	ファイル名
	 * @return ファイルハンドル
	 * @throws Exception 検索異常
	 */
	//--------------------------------------------------------------------------
	private byte[] lookupFile(byte[] rootHandle, String fileName) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, fileName) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 4, arguments) ;
		int status = reader.readInt() ;

		// LOOKUPが失敗した場合
		if( status != 0) {
			throw new AssertionError( "LOOKUP failed: " + status) ;
		}

		byte[] handle = reader.readFixedOpaque( FileHandle.LENGTH) ;
		skipAttributes( reader) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイル書込</p>
	 *
	 * @param fileHandle	ファイルハンドル
	 * @param value		値
	 * @throws Exception 書込異常
	 */
	//--------------------------------------------------------------------------
	private void writeFile(byte[] fileHandle, String value) throws Exception {
		writeFileAtOffset( fileHandle, 0, value) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルの指定位置へ書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイル指定位置書込</p>
	 *
	 * @param fileHandle	ファイルハンドル
	 * @param offset	書込位置
	 * @param value		値
	 * @throws Exception 書込異常
	 */
	//--------------------------------------------------------------------------
	private void writeFileAtOffset(byte[] fileHandle, long offset, String value) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		byte[] data = value.getBytes( StandardCharsets.UTF_8) ;
		arguments.writeFixedOpaque( fileHandle) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( offset) ;
		arguments.writeUnsignedInt( data.length) ;
		arguments.writeOpaque( data) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 8, arguments) ;
		int status = reader.readInt() ;

		// WRITEが失敗した場合
		if( status != 0) {
			throw new AssertionError( "WRITE failed: " + status) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 一時ファイルrenameによる上書きを確認します。<br><br>
	 *
	 * <p>メソッド名称： 一時ファイルrename上書き確認</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param fileName	ファイル名
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void assertRenameOverwrite(byte[] rootHandle, String fileName) throws Exception {
		String temporaryName = "." + fileName + ".tmp" ;
		byte[] temporaryHandle = createFile( rootHandle, temporaryName) ;
		writeFile( temporaryHandle, "service rename overwrite" ) ;
		renameFile( rootHandle, temporaryName, fileName) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルをリネームします。<br><br>
	 *
	 * <p>メソッド名称： ファイルリネーム</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param sourceName	移動元ファイル名
	 * @param targetName	移動先ファイル名
	 * @throws Exception リネーム異常
	 */
	//--------------------------------------------------------------------------
	private void renameFile(byte[] rootHandle, String sourceName, String targetName) throws Exception {

		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, sourceName) ;
		writeDiropargs( arguments, rootHandle, targetName) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 11, arguments) ;
		int status = reader.readInt() ;

		// RENAMEが失敗した場合
		if( status != 0) {
			throw new AssertionError( "RENAME failed: " + status) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバー側実ファイルの内容を確認します。<br><br>
	 *
	 * <p>メソッド名称： サーバー側実ファイル内容確認</p>
	 *
	 * @param path		ファイルパス
	 * @param expected	期待値
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void assertDiskContent(Path path, String expected) throws Exception {
		assertEquals( "server disk content", expected, Files.readString( path, StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルREADを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルREAD確認</p>
	 *
	 * @param fileHandle	ファイルハンドル
	 * @param expected	期待値
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadFile(byte[] fileHandle, String expected) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( fileHandle) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeInt( 1024) ;
		arguments.writeInt( 1024) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 6, arguments) ;
		int status = reader.readInt() ;

		// READが失敗した場合
		if( status != 0) {
			throw new AssertionError( "READ failed: " + status) ;
		}

		skipAttributes( reader) ;
		assertEquals( "read data", expected, new String( reader.readOpaque(), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルサイズを設定します。<br><br>
	 *
	 * <p>メソッド名称： ファイルサイズ設定</p>
	 *
	 * @param fileHandle	ファイルハンドル
	 * @param size		サイズ
	 * @throws Exception 設定異常
	 */
	//--------------------------------------------------------------------------
	private void setFileSize(byte[] fileHandle, int size) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( fileHandle) ;
		arguments.writeInt( -1) ;
		arguments.writeInt( -1) ;
		arguments.writeInt( -1) ;
		arguments.writeUnsignedInt( size) ;
		arguments.writeInt( -1) ;
		arguments.writeInt( -1) ;
		arguments.writeInt( -1) ;
		arguments.writeInt( -1) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 2, arguments) ;
		int status = reader.readInt() ;

		// SETATTRが失敗した場合
		if( status != 0) {
			throw new AssertionError( "SETATTR failed: " + status) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを削除します。<br><br>
	 *
	 * <p>メソッド名称： ファイル削除</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param fileName	ファイル名
	 * @throws Exception 削除異常
	 */
	//--------------------------------------------------------------------------
	private void removeFile(byte[] rootHandle, String fileName) throws Exception {
		int status = removeFileStatus( rootHandle, fileName) ;

		// REMOVEが失敗した場合
		if( status != NfsStatus.OK) {
			throw new AssertionError( "REMOVE failed: " + status) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルを存在する場合に削除します。<br><br>
	 *
	 * <p>メソッド名称： ファイル存在時削除</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param fileName	ファイル名
	 * @throws Exception 削除異常
	 */
	//--------------------------------------------------------------------------
	private void removeFileIfExists(byte[] rootHandle, String fileName) throws Exception {
		int status = removeFileStatus( rootHandle, fileName) ;

		// 削除できない場合
		if( status != NfsStatus.OK && status != NfsStatus.NOENT) {
			throw new AssertionError( "REMOVE failed: " + status) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイル削除ステータスを取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイル削除ステータス取得</p>
	 *
	 * @param rootHandle	ルートファイルハンドル
	 * @param fileName	ファイル名
	 * @return ステータス
	 * @throws Exception 削除異常
	 */
	//--------------------------------------------------------------------------
	private int removeFileStatus(byte[] rootHandle, String fileName) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, fileName) ;
		XdrReader reader = call( NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 10, arguments) ;
		return reader.readInt() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCを呼び出します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出</p>
	 *
	 * @param port		ポート
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return 応答本文
	 * @throws Exception 呼出異常
	 */
	//--------------------------------------------------------------------------
	private XdrReader call(int port, int program, int version, int procedure, XdrWriter arguments) throws Exception {
		// TCP transportの場合
		if( tcpTransport) {
			return callTcp( port, program, version, procedure, arguments) ;
		}

		return callUdp( port, program, version, procedure, arguments) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * UDP RPCを呼び出します。<br><br>
	 *
	 * <p>メソッド名称： UDP RPC呼出</p>
	 *
	 * @param port		ポート
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return 応答本文
	 * @throws Exception 呼出異常
	 */
	//--------------------------------------------------------------------------
	private XdrReader callUdp(int port, int program, int version, int procedure, XdrWriter arguments) throws Exception {
		int requestXid = ++xid ;
		byte[] request = createCall( requestXid, program, version, procedure, arguments) ;
		byte[] buffer = new byte[65535] ;
		DatagramPacket response = new DatagramPacket( buffer, buffer.length) ;

		try( DatagramSocket socket = new DatagramSocket()) {
			socket.setSoTimeout( TIMEOUT) ;
			DatagramPacket packet = new DatagramPacket( request, request.length, InetAddress.getByName( HOST), port) ;
			socket.send( packet) ;
			socket.receive( response) ;
		}

		XdrReader reader = new XdrReader( response.getData(), response.getLength()) ;
		assertEquals( "xid", requestXid, reader.readInt()) ;
		assertEquals( "message type", RpcConstants.MSG_REPLY, reader.readInt()) ;
		assertEquals( "reply status", RpcConstants.REPLY_STAT_ACCEPTED, reader.readInt()) ;
		reader.readInt() ;
		reader.readOpaque() ;
		assertEquals( "accept status", RpcConstants.ACCEPT_SUCCESS, reader.readInt()) ;
		return new XdrReader( reader.readRemaining()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPCを呼び出します。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC呼出</p>
	 *
	 * @param port		ポート
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return 応答本文
	 * @throws Exception 呼出異常
	 */
	//--------------------------------------------------------------------------
	private XdrReader callTcp(int port, int program, int version, int procedure, XdrWriter arguments) throws Exception {
		int requestXid = ++xid ;
		byte[] request = createCall( requestXid, program, version, procedure, arguments) ;

		try( Socket socket = new Socket( HOST, port)) {
			socket.setSoTimeout( TIMEOUT) ;
			DataOutputStream output = new DataOutputStream( socket.getOutputStream()) ;
			DataInputStream input = new DataInputStream( socket.getInputStream()) ;
			output.writeInt( 0x80000000 | request.length) ;
			output.write( request) ;
			output.flush() ;

			int header = input.readInt() ;
			int length = header & 0x7fffffff ;
			byte[] response = input.readNBytes( length) ;

			assertEquals( "tcp response length", length, response.length) ;
			XdrReader reader = new XdrReader( response) ;
			assertEquals( "xid", requestXid, reader.readInt()) ;
			assertEquals( "message type", RpcConstants.MSG_REPLY, reader.readInt()) ;
			assertEquals( "reply status", RpcConstants.REPLY_STAT_ACCEPTED, reader.readInt()) ;
			reader.readInt() ;
			reader.readOpaque() ;
			assertEquals( "accept status", RpcConstants.ACCEPT_SUCCESS, reader.readInt()) ;
			return new XdrReader( reader.readRemaining()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC CALLを作成します。<br><br>
	 *
	 * <p>メソッド名称： RPC CALL作成</p>
	 *
	 * @param xid		XID
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return RPC CALL
	 */
	//--------------------------------------------------------------------------
	private byte[] createCall(int xid, int program, int version, int procedure, XdrWriter arguments) {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( xid) ;
		writer.writeInt( RpcConstants.MSG_CALL) ;
		writer.writeInt( RpcConstants.RPC_VERSION) ;
		writer.writeInt( program) ;
		writer.writeInt( version) ;
		writer.writeInt( procedure) ;
		writer.writeInt( RpcConstants.AUTH_SYS) ;
		writer.writeOpaque( createAuthSys()) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeFixedOpaque( arguments.toByteArray()) ;
		return writer.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYSを作成します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS作成</p>
	 *
	 * @return AUTH_SYSデータ
	 */
	//--------------------------------------------------------------------------
	private byte[] createAuthSys() {
		XdrWriter writer = new XdrWriter() ;
		writer.writeUnsignedInt( System.currentTimeMillis() / 1000L) ;
		writer.writeString( "localhost") ;
		writer.writeInt( 0) ;
		writer.writeInt( 0) ;
		writer.writeInt( 0) ;
		return writer.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * diropargsを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： diropargs書込</p>
	 *
	 * @param writer		書込
	 * @param directory		ディレクトリハンドル
	 * @param name			名前
	 */
	//--------------------------------------------------------------------------
	private void writeDiropargs(XdrWriter writer, byte[] directory, String name) {
		writer.writeFixedOpaque( directory) ;
		writer.writeString( name) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 未指定sattrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 未指定sattr書込</p>
	 *
	 * @param writer	書込
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
	 * 属性を読み飛ばします。<br><br>
	 *
	 * <p>メソッド名称： 属性読飛</p>
	 *
	 * @param reader	XDR読込
	 */
	//--------------------------------------------------------------------------
	private void skipAttributes(XdrReader reader) {
		// fattrの17個のint値を読み飛ばす
		for( int i = 0; i < 17; i++) {
			reader.readInt() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 数値を検証します。<br><br>
	 *
	 * <p>メソッド名称： 数値検証</p>
	 *
	 * @param name		名称
	 * @param expected	期待値
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void assertEquals(String name, int expected, int actual) {
		// 値が一致しない場合
		if( expected != actual) {
			throw new AssertionError( name + " expected=" + expected + " actual=" + actual) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列を検証します。<br><br>
	 *
	 * <p>メソッド名称： 文字列検証</p>
	 *
	 * @param name		名称
	 * @param expected	期待値
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void assertEquals(String name, String expected, String actual) {
		// 値が一致しない場合
		if( !expected.equals( actual)) {
			throw new AssertionError( name + " expected=" + expected + " actual=" + actual) ;
		}
	}
}
