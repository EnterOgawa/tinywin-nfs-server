package jp.co.enterogawa.nfs;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.config.ConfigBackup;
import jp.co.enterogawa.nfs.diagnostic.NfsDiagnostics;
import jp.co.enterogawa.nfs.diagnostic.NfsDiagnostics.CaseCollision;
import jp.co.enterogawa.nfs.diagnostic.NfsDiagnostics.DiagnosticReport;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.io.WriteFileCache;
import jp.co.enterogawa.nfs.program.MountV1Program;
import jp.co.enterogawa.nfs.program.NfsStatus;
import jp.co.enterogawa.nfs.program.NfsV2Program;
import jp.co.enterogawa.nfs.program.NfsV3Program;
import jp.co.enterogawa.nfs.program.PortmapV2Program;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcRequestContext;
import jp.co.enterogawa.nfs.server.NfsServer;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * 単体テストクラスです。<br><br>
 *
 * <p>クラス名称： 単体テスト</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class AllTests {
	//	定数定義	------------------------------------------------------------
	/** テスト用Portmapポート */
	private static final int				TEST_PORTMAP_PORT = 11111 ;

	/** テスト用NFSポート */
	private static final int				TEST_NFS_PORT = 12049 ;

	/** テスト用MOUNTポート */
	private static final int				TEST_MOUNT_PORT = 12048 ;

	/** XID */
	private static final int				XID = 0x10203040 ;

	//	内部定義	------------------------------------------------------------
	/** 実行数 */
	private int							runCount ;

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
		new AllTests().run() ;
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
		runTest( "XDR round trip", this::testXdrRoundTrip) ;
		runTest( "RPC call parse", this::testRpcCallParse) ;
		runTest( "Portmap GETPORT", this::testPortmapGetPort) ;
		runTest( "TCP RPC transport", this::testTcpRpcTransport) ;
		runTest( "Mount MNT", this::testMount) ;
		runTest( "Mount MNT v2", this::testMountV2) ;
		runTest( "Multiple exports", this::testMultipleExports) ;
		runTest( "Config validation", this::testConfigValidation) ;
		runTest( "Config relative export base", this::testConfigRelativeExportBase) ;
		runTest( "Config backup", this::testConfigBackup) ;
		runTest( "Operational diagnostics", this::testOperationalDiagnostics) ;
		runTest( "Client access restrictions", this::testClientAccessRestrictions) ;
		runTest( "NFSv2 procedures", this::testNfsV2Procedures) ;
		runTest( "NFSv2 large READDIR", this::testNfsV2LargeReadDir) ;
		runTest( "NFSv3 procedures", this::testNfsV3Procedures) ;
		runTest( "NFS status and attributes", this::testNfsStatusAndAttributes) ;
		runTest( "Cross client edit regression", this::testCrossClientEditRegression) ;
		runTest( "Write cache flush", this::testWriteCacheFlush) ;
		runTest( "NFSv2 filename charset", this::testNfsV2FilenameCharset) ;
		runTest( "File handle persistence", this::testFileHandlePersistence) ;
		System.out.println( "TEST PASSED: " + runCount + " tests") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 個別テストを実行します。<br><br>
	 *
	 * <p>メソッド名称： 個別テスト実行</p>
	 *
	 * @param name		テスト名
	 * @param runnable	テスト処理
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void runTest(String name, TestRunnable runnable) throws Exception {
		runnable.run() ;
		runCount++ ;
		System.out.println( "PASS: " + name) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * XDR変換テストを行います。<br><br>
	 *
	 * <p>メソッド名称： XDR変換テスト</p>
	 */
	//--------------------------------------------------------------------------
	private void testXdrRoundTrip() {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( 0x01020304) ;
		writer.writeString( "abc") ;
		writer.writeOpaque( new byte[] { 1, 2, 3 }) ;
		writer.writeBoolean( true) ;

		XdrReader reader = new XdrReader( writer.toByteArray()) ;
		assertEquals( "int", 0x01020304, reader.readInt()) ;
		assertEquals( "string", "abc", reader.readString()) ;
		assertBytes( "opaque", new byte[] { 1, 2, 3 }, reader.readOpaque()) ;
		assertTrue( "boolean", reader.readBoolean()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出解析テストを行います。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出解析テスト</p>
	 */
	//--------------------------------------------------------------------------
	private void testRpcCallParse() {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeInt( 1234) ;
		RpcCall call = createCall( RpcConstants.PROGRAM_NFS, 2, 1, arguments) ;

		assertEquals( "xid", XID, call.getXid()) ;
		assertEquals( "program", RpcConstants.PROGRAM_NFS, call.getProgram()) ;
		assertEquals( "version", 2, call.getVersion()) ;
		assertEquals( "procedure", 1, call.getProcedure()) ;
		assertEquals( "argument", 1234, call.getArguments().readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Portmap GETPORTテストを行います。<br><br>
	 *
	 * <p>メソッド名称： Portmap GETPORTテスト</p>
	 *
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void testPortmapGetPort() throws IOException {
		PortmapV2Program program = new PortmapV2Program( TEST_PORTMAP_PORT, TEST_NFS_PORT, TEST_MOUNT_PORT) ;
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeInt( RpcConstants.PROGRAM_NFS) ;
		arguments.writeInt( 2) ;
		arguments.writeInt( RpcConstants.IPPROTO_UDP) ;
		arguments.writeInt( 0) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_PORTMAP, 2, 3, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "nfs port", TEST_NFS_PORT, reader.readInt()) ;

		XdrWriter tcpArguments = new XdrWriter() ;
		tcpArguments.writeInt( RpcConstants.PROGRAM_NFS) ;
		tcpArguments.writeInt( 3) ;
		tcpArguments.writeInt( RpcConstants.IPPROTO_TCP) ;
		tcpArguments.writeInt( 0) ;
		XdrWriter tcpResponse = handle( program, RpcConstants.PROGRAM_PORTMAP, 2, 3, tcpArguments) ;
		XdrReader tcpReader = new XdrReader( tcpResponse.toByteArray()) ;

		assertEquals( "nfs tcp port", TEST_NFS_PORT, tcpReader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPC transportテストを行います。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC transportテスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testTcpRpcTransport() throws Exception {
		TestContext context = createContext() ;
		NfsServer server = new NfsServer( context.getConfig()) ;

		try {
			server.start() ;
			assertTcpPortmapGetPort() ;
			FileHandle rootHandle = mountExportTcp() ;
			assertTcpRootGetAttr( rootHandle) ;
			assertTcpMultipleRecords() ;
		} finally {
			server.stop() ;
			context.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP Portmap GETPORTを確認します。<br><br>
	 *
	 * <p>メソッド名称： TCP Portmap GETPORT確認</p>
	 *
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertTcpPortmapGetPort() throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeInt( RpcConstants.PROGRAM_NFS) ;
		arguments.writeInt( 3) ;
		arguments.writeInt( RpcConstants.IPPROTO_TCP) ;
		arguments.writeInt( 0) ;
		XdrReader reader = callTcp( TEST_PORTMAP_PORT, RpcConstants.PROGRAM_PORTMAP, 2, 3, arguments, true) ;

		assertEquals( "tcp nfs port", TEST_NFS_PORT, reader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP MOUNTで公開をマウントします。<br><br>
	 *
	 * <p>メソッド名称： TCP MOUNT公開マウント</p>
	 *
	 * @return ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle mountExportTcp() throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( "/export") ;
		XdrReader reader = callTcp( TEST_MOUNT_PORT, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments, false) ;

		assertEquals( "tcp mount status", NfsStatus.OK, reader.readInt()) ;
		return new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP NFS GETATTRを確認します。<br><br>
	 *
	 * <p>メソッド名称： TCP NFS GETATTR確認</p>
	 *
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertTcpRootGetAttr(FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		XdrReader reader = callTcp( TEST_NFS_PORT, RpcConstants.PROGRAM_NFS, 2, 1, arguments, false) ;

		assertEquals( "tcp getattr status", NfsStatus.OK, reader.readInt()) ;
		assertEquals( "tcp getattr type", 2, reader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP 1接続複数recordを確認します。<br><br>
	 *
	 * <p>メソッド名称： TCP 1接続複数record確認</p>
	 *
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertTcpMultipleRecords() throws IOException {
		XdrWriter nfsArguments = new XdrWriter() ;
		nfsArguments.writeInt( RpcConstants.PROGRAM_NFS) ;
		nfsArguments.writeInt( 2) ;
		nfsArguments.writeInt( RpcConstants.IPPROTO_TCP) ;
		nfsArguments.writeInt( 0) ;
		byte[] firstRequest = createCallBytes( XID + 101, RpcConstants.PROGRAM_PORTMAP, 2, 3, nfsArguments) ;

		XdrWriter mountArguments = new XdrWriter() ;
		mountArguments.writeInt( RpcConstants.PROGRAM_MOUNT) ;
		mountArguments.writeInt( 3) ;
		mountArguments.writeInt( RpcConstants.IPPROTO_TCP) ;
		mountArguments.writeInt( 0) ;
		byte[] secondRequest = createCallBytes( XID + 102, RpcConstants.PROGRAM_PORTMAP, 2, 3, mountArguments) ;

		try( Socket socket = new Socket( "127.0.0.1", TEST_PORTMAP_PORT)) {
			socket.setSoTimeout( 3000) ;
			DataOutputStream output = new DataOutputStream( socket.getOutputStream()) ;
			DataInputStream input = new DataInputStream( socket.getInputStream()) ;
			writeTcpRecord( output, firstRequest, false) ;
			writeTcpRecord( output, secondRequest, false) ;

			XdrReader firstReader = parseAcceptedReply( XID + 101, readTcpRecord( input)) ;
			XdrReader secondReader = parseAcceptedReply( XID + 102, readTcpRecord( input)) ;

			assertEquals( "tcp multi nfs port", TEST_NFS_PORT, firstReader.readInt()) ;
			assertEquals( "tcp multi mount port", TEST_MOUNT_PORT, secondReader.readInt()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * MOUNT MNTテストを行います。<br><br>
	 *
	 * <p>メソッド名称： MOUNT MNTテスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testMount() throws Exception {
		TestContext context = createContext() ;
		MountV1Program program = new MountV1Program( context.getConfig(), context.getHandleTable()) ;
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( "/export") ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "mount status", 0, reader.readInt()) ;
		assertEquals( "mount handle length", FileHandle.LENGTH, reader.readFixedOpaque( FileHandle.LENGTH).length) ;
		context.close() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * MOUNT Version 2 MNTテストを行います。<br><br>
	 *
	 * <p>メソッド名称： MOUNT Version 2 MNTテスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testMountV2() throws Exception {
		TestContext context = createContext() ;
		MountV1Program program = new MountV1Program( context.getConfig(), context.getHandleTable()) ;
		XdrWriter portArguments = new XdrWriter() ;
		PortmapV2Program portmap = new PortmapV2Program( TEST_PORTMAP_PORT, TEST_NFS_PORT, TEST_MOUNT_PORT) ;
		portArguments.writeInt( RpcConstants.PROGRAM_MOUNT) ;
		portArguments.writeInt( 2) ;
		portArguments.writeInt( RpcConstants.IPPROTO_UDP) ;
		portArguments.writeInt( 0) ;
		XdrWriter portResponse = handle( portmap, RpcConstants.PROGRAM_PORTMAP, 2, 3, portArguments) ;
		assertEquals( "mount v2 port", TEST_MOUNT_PORT, new XdrReader( portResponse.toByteArray()).readInt()) ;

		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( "/export") ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_MOUNT, 2, 1, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "mount v2 status", 0, reader.readInt()) ;
		assertEquals( "mount v2 handle length", FileHandle.LENGTH, reader.readFixedOpaque( FileHandle.LENGTH).length) ;
		context.close() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 複数公開定義のMOUNTを確認します。<br><br>
	 *
	 * <p>メソッド名称： 複数公開定義MOUNT確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testMultipleExports() throws Exception {
		Path root = Path.of( "work", "tmp", "test-export-multi-a").toAbsolutePath().normalize() ;
		Path readOnlyRoot = Path.of( "work", "tmp", "test-export-multi-b").toAbsolutePath().normalize() ;
		Path data = Path.of( "work", "tmp", "test-data").toAbsolutePath().normalize() ;
		deleteDirectory( root) ;
		deleteDirectory( readOnlyRoot) ;
		deleteDirectory( data) ;
		System.setProperty( "tinywin.nfs.data", data.toString()) ;
		Files.createDirectories( root) ;
		Files.createDirectories( readOnlyRoot) ;
		Files.writeString( root.resolve( "hello.txt"), "hello qnx", StandardCharsets.UTF_8) ;
		Files.writeString( readOnlyRoot.resolve( "hello.txt"), "hello qnx", StandardCharsets.UTF_8) ;
		Path configPath = Path.of( "work", "tmp", "test-nfs-server-multi.properties").toAbsolutePath().normalize() ;
		String configText = ""
				+ "portmap.port=" + TEST_PORTMAP_PORT + "\n"
				+ "nfs.port=" + TEST_NFS_PORT + "\n"
				+ "mount.port=" + TEST_MOUNT_PORT + "\n"
				+ "export.name=/export\n"
				+ "export.path=" + root.toString().replace( "\\", "\\\\") + "\n"
				+ "export.writable=true\n"
				+ "exports.count=2\n"
				+ "exports.1.name=/export\n"
				+ "exports.1.path=" + root.toString().replace( "\\", "\\\\") + "\n"
				+ "exports.1.writable=true\n"
				+ "exports.2.name=/readonly\n"
				+ "exports.2.path=" + readOnlyRoot.toString().replace( "\\", "\\\\") + "\n"
				+ "exports.2.writable=false\n"
				+ "uid=0\n"
				+ "gid=0\n"
				+ "file.mode=0644\n"
				+ "directory.mode=0755\n"
				+ "block.size=4096\n"
				+ "read.size=8192\n"
				+ "write.sync=true\n" ;
		Files.writeString( configPath, configText, StandardCharsets.UTF_8) ;
		NfsServerConfig config = NfsServerConfig.load( configPath) ;
		FileHandleTable handleTable = new FileHandleTable( config.getExports()) ;
		MountV1Program mountProgram = new MountV1Program( config, handleTable) ;
		NfsV2Program nfsProgram = new NfsV2Program( config, handleTable) ;

		try {
			FileHandle rootHandle = mountExport( mountProgram, "/export") ;
			FileHandle readOnlyHandle = mountExport( mountProgram, "/readonly") ;
			FileHandle writableFile = assertLookupFile( nfsProgram, rootHandle) ;
			FileHandle readOnlyFile = assertLookupFile( nfsProgram, readOnlyHandle) ;
			assertWriteFile( nfsProgram, writableFile, root) ;
			assertWriteReadOnlyFile( nfsProgram, readOnlyFile, readOnlyRoot) ;
		} finally {
			nfsProgram.close() ;
			deleteDirectory( root) ;
			deleteDirectory( readOnlyRoot) ;
			deleteDirectory( data) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定値検証を確認します。<br><br>
	 *
	 * <p>メソッド名称： 設定値検証確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testConfigValidation() throws Exception {
		Path root = Path.of( "work", "tmp", "test-config-validation").toAbsolutePath().normalize() ;
		deleteDirectory( root) ;
		Files.createDirectories( root) ;

		try {
			Path validConfig = writeConfig(
					"test-valid-config.properties",
					root,
					"/export",
					"127.0.0.1,192.168.1.30") ;
			NfsServerConfig config = NfsServerConfig.load( validConfig) ;

			assertEquals( "allowed client count", 2, config.getExports().get( 0).getAllowedClients().size()) ;
			assertTrue( "allowed client", config.getExports().get( 0).allowsClient( "127.0.0.1")) ;
			assertFalse( "denied client", config.getExports().get( 0).allowsClient( "192.0.2.10")) ;
			assertTrue( "rpc udp workers", config.getRpcUdpWorkers() > 0) ;
			assertEquals( "rpc udp queue size", 1024, config.getRpcUdpQueueSize()) ;
			assertEquals( "rpc tcp timeout", 30000, config.getRpcTcpTimeoutMillis()) ;

			Path invalidNameConfig = writeConfig(
					"test-invalid-name-config.properties",
					root,
					"export",
					"" ) ;
			assertThrows( "invalid export name", () -> NfsServerConfig.load( invalidNameConfig)) ;

			Path invalidClientConfig = writeConfig(
					"test-invalid-client-config.properties",
					root,
					"/export",
					"999.0.0.1") ;
			assertThrows( "invalid allowed client", () -> NfsServerConfig.load( invalidClientConfig)) ;

			Path missingRoot = root.resolve( "missing-export") ;
			Path missingPathConfig = writeConfig(
					"test-missing-path-config.properties",
					missingRoot,
					"/export",
					"" ) ;
			assertThrows( "missing export path", () -> NfsServerConfig.load( missingPathConfig)) ;

			Path invalidPathconfConfig = writeConfig(
					"test-invalid-pathconf-config.properties",
					root,
					"/export",
					"" ) ;
			Files.writeString(
					invalidPathconfConfig,
					Files.readString( invalidPathconfConfig, StandardCharsets.UTF_8) + "pathconf.name.max=300\n",
					StandardCharsets.UTF_8) ;
			assertThrows( "invalid pathconf name max", () -> NfsServerConfig.load( invalidPathconfConfig)) ;

			Path invalidUdpWorkersConfig = writeConfig(
					"test-invalid-udp-workers-config.properties",
					root,
					"/export",
					"" ) ;
			Files.writeString(
					invalidUdpWorkersConfig,
					Files.readString( invalidUdpWorkersConfig, StandardCharsets.UTF_8) + "rpc.udp.workers=0\n",
					StandardCharsets.UTF_8) ;
			assertThrows( "invalid udp workers", () -> NfsServerConfig.load( invalidUdpWorkersConfig)) ;

			Path invalidTcpTimeoutConfig = writeConfig(
					"test-invalid-tcp-timeout-config.properties",
					root,
					"/export",
					"" ) ;
			Files.writeString(
					invalidTcpTimeoutConfig,
					Files.readString( invalidTcpTimeoutConfig, StandardCharsets.UTF_8) + "rpc.tcp.timeout.millis=0\n",
					StandardCharsets.UTF_8) ;
			assertThrows( "invalid tcp timeout", () -> NfsServerConfig.load( invalidTcpTimeoutConfig)) ;
		} finally {
			deleteDirectory( root) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定ファイル基準の相対公開パス解決を確認します。<br><br>
	 *
	 * <p>メソッド名称： 設定相対公開パス解決確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testConfigRelativeExportBase() throws Exception {
		Path dataRoot = Path.of( "work", "tmp", "test-config-relative-base").toAbsolutePath().normalize() ;
		Path configDirectory = dataRoot.resolve( "conf") ;
		Path exportRoot = dataRoot.resolve( "export") ;
		Path configPath = configDirectory.resolve( "nfs-server.properties") ;
		deleteDirectory( dataRoot) ;
		Files.createDirectories( configDirectory) ;
		Files.createDirectories( exportRoot) ;

		try {
			String configText = ""
					+ "portmap.port=" + TEST_PORTMAP_PORT + "\n"
					+ "nfs.port=" + TEST_NFS_PORT + "\n"
					+ "mount.port=" + TEST_MOUNT_PORT + "\n"
					+ "export.name=/export\n"
					+ "export.path=export\n"
					+ "export.writable=true\n"
					+ "uid=0\n"
					+ "gid=0\n"
					+ "file.mode=0644\n"
					+ "directory.mode=0755\n"
					+ "block.size=4096\n"
					+ "read.size=8192\n"
					+ "write.sync=true\n"
					+ "filename.charset=UTF-8\n" ;
			Files.writeString( configPath, configText, StandardCharsets.UTF_8) ;
			NfsServerConfig config = NfsServerConfig.load( configPath) ;

			assertEquals( "relative export path", exportRoot.toString(), config.getExports().get( 0).getPath().toString()) ;
		} finally {
			deleteDirectory( dataRoot) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定バックアップを確認します。<br><br>
	 *
	 * <p>メソッド名称： 設定バックアップ確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testConfigBackup() throws Exception {
		Path root = Path.of( "work", "tmp", "test-config-backup").toAbsolutePath().normalize() ;
		Path configDirectory = root.resolve( "conf") ;
		Path configPath = configDirectory.resolve( "nfs-server.properties") ;

		deleteDirectory( root) ;
		Files.createDirectories( configDirectory) ;

		try {
			Files.writeString( configPath, "version=0", StandardCharsets.UTF_8) ;
			Path firstBackup = ConfigBackup.backupIfExists( configPath) ;

			assertTrue( "first backup exists", Files.exists( firstBackup)) ;
			assertEquals( "first backup content", "version=0", Files.readString( firstBackup, StandardCharsets.UTF_8)) ;

			// バックアップ保持世代を超える回数の保存を行う
			for( int i = 1; i <= 12; i++) {
				Files.writeString( configPath, "version=" + i, StandardCharsets.UTF_8) ;
				ConfigBackup.backupIfExists( configPath) ;
			}

			Path backupDirectory = configDirectory.resolve( "backups") ;

			try( var stream = Files.list( backupDirectory)) {
				long backupCount = stream
						.filter( path -> path.getFileName().toString().startsWith( "nfs-server-" ) )
						.count() ;
				assertEquals( "backup count", 10L, backupCount) ;
			}
		} finally {
			deleteDirectory( root) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 運用診断を確認します。<br><br>
	 *
	 * <p>メソッド名称： 運用診断確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testOperationalDiagnostics() throws Exception {
		Path root = Path.of( "work", "tmp", "test-operational-diagnostics").toAbsolutePath().normalize() ;
		deleteDirectory( root) ;
		Files.createDirectories( root.resolve( "nested")) ;
		Files.writeString( root.resolve( "hello.txt"), "hello", StandardCharsets.UTF_8) ;
		Files.writeString( root.resolve( "nested").resolve( "child.txt"), "child", StandardCharsets.UTF_8) ;

		try {
			List<CaseCollision> collisions = NfsDiagnostics.detectCaseCollisions( List.of(
					"SKS/inc/File.h",
					"SKS/inc/file.h",
					"SKS/inc/Other.h")) ;

			assertEquals( "case collision count", 1, collisions.size()) ;
			assertEquals( "case collision normalized path", "sks/inc/file.h", collisions.get( 0).getNormalizedPath()) ;

			Path configPath = writeConfig(
					"test-operational-diagnostics.properties",
					root,
					"/export",
					"" ) ;
			NfsServerConfig config = NfsServerConfig.load( configPath) ;
			DiagnosticReport report = NfsDiagnostics.collect( config) ;
			String text = report.formatText() ;

			assertEquals( "diagnostic export count", 1, report.getExportReports().size()) ;
			assertEquals( "diagnostic file count", 2L, report.getExportReports().get( 0).getFileCount()) ;
			assertEquals( "diagnostic directory count", 1L, report.getExportReports().get( 0).getDirectoryCount()) ;
			assertTrue( "diagnostic bytes", report.getExportReports().get( 0).getTotalBytes() > 0L) ;
			assertTrue( "diagnostic allowed clients", text.contains( "CONFIG_ALLOWED_CLIENTS_ANY")) ;
		} finally {
			deleteDirectory( root) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアントアクセス制限を確認します。<br><br>
	 *
	 * <p>メソッド名称： クライアントアクセス制限確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testClientAccessRestrictions() throws Exception {
		Path root = Path.of( "work", "tmp", "test-access-restrictions").toAbsolutePath().normalize() ;
		Path data = Path.of( "work", "tmp", "test-access-data").toAbsolutePath().normalize() ;
		deleteDirectory( root) ;
		deleteDirectory( data) ;
		System.setProperty( "tinywin.nfs.data", data.toString()) ;
		Files.createDirectories( root) ;
		Files.writeString( root.resolve( "hello.txt"), "hello qnx", StandardCharsets.UTF_8) ;
		Path configPath = writeConfig(
				"test-access-restrictions.properties",
				root,
				"/export",
				"127.0.0.1") ;
		NfsServerConfig config = NfsServerConfig.load( configPath) ;
		FileHandleTable handleTable = new FileHandleTable( config.getExports()) ;
		MountV1Program mountProgram = new MountV1Program( config, handleTable) ;
		NfsV2Program nfsProgram = new NfsV2Program( config, handleTable) ;

		try {
			XdrWriter deniedMountArguments = new XdrWriter() ;
			deniedMountArguments.writeString( "/export") ;
			XdrWriter deniedMountResponse = handleWithClient(
					mountProgram,
					RpcConstants.PROGRAM_MOUNT,
					1,
					1,
					deniedMountArguments,
					"192.0.2.10") ;

			assertEquals( "denied mount status", NfsStatus.ACCES, new XdrReader( deniedMountResponse.toByteArray()).readInt()) ;

			FileHandle rootHandle = mountExportWithClient( mountProgram, "/export", "127.0.0.1") ;
			XdrWriter deniedGetAttrArguments = new XdrWriter() ;
			deniedGetAttrArguments.writeFixedOpaque( rootHandle.getValue()) ;
			XdrWriter deniedGetAttrResponse = handleWithClient(
					nfsProgram,
					RpcConstants.PROGRAM_NFS,
					2,
					1,
					deniedGetAttrArguments,
					"192.0.2.10") ;

			assertEquals( "denied getattr status", NfsStatus.ACCES, new XdrReader( deniedGetAttrResponse.toByteArray()).readInt()) ;

			XdrWriter allowedGetAttrArguments = new XdrWriter() ;
			allowedGetAttrArguments.writeFixedOpaque( rootHandle.getValue()) ;
			XdrWriter allowedGetAttrResponse = handleWithClient(
					nfsProgram,
					RpcConstants.PROGRAM_NFS,
					2,
					1,
					allowedGetAttrArguments,
					"127.0.0.1") ;

			assertEquals( "allowed getattr status", NfsStatus.OK, new XdrReader( allowedGetAttrResponse.toByteArray()).readInt()) ;
		} finally {
			nfsProgram.close() ;
			deleteDirectory( root) ;
			deleteDirectory( data) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2手続きテストを行います。<br><br>
	 *
	 * <p>メソッド名称： NFSv2手続きテスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testNfsV2Procedures() throws Exception {
		TestContext context = createContext() ;
		NfsV2Program program = new NfsV2Program( context.getConfig(), context.getHandleTable()) ;
		FileHandle rootHandle = context.getHandleTable().getRootHandle() ;
		FileHandle fileHandle = assertLookupFile( program, rootHandle) ;
		assertGetAttrRoot( program, rootHandle) ;
		assertGetAttrRootAuthSys( program, rootHandle) ;
		assertReadFile( program, fileHandle) ;
		assertWriteCache( program) ;
		assertWriteFile( program, fileHandle, context.getRoot()) ;
		assertQnxFixedOpaqueWriteFile( program, fileHandle, context.getRoot()) ;
		assertQnxUnpaddedOpaqueWriteFile( program, fileHandle, context.getRoot()) ;
		assertSetAttrFile( program, fileHandle, context.getRoot()) ;
		assertSetAttrMode( program, fileHandle) ;
		assertSetAttrTime( program, fileHandle) ;
		assertBidirectionalEditFile( program, fileHandle, context.getRoot()) ;
		assertTempRenameOverwriteFile( program, rootHandle, context.getRoot()) ;
		assertCreateFile( program, rootHandle, context.getRoot()) ;
		assertRenameFile( program, rootHandle, context.getRoot()) ;
		assertMkdirAndRmdir( program, rootHandle, context.getRoot()) ;
		assertRemoveDirectoryCompatibility( program, rootHandle, context.getRoot()) ;
		assertReadDir( program, rootHandle) ;
		assertStatFs( program, rootHandle) ;
		assertInvalidLookup( program, rootHandle) ;
		assertReadLinkOnRegularFile( program, fileHandle) ;
		assertHardLink( program, rootHandle, fileHandle, context.getRoot()) ;
		assertLongSymlinkTarget( program, rootHandle) ;
		assertSymbolicLinkV2( program, rootHandle, context.getRoot()) ;
		assertBrokenSymbolicLinkV2( program, rootHandle, context.getRoot()) ;
		assertJapaneseCreate( program, rootHandle, context.getRoot()) ;
		program.close() ;
		context.close() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2大量READDIRを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2大量READDIR確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testNfsV2LargeReadDir() throws Exception {
		TestContext context = createContext() ;
		NfsV2Program program = new NfsV2Program( context.getConfig(), context.getHandleTable()) ;

		try {
			FileHandle rootHandle = context.getHandleTable().getRootHandle() ;

			for( int i = 0; i < 180; i++) {
				Files.writeString( context.getRoot().resolve( String.format( "bulk-%03d.txt", i)), "bulk-" + i, StandardCharsets.UTF_8) ;
			}

			int cookie = 0 ;
			boolean eof = false ;
			List<String> names = new ArrayList<String>() ;

			for( int page = 0; page < 40 && !eof; page++) {
				ReadDirPage current = readDirPage( program, rootHandle, cookie, 512) ;
				assertTrue( "large readdir page not empty", current.isEof() || !current.getNames().isEmpty()) ;
				names.addAll( current.getNames()) ;
				eof = current.isEof() ;

				// 途中ページの場合
				if( !eof) {
					cookie = current.getLastCookie() ;
				}
			}

			assertTrue( "large readdir eof", eof) ;
			assertEquals( "large readdir no duplicate", names.size(), new HashSet<String>( names).size()) ;
			assertTrue( "large readdir first bulk", names.contains( "bulk-000.txt")) ;
			assertTrue( "large readdir last bulk", names.contains( "bulk-179.txt")) ;
		} finally {
			program.close() ;
			context.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3手続きテストを行います。<br><br>
	 *
	 * <p>メソッド名称： NFSv3手続きテスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testNfsV3Procedures() throws Exception {
		TestContext context = createContext() ;
		MountV1Program mountProgram = new MountV1Program( context.getConfig(), context.getHandleTable()) ;
		NfsV3Program program = new NfsV3Program( context.getConfig(), context.getHandleTable()) ;
		PortmapV2Program portmap = new PortmapV2Program( TEST_PORTMAP_PORT, TEST_NFS_PORT, TEST_MOUNT_PORT) ;

		try {
			FileHandle rootHandle = mountExportV3( mountProgram, portmap) ;
			FileHandle fileHandle = assertLookupFileV3( program, rootHandle) ;
			assertGetAttrV3( program, rootHandle) ;
			assertGetAttrAuthSysV3( program, rootHandle) ;
			assertAccessV3( program, rootHandle) ;
			assertReadFileV3( program, fileHandle) ;
			assertReadLinkOnRegularFileV3( program, fileHandle) ;
			assertWriteAndCommitV3( program, fileHandle, context.getRoot()) ;
			assertSetAttrV3( program, fileHandle, context.getRoot()) ;
			assertCreateRemoveV3( program, rootHandle, context.getRoot()) ;
			assertMkdirRmdirV3( program, rootHandle, context.getRoot()) ;
			assertRenameV3( program, rootHandle, context.getRoot()) ;
			assertSymbolicLinkV3( program, rootHandle, context.getRoot()) ;
			assertBrokenSymbolicLinkV3( program, rootHandle, context.getRoot()) ;
			assertInvalidSymlinkTargetV3( program, rootHandle, context.getRoot()) ;
			assertMknodV3( program, rootHandle, context.getRoot()) ;
			assertReadDirPlusV3( program, rootHandle) ;
			assertFsInfoV3( program, rootHandle) ;
		} finally {
			program.close() ;
			context.close() ;
		}

		TestContext asyncContext = createContext( "UTF-8", false) ;
		MountV1Program asyncMountProgram = new MountV1Program( asyncContext.getConfig(), asyncContext.getHandleTable()) ;
		NfsV3Program asyncProgram = new NfsV3Program( asyncContext.getConfig(), asyncContext.getHandleTable()) ;

		try {
			FileHandle rootHandle = mountExportV3( asyncMountProgram, portmap) ;
			FileHandle fileHandle = assertLookupFileV3( asyncProgram, rootHandle) ;
			assertAsyncWriteV3( asyncProgram, fileHandle, asyncContext.getRoot()) ;
		} finally {
			asyncProgram.close() ;
			asyncContext.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSステータスと属性応答を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSステータス属性応答確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testNfsStatusAndAttributes() throws Exception {
		TestContext context = createContext() ;
		NfsV2Program v2Program = new NfsV2Program( context.getConfig(), context.getHandleTable()) ;
		NfsV3Program v3Program = new NfsV3Program( context.getConfig(), context.getHandleTable()) ;

		try {
			FileHandle rootHandle = context.getHandleTable().getRootHandle() ;
			FileHandle fileHandle = assertLookupFile( v2Program, rootHandle) ;
			assertStatusAndAttributesV2( v2Program, rootHandle, fileHandle) ;
			assertStatusAndAttributesV3( v3Program, rootHandle, fileHandle) ;
		} finally {
			v2Program.close() ;
			v3Program.close() ;
			context.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 複数クライアント相当の相互編集を確認します。<br><br>
	 *
	 * <p>メソッド名称： 複数クライアント相互編集確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testCrossClientEditRegression() throws Exception {
		TestContext context = createContext( "UTF-8", false) ;
		WriteFileCache writeFileCache = new WriteFileCache( context.getConfig()) ;
		NfsV2Program v2Program = new NfsV2Program( context.getConfig(), context.getHandleTable(), writeFileCache) ;
		NfsV3Program v3Program = new NfsV3Program( context.getConfig(), context.getHandleTable(), writeFileCache) ;

		try {
			FileHandle rootHandle = context.getHandleTable().getRootHandle() ;
			FileHandle v2FileHandle = assertLookupFile( v2Program, rootHandle) ;
			FileHandle v3FileHandle = assertLookupFileV3( v3Program, rootHandle) ;
			Path file = context.getRoot().resolve( "hello.txt") ;

			Files.writeString( file, "windows side edit", StandardCharsets.UTF_8) ;
			assertEquals( "cross edit v2 read windows", "windows side edit", readFileValue( v2Program, v2FileHandle)) ;

			writeFileByV2( v2Program, v2FileHandle, "qnx side edit") ;
			assertEquals( "cross edit windows read v2", "qnx side edit", Files.readString( file, StandardCharsets.UTF_8)) ;

			Files.writeString( file, "windows side v3 edit", StandardCharsets.UTF_8) ;
			assertEquals( "cross edit v3 read windows", "windows side v3 edit", readFileValueV3( v3Program, v3FileHandle)) ;

			writeFileByV3( v3Program, v3FileHandle, "windows client edit") ;
			assertEquals( "cross edit windows read v3", "windows client edit", Files.readString( file, StandardCharsets.UTF_8)) ;
		} finally {
			v2Program.close() ;
			v3Program.close() ;
			context.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 書込キャッシュflushを確認します。<br><br>
	 *
	 * <p>メソッド名称： 書込キャッシュflush確認</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testWriteCacheFlush() throws Exception {
		Path root = Path.of( "work", "tmp", "test-write-cache-flush").toAbsolutePath().normalize() ;
		deleteDirectory( root) ;
		Files.createDirectories( root) ;

		try {
			Path configPath = writeConfig(
					"test-write-cache-flush.properties",
					root,
					"/export",
					"" ) ;
			Files.writeString(
					configPath,
					Files.readString( configPath, StandardCharsets.UTF_8)
							+ "write.sync=false\n"
							+ "write.cache.enabled=true\n"
							+ "write.cache.max.open=1\n"
							+ "write.cache.idle.millis=3000\n",
					StandardCharsets.UTF_8) ;
			NfsServerConfig config = NfsServerConfig.load( configPath) ;
			Path first = root.resolve( "cache-first.txt") ;
			Path second = root.resolve( "cache-second.txt") ;

			try( WriteFileCache cache = new WriteFileCache( config)) {
				cache.write( first, 0, "first".getBytes( StandardCharsets.UTF_8), 5, false) ;
				cache.write( second, 0, "second".getBytes( StandardCharsets.UTF_8), 6, false) ;
				assertEquals( "write cache evicted first", "first", Files.readString( first, StandardCharsets.UTF_8)) ;
				cache.sync( second) ;
			}

			assertEquals( "write cache closed second", "second", Files.readString( second, StandardCharsets.UTF_8)) ;
		} finally {
			deleteDirectory( root) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * MOUNT v3で公開をマウントします。<br><br>
	 *
	 * <p>メソッド名称： MOUNT v3公開マウント</p>
	 *
	 * @param mountProgram	MOUNTプログラム
	 * @param portmap		Portmapプログラム
	 * @return ルートファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle mountExportV3(MountV1Program mountProgram, PortmapV2Program portmap) throws IOException {
		XdrWriter nfsPortArguments = new XdrWriter() ;
		nfsPortArguments.writeInt( RpcConstants.PROGRAM_NFS) ;
		nfsPortArguments.writeInt( 3) ;
		nfsPortArguments.writeInt( RpcConstants.IPPROTO_UDP) ;
		nfsPortArguments.writeInt( 0) ;
		XdrReader nfsPortReader = new XdrReader( handle( portmap, RpcConstants.PROGRAM_PORTMAP, 2, 3, nfsPortArguments).toByteArray()) ;

		assertEquals( "nfs v3 port", TEST_NFS_PORT, nfsPortReader.readInt()) ;

		XdrWriter mountPortArguments = new XdrWriter() ;
		mountPortArguments.writeInt( RpcConstants.PROGRAM_MOUNT) ;
		mountPortArguments.writeInt( 3) ;
		mountPortArguments.writeInt( RpcConstants.IPPROTO_UDP) ;
		mountPortArguments.writeInt( 0) ;
		XdrReader mountPortReader = new XdrReader( handle( portmap, RpcConstants.PROGRAM_PORTMAP, 2, 3, mountPortArguments).toByteArray()) ;

		assertEquals( "mount v3 port", TEST_MOUNT_PORT, mountPortReader.readInt()) ;

		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( "/export") ;
		XdrWriter response = handle( mountProgram, RpcConstants.PROGRAM_MOUNT, 3, 1, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "mount v3 status", 0, reader.readInt()) ;
		FileHandle handle = new FileHandle( reader.readOpaque()) ;
		assertEquals( "mount v3 auth flavor count", 2, reader.readInt()) ;
		assertEquals( "mount v3 auth none", RpcConstants.AUTH_NONE, reader.readInt()) ;
		assertEquals( "mount v3 auth sys", RpcConstants.AUTH_SYS, reader.readInt()) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 LOOKUPを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 LOOKUP確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @return ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle assertLookupFileV3(NfsV3Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargsV3( arguments, rootHandle, "hello.txt") ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 3, arguments).toByteArray()) ;

		assertEquals( "v3 lookup status", NfsStatus.OK, reader.readInt()) ;
		FileHandle handle = new FileHandle( reader.readOpaque()) ;
		skipPostOpAttrV3( reader) ;
		skipPostOpAttrV3( reader) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 GETATTRを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 GETATTR確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertGetAttrV3(NfsV3Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 1, arguments).toByteArray()) ;

		assertEquals( "v3 getattr status", NfsStatus.OK, reader.readInt()) ;
		skipAttributesV3( reader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 GETATTRのAUTH_SYS属性反映を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 GETATTR AUTH_SYS確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertGetAttrAuthSysV3(NfsV3Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		XdrReader reader = new XdrReader( handleWithAuthSys( program, RpcConstants.PROGRAM_NFS, 3, 1, arguments, -2, -2).toByteArray()) ;

		assertEquals( "v3 authsys getattr status", NfsStatus.OK, reader.readInt()) ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		assertEquals( "v3 authsys uid", -2, reader.readInt()) ;
		assertEquals( "v3 authsys gid", -2, reader.readInt()) ;
		skipAttributesV3Remainder( reader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 ACCESSを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 ACCESS確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertAccessV3(NfsV3Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		arguments.writeInt( 0x003f) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 4, arguments).toByteArray()) ;

		assertEquals( "v3 access status", NfsStatus.OK, reader.readInt()) ;
		skipPostOpAttrV3( reader) ;
		assertTrue( "v3 access read", (reader.readInt() & 0x0001) != 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 READを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 READ確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadFileV3(NfsV3Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		arguments.writeLong( 0L) ;
		arguments.writeInt( 8192) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 6, arguments).toByteArray()) ;

		assertEquals( "v3 read status", NfsStatus.OK, reader.readInt()) ;
		skipPostOpAttrV3( reader) ;
		assertEquals( "v3 read count", "hello qnx".length(), reader.readInt()) ;
		assertTrue( "v3 read eof", reader.readBoolean()) ;
		assertEquals( "v3 read content", "hello qnx", new String( reader.readOpaque(), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3通常ファイルREADLINKを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3通常ファイルREADLINK確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadLinkOnRegularFileV3(NfsV3Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 5, arguments).toByteArray()) ;

		assertEquals( "v3 regular readlink status", NfsStatus.INVAL, reader.readInt()) ;
		skipPostOpAttrV3( reader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 WRITE/COMMITを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 WRITE/COMMIT確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertWriteAndCommitV3(NfsV3Program program, FileHandle handle, Path root) throws IOException {
		byte[] data = "v3 edit".getBytes( StandardCharsets.UTF_8) ;
		XdrWriter writeArguments = new XdrWriter() ;
		writeArguments.writeOpaque( handle.getValue()) ;
		writeArguments.writeLong( 0L) ;
		writeArguments.writeInt( data.length) ;
		writeArguments.writeInt( 2) ;
		writeArguments.writeOpaque( data) ;
		XdrReader writeReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 7, writeArguments).toByteArray()) ;

		assertEquals( "v3 write status", NfsStatus.OK, writeReader.readInt()) ;
		assertWccPreSizeV3( writeReader, "hello qnx".length()) ;
		assertEquals( "v3 write count", data.length, writeReader.readInt()) ;
		assertEquals( "v3 write committed", 2, writeReader.readInt()) ;
		assertEquals( "v3 write verifier", 8, writeReader.readFixedOpaque( 8).length) ;
		assertEquals( "v3 write content", "v3 editnx", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;

		XdrWriter commitArguments = new XdrWriter() ;
		commitArguments.writeOpaque( handle.getValue()) ;
		commitArguments.writeLong( 0L) ;
		commitArguments.writeInt( data.length) ;
		XdrReader commitReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 21, commitArguments).toByteArray()) ;

		assertEquals( "v3 commit status", NfsStatus.OK, commitReader.readInt()) ;
		assertWccPreSizeV3( commitReader, "v3 editnx".length()) ;
		assertEquals( "v3 commit verifier", 8, commitReader.readFixedOpaque( 8).length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3非同期WRITEを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3非同期WRITE確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertAsyncWriteV3(NfsV3Program program, FileHandle handle, Path root) throws IOException {
		byte[] data = "v3 async".getBytes( StandardCharsets.UTF_8) ;
		XdrWriter writeArguments = new XdrWriter() ;
		writeArguments.writeOpaque( handle.getValue()) ;
		writeArguments.writeLong( 0L) ;
		writeArguments.writeInt( data.length) ;
		writeArguments.writeInt( 2) ;
		writeArguments.writeOpaque( data) ;
		XdrReader writeReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 7, writeArguments).toByteArray()) ;

		assertEquals( "v3 async write status", NfsStatus.OK, writeReader.readInt()) ;
		assertWccPreSizeV3( writeReader, "hello qnx".length()) ;
		assertEquals( "v3 async write count", data.length, writeReader.readInt()) ;
		assertEquals( "v3 async write committed", 0, writeReader.readInt()) ;
		assertEquals( "v3 async write verifier", 8, writeReader.readFixedOpaque( 8).length) ;
		assertEquals( "v3 async write content", "v3 asyncx", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 SETATTRを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 SETATTR確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertSetAttrV3(NfsV3Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		writeSattr3Size( arguments, 4L) ;
		arguments.writeBoolean( false) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 2, arguments).toByteArray()) ;

		assertEquals( "v3 setattr status", NfsStatus.OK, reader.readInt()) ;
		assertWccPreSizeV3( reader, "v3 editnx".length()) ;
		assertEquals( "v3 setattr content", "v3 e", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 CREATE/REMOVEを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 CREATE/REMOVE確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertCreateRemoveV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		XdrWriter createArguments = new XdrWriter() ;
		writeDiropargsV3( createArguments, rootHandle, "v3-created.txt") ;
		createArguments.writeInt( 0) ;
		writeSattr3Unset( createArguments) ;
		XdrReader createReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 8, createArguments).toByteArray()) ;

		assertEquals( "v3 create status", NfsStatus.OK, createReader.readInt()) ;
		assertTrue( "v3 create handle follows", createReader.readBoolean()) ;
		assertEquals( "v3 create handle", FileHandle.LENGTH, createReader.readOpaque().length) ;
		skipPostOpAttrV3( createReader) ;
		skipWccDataV3( createReader) ;
		assertTrue( "v3 create exists", Files.exists( root.resolve( "v3-created.txt"), LinkOption.NOFOLLOW_LINKS)) ;

		XdrWriter removeArguments = new XdrWriter() ;
		writeDiropargsV3( removeArguments, rootHandle, "v3-created.txt") ;
		XdrReader removeReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 12, removeArguments).toByteArray()) ;

		assertEquals( "v3 remove status", NfsStatus.OK, removeReader.readInt()) ;
		skipWccDataV3( removeReader) ;
		assertTrue( "v3 remove missing", !Files.exists( root.resolve( "v3-created.txt"), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 MKDIR/RMDIRを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 MKDIR/RMDIR確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertMkdirRmdirV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		XdrWriter mkdirArguments = new XdrWriter() ;
		writeDiropargsV3( mkdirArguments, rootHandle, "v3-dir") ;
		writeSattr3Unset( mkdirArguments) ;
		XdrReader mkdirReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 9, mkdirArguments).toByteArray()) ;

		assertEquals( "v3 mkdir status", NfsStatus.OK, mkdirReader.readInt()) ;
		assertTrue( "v3 mkdir handle follows", mkdirReader.readBoolean()) ;
		mkdirReader.readOpaque() ;
		skipPostOpAttrV3( mkdirReader) ;
		skipWccDataV3( mkdirReader) ;
		assertTrue( "v3 mkdir exists", Files.isDirectory( root.resolve( "v3-dir"), LinkOption.NOFOLLOW_LINKS)) ;

		XdrWriter rmdirArguments = new XdrWriter() ;
		writeDiropargsV3( rmdirArguments, rootHandle, "v3-dir") ;
		XdrReader rmdirReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 13, rmdirArguments).toByteArray()) ;

		assertEquals( "v3 rmdir status", NfsStatus.OK, rmdirReader.readInt()) ;
		skipWccDataV3( rmdirReader) ;
		assertTrue( "v3 rmdir missing", !Files.exists( root.resolve( "v3-dir"), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 RENAMEを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 RENAME確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertRenameV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		Files.writeString( root.resolve( "v3-rename-source.txt"), "rename", StandardCharsets.UTF_8) ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargsV3( arguments, rootHandle, "v3-rename-source.txt") ;
		writeDiropargsV3( arguments, rootHandle, "v3-rename-target.txt") ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 14, arguments).toByteArray()) ;

		assertEquals( "v3 rename status", NfsStatus.OK, reader.readInt()) ;
		skipWccDataV3( reader) ;
		skipWccDataV3( reader) ;
		assertTrue( "v3 rename source missing", !Files.exists( root.resolve( "v3-rename-source.txt"), LinkOption.NOFOLLOW_LINKS)) ;
		assertEquals( "v3 rename target", "rename", Files.readString( root.resolve( "v3-rename-target.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 READDIRPLUSを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 READDIRPLUS確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadDirPlusV3(NfsV3Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( rootHandle.getValue()) ;
		arguments.writeLong( 0L) ;
		arguments.writeFixedOpaque( new byte[8]) ;
		arguments.writeInt( 4096) ;
		arguments.writeInt( 8192) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 17, arguments).toByteArray()) ;
		List<String> names = new ArrayList<String>() ;

		assertEquals( "v3 readdirplus status", NfsStatus.OK, reader.readInt()) ;
		skipPostOpAttrV3( reader) ;
		reader.readFixedOpaque( 8) ;

		while( reader.readBoolean()) {
			reader.readLong() ;
			names.add( reader.readString()) ;
			reader.readLong() ;
			skipPostOpAttrV3( reader) ;
			assertTrue( "v3 readdirplus handle follows", reader.readBoolean()) ;
			reader.readOpaque() ;
		}

		assertTrue( "v3 readdirplus eof", reader.readBoolean()) ;
		assertTrue( "v3 readdirplus file", names.contains( "hello.txt")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 FSINFO/FSSTAT/PATHCONFを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 FSINFO/FSSTAT/PATHCONF確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertFsInfoV3(NfsV3Program program, FileHandle rootHandle) throws IOException {
		XdrWriter fsInfoArguments = new XdrWriter() ;
		fsInfoArguments.writeOpaque( rootHandle.getValue()) ;
		XdrReader fsInfoReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 19, fsInfoArguments).toByteArray()) ;

		assertEquals( "v3 fsinfo status", NfsStatus.OK, fsInfoReader.readInt()) ;
		skipPostOpAttrV3( fsInfoReader) ;
		assertEquals( "v3 fsinfo rtmax", 8192, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo rtpref", 8192, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo rtmult", 4096, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo wtmax", 16384, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo wtpref", 16384, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo wtmult", 4096, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo dtpref", 4096, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo maxfilesize", 1099511627776L, fsInfoReader.readLong()) ;
		assertEquals( "v3 fsinfo time delta seconds", 0, fsInfoReader.readInt()) ;
		assertEquals( "v3 fsinfo time delta nanos", 1000000, fsInfoReader.readInt()) ;
		assertTrue( "v3 fsinfo properties", fsInfoReader.readInt() != 0) ;

		XdrWriter fsStatArguments = new XdrWriter() ;
		fsStatArguments.writeOpaque( rootHandle.getValue()) ;
		XdrReader fsStatReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 18, fsStatArguments).toByteArray()) ;

		assertEquals( "v3 fsstat status", NfsStatus.OK, fsStatReader.readInt()) ;
		skipPostOpAttrV3( fsStatReader) ;
		assertTrue( "v3 fsstat total", fsStatReader.readLong() > 0L) ;
		assertTrue( "v3 fsstat free", fsStatReader.readLong() >= 0L) ;
		assertTrue( "v3 fsstat available", fsStatReader.readLong() >= 0L) ;
		assertTrue( "v3 fsstat total files", fsStatReader.readLong() > 0L) ;
		fsStatReader.readLong() ;
		fsStatReader.readLong() ;
		assertEquals( "v3 fsstat invariant seconds", 0, fsStatReader.readInt()) ;

		XdrWriter pathConfArguments = new XdrWriter() ;
		pathConfArguments.writeOpaque( rootHandle.getValue()) ;
		XdrReader pathConfReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 20, pathConfArguments).toByteArray()) ;

		assertEquals( "v3 pathconf status", NfsStatus.OK, pathConfReader.readInt()) ;
		skipPostOpAttrV3( pathConfReader) ;
		assertEquals( "v3 pathconf link max", 1024, pathConfReader.readInt()) ;
		assertEquals( "v3 pathconf name max", 255, pathConfReader.readInt()) ;
		assertTrue( "v3 pathconf no trunc", pathConfReader.readBoolean()) ;
		assertTrue( "v3 pathconf chown restricted", pathConfReader.readBoolean()) ;
		assertTrue( "v3 pathconf case insensitive", pathConfReader.readBoolean()) ;
		assertTrue( "v3 pathconf case preserving", pathConfReader.readBoolean()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 symlink作成を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 symlink作成確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertSymbolicLinkV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		String linkName = "v3-link.txt" ;
		String linkTarget = "hello.txt" ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargsV3( arguments, rootHandle, linkName) ;
		writeSattr3Unset( arguments) ;
		arguments.writeString( linkTarget) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 10, arguments).toByteArray()) ;
		int status = reader.readInt() ;
		Path linkPath = root.resolve( linkName) ;

		// Windows環境でsymlink作成権限がない場合は安定したエラーを確認する
		if( status != NfsStatus.OK) {
			assertLinkCreationFailureStatus( "v3 symlink status", status) ;
			skipWccDataV3( reader) ;
			assertFalse( "v3 symlink not created", Files.exists( linkPath, LinkOption.NOFOLLOW_LINKS)) ;
			return ;
		}

		assertTrue( "v3 symlink handle follows", reader.readBoolean()) ;
		FileHandle linkHandle = new FileHandle( reader.readOpaque()) ;
		skipPostOpAttrV3( reader) ;
		skipWccDataV3( reader) ;
		assertTrue( "v3 symlink exists", Files.isSymbolicLink( linkPath)) ;
		assertEquals( "v3 symlink target", linkTarget, Files.readSymbolicLink( linkPath).toString()) ;
		assertReadLinkV3( program, linkHandle, linkTarget) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3の壊れたsymlink READLINKを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3壊れたsymlink READLINK確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertBrokenSymbolicLinkV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		String linkName = "v3-broken-link.txt" ;
		String linkTarget = "missing-target.txt" ;
		Path linkPath = root.resolve( linkName) ;

		if( !createLocalSymbolicLink( linkPath, linkTarget)) {
			return ;
		}

		FileHandle linkHandle = lookupV3( program, rootHandle, linkName) ;
		assertTrue( "v3 broken symlink exists", Files.isSymbolicLink( linkPath)) ;
		assertFalse( "v3 broken symlink target missing", Files.exists( root.resolve( linkTarget), LinkOption.NOFOLLOW_LINKS)) ;
		assertReadLinkV3( program, linkHandle, linkTarget) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3不正symlink先を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3不正symlink先確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertInvalidSymlinkTargetV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		String linkName = "v3-invalid-link.txt" ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargsV3( arguments, rootHandle, linkName) ;
		writeSattr3Unset( arguments) ;
		arguments.writeString( "bad" + '\0' + "target") ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 10, arguments).toByteArray()) ;

		assertEquals( "v3 invalid symlink target status", NfsStatus.ACCES, reader.readInt()) ;
		skipWccDataV3( reader) ;
		assertFalse( "v3 invalid symlink not created", Files.exists( root.resolve( linkName), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 MKNOD非対応を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 MKNOD非対応確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertMknodV3(NfsV3Program program, FileHandle rootHandle, Path root) throws IOException {
		String name = "v3-special-node" ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargsV3( arguments, rootHandle, name) ;
		arguments.writeInt( 4) ;
		writeSattr3Unset( arguments) ;
		arguments.writeInt( 0) ;
		arguments.writeInt( 0) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 11, arguments).toByteArray()) ;

		assertEquals( "v3 mknod status", NfsStatus.NOTSUPP, reader.readInt()) ;
		skipWccDataV3( reader) ;
		assertFalse( "v3 mknod not created", Files.exists( root.resolve( name), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 READLINKを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 READLINK確認</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @param expected	期待リンク先
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadLinkV3(NfsV3Program program, FileHandle handle, String expected) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 5, arguments).toByteArray()) ;

		assertEquals( "v3 readlink status", NfsStatus.OK, reader.readInt()) ;
		skipPostOpAttrV3( reader) ;
		assertEquals( "v3 readlink target", expected, reader.readString()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2ファイル名文字コードテストを行います。<br><br>
	 *
	 * <p>メソッド名称： NFSv2ファイル名文字コードテスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testNfsV2FilenameCharset() throws Exception {
		TestContext context = createContext( "Shift_JIS") ;
		NfsV2Program program = new NfsV2Program( context.getConfig(), context.getHandleTable()) ;
		FileHandle rootHandle = context.getHandleTable().getRootHandle() ;
		Charset charset = Charset.forName( "Shift_JIS") ;
		String name = "日本語sjis.txt" ;

		try {
			XdrWriter createArguments = new XdrWriter() ;
			writeDiropargs( createArguments, rootHandle, name, charset) ;
			writeSattrUnset( createArguments) ;
			XdrWriter createResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 9, createArguments) ;
			XdrReader createReader = new XdrReader( createResponse.toByteArray()) ;

			assertEquals( "shift_jis create status", NfsStatus.OK, createReader.readInt()) ;
			createReader.readFixedOpaque( FileHandle.LENGTH) ;
			skipAttributes( createReader) ;
			assertTrue( "shift_jis file exists", Files.exists( context.getRoot().resolve( name), LinkOption.NOFOLLOW_LINKS)) ;

			XdrWriter lookupArguments = new XdrWriter() ;
			writeDiropargs( lookupArguments, rootHandle, name, charset) ;
			XdrWriter lookupResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 4, lookupArguments) ;
			XdrReader lookupReader = new XdrReader( lookupResponse.toByteArray()) ;

			assertEquals( "shift_jis lookup status", NfsStatus.OK, lookupReader.readInt()) ;
			lookupReader.readFixedOpaque( FileHandle.LENGTH) ;
			skipAttributes( lookupReader) ;

			ReadDirPage page = readDirPage( program, rootHandle, 0, 8192, charset) ;
			assertTrue( "shift_jis readdir", page.getNames().contains( name)) ;
		} finally {
			program.close() ;
			context.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドル永続化テストを行います。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル永続化テスト</p>
	 *
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private void testFileHandlePersistence() throws Exception {
		TestContext context = createContext() ;
		Path file = context.getRoot().resolve( "hello.txt") ;
		FileHandle firstHandle = context.getHandleTable().getOrCreate( file) ;
		context.getHandleTable().flush() ;
		FileHandleTable restartedTable = new FileHandleTable( context.getRoot()) ;
		FileHandle secondHandle = restartedTable.getOrCreate( file) ;

		assertBytes( "persistent handle", firstHandle.getValue(), secondHandle.getValue()) ;
		assertEquals( "persistent path", file.toString(), restartedTable.getPath( firstHandle).toString()) ;
		context.close() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ルート属性を確認します。<br><br>
	 *
	 * <p>メソッド名称： ルート属性確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertGetAttrRoot(NfsV2Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 1, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "getattr status", NfsStatus.OK, reader.readInt()) ;
		assertEquals( "root type", 2, reader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2 GETATTRのAUTH_SYS属性反映を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2 GETATTR AUTH_SYS確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertGetAttrRootAuthSys(NfsV2Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		XdrWriter response = handleWithAuthSys( program, RpcConstants.PROGRAM_NFS, 2, 1, arguments, -2, -2) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "authsys getattr status", NfsStatus.OK, reader.readInt()) ;
		assertEquals( "authsys root type", 2, reader.readInt()) ;
		reader.readInt() ;
		reader.readInt() ;
		assertEquals( "authsys uid", -2, reader.readInt()) ;
		assertEquals( "authsys gid", -2, reader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルLOOKUPを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルLOOKUP確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @return ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle assertLookupFile(NfsV2Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		arguments.writeString( "hello.txt") ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 4, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "lookup status", NfsStatus.OK, reader.readInt()) ;
		FileHandle handle = new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
		assertEquals( "lookup type", 1, reader.readInt()) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2ステータスと属性応答を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2ステータス属性応答確認</p>
	 *
	 * @param program		NFSv2プログラム
	 * @param rootHandle	ルートハンドル
	 * @param fileHandle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertStatusAndAttributesV2(NfsV2Program program, FileHandle rootHandle, FileHandle fileHandle) throws IOException {
		XdrWriter rootArguments = new XdrWriter() ;
		rootArguments.writeFixedOpaque( rootHandle.getValue()) ;
		XdrReader rootReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 1, rootArguments).toByteArray()) ;

		assertEquals( "v2 root attr status", NfsStatus.OK, rootReader.readInt()) ;
		assertEquals( "v2 root attr type", 2, rootReader.readInt()) ;
		assertEquals( "v2 root attr mode", 0755, rootReader.readInt() & 0777) ;
		assertTrue( "v2 root attr nlink", rootReader.readInt() >= 1) ;
		assertEquals( "v2 root attr uid", 0, rootReader.readInt()) ;
		assertEquals( "v2 root attr gid", 0, rootReader.readInt()) ;

		XdrWriter fileArguments = new XdrWriter() ;
		fileArguments.writeFixedOpaque( fileHandle.getValue()) ;
		XdrReader fileReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 1, fileArguments).toByteArray()) ;

		assertEquals( "v2 file attr status", NfsStatus.OK, fileReader.readInt()) ;
		assertEquals( "v2 file attr type", 1, fileReader.readInt()) ;
		assertEquals( "v2 file attr mode", 0644, fileReader.readInt() & 0777) ;
		assertTrue( "v2 file attr nlink", fileReader.readInt() >= 1) ;
		assertEquals( "v2 file attr uid", 0, fileReader.readInt()) ;
		assertEquals( "v2 file attr gid", 0, fileReader.readInt()) ;
		assertEquals( "v2 file attr size", "hello qnx".length(), fileReader.readInt()) ;

		XdrWriter missingArguments = new XdrWriter() ;
		writeDiropargs( missingArguments, rootHandle, "missing-status.txt") ;
		XdrReader missingReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 4, missingArguments).toByteArray()) ;

		assertEquals( "v2 missing lookup status", NfsStatus.NOENT, missingReader.readInt()) ;

		XdrWriter existingArguments = new XdrWriter() ;
		writeDiropargs( existingArguments, rootHandle, "hello.txt") ;
		writeSattrUnset( existingArguments) ;
		XdrReader existingReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 9, existingArguments).toByteArray()) ;

		assertEquals( "v2 existing create status", NfsStatus.EXIST, existingReader.readInt()) ;

		XdrWriter rmdirFileArguments = new XdrWriter() ;
		writeDiropargs( rmdirFileArguments, rootHandle, "hello.txt") ;
		XdrReader rmdirFileReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 15, rmdirFileArguments).toByteArray()) ;

		assertEquals( "v2 rmdir file status", NfsStatus.NOTDIR, rmdirFileReader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3ステータスと属性応答を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3ステータス属性応答確認</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param fileHandle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertStatusAndAttributesV3(NfsV3Program program, FileHandle rootHandle, FileHandle fileHandle) throws IOException {
		XdrWriter rootArguments = new XdrWriter() ;
		rootArguments.writeOpaque( rootHandle.getValue()) ;
		XdrReader rootReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 1, rootArguments).toByteArray()) ;

		assertEquals( "v3 root attr status", NfsStatus.OK, rootReader.readInt()) ;
		assertEquals( "v3 root attr type", 2, rootReader.readInt()) ;
		assertEquals( "v3 root attr mode", 0755, rootReader.readInt() & 0777) ;
		assertTrue( "v3 root attr nlink", rootReader.readInt() >= 1) ;
		assertEquals( "v3 root attr uid", 0, rootReader.readInt()) ;
		assertEquals( "v3 root attr gid", 0, rootReader.readInt()) ;

		XdrWriter fileArguments = new XdrWriter() ;
		fileArguments.writeOpaque( fileHandle.getValue()) ;
		XdrReader fileReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 1, fileArguments).toByteArray()) ;

		assertEquals( "v3 file attr status", NfsStatus.OK, fileReader.readInt()) ;
		assertEquals( "v3 file attr type", 1, fileReader.readInt()) ;
		assertEquals( "v3 file attr mode", 0644, fileReader.readInt() & 0777) ;
		assertTrue( "v3 file attr nlink", fileReader.readInt() >= 1) ;
		assertEquals( "v3 file attr uid", 0, fileReader.readInt()) ;
		assertEquals( "v3 file attr gid", 0, fileReader.readInt()) ;
		assertEquals( "v3 file attr size", "hello qnx".length(), fileReader.readLong()) ;

		XdrWriter missingArguments = new XdrWriter() ;
		writeDiropargsV3( missingArguments, rootHandle, "missing-status.txt") ;
		XdrReader missingReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 3, missingArguments).toByteArray()) ;

		assertEquals( "v3 missing lookup status", NfsStatus.NOENT, missingReader.readInt()) ;

		XdrWriter existingArguments = new XdrWriter() ;
		writeDiropargsV3( existingArguments, rootHandle, "hello.txt") ;
		existingArguments.writeInt( 1) ;
		writeSattr3Unset( existingArguments) ;
		XdrReader existingReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 8, existingArguments).toByteArray()) ;

		assertEquals( "v3 guarded create existing status", NfsStatus.EXIST, existingReader.readInt()) ;

		XdrWriter rmdirFileArguments = new XdrWriter() ;
		writeDiropargsV3( rmdirFileArguments, rootHandle, "hello.txt") ;
		XdrReader rmdirFileReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 13, rmdirFileArguments).toByteArray()) ;

		assertEquals( "v3 rmdir file status", NfsStatus.NOTDIR, rmdirFileReader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * exportをMOUNTします。<br><br>
	 *
	 * <p>メソッド名称： export MOUNT</p>
	 *
	 * @param program	MOUNTプログラム
	 * @param name		export名
	 * @return ルートファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle mountExport(MountV1Program program, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( name) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "mount status " + name, 0, reader.readInt()) ;
		return new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 指定クライアントでexportをMOUNTします。<br><br>
	 *
	 * <p>メソッド名称： 指定クライアントexport MOUNT</p>
	 *
	 * @param program		MOUNTプログラム
	 * @param name			export名
	 * @param clientAddress	クライアントアドレス
	 * @return ルートファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle mountExportWithClient(MountV1Program program, String name, String clientAddress) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( name) ;
		XdrWriter response = handleWithClient( program, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments, clientAddress) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "mount status " + name, 0, reader.readInt()) ;
		return new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルREADを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルREAD確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadFile(NfsV2Program program, FileHandle handle) throws IOException {
		assertEquals( "read data", "hello qnx", readFileValue( program, handle)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * WRITECACHEを確認します。<br><br>
	 *
	 * <p>メソッド名称： WRITECACHE確認</p>
	 *
	 * @param program	NFSプログラム
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertWriteCache(NfsV2Program program) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 7, arguments) ;

		assertEquals( "writecache response length", 0, response.toByteArray().length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルREAD値を取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイルREAD値取得</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @return READ値
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private String readFileValue(NfsV2Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeInt( 64) ;
		arguments.writeInt( 64) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 6, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "read status", NfsStatus.OK, reader.readInt()) ;
		skipAttributes( reader) ;
		return new String( reader.readOpaque(), StandardCharsets.UTF_8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3ファイルREAD値を取得します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3ファイルREAD値取得</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @return READ値
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private String readFileValueV3(NfsV3Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeOpaque( handle.getValue()) ;
		arguments.writeLong( 0L) ;
		arguments.writeInt( 128) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 6, arguments).toByteArray()) ;

		assertEquals( "v3 read value status", NfsStatus.OK, reader.readInt()) ;
		skipPostOpAttrV3( reader) ;
		reader.readInt() ;
		reader.readBoolean() ;
		return new String( reader.readOpaque(), StandardCharsets.UTF_8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2からファイルを置換します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2ファイル置換</p>
	 *
	 * @param program	NFSv2プログラム
	 * @param handle	ファイルハンドル
	 * @param value		値
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void writeFileByV2(NfsV2Program program, FileHandle handle, String value) throws IOException {
		XdrWriter truncateArguments = new XdrWriter() ;
		truncateArguments.writeFixedOpaque( handle.getValue()) ;
		writeSattrSize( truncateArguments, 0) ;
		XdrReader truncateReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 2, truncateArguments).toByteArray()) ;

		assertEquals( "v2 replace truncate status", NfsStatus.OK, truncateReader.readInt()) ;
		skipAttributes( truncateReader) ;

		byte[] data = value.getBytes( StandardCharsets.UTF_8) ;
		XdrWriter writeArguments = new XdrWriter() ;
		writeArguments.writeFixedOpaque( handle.getValue()) ;
		writeArguments.writeUnsignedInt( 0) ;
		writeArguments.writeUnsignedInt( 0) ;
		writeArguments.writeUnsignedInt( data.length) ;
		writeArguments.writeOpaque( data) ;
		XdrReader writeReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 8, writeArguments).toByteArray()) ;

		assertEquals( "v2 replace write status", NfsStatus.OK, writeReader.readInt()) ;
		skipAttributes( writeReader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3からファイルを置換します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3ファイル置換</p>
	 *
	 * @param program	NFSv3プログラム
	 * @param handle	ファイルハンドル
	 * @param value		値
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void writeFileByV3(NfsV3Program program, FileHandle handle, String value) throws IOException {
		XdrWriter truncateArguments = new XdrWriter() ;
		truncateArguments.writeOpaque( handle.getValue()) ;
		writeSattr3Size( truncateArguments, 0L) ;
		truncateArguments.writeBoolean( false) ;
		XdrReader truncateReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 2, truncateArguments).toByteArray()) ;

		assertEquals( "v3 replace truncate status", NfsStatus.OK, truncateReader.readInt()) ;
		skipWccDataV3( truncateReader) ;

		byte[] data = value.getBytes( StandardCharsets.UTF_8) ;
		XdrWriter writeArguments = new XdrWriter() ;
		writeArguments.writeOpaque( handle.getValue()) ;
		writeArguments.writeLong( 0L) ;
		writeArguments.writeInt( data.length) ;
		writeArguments.writeInt( 2) ;
		writeArguments.writeOpaque( data) ;
		XdrReader writeReader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 7, writeArguments).toByteArray()) ;

		assertEquals( "v3 replace write status", NfsStatus.OK, writeReader.readInt()) ;
		skipWccDataV3( writeReader) ;
		assertEquals( "v3 replace write count", data.length, writeReader.readInt()) ;
		int committed = writeReader.readInt() ;
		assertTrue( "v3 replace write committed", committed == 0 || committed == 2) ;
		assertEquals( "v3 replace write verifier", 8, writeReader.readFixedOpaque( 8).length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルWRITEを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルWRITE確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertWriteFile(NfsV2Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( 6) ;
		arguments.writeUnsignedInt( 3) ;
		arguments.writeOpaque( "nfs".getBytes( StandardCharsets.UTF_8)) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 8, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "write status", NfsStatus.OK, reader.readInt()) ;
		skipAttributes( reader) ;
		assertEquals( "write content", "hello nfs", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * QNX互換形式のファイルWRITEを確認します。<br><br>
	 *
	 * <p>メソッド名称： QNX互換形式ファイルWRITE確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertQnxFixedOpaqueWriteFile(NfsV2Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		byte[] data = "qnx".getBytes( StandardCharsets.UTF_8) ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( 6) ;
		arguments.writeUnsignedInt( data.length) ;
		arguments.writeFixedOpaque( data) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 8, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "qnx fixed write status", NfsStatus.OK, reader.readInt()) ;
		skipAttributes( reader) ;
		assertEquals( "qnx fixed write content", "hello qnx", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * QNXパディングなし形式のファイルWRITEを確認します。<br><br>
	 *
	 * <p>メソッド名称： QNXパディングなし形式ファイルWRITE確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertQnxUnpaddedOpaqueWriteFile(NfsV2Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		byte[] data = "qnx unpadded!".getBytes( StandardCharsets.UTF_8) ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( data.length) ;
		arguments.writeFixedOpaqueWithoutPadding( data) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 8, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "qnx unpadded write status", NfsStatus.OK, reader.readInt()) ;
		skipAttributes( reader) ;
		assertEquals( "qnx unpadded write content", "qnx unpadded!", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
		Files.writeString( root.resolve( "hello.txt"), "hello qnx", StandardCharsets.UTF_8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 読込専用ファイルWRITEを確認します。<br><br>
	 *
	 * <p>メソッド名称： 読込専用ファイルWRITE確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertWriteReadOnlyFile(NfsV2Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		arguments.writeUnsignedInt( 0) ;
		arguments.writeUnsignedInt( 6) ;
		arguments.writeUnsignedInt( 3) ;
		arguments.writeOpaque( "nfs".getBytes( StandardCharsets.UTF_8)) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 8, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "read-only write status", NfsStatus.ROFS, reader.readInt()) ;
		assertEquals( "read-only content", "hello qnx", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルSETATTRを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルSETATTR確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertSetAttrFile(NfsV2Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		writeSattrSize( arguments, 5) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 2, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "setattr status", NfsStatus.OK, reader.readInt()) ;
		skipAttributes( reader) ;
		assertEquals( "setattr content", "hello", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルSETATTR modeを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルSETATTR mode確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertSetAttrMode(NfsV2Program program, FileHandle handle) throws IOException {
		XdrWriter readOnlyArguments = new XdrWriter() ;
		readOnlyArguments.writeFixedOpaque( handle.getValue()) ;
		writeSattrMode( readOnlyArguments, 0444) ;
		XdrWriter readOnlyResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 2, readOnlyArguments) ;
		XdrReader readOnlyReader = new XdrReader( readOnlyResponse.toByteArray()) ;

		assertEquals( "setattr readonly status", NfsStatus.OK, readOnlyReader.readInt()) ;
		readOnlyReader.readInt() ;
		assertEquals( "setattr readonly mode", 0, readOnlyReader.readInt() & 0222) ;

		XdrWriter writableArguments = new XdrWriter() ;
		writableArguments.writeFixedOpaque( handle.getValue()) ;
		writeSattrMode( writableArguments, 0644) ;
		XdrWriter writableResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 2, writableArguments) ;
		XdrReader writableReader = new XdrReader( writableResponse.toByteArray()) ;

		assertEquals( "setattr writable status", NfsStatus.OK, writableReader.readInt()) ;
		writableReader.readInt() ;
		assertEquals( "setattr writable mode", 0200, writableReader.readInt() & 0200) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルSETATTR時刻を確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルSETATTR時刻確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertSetAttrTime(NfsV2Program program, FileHandle handle) throws IOException {
		int atimeSeconds = 1700000000 ;
		int mtimeSeconds = 1700000100 ;
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		writeSattrTimes( arguments, atimeSeconds, mtimeSeconds) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 2, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "setattr time status", NfsStatus.OK, reader.readInt()) ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		assertEquals( "setattr atime seconds", atimeSeconds, (int)reader.readUnsignedInt()) ;
		reader.readUnsignedInt() ;
		assertEquals( "setattr mtime seconds", mtimeSeconds, (int)reader.readUnsignedInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 双方向編集を確認します。<br><br>
	 *
	 * <p>メソッド名称： 双方向編集確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param root		公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertBidirectionalEditFile(NfsV2Program program, FileHandle handle, Path root) throws IOException {
		XdrWriter truncateArguments = new XdrWriter() ;
		truncateArguments.writeFixedOpaque( handle.getValue()) ;
		writeSattrSize( truncateArguments, 0) ;
		XdrWriter truncateResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 2, truncateArguments) ;
		XdrReader truncateReader = new XdrReader( truncateResponse.toByteArray()) ;

		assertEquals( "edit truncate status", NfsStatus.OK, truncateReader.readInt()) ;
		skipAttributes( truncateReader) ;

		XdrWriter writeArguments = new XdrWriter() ;
		byte[] data = "qnx edit".getBytes( StandardCharsets.UTF_8) ;
		writeArguments.writeFixedOpaque( handle.getValue()) ;
		writeArguments.writeUnsignedInt( 0) ;
		writeArguments.writeUnsignedInt( 0) ;
		writeArguments.writeUnsignedInt( data.length) ;
		writeArguments.writeOpaque( data) ;
		XdrWriter writeResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 8, writeArguments) ;
		XdrReader writeReader = new XdrReader( writeResponse.toByteArray()) ;

		assertEquals( "edit write status", NfsStatus.OK, writeReader.readInt()) ;
		skipAttributes( writeReader) ;
		assertEquals( "edit windows content", "qnx edit", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;

		Files.writeString( root.resolve( "hello.txt"), "windows edit", StandardCharsets.UTF_8) ;
		assertEquals( "edit nfs read", "windows edit", readFileValue( program, handle)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 一時ファイル保存形式の上書きを確認します。<br><br>
	 *
	 * <p>メソッド名称： 一時ファイル保存形式上書き確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertTempRenameOverwriteFile(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		Files.writeString( root.resolve( "rename-overwrite-target.txt"), "old", StandardCharsets.UTF_8) ;

		XdrWriter createArguments = new XdrWriter() ;
		writeDiropargs( createArguments, rootHandle, ".rename-overwrite-target.txt.tmp") ;
		writeSattrUnset( createArguments) ;
		XdrWriter createResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 9, createArguments) ;
		XdrReader createReader = new XdrReader( createResponse.toByteArray()) ;

		assertEquals( "temp create status", NfsStatus.OK, createReader.readInt()) ;
		FileHandle tempHandle = new FileHandle( createReader.readFixedOpaque( FileHandle.LENGTH)) ;
		skipAttributes( createReader) ;

		XdrWriter writeArguments = new XdrWriter() ;
		byte[] data = "temp rename edit".getBytes( StandardCharsets.UTF_8) ;
		writeArguments.writeFixedOpaque( tempHandle.getValue()) ;
		writeArguments.writeUnsignedInt( 0) ;
		writeArguments.writeUnsignedInt( 0) ;
		writeArguments.writeUnsignedInt( data.length) ;
		writeArguments.writeOpaque( data) ;
		XdrWriter writeResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 8, writeArguments) ;
		XdrReader writeReader = new XdrReader( writeResponse.toByteArray()) ;

		assertEquals( "temp write status", NfsStatus.OK, writeReader.readInt()) ;
		skipAttributes( writeReader) ;

		XdrWriter renameArguments = new XdrWriter() ;
		writeDiropargs( renameArguments, rootHandle, ".rename-overwrite-target.txt.tmp") ;
		writeDiropargs( renameArguments, rootHandle, "rename-overwrite-target.txt") ;
		XdrWriter renameResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 11, renameArguments) ;
		XdrReader renameReader = new XdrReader( renameResponse.toByteArray()) ;

		assertEquals( "temp rename status", NfsStatus.OK, renameReader.readInt()) ;
		assertEquals( "temp rename content", "temp rename edit", Files.readString( root.resolve( "rename-overwrite-target.txt"), StandardCharsets.UTF_8)) ;
		assertTrue( "temp rename source missing", !Files.exists( root.resolve( ".rename-overwrite-target.txt.tmp"), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルCREATE/REMOVEを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルCREATE/REMOVE確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertCreateFile(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		XdrWriter createArguments = new XdrWriter() ;
		writeDiropargs( createArguments, rootHandle, "created.txt") ;
		writeSattrUnset( createArguments) ;
		XdrWriter createResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 9, createArguments) ;
		XdrReader createReader = new XdrReader( createResponse.toByteArray()) ;

		assertEquals( "create status", NfsStatus.OK, createReader.readInt()) ;
		assertEquals( "create handle length", FileHandle.LENGTH, createReader.readFixedOpaque( FileHandle.LENGTH).length) ;
		skipAttributes( createReader) ;
		assertTrue( "create exists", Files.exists( root.resolve( "created.txt"), LinkOption.NOFOLLOW_LINKS)) ;

		XdrWriter removeArguments = new XdrWriter() ;
		writeDiropargs( removeArguments, rootHandle, "created.txt") ;
		XdrWriter removeResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 10, removeArguments) ;
		XdrReader removeReader = new XdrReader( removeResponse.toByteArray()) ;

		assertEquals( "remove status", NfsStatus.OK, removeReader.readInt()) ;
		assertTrue( "remove missing", !Files.exists( root.resolve( "created.txt"), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルRENAMEを確認します。<br><br>
	 *
	 * <p>メソッド名称： ファイルRENAME確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertRenameFile(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		Files.writeString( root.resolve( "rename-source.txt"), "rename", StandardCharsets.UTF_8) ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, "rename-source.txt") ;
		writeDiropargs( arguments, rootHandle, "rename-target.txt") ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 11, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "rename status", NfsStatus.OK, reader.readInt()) ;
		assertTrue( "rename source missing", !Files.exists( root.resolve( "rename-source.txt"), LinkOption.NOFOLLOW_LINKS)) ;
		assertEquals( "rename target", "rename", Files.readString( root.resolve( "rename-target.txt"), StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * MKDIR/RMDIRを確認します。<br><br>
	 *
	 * <p>メソッド名称： MKDIR/RMDIR確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertMkdirAndRmdir(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		XdrWriter mkdirArguments = new XdrWriter() ;
		writeDiropargs( mkdirArguments, rootHandle, "created-dir") ;
		writeSattrUnset( mkdirArguments) ;
		XdrWriter mkdirResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 14, mkdirArguments) ;
		XdrReader mkdirReader = new XdrReader( mkdirResponse.toByteArray()) ;

		assertEquals( "mkdir status", NfsStatus.OK, mkdirReader.readInt()) ;
		assertEquals( "mkdir handle length", FileHandle.LENGTH, mkdirReader.readFixedOpaque( FileHandle.LENGTH).length) ;
		skipAttributes( mkdirReader) ;
		assertTrue( "mkdir exists", Files.isDirectory( root.resolve( "created-dir"), LinkOption.NOFOLLOW_LINKS)) ;

		XdrWriter rmdirArguments = new XdrWriter() ;
		writeDiropargs( rmdirArguments, rootHandle, "created-dir") ;
		XdrWriter rmdirResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 15, rmdirArguments) ;
		XdrReader rmdirReader = new XdrReader( rmdirResponse.toByteArray()) ;

		assertEquals( "rmdir status", NfsStatus.OK, rmdirReader.readInt()) ;
		assertTrue( "rmdir missing", !Files.exists( root.resolve( "created-dir"), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリREMOVE互換処理を確認します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリREMOVE互換処理確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertRemoveDirectoryCompatibility(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		Files.createDirectory( root.resolve( "remove-dir-compatible") ) ;
		XdrWriter removeDirectoryArguments = new XdrWriter() ;
		writeDiropargs( removeDirectoryArguments, rootHandle, "remove-dir-compatible") ;
		XdrWriter removeDirectoryResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 10, removeDirectoryArguments) ;
		XdrReader removeDirectoryReader = new XdrReader( removeDirectoryResponse.toByteArray()) ;

		assertEquals( "remove directory status", NfsStatus.OK, removeDirectoryReader.readInt()) ;
		assertTrue( "remove directory missing", !Files.exists( root.resolve( "remove-dir-compatible"), LinkOption.NOFOLLOW_LINKS)) ;

		Files.createDirectory( root.resolve( "remove-nonempty-dir") ) ;
		Files.writeString( root.resolve( "remove-nonempty-dir").resolve( "child.txt"), "child", StandardCharsets.UTF_8) ;
		XdrWriter removeNonemptyArguments = new XdrWriter() ;
		writeDiropargs( removeNonemptyArguments, rootHandle, "remove-nonempty-dir") ;
		XdrWriter removeNonemptyResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 10, removeNonemptyArguments) ;
		XdrReader removeNonemptyReader = new XdrReader( removeNonemptyResponse.toByteArray()) ;

		assertEquals( "remove nonempty directory status", NfsStatus.NOTEMPTY, removeNonemptyReader.readInt()) ;
		assertTrue( "remove nonempty directory remains", Files.isDirectory( root.resolve( "remove-nonempty-dir"), LinkOption.NOFOLLOW_LINKS)) ;
		Files.delete( root.resolve( "remove-nonempty-dir").resolve( "child.txt")) ;
		Files.delete( root.resolve( "remove-nonempty-dir")) ;

		Path qnxTemporaryDirectory = root.resolve( ".nfsX8") ;
		Files.createDirectory( qnxTemporaryDirectory) ;
		Files.writeString( qnxTemporaryDirectory.resolve( "child.txt"), "child", StandardCharsets.UTF_8) ;
		Files.createDirectory( qnxTemporaryDirectory.resolve( "nested")) ;
		Files.writeString( qnxTemporaryDirectory.resolve( "nested").resolve( "nested.txt"), "nested", StandardCharsets.UTF_8) ;
		Path readOnlyChild = qnxTemporaryDirectory.resolve( "nested").resolve( "readonly.txt") ;
		Files.writeString( readOnlyChild, "readonly", StandardCharsets.UTF_8) ;
		DosFileAttributeView dosView = Files.getFileAttributeView( readOnlyChild, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

		// Windowsの読み取り専用属性を含むQNX削除用一時ディレクトリを確認する
		if( dosView != null) {
			dosView.setReadOnly( true) ;
		}

		XdrWriter removeQnxTemporaryArguments = new XdrWriter() ;
		writeDiropargs( removeQnxTemporaryArguments, rootHandle, ".nfsX8") ;
		XdrWriter removeQnxTemporaryResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 10, removeQnxTemporaryArguments) ;
		XdrReader removeQnxTemporaryReader = new XdrReader( removeQnxTemporaryResponse.toByteArray()) ;

		assertEquals( "remove qnx temporary directory status", NfsStatus.OK, removeQnxTemporaryReader.readInt()) ;
		assertTrue( "remove qnx temporary directory missing", !Files.exists( qnxTemporaryDirectory, LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIRを確認します。<br><br>
	 *
	 * <p>メソッド名称： READDIR確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadDir(NfsV2Program program, FileHandle rootHandle) throws IOException {
		ReadDirPage page = readDirPage( program, rootHandle, 0, 8192) ;
		List<String> names = new ArrayList<String>( page.getNames()) ;

		assertTrue( "readdir eof", page.isEof()) ;
		assertTrue( "readdir dot", names.contains( ".")) ;
		assertTrue( "readdir dotdot", names.contains( "..")) ;
		assertTrue( "readdir file", names.contains( "hello.txt")) ;

		ReadDirPage firstPage = readDirPage( program, rootHandle, 0, 40) ;
		ReadDirPage secondPage = readDirPage( program, rootHandle, firstPage.getLastCookie(), 8192) ;
		List<String> pagedNames = new ArrayList<String>() ;
		pagedNames.addAll( firstPage.getNames()) ;
		pagedNames.addAll( secondPage.getNames()) ;

		assertTrue( "readdir first page not eof", !firstPage.isEof()) ;
		assertTrue( "readdir second page eof", secondPage.isEof()) ;
		assertTrue( "readdir paged file", pagedNames.contains( "hello.txt")) ;
		assertEquals( "readdir paged no duplicate", pagedNames.size(), new HashSet<String>( pagedNames).size()) ;

		ReadDirPage invalidCookiePage = readDirPage( program, rootHandle, 0x7fffffff, 8192) ;

		assertEquals( "readdir invalid cookie empty", 0, invalidCookiePage.getNames().size()) ;
		assertTrue( "readdir invalid cookie eof", invalidCookiePage.isEof()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIRページを取得します。<br><br>
	 *
	 * <p>メソッド名称： READDIRページ取得</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param cookie		cookie
	 * @param count			読込サイズ
	 * @return READDIRページ
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private ReadDirPage readDirPage(NfsV2Program program, FileHandle rootHandle, int cookie, int count) throws IOException {
		return readDirPage( program, rootHandle, cookie, count, StandardCharsets.UTF_8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIRページを取得します。<br><br>
	 *
	 * <p>メソッド名称： READDIRページ取得</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param cookie		cookie
	 * @param count			読込サイズ
	 * @param charset		ファイル名文字コード
	 * @return READDIRページ
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private ReadDirPage readDirPage(NfsV2Program program, FileHandle rootHandle, int cookie, int count, Charset charset) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		arguments.writeInt( cookie) ;
		arguments.writeInt( count) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 16, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;
		List<String> names = new ArrayList<String>() ;
		List<Integer> cookies = new ArrayList<Integer>() ;

		assertEquals( "readdir status", NfsStatus.OK, reader.readInt()) ;

		while( reader.readBoolean()) {
			reader.readUnsignedInt() ;
			names.add( reader.readString( charset)) ;
			cookies.add( (int)reader.readUnsignedInt()) ;
		}

		return new ReadDirPage( names, cookies, reader.readBoolean()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * STATFSを確認します。<br><br>
	 *
	 * <p>メソッド名称： STATFS確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertStatFs(NfsV2Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 17, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "statfs status", NfsStatus.OK, reader.readInt()) ;
		assertEquals( "statfs transfer size", 8192, reader.readInt()) ;
		assertTrue( "statfs block size", reader.readUnsignedInt() > 0) ;
		assertTrue( "statfs blocks", reader.readUnsignedInt() > 0) ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 不正LOOKUPを確認します。<br><br>
	 *
	 * <p>メソッド名称： 不正LOOKUP確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertInvalidLookup(NfsV2Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle.getValue()) ;
		arguments.writeString( "..\\outside") ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 4, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "invalid lookup status", NfsStatus.ACCES, reader.readInt()) ;

		XdrWriter windowsArguments = new XdrWriter() ;
		windowsArguments.writeFixedOpaque( rootHandle.getValue()) ;
		windowsArguments.writeString( "bad:name") ;
		XdrWriter windowsResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 4, windowsArguments) ;
		XdrReader windowsReader = new XdrReader( windowsResponse.toByteArray()) ;

		assertEquals( "invalid windows name status", NfsStatus.ACCES, windowsReader.readInt()) ;

		XdrWriter longArguments = new XdrWriter() ;
		longArguments.writeFixedOpaque( rootHandle.getValue()) ;
		longArguments.writeString( "a".repeat( 256)) ;
		XdrWriter longResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 4, longArguments) ;
		XdrReader longReader = new XdrReader( longResponse.toByteArray()) ;

		assertEquals( "long lookup name status", NfsStatus.ACCES, longReader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 通常ファイルREADLINKを確認します。<br><br>
	 *
	 * <p>メソッド名称： 通常ファイルREADLINK確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadLinkOnRegularFile(NfsV2Program program, FileHandle handle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 5, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "readlink status", NfsStatus.INVAL, reader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ハードリンクを確認します。<br><br>
	 *
	 * <p>メソッド名称： ハードリンク確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param fileHandle	ファイルハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertHardLink(NfsV2Program program, FileHandle rootHandle, FileHandle fileHandle, Path root) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( fileHandle.getValue()) ;
		writeDiropargs( arguments, rootHandle, "hard-link.txt") ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 12, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "hard link status", NfsStatus.OK, reader.readInt()) ;
		assertEquals( "hard link content", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8), Files.readString( root.resolve( "hard-link.txt"), StandardCharsets.UTF_8)) ;
		assertTrue( "hard link same file", Files.isSameFile( root.resolve( "hello.txt"), root.resolve( "hard-link.txt"))) ;
		Files.writeString( root.resolve( "hard-link.txt"), "hard link edit", StandardCharsets.UTF_8) ;
		assertEquals( "hard link shared source", "hard link edit", Files.readString( root.resolve( "hello.txt"), StandardCharsets.UTF_8)) ;

		XdrWriter getattrArguments = new XdrWriter() ;
		getattrArguments.writeFixedOpaque( fileHandle.getValue()) ;
		XdrWriter getattrResponse = handle( program, RpcConstants.PROGRAM_NFS, 2, 1, getattrArguments) ;
		XdrReader getattrReader = new XdrReader( getattrResponse.toByteArray()) ;

		assertEquals( "hard link getattr status", NfsStatus.OK, getattrReader.readInt()) ;
		getattrReader.readInt() ;
		getattrReader.readInt() ;
		assertTrue( "hard link nlink", getattrReader.readUnsignedInt() >= 2) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 長すぎるsymlink先を確認します。<br><br>
	 *
	 * <p>メソッド名称： 長すぎるsymlink先確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertLongSymlinkTarget(NfsV2Program program, FileHandle rootHandle) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, "long-link") ;
		arguments.writeString( "a".repeat( 1025)) ;
		writeSattrUnset( arguments) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 13, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "long symlink status", NfsStatus.NAMETOOLONG, reader.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2 symlink作成を確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2 symlink作成確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertSymbolicLinkV2(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		String linkName = "v2-link.txt" ;
		String linkTarget = "hello.txt" ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, linkName) ;
		arguments.writeString( linkTarget) ;
		writeSattrUnset( arguments) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 13, arguments).toByteArray()) ;
		int status = reader.readInt() ;
		Path linkPath = root.resolve( linkName) ;

		// Windows環境でsymlink作成権限がない場合は安定したエラーを確認する
		if( status != NfsStatus.OK) {
			assertLinkCreationFailureStatus( "v2 symlink status", status) ;
			assertFalse( "v2 symlink not created", Files.exists( linkPath, LinkOption.NOFOLLOW_LINKS)) ;
			return ;
		}

		assertTrue( "v2 symlink exists", Files.isSymbolicLink( linkPath)) ;
		assertEquals( "v2 symlink target", linkTarget, Files.readSymbolicLink( linkPath).toString()) ;
		assertReadLinkV2( program, lookupV2( program, rootHandle, linkName), linkTarget) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2の壊れたsymlink READLINKを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2壊れたsymlink READLINK確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertBrokenSymbolicLinkV2(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		String linkName = "v2-broken-link.txt" ;
		String linkTarget = "missing-target.txt" ;
		Path linkPath = root.resolve( linkName) ;

		if( !createLocalSymbolicLink( linkPath, linkTarget)) {
			return ;
		}

		FileHandle linkHandle = lookupV2( program, rootHandle, linkName) ;
		assertTrue( "v2 broken symlink exists", Files.isSymbolicLink( linkPath)) ;
		assertFalse( "v2 broken symlink target missing", Files.exists( root.resolve( linkTarget), LinkOption.NOFOLLOW_LINKS)) ;
		assertReadLinkV2( program, linkHandle, linkTarget) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2 READLINKを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv2 READLINK確認</p>
	 *
	 * @param program	NFSプログラム
	 * @param handle	ファイルハンドル
	 * @param expected	期待リンク先
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertReadLinkV2(NfsV2Program program, FileHandle handle, String expected) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( handle.getValue()) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 5, arguments).toByteArray()) ;

		assertEquals( "v2 readlink status", NfsStatus.OK, reader.readInt()) ;
		assertEquals( "v2 readlink target", expected, reader.readString()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 日本語ファイル名CREATEを確認します。<br><br>
	 *
	 * <p>メソッド名称： 日本語ファイル名CREATE確認</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param root			公開ルート
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void assertJapaneseCreate(NfsV2Program program, FileHandle rootHandle, Path root) throws IOException {
		String name = "日本語.txt" ;
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, name) ;
		writeSattrUnset( arguments) ;
		XdrWriter response = handle( program, RpcConstants.PROGRAM_NFS, 2, 9, arguments) ;
		XdrReader reader = new XdrReader( response.toByteArray()) ;

		assertEquals( "japanese create status", NfsStatus.OK, reader.readInt()) ;
		reader.readFixedOpaque( FileHandle.LENGTH) ;
		skipAttributes( reader) ;
		assertTrue( "japanese file exists", Files.exists( root.resolve( name), LinkOption.NOFOLLOW_LINKS)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2 LOOKUPを行います。<br><br>
	 *
	 * <p>メソッド名称： NFSv2 LOOKUP実行</p>
	 *
	 * @param program		NFSプログラム
	 * @param rootHandle	ルートハンドル
	 * @param name			名前
	 * @return ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle lookupV2(NfsV2Program program, FileHandle rootHandle, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargs( arguments, rootHandle, name) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 2, 4, arguments).toByteArray()) ;

		assertEquals( "v2 lookup " + name + " status", NfsStatus.OK, reader.readInt()) ;
		FileHandle handle = new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
		skipAttributes( reader) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 LOOKUPを行います。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 LOOKUP実行</p>
	 *
	 * @param program		NFSv3プログラム
	 * @param rootHandle	ルートハンドル
	 * @param name			名前
	 * @return ファイルハンドル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private FileHandle lookupV3(NfsV3Program program, FileHandle rootHandle, String name) throws IOException {
		XdrWriter arguments = new XdrWriter() ;
		writeDiropargsV3( arguments, rootHandle, name) ;
		XdrReader reader = new XdrReader( handle( program, RpcConstants.PROGRAM_NFS, 3, 3, arguments).toByteArray()) ;

		assertEquals( "v3 lookup " + name + " status", NfsStatus.OK, reader.readInt()) ;
		FileHandle handle = new FileHandle( reader.readOpaque()) ;
		skipPostOpAttrV3( reader) ;
		skipPostOpAttrV3( reader) ;
		return handle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ローカルsymlinkを作成します。<br><br>
	 *
	 * <p>メソッド名称： ローカルsymlink作成</p>
	 *
	 * @param link		リンクパス
	 * @param target	リンク先
	 * @return true:作成済み false:環境がsymlink作成不可
	 * @throws IOException 削除異常
	 */
	//--------------------------------------------------------------------------
	private boolean createLocalSymbolicLink(Path link, String target) throws IOException {
		Files.deleteIfExists( link) ;

		try {
			Files.createSymbolicLink( link, Path.of( target)) ;
			return true ;
		} catch( IOException | UnsupportedOperationException | SecurityException ex) {
			Files.deleteIfExists( link) ;
			return false ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * symlink作成失敗ステータスを確認します。<br><br>
	 *
	 * <p>メソッド名称： symlink作成失敗ステータス確認</p>
	 *
	 * @param name		検証名
	 * @param status	ステータス
	 */
	//--------------------------------------------------------------------------
	private void assertLinkCreationFailureStatus(String name, int status) {
		// Windowsのsymlink権限不足やファイルシステム非対応は作成不可として扱う
		if( status == NfsStatus.PERM || status == NfsStatus.ACCES || status == NfsStatus.NOTSUPP) {
			return ;
		}

		throw new AssertionError( name + " expected link creation failure status actual=" + status) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCプログラムを呼び出します。<br><br>
	 *
	 * <p>メソッド名称： RPCプログラム呼出</p>
	 *
	 * @param program		RPCプログラム
	 * @param programNumber	Program番号
	 * @param version		Version
	 * @param procedure		Procedure
	 * @param arguments		引数
	 * @return 応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private XdrWriter handle(jp.co.enterogawa.nfs.rpc.RpcProgram program, int programNumber, int version, int procedure, XdrWriter arguments) throws IOException {
		return handleWithClient( program, programNumber, version, procedure, arguments, "127.0.0.1") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 指定クライアントでRPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： 指定クライアントRPC呼出処理</p>
	 *
	 * @param program		RPCプログラム
	 * @param programNumber	Program番号
	 * @param version		Version
	 * @param procedure		Procedure
	 * @param arguments		引数
	 * @param clientAddress	クライアントアドレス
	 * @return 応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private XdrWriter handleWithClient(jp.co.enterogawa.nfs.rpc.RpcProgram program, int programNumber, int version, int procedure, XdrWriter arguments, String clientAddress) throws IOException {
		RpcCall call = createCall( programNumber, version, procedure, arguments) ;
		return handleCallWithClient( program, programNumber, version, procedure, clientAddress, call) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS資格情報でRPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS RPC呼出処理</p>
	 *
	 * @param program		RPCプログラム
	 * @param programNumber	Program番号
	 * @param version		Version
	 * @param procedure		Procedure
	 * @param arguments		引数
	 * @param uid			UID
	 * @param gid			GID
	 * @return 応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private XdrWriter handleWithAuthSys(jp.co.enterogawa.nfs.rpc.RpcProgram program, int programNumber, int version, int procedure, XdrWriter arguments, int uid, int gid) throws IOException {
		RpcCall call = createCallAuthSys( programNumber, version, procedure, arguments, uid, gid) ;
		return handleCallWithClient( program, programNumber, version, procedure, "127.0.0.1", call) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出処理</p>
	 *
	 * @param program		RPCプログラム
	 * @param programNumber	Program番号
	 * @param version		Version
	 * @param procedure		Procedure
	 * @param clientAddress	クライアントアドレス
	 * @param call			RPC呼出
	 * @return 応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private XdrWriter handleCallWithClient(jp.co.enterogawa.nfs.rpc.RpcProgram program, int programNumber, int version, int procedure, String clientAddress, RpcCall call) throws IOException {
		XdrWriter response = new XdrWriter() ;
		RpcRequestContext context = new RpcRequestContext(
				clientAddress,
				12345,
				"test",
				XID,
				programNumber,
				version,
				procedure,
				call.getCredential()) ;
		int status = program.handle( call, context, response) ;

		assertEquals( "accept status", RpcConstants.ACCEPT_SUCCESS, status) ;
		return response ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPCを呼び出します。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC呼出</p>
	 *
	 * @param port			ポート
	 * @param program		Program
	 * @param version		Version
	 * @param procedure		Procedure
	 * @param arguments		引数
	 * @param fragmented	fragment分割有無
	 * @return 応答本文
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private XdrReader callTcp(int port, int program, int version, int procedure, XdrWriter arguments, boolean fragmented) throws IOException {
		int requestXid = XID + 100 ;
		byte[] request = createCallBytes( requestXid, program, version, procedure, arguments) ;

		try( Socket socket = new Socket( "127.0.0.1", port)) {
			socket.setSoTimeout( 3000) ;
			DataOutputStream output = new DataOutputStream( socket.getOutputStream()) ;
			DataInputStream input = new DataInputStream( socket.getInputStream()) ;
			writeTcpRecord( output, request, fragmented) ;
			return parseAcceptedReply( requestXid, readTcpRecord( input)) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPC recordを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC record書込</p>
	 *
	 * @param output		出力
	 * @param record		record
	 * @param fragmented	fragment分割有無
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private void writeTcpRecord(DataOutputStream output, byte[] record, boolean fragmented) throws IOException {
		// 分割しない場合
		if( !fragmented || record.length < 8) {
			output.writeInt( 0x80000000 | record.length) ;
			output.write( record) ;
			output.flush() ;
			return ;
		}

		int split = record.length / 2 ;
		output.writeInt( split) ;
		output.write( record, 0, split) ;
		output.writeInt( 0x80000000 | (record.length - split)) ;
		output.write( record, split, record.length - split) ;
		output.flush() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPC recordを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC record読込</p>
	 *
	 * @param input	入力
	 * @return record
	 * @throws IOException 読込異常
	 */
	//--------------------------------------------------------------------------
	private byte[] readTcpRecord(DataInputStream input) throws IOException {
		int header = input.readInt() ;
		int length = header & 0x7fffffff ;
		byte[] record = input.readNBytes( length) ;

		assertEquals( "tcp record length", length, record.length) ;
		return record ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ACCEPT応答を解析します。<br><br>
	 *
	 * <p>メソッド名称： ACCEPT応答解析</p>
	 *
	 * @param xid		期待XID
	 * @param response	RPC応答
	 * @return 応答本文
	 */
	//--------------------------------------------------------------------------
	private XdrReader parseAcceptedReply(int xid, byte[] response) {
		XdrReader reader = new XdrReader( response) ;
		assertEquals( "tcp xid", xid, reader.readInt()) ;
		assertEquals( "tcp message type", RpcConstants.MSG_REPLY, reader.readInt()) ;
		assertEquals( "tcp reply status", RpcConstants.REPLY_STAT_ACCEPTED, reader.readInt()) ;
		reader.readInt() ;
		reader.readOpaque() ;
		assertEquals( "tcp accept status", RpcConstants.ACCEPT_SUCCESS, reader.readInt()) ;
		return new XdrReader( reader.readRemaining()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を作成します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出作成</p>
	 *
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return RPC呼出
	 */
	//--------------------------------------------------------------------------
	private RpcCall createCall(int program, int version, int procedure, XdrWriter arguments) {
		byte[] request = createCallBytes( XID, program, version, procedure, arguments) ;
		return RpcCall.read( request, request.length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS付きRPC呼出を作成します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS付きRPC呼出作成</p>
	 *
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @param uid		UID
	 * @param gid		GID
	 * @return RPC呼出
	 */
	//--------------------------------------------------------------------------
	private RpcCall createCallAuthSys(int program, int version, int procedure, XdrWriter arguments, int uid, int gid) {
		byte[] request = createCallBytes( XID, program, version, procedure, arguments, RpcConstants.AUTH_SYS, createAuthSysCredential( uid, gid)) ;
		return RpcCall.read( request, request.length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出バイト列を作成します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出バイト列作成</p>
	 *
	 * @param xid		XID
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return RPC呼出バイト列
	 */
	//--------------------------------------------------------------------------
	private byte[] createCallBytes(int xid, int program, int version, int procedure, XdrWriter arguments) {
		return createCallBytes( xid, program, version, procedure, arguments, RpcConstants.AUTH_NONE, new byte[0]) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出バイト列を作成します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出バイト列作成</p>
	 *
	 * @param xid					XID
	 * @param program				Program
	 * @param version				Version
	 * @param procedure				Procedure
	 * @param arguments				引数
	 * @param credentialFlavor		認証方式
	 * @param credentialBody		認証データ
	 * @return RPC呼出バイト列
	 */
	//--------------------------------------------------------------------------
	private byte[] createCallBytes(int xid, int program, int version, int procedure, XdrWriter arguments, int credentialFlavor, byte[] credentialBody) {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( xid) ;
		writer.writeInt( RpcConstants.MSG_CALL) ;
		writer.writeInt( RpcConstants.RPC_VERSION) ;
		writer.writeInt( program) ;
		writer.writeInt( version) ;
		writer.writeInt( procedure) ;
		writer.writeInt( credentialFlavor) ;
		writer.writeOpaque( credentialBody) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeFixedOpaque( arguments.toByteArray()) ;
		return writer.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS認証データを作成します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS認証データ作成</p>
	 *
	 * @param uid	UID
	 * @param gid	GID
	 * @return 認証データ
	 */
	//--------------------------------------------------------------------------
	private byte[] createAuthSysCredential(int uid, int gid) {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( 0) ;
		writer.writeString( "test-client") ;
		writer.writeInt( uid) ;
		writer.writeInt( gid) ;
		writer.writeInt( 0) ;
		return writer.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テストコンテキストを作成します。<br><br>
	 *
	 * <p>メソッド名称： テストコンテキスト作成</p>
	 *
	 * @return テストコンテキスト
	 * @throws IOException 作成異常
	 */
	//--------------------------------------------------------------------------
	private TestContext createContext() throws IOException {
		return createContext( "UTF-8", true) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テストコンテキストを作成します。<br><br>
	 *
	 * <p>メソッド名称： テストコンテキスト作成</p>
	 *
	 * @param filenameCharset	ファイル名文字コード
	 * @return テストコンテキスト
	 * @throws IOException 作成異常
	 */
	//--------------------------------------------------------------------------
	private TestContext createContext(String filenameCharset) throws IOException {
		return createContext( filenameCharset, true) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テストコンテキストを作成します。<br><br>
	 *
	 * <p>メソッド名称： テストコンテキスト作成</p>
	 *
	 * @param filenameCharset	ファイル名文字コード
	 * @param writeSync			同期書込有無
	 * @return テストコンテキスト
	 * @throws IOException 作成異常
	 */
	//--------------------------------------------------------------------------
	private TestContext createContext(String filenameCharset, boolean writeSync) throws IOException {
		Path root = Path.of( "work", "tmp", "test-export").toAbsolutePath().normalize() ;
		Path data = Path.of( "work", "tmp", "test-data").toAbsolutePath().normalize() ;
		deleteDirectory( root) ;
		deleteDirectory( data) ;
		System.setProperty( "tinywin.nfs.data", data.toString()) ;
		Files.createDirectories( root) ;
		Files.writeString( root.resolve( "hello.txt"), "hello qnx", StandardCharsets.UTF_8) ;
		Path configPath = Path.of( "work", "tmp", "test-nfs-server.properties").toAbsolutePath().normalize() ;
		String configText = ""
				+ "portmap.port=" + TEST_PORTMAP_PORT + "\n"
				+ "nfs.port=" + TEST_NFS_PORT + "\n"
				+ "mount.port=" + TEST_MOUNT_PORT + "\n"
				+ "export.name=/export\n"
				+ "export.path=" + root.toString().replace( "\\", "\\\\") + "\n"
				+ "export.writable=true\n"
				+ "uid=0\n"
				+ "gid=0\n"
				+ "file.mode=0644\n"
				+ "directory.mode=0755\n"
				+ "block.size=4096\n"
				+ "read.size=8192\n"
				+ "write.size=16384\n"
				+ "directory.preferred.size=4096\n"
				+ "max.file.size=1099511627776\n"
				+ "time.delta.nanos=1000000\n"
				+ "pathconf.link.max=1024\n"
				+ "pathconf.name.max=255\n"
				+ "write.sync=" + writeSync + "\n"
				+ "filename.charset=" + filenameCharset + "\n" ;
		Files.writeString( configPath, configText, StandardCharsets.UTF_8) ;
		NfsServerConfig config = NfsServerConfig.load( configPath) ;
		return new TestContext( root, config, new FileHandleTable( config.getExports())) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定ファイルを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 設定ファイル書込</p>
	 *
	 * @param fileName			ファイル名
	 * @param root				公開ルート
	 * @param exportName		公開名
	 * @param allowedClients	許可クライアント
	 * @return 設定ファイル
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private Path writeConfig(String fileName, Path root, String exportName, String allowedClients) throws IOException {
		Path configPath = Path.of( "work", "tmp", fileName).toAbsolutePath().normalize() ;
		String configText = ""
				+ "portmap.port=" + TEST_PORTMAP_PORT + "\n"
				+ "nfs.port=" + TEST_NFS_PORT + "\n"
				+ "mount.port=" + TEST_MOUNT_PORT + "\n"
				+ "export.name=" + exportName + "\n"
				+ "export.path=" + root.toString().replace( "\\", "\\\\") + "\n"
				+ "export.writable=true\n"
				+ "export.allowed.clients=" + allowedClients + "\n"
				+ "exports.count=1\n"
				+ "exports.1.name=" + exportName + "\n"
				+ "exports.1.path=" + root.toString().replace( "\\", "\\\\") + "\n"
				+ "exports.1.writable=true\n"
				+ "exports.1.allowed.clients=" + allowedClients + "\n"
				+ "uid=0\n"
				+ "gid=0\n"
				+ "file.mode=0644\n"
				+ "directory.mode=0755\n"
				+ "block.size=4096\n"
				+ "read.size=8192\n"
				+ "write.size=16384\n"
				+ "directory.preferred.size=4096\n"
				+ "max.file.size=1099511627776\n"
				+ "time.delta.nanos=1000000\n"
				+ "pathconf.link.max=1024\n"
				+ "pathconf.name.max=255\n"
				+ "write.sync=true\n"
				+ "filename.charset=UTF-8\n" ;
		Files.writeString( configPath, configText, StandardCharsets.UTF_8) ;
		return configPath ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリを削除します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリ削除</p>
	 *
	 * @param path	対象パス
	 * @throws IOException 削除異常
	 */
	//--------------------------------------------------------------------------
	private void deleteDirectory(Path path) throws IOException {
		// ディレクトリが存在しない場合
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			return ;
		}

		try( var stream = Files.walk( path)) {
			List<Path> paths = stream.sorted( Comparator.reverseOrder()).toList() ;

			// 子から順に削除する
			for( Path target : paths) {
				Files.deleteIfExists( target) ;
			}
		}
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
	 * NFSv3 post_op_attrを読み飛ばします。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 post_op_attr読飛</p>
	 *
	 * @param reader	XDR読込
	 */
	//--------------------------------------------------------------------------
	private void skipPostOpAttrV3(XdrReader reader) {
		// 属性が存在しない場合
		if( !reader.readBoolean()) {
			return ;
		}

		skipAttributesV3( reader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 wcc_dataを読み飛ばします。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 wcc_data読飛</p>
	 *
	 * @param reader	XDR読込
	 */
	//--------------------------------------------------------------------------
	private void skipWccDataV3(XdrReader reader) {
		// pre_op_attrが存在する場合
		if( reader.readBoolean()) {
			reader.readLong() ;
			reader.readUnsignedInt() ;
			reader.readUnsignedInt() ;
			reader.readUnsignedInt() ;
			reader.readUnsignedInt() ;
		}

		skipPostOpAttrV3( reader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 wcc_dataの更新前サイズを確認します。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 wcc_data更新前サイズ確認</p>
	 *
	 * @param reader		XDR読込
	 * @param expectedSize	期待サイズ
	 */
	//--------------------------------------------------------------------------
	private void assertWccPreSizeV3(XdrReader reader, long expectedSize) {
		assertTrue( "v3 wcc pre-op follows", reader.readBoolean()) ;
		assertEquals( "v3 wcc pre-op size", expectedSize, reader.readLong()) ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		skipPostOpAttrV3( reader) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 fattr3を読み飛ばします。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 fattr3読飛</p>
	 *
	 * @param reader	XDR読込
	 */
	//--------------------------------------------------------------------------
	private void skipAttributesV3(XdrReader reader) {
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readLong() ;
		reader.readLong() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readLong() ;
		reader.readLong() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 fattr3のUID/GID以降を読み飛ばします。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 fattr3残属性読飛</p>
	 *
	 * @param reader	XDR読込
	 */
	//--------------------------------------------------------------------------
	private void skipAttributesV3Remainder(XdrReader reader) {
		reader.readLong() ;
		reader.readLong() ;
		reader.readInt() ;
		reader.readInt() ;
		reader.readLong() ;
		reader.readLong() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
		reader.readUnsignedInt() ;
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
	private void writeDiropargs(XdrWriter writer, FileHandle directory, String name) {
		writer.writeFixedOpaque( directory.getValue()) ;
		writer.writeString( name) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3 diropargsを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFSv3 diropargs書込</p>
	 *
	 * @param writer		書込
	 * @param directory		ディレクトリハンドル
	 * @param name			名前
	 */
	//--------------------------------------------------------------------------
	private void writeDiropargsV3(XdrWriter writer, FileHandle directory, String name) {
		writer.writeOpaque( directory.getValue()) ;
		writer.writeString( name) ;
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
	 * @param charset		ファイル名文字コード
	 */
	//--------------------------------------------------------------------------
	private void writeDiropargs(XdrWriter writer, FileHandle directory, String name, Charset charset) {
		writer.writeFixedOpaque( directory.getValue()) ;
		writer.writeString( name, charset) ;
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
	 * NFSv3未指定sattrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFSv3未指定sattr書込</p>
	 *
	 * @param writer	書込
	 */
	//--------------------------------------------------------------------------
	private void writeSattr3Unset(XdrWriter writer) {
		writer.writeBoolean( false) ;
		writer.writeBoolean( false) ;
		writer.writeBoolean( false) ;
		writer.writeBoolean( false) ;
		writer.writeInt( 0) ;
		writer.writeInt( 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3サイズ指定sattrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFSv3サイズ指定sattr書込</p>
	 *
	 * @param writer	書込
	 * @param size		サイズ
	 */
	//--------------------------------------------------------------------------
	private void writeSattr3Size(XdrWriter writer, long size) {
		writer.writeBoolean( false) ;
		writer.writeBoolean( false) ;
		writer.writeBoolean( false) ;
		writer.writeBoolean( true) ;
		writer.writeLong( size) ;
		writer.writeInt( 0) ;
		writer.writeInt( 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サイズ指定sattrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： サイズ指定sattr書込</p>
	 *
	 * @param writer	書込
	 * @param size		サイズ
	 */
	//--------------------------------------------------------------------------
	private void writeSattrSize(XdrWriter writer, int size) {
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeUnsignedInt( size) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * モード指定sattrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： モード指定sattr書込</p>
	 *
	 * @param writer	書込
	 * @param mode		モード
	 */
	//--------------------------------------------------------------------------
	private void writeSattrMode(XdrWriter writer, int mode) {
		writer.writeUnsignedInt( mode) ;
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
	 * 時刻指定sattrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 時刻指定sattr書込</p>
	 *
	 * @param writer		書込
	 * @param atimeSeconds	アクセス時刻秒
	 * @param mtimeSeconds	更新時刻秒
	 */
	//--------------------------------------------------------------------------
	private void writeSattrTimes(XdrWriter writer, int atimeSeconds, int mtimeSeconds) {
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeInt( -1) ;
		writer.writeUnsignedInt( atimeSeconds) ;
		writer.writeUnsignedInt( 0) ;
		writer.writeUnsignedInt( mtimeSeconds) ;
		writer.writeUnsignedInt( 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 真偽値を検証します。<br><br>
	 *
	 * <p>メソッド名称： 真偽値検証</p>
	 *
	 * @param name		名称
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void assertTrue(String name, boolean actual) {
		// 条件を満たさない場合
		if( !actual) {
			throw new AssertionError( name + " expected true.") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * false値を検証します。<br><br>
	 *
	 * <p>メソッド名称： false値検証</p>
	 *
	 * @param name		名称
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void assertFalse(String name, boolean actual) {
		// 条件がtrueの場合
		if( actual) {
			throw new AssertionError( name + " expected false.") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 例外発生を検証します。<br><br>
	 *
	 * <p>メソッド名称： 例外発生検証</p>
	 *
	 * @param name		名称
	 * @param runnable	処理
	 * @throws Exception 想定外異常
	 */
	//--------------------------------------------------------------------------
	private void assertThrows(String name, TestRunnable runnable) throws Exception {
		try {
			runnable.run() ;
		} catch( IllegalArgumentException iaex) {
			return ;
		}

		throw new AssertionError( name + " expected IllegalArgumentException.") ;
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
	 * long値を検証します。<br><br>
	 *
	 * <p>メソッド名称： long値検証</p>
	 *
	 * @param name		名称
	 * @param expected	期待値
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void assertEquals(String name, long expected, long actual) {
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

	//--------------------------------------------------------------------------
	/**
	 * バイト配列を検証します。<br><br>
	 *
	 * <p>メソッド名称： バイト配列検証</p>
	 *
	 * @param name		名称
	 * @param expected	期待値
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void assertBytes(String name, byte[] expected, byte[] actual) {
		assertEquals( name + " length", expected.length, actual.length) ;

		// バイト配列を先頭から比較する
		for( int i = 0; i < expected.length; i++) {
			assertEquals( name + "[" + i + "]", expected[i], actual[i]) ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * テスト処理インターフェースです。<br><br>
	 *
	 * <p>クラス名称： テスト処理</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	@FunctionalInterface
	private interface TestRunnable {
		//----------------------------------------------------------------------
		/**
		 * テストを実行します。<br><br>
		 *
		 * <p>メソッド名称： テスト実行</p>
		 *
		 * @throws Exception テスト異常
		 */
		//----------------------------------------------------------------------
		void run() throws Exception ;
	}

	//------------------------------------------------------------------------------
	/**
	 * READDIRページクラスです。<br><br>
	 *
	 * <p>クラス名称： READDIRページ</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class ReadDirPage {
		//	内部定義	--------------------------------------------------------
		/** 名前一覧 */
		private final List<String>		names ;

		/** cookie一覧 */
		private final List<Integer>		cookies ;

		/** EOF */
		private final boolean			eof ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param names		名前一覧
		 * @param cookies	cookie一覧
		 * @param eof		EOF
		 */
		//----------------------------------------------------------------------
		ReadDirPage(List<String> names, List<Integer> cookies, boolean eof) {
			this.names = names ;
			this.cookies = cookies ;
			this.eof = eof ;
		}

		//----------------------------------------------------------------------
		/**
		 * 名前一覧を取得します。<br><br>
		 *
		 * <p>メソッド名称： 名前一覧取得</p>
		 *
		 * @return 名前一覧
		 */
		//----------------------------------------------------------------------
		List<String> getNames() {
			return names ;
		}

		//----------------------------------------------------------------------
		/**
		 * 最後のcookieを取得します。<br><br>
		 *
		 * <p>メソッド名称： 最後のcookie取得</p>
		 *
		 * @return 最後のcookie
		 */
		//----------------------------------------------------------------------
		int getLastCookie() {
			return cookies.get( cookies.size() - 1) ;
		}

		//----------------------------------------------------------------------
		/**
		 * EOFを取得します。<br><br>
		 *
		 * <p>メソッド名称： EOF取得</p>
		 *
		 * @return true:EOF false:継続あり
		 */
		//----------------------------------------------------------------------
		boolean isEof() {
			return eof ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * テストコンテキストクラスです。<br><br>
	 *
	 * <p>クラス名称： テストコンテキスト</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private class TestContext {
		//	内部定義	--------------------------------------------------------
		/** 公開ルート */
		private final Path				root ;

		/** 設定 */
		private final NfsServerConfig	config ;

		/** ファイルハンドル管理 */
		private final FileHandleTable	handleTable ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param root			公開ルート
		 * @param config		設定
		 * @param handleTable	ファイルハンドル管理
		 */
		//----------------------------------------------------------------------
		TestContext(Path root, NfsServerConfig config, FileHandleTable handleTable) {
			this.root = root ;
			this.config = config ;
			this.handleTable = handleTable ;
		}

		//----------------------------------------------------------------------
		/**
		 * 設定を取得します。<br><br>
		 *
		 * <p>メソッド名称： 設定取得</p>
		 *
		 * @return 設定
		 */
		//----------------------------------------------------------------------
		NfsServerConfig getConfig() {
			return config ;
		}

		//----------------------------------------------------------------------
		/**
		 * ファイルハンドル管理を取得します。<br><br>
		 *
		 * <p>メソッド名称： ファイルハンドル管理取得</p>
		 *
		 * @return ファイルハンドル管理
		 */
		//----------------------------------------------------------------------
		FileHandleTable getHandleTable() {
			return handleTable ;
		}

		//----------------------------------------------------------------------
		/**
		 * 公開ルートを取得します。<br><br>
		 *
		 * <p>メソッド名称： 公開ルート取得</p>
		 *
		 * @return 公開ルート
		 */
		//----------------------------------------------------------------------
		Path getRoot() {
			return root ;
		}

		//----------------------------------------------------------------------
		/**
		 * テストコンテキストを終了します。<br><br>
		 *
		 * <p>メソッド名称： テストコンテキスト終了</p>
		 *
		 * @throws IOException 削除異常
		 */
		//----------------------------------------------------------------------
		void close() throws IOException {
			deleteDirectory( root) ;
		}
	}
}
