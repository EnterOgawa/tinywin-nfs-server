package jp.co.enterogawa.nfs;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.program.MountV1Program;
import jp.co.enterogawa.nfs.program.NfsStatus;
import jp.co.enterogawa.nfs.program.NfsV2Program;
import jp.co.enterogawa.nfs.program.PortmapV2Program;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcRequestContext;
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
		runTest( "Mount MNT", this::testMount) ;
		runTest( "Mount MNT v2", this::testMountV2) ;
		runTest( "Multiple exports", this::testMultipleExports) ;
		runTest( "Config validation", this::testConfigValidation) ;
		runTest( "Client access restrictions", this::testClientAccessRestrictions) ;
		runTest( "NFSv2 procedures", this::testNfsV2Procedures) ;
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
				+ "read.size=8192\n" ;
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
		assertReadFile( program, fileHandle) ;
		assertWriteCache( program) ;
		assertWriteFile( program, fileHandle, context.getRoot()) ;
		assertSetAttrFile( program, fileHandle, context.getRoot()) ;
		assertSetAttrMode( program, fileHandle) ;
		assertSetAttrTime( program, fileHandle) ;
		assertBidirectionalEditFile( program, fileHandle, context.getRoot()) ;
		assertTempRenameOverwriteFile( program, rootHandle, context.getRoot()) ;
		assertCreateFile( program, rootHandle, context.getRoot()) ;
		assertRenameFile( program, rootHandle, context.getRoot()) ;
		assertMkdirAndRmdir( program, rootHandle, context.getRoot()) ;
		assertReadDir( program, rootHandle) ;
		assertStatFs( program, rootHandle) ;
		assertInvalidLookup( program, rootHandle) ;
		assertReadLinkOnRegularFile( program, fileHandle) ;
		assertHardLink( program, rootHandle, fileHandle, context.getRoot()) ;
		assertLongSymlinkTarget( program, rootHandle) ;
		assertJapaneseCreate( program, rootHandle, context.getRoot()) ;
		context.close() ;
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
		XdrWriter response = new XdrWriter() ;
		RpcRequestContext context = new RpcRequestContext(
				clientAddress,
				12345,
				"test",
				XID,
				programNumber,
				version,
				procedure) ;
		int status = program.handle( call, context, response) ;

		assertEquals( "accept status", RpcConstants.ACCEPT_SUCCESS, status) ;
		return response ;
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
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( XID) ;
		writer.writeInt( RpcConstants.MSG_CALL) ;
		writer.writeInt( RpcConstants.RPC_VERSION) ;
		writer.writeInt( program) ;
		writer.writeInt( version) ;
		writer.writeInt( procedure) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeFixedOpaque( arguments.toByteArray()) ;
		return RpcCall.read( writer.toByteArray(), writer.toByteArray().length) ;
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
		return createContext( "UTF-8") ;
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
