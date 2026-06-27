package jp.co.enterogawa.nfs.program;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jp.co.enterogawa.nfs.config.NfsExport;
import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcProgram;
import jp.co.enterogawa.nfs.rpc.RpcRequestContext;
import jp.co.enterogawa.nfs.util.ServerLog;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * NFS Version 3プログラムクラスです。<br><br>
 *
 * <p>クラス名称： NFS Version 3プログラム</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsV3Program implements RpcProgram {
	//	定数定義	------------------------------------------------------------
	/** Version */
	private static final int				VERSION = 3 ;

	/** 最大ファイル名バイト数 */
	private static final int				MAX_NAME_BYTES = 255 ;

	/** READDIR cookie基底値 */
	private static final long			READDIR_COOKIE_BASE = 0x10000L ;

	/** 書込権限ビット */
	private static final int				MODE_WRITE_BITS = 0222 ;

	/** verifier */
	private static final byte[]			WRITE_VERIFIER = new byte[] { 'T', 'W', 'N', 'F', 'S', 'v', '3', 0 } ;

	/** cookie verifier */
	private static final byte[]			COOKIE_VERIFIER = new byte[8] ;

	/** NF3REG */
	private static final int				NF3REG = 1 ;

	/** NF3DIR */
	private static final int				NF3DIR = 2 ;

	/** NF3LNK */
	private static final int				NF3LNK = 5 ;

	/** ACCESS3_READ */
	private static final int				ACCESS_READ = 0x0001 ;

	/** ACCESS3_LOOKUP */
	private static final int				ACCESS_LOOKUP = 0x0002 ;

	/** ACCESS3_MODIFY */
	private static final int				ACCESS_MODIFY = 0x0004 ;

	/** ACCESS3_EXTEND */
	private static final int				ACCESS_EXTEND = 0x0008 ;

	/** ACCESS3_DELETE */
	private static final int				ACCESS_DELETE = 0x0010 ;

	/** ACCESS3_EXECUTE */
	private static final int				ACCESS_EXECUTE = 0x0020 ;

	/** FILE_SYNC */
	private static final int				FILE_SYNC = 2 ;

	/** UNCHECKED */
	private static final int				CREATE_UNCHECKED = 0 ;

	/** GUARDED */
	private static final int				CREATE_GUARDED = 1 ;

	/** EXCLUSIVE */
	private static final int				CREATE_EXCLUSIVE = 2 ;

	/** SET_TO_SERVER_TIME */
	private static final int				TIME_SET_TO_SERVER = 1 ;

	/** SET_TO_CLIENT_TIME */
	private static final int				TIME_SET_TO_CLIENT = 2 ;

	/** FSF3_LINK */
	private static final int				FSF_LINK = 0x0001 ;

	/** FSF3_SYMLINK */
	private static final int				FSF_SYMLINK = 0x0002 ;

	/** FSF3_HOMOGENEOUS */
	private static final int				FSF_HOMOGENEOUS = 0x0008 ;

	/** FSF3_CANSETTIME */
	private static final int				FSF_CANSETTIME = 0x0010 ;

	//	手続き定義	----------------------------------------------------------
	private static final int				PROC_NULL = 0 ;
	private static final int				PROC_GETATTR = 1 ;
	private static final int				PROC_SETATTR = 2 ;
	private static final int				PROC_LOOKUP = 3 ;
	private static final int				PROC_ACCESS = 4 ;
	private static final int				PROC_READLINK = 5 ;
	private static final int				PROC_READ = 6 ;
	private static final int				PROC_WRITE = 7 ;
	private static final int				PROC_CREATE = 8 ;
	private static final int				PROC_MKDIR = 9 ;
	private static final int				PROC_SYMLINK = 10 ;
	private static final int				PROC_MKNOD = 11 ;
	private static final int				PROC_REMOVE = 12 ;
	private static final int				PROC_RMDIR = 13 ;
	private static final int				PROC_RENAME = 14 ;
	private static final int				PROC_LINK = 15 ;
	private static final int				PROC_READDIR = 16 ;
	private static final int				PROC_READDIRPLUS = 17 ;
	private static final int				PROC_FSSTAT = 18 ;
	private static final int				PROC_FSINFO = 19 ;
	private static final int				PROC_PATHCONF = 20 ;
	private static final int				PROC_COMMIT = 21 ;

	//	内部定義	------------------------------------------------------------
	/** 設定 */
	private final NfsServerConfig		config ;

	/** ファイルハンドル管理 */
	private final FileHandleTable		handleTable ;

	/** ファイル名文字コード */
	private final Charset				filenameCharset ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param config		設定
	 * @param handleTable	ファイルハンドル管理
	 */
	//--------------------------------------------------------------------------
	public NfsV3Program(NfsServerConfig config, FileHandleTable handleTable) {
		this.config = config ;
		this.handleTable = handleTable ;
		filenameCharset = config.getFilenameCharset() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Program番号を取得します。<br><br>
	 *
	 * <p>メソッド名称： Program番号取得</p>
	 *
	 * @return Program番号
	 */
	//--------------------------------------------------------------------------
	@Override
	public int getProgramNumber() {
		return RpcConstants.PROGRAM_NFS ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Versionを取得します。<br><br>
	 *
	 * <p>メソッド名称： Version取得</p>
	 *
	 * @return Version
	 */
	//--------------------------------------------------------------------------
	@Override
	public int getVersion() {
		return VERSION ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出処理</p>
	 *
	 * @param call		RPC呼出
	 * @param response	応答
	 * @return ACCEPTステータス
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	@Override
	public int handle(RpcCall call, XdrWriter response) throws IOException {
		try {
			switch( call.getProcedure()) {
			case PROC_NULL:
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_GETATTR:
				handleGetAttr( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_SETATTR:
				handleSetAttr( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_LOOKUP:
				handleLookup( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_ACCESS:
				handleAccess( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READLINK:
				handleReadLink( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READ:
				handleRead( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_WRITE:
				handleWrite( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_CREATE:
				handleCreate( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_MKDIR:
				handleMkdir( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_SYMLINK:
				handleSymlink( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_MKNOD:
				handleMknod( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_REMOVE:
				handleRemove( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_RMDIR:
				handleRmdir( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_RENAME:
				handleRename( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_LINK:
				handleLink( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READDIR:
				handleReadDir( call.getArguments(), response, false) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READDIRPLUS:
				handleReadDir( call.getArguments(), response, true) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_FSSTAT:
				handleFsStat( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_FSINFO:
				handleFsInfo( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_PATHCONF:
				handlePathConf( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_COMMIT:
				handleCommit( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			default:
				return RpcConstants.ACCEPT_PROC_UNAVAIL ;
			}
		} catch( IllegalArgumentException iaex) {
			response.writeInt( NfsStatus.INVAL) ;
			return RpcConstants.ACCEPT_SUCCESS ;
		} catch( IOException ioex) {
			response.writeInt( mapIoStatus( ioex)) ;
			return RpcConstants.ACCEPT_SUCCESS ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * GETATTRを処理します。<br><br>
	 *
	 * <p>メソッド名称： GETATTR処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleGetAttr(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "READLINK", path, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "GETATTR", path, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		response.writeInt( NfsStatus.OK) ;
		writeAttributes( response, path, handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * SETATTRを処理します。<br><br>
	 *
	 * <p>メソッド名称： SETATTR処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleSetAttr(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		SetAttributes attributes = readSetAttributes( arguments) ;
		boolean guarded = arguments.readBoolean() ;

		// guard時刻が存在する場合
		if( guarded) {
			readTime( arguments) ;
		}

		Path path = handleTable.getPath( handle) ;
		Path beforePath = path ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			logMutation( "SETATTR", path, NfsStatus.STALE, "unknown-handle") ;
			response.writeInt( NfsStatus.STALE) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logMutation( "SETATTR", path, NfsStatus.ACCES, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "SETATTR", path, NfsStatus.ROFS, "read-only") ;
			response.writeInt( NfsStatus.ROFS) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		try {
			applySetAttributes( path, attributes, true) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "SETATTR", path, status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		logMutation( "SETATTR", path, NfsStatus.OK, attributes.describe()) ;
		response.writeInt( NfsStatus.OK) ;
		writeWccData( response, beforePath, path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * LOOKUPを処理します。<br><br>
	 *
	 * <p>メソッド名称： LOOKUP処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleLookup(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;
		Path directory = target.getDirectory() ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			writePostOpAttr( response, directory) ;
			return ;
		}

		Path path = target.getPath() ;

		// 子パスが存在しない場合
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOENT) ;
			writePostOpAttr( response, directory) ;
			return ;
		}

		FileHandle handle = handleTable.getOrCreate( path) ;
		response.writeInt( NfsStatus.OK) ;
		writeHandle( response, handle) ;
		writePostOpAttr( response, path) ;
		writePostOpAttr( response, directory) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ACCESSを処理します。<br><br>
	 *
	 * <p>メソッド名称： ACCESS処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleAccess(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		int requested = arguments.readInt() ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "ACCESS", path, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		int allowed = calculateAccess( path) & requested ;
		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, path) ;
		response.writeInt( allowed) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READLINKを処理します。<br><br>
	 *
	 * <p>メソッド名称： READLINK処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleReadLink(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// シンボリックリンクではない場合
		if( !Files.isSymbolicLink( path)) {
			response.writeInt( NfsStatus.INVAL) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		Path target = Files.readSymbolicLink( path) ;
		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, path) ;
		response.writeString( target.toString(), filenameCharset) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READを処理します。<br><br>
	 *
	 * <p>メソッド名称： READ処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleRead(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		long offset = arguments.readLong() ;
		int count = arguments.readInt() ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "READ", path, "client-denied offset=" + offset + " bytes=" + count) ;
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// 通常ファイルではない場合
		if( !Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.ISDIR) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		int readLength = Math.max( 0, Math.min( count, config.getReadSize())) ;
		byte[] data = new byte[readLength] ;
		int actualLength = 0 ;
		long fileLength = Files.size( path) ;

		try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "r")) {
			file.seek( offset) ;
			actualLength = file.read( data) ;
		}

		// EOFの場合
		if( actualLength < 0) {
			actualLength = 0 ;
		}

		byte[] actualData = new byte[actualLength] ;
		System.arraycopy( data, 0, actualData, 0, actualLength) ;
		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, path) ;
		response.writeInt( actualLength) ;
		response.writeBoolean( offset + actualLength >= fileLength) ;
		response.writeOpaque( actualData) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * WRITEを処理します。<br><br>
	 *
	 * <p>メソッド名称： WRITE処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleWrite(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		long offset = arguments.readLong() ;
		int count = arguments.readInt() ;
		arguments.readInt() ;
		byte[] data = arguments.readOpaque() ;
		Path path = handleTable.getPath( handle) ;
		Path beforePath = path ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logMutation( "WRITE", path, NfsStatus.ACCES, "client-denied offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.ACCES) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "WRITE", path, NfsStatus.ROFS, "read-only offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.ROFS) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		// 通常ファイルではない場合
		if( !Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "WRITE", path, NfsStatus.ISDIR, "not-regular offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.ISDIR) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		int writeLength = Math.min( count, data.length) ;

		try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "rw")) {
			file.seek( offset) ;
			file.write( data, 0, writeLength) ;
			file.getFD().sync() ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "WRITE", path, status, ioex.getClass().getSimpleName() + " offset=" + offset + " bytes=" + writeLength) ;
			response.writeInt( status) ;
			writeWccData( response, beforePath, path) ;
			return ;
		}

		logMutation( "WRITE", path, NfsStatus.OK, "offset=" + offset + " bytes=" + writeLength) ;
		response.writeInt( NfsStatus.OK) ;
		writeWccData( response, beforePath, path) ;
		response.writeInt( writeLength) ;
		response.writeInt( FILE_SYNC) ;
		response.writeFixedOpaque( WRITE_VERIFIER) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * CREATEを処理します。<br><br>
	 *
	 * <p>メソッド名称： CREATE処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleCreate(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;
		int mode = arguments.readInt() ;
		SetAttributes attributes = null ;

		// 属性指定がある場合
		if( mode == CREATE_UNCHECKED || mode == CREATE_GUARDED) {
			attributes = readSetAttributes( arguments) ;
		}
		// 排他作成の場合
		else if( mode == CREATE_EXCLUSIVE) {
			arguments.readFixedOpaque( 8) ;
		}

		// 対象パスが不正な場合
		if( !target.isOk()) {
			logMutation( "CREATE", target.getPath(), target.getStatus(), "invalid-target") ;
			writeCreateResult( response, target.getStatus(), target.getPath(), target.getDirectory(), null) ;
			return ;
		}

		Path path = target.getPath() ;
		Path directory = target.getDirectory() ;

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "CREATE", path, NfsStatus.ROFS, "read-only") ;
			writeCreateResult( response, NfsStatus.ROFS, path, directory, null) ;
			return ;
		}

		// guarded作成で既に存在する場合
		if( mode == CREATE_GUARDED && Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "CREATE", path, NfsStatus.EXIST, "exists") ;
			writeCreateResult( response, NfsStatus.EXIST, path, directory, null) ;
			return ;
		}

		try {
			// ファイルが存在しない場合
			if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
				Files.createFile( path) ;
			}

			// 属性が指定されている場合
			if( attributes != null) {
				applySetAttributes( path, attributes, false) ;
			}
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "CREATE", path, status, ioex.getClass().getSimpleName()) ;
			writeCreateResult( response, status, path, directory, null) ;
			return ;
		}

		FileHandle handle = handleTable.getOrCreate( path) ;
		logMutation( "CREATE", path, NfsStatus.OK, "mode=" + mode) ;
		writeCreateResult( response, NfsStatus.OK, path, directory, handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * MKDIRを処理します。<br><br>
	 *
	 * <p>メソッド名称： MKDIR処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleMkdir(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;
		SetAttributes attributes = readSetAttributes( arguments) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			writeCreateResult( response, target.getStatus(), target.getPath(), target.getDirectory(), null) ;
			return ;
		}

		Path path = target.getPath() ;

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "MKDIR", path, NfsStatus.ROFS, "read-only") ;
			writeCreateResult( response, NfsStatus.ROFS, path, target.getDirectory(), null) ;
			return ;
		}

		try {
			Files.createDirectory( path) ;
			applySetAttributes( path, attributes, false) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "MKDIR", path, status, ioex.getClass().getSimpleName()) ;
			writeCreateResult( response, status, path, target.getDirectory(), null) ;
			return ;
		}

		FileHandle handle = handleTable.getOrCreate( path) ;
		logMutation( "MKDIR", path, NfsStatus.OK, attributes.describe()) ;
		writeCreateResult( response, NfsStatus.OK, path, target.getDirectory(), handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * SYMLINKを処理します。<br><br>
	 *
	 * <p>メソッド名称： SYMLINK処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleSymlink(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;
		readSetAttributes( arguments) ;
		String linkTarget = arguments.readString( filenameCharset) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			writeCreateResult( response, target.getStatus(), target.getPath(), target.getDirectory(), null) ;
			return ;
		}

		Path path = target.getPath() ;

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "SYMLINK", path, NfsStatus.ROFS, "read-only") ;
			writeCreateResult( response, NfsStatus.ROFS, path, target.getDirectory(), null) ;
			return ;
		}

		try {
			Files.createSymbolicLink( path, Path.of( linkTarget)) ;
		} catch( IOException | UnsupportedOperationException | SecurityException ex) {
			int status = ex instanceof IOException ioex ? mapIoStatus( ioex) : NfsStatus.PERM ;
			logMutation( "SYMLINK", path, status, ex.getClass().getSimpleName()) ;
			writeCreateResult( response, status, path, target.getDirectory(), null) ;
			return ;
		}

		FileHandle handle = handleTable.getOrCreate( path) ;
		logMutation( "SYMLINK", path, NfsStatus.OK, "target=" + linkTarget) ;
		writeCreateResult( response, NfsStatus.OK, path, target.getDirectory(), handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * MKNODを処理します。<br><br>
	 *
	 * <p>メソッド名称： MKNOD処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleMknod(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;
		response.writeInt( NfsStatus.NOTSUPP) ;
		writeWccData( response, target.getDirectory(), target.getDirectory()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * REMOVEを処理します。<br><br>
	 *
	 * <p>メソッド名称： REMOVE処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleRemove(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		Path path = target.getPath() ;

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "REMOVE", path, NfsStatus.ROFS, "read-only") ;
			response.writeInt( NfsStatus.ROFS) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		try {
			Files.delete( path) ;
			handleTable.forget( path) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "REMOVE", path, status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		logMutation( "REMOVE", path, NfsStatus.OK, "") ;
		response.writeInt( NfsStatus.OK) ;
		writeWccData( response, target.getDirectory(), target.getDirectory()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RMDIRを処理します。<br><br>
	 *
	 * <p>メソッド名称： RMDIR処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleRmdir(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath target = readOperationPath( arguments) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		Path path = target.getPath() ;

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "RMDIR", path, NfsStatus.ROFS, "read-only") ;
			response.writeInt( NfsStatus.ROFS) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		try {
			Files.delete( path) ;
			handleTable.forget( path) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "RMDIR", path, status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		logMutation( "RMDIR", path, NfsStatus.OK, "") ;
		response.writeInt( NfsStatus.OK) ;
		writeWccData( response, target.getDirectory(), target.getDirectory()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RENAMEを処理します。<br><br>
	 *
	 * <p>メソッド名称： RENAME処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleRename(XdrReader arguments, XdrWriter response) throws IOException {
		ResolvedPath source = readOperationPath( arguments) ;
		ResolvedPath target = readOperationPath( arguments) ;

		// 移動元が不正な場合
		if( !source.isOk()) {
			response.writeInt( source.getStatus()) ;
			writeWccData( response, source.getDirectory(), source.getDirectory()) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		// 移動先が不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			writeWccData( response, source.getDirectory(), source.getDirectory()) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( source.getPath()) || !isWritable( target.getPath())) {
			response.writeInt( NfsStatus.ROFS) ;
			writeWccData( response, source.getDirectory(), source.getDirectory()) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		try {
			Files.move( source.getPath(), target.getPath(), StandardCopyOption.REPLACE_EXISTING) ;
			handleTable.move( source.getPath(), target.getPath()) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "RENAME", source.getPath(), status, ioex.getClass().getSimpleName() + " target=" + target.getPath()) ;
			response.writeInt( status) ;
			writeWccData( response, source.getDirectory(), source.getDirectory()) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		logMutation( "RENAME", source.getPath(), NfsStatus.OK, "target=" + target.getPath()) ;
		response.writeInt( NfsStatus.OK) ;
		writeWccData( response, source.getDirectory(), source.getDirectory()) ;
		writeWccData( response, target.getDirectory(), target.getDirectory()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * LINKを処理します。<br><br>
	 *
	 * <p>メソッド名称： LINK処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleLink(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle sourceHandle = readHandle( arguments) ;
		ResolvedPath target = readOperationPath( arguments) ;
		Path source = handleTable.getPath( sourceHandle) ;

		// 移動元が不明な場合
		if( source == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, source) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( source)) {
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, source) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		// 対象パスが不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			writePostOpAttr( response, source) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( source) || !isWritable( target.getPath())) {
			response.writeInt( NfsStatus.ROFS) ;
			writePostOpAttr( response, source) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		try {
			Files.createLink( target.getPath(), source) ;
			handleTable.getOrCreate( target.getPath()) ;
		} catch( IOException | UnsupportedOperationException | SecurityException ex) {
			int status = ex instanceof IOException ioex ? mapIoStatus( ioex) : NfsStatus.PERM ;
			response.writeInt( status) ;
			writePostOpAttr( response, source) ;
			writeWccData( response, target.getDirectory(), target.getDirectory()) ;
			return ;
		}

		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, source) ;
		writeWccData( response, target.getDirectory(), target.getDirectory()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIR/READDIRPLUSを処理します。<br><br>
	 *
	 * <p>メソッド名称： READDIR処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @param plus		READDIRPLUS有無
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleReadDir(XdrReader arguments, XdrWriter response, boolean plus) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		long cookie = arguments.readLong() ;
		arguments.readFixedOpaque( 8) ;
		int maxCount = 0 ;

		// READDIRPLUSの場合
		if( plus) {
			arguments.readInt() ;
			maxCount = arguments.readInt() ;
		}
		// READDIRの場合
		else {
			maxCount = arguments.readInt() ;
		}

		Path directory = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( directory == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, directory) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( directory)) {
			logAccessDenied( "READDIR", directory, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, directory) ;
			return ;
		}

		// ディレクトリではない場合
		if( !Files.isDirectory( directory, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOTDIR) ;
			writePostOpAttr( response, directory) ;
			return ;
		}

		List<DirectoryEntry> entries = listDirectory( directory) ;
		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, directory) ;
		response.writeFixedOpaque( COOKIE_VERIFIER) ;
		boolean eof = true ;
		int encodedSize = 16 ;

		// ディレクトリエントリを出力する
		for( DirectoryEntry entry : entries) {
			// 既に返却済みのentryの場合
			if( entry.getCookie() <= cookie) {
				continue ;
			}

			int entrySize = estimateDirectoryEntrySize( entry, plus) ;

			// 最大応答サイズを超える場合
			if( encodedSize + entrySize > maxCount && encodedSize > 16) {
				eof = false ;
				break ;
			}

			response.writeBoolean( true) ;
			response.writeLong( entry.getFileId()) ;
			response.writeString( entry.getName(), filenameCharset) ;
			response.writeLong( entry.getCookie()) ;
			encodedSize += entrySize ;

			// READDIRPLUSの場合
			if( plus) {
				writePostOpAttr( response, entry.getPath()) ;
				response.writeBoolean( true) ;
				writeHandle( response, handleTable.getOrCreate( entry.getPath())) ;
			}
		}

		response.writeBoolean( false) ;
		response.writeBoolean( eof) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリエントリの応答サイズを見積もります。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリエントリ応答サイズ見積</p>
	 *
	 * @param entry	エントリ
	 * @param plus	READDIRPLUS有無
	 * @return 応答サイズ
	 */
	//--------------------------------------------------------------------------
	private int estimateDirectoryEntrySize(DirectoryEntry entry, boolean plus) {
		int nameLength = entry.getName().getBytes( filenameCharset).length ;
		int padding = (4 - (nameLength % 4)) % 4 ;
		int size = 4 + 8 + 4 + nameLength + padding + 8 ;

		// READDIRPLUSの場合
		if( plus) {
			size += 4 + 84 + 4 + 4 + FileHandle.LENGTH ;
		}

		return size ;
	}

	//--------------------------------------------------------------------------
	/**
	 * FSSTATを処理します。<br><br>
	 *
	 * <p>メソッド名称： FSSTAT処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleFsStat(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		FileStore store = Files.getFileStore( path) ;
		long blockSize = config.getBlockSize() ;
		long total = store.getTotalSpace() ;
		long free = store.getUnallocatedSpace() ;
		long usable = store.getUsableSpace() ;
		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, path) ;
		response.writeLong( total) ;
		response.writeLong( free) ;
		response.writeLong( usable) ;
		response.writeLong( Math.max( 1L, total / blockSize)) ;
		response.writeLong( Math.max( 0L, free / blockSize)) ;
		response.writeLong( Math.max( 0L, usable / blockSize)) ;
		response.writeInt( 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * FSINFOを処理します。<br><br>
	 *
	 * <p>メソッド名称： FSINFO処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleFsInfo(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		int readSize = config.getReadSize() ;
		int writeSize = Math.max( 1024, config.getReadSize()) ;
		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, path) ;
		response.writeInt( readSize) ;
		response.writeInt( readSize) ;
		response.writeInt( 512) ;
		response.writeInt( writeSize) ;
		response.writeInt( writeSize) ;
		response.writeInt( 512) ;
		response.writeInt( 4096) ;
		response.writeLong( Long.MAX_VALUE) ;
		writeTimeValue( response, 0L, 1000000L) ;
		response.writeInt( FSF_LINK | FSF_SYMLINK | FSF_HOMOGENEOUS | FSF_CANSETTIME) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * PATHCONFを処理します。<br><br>
	 *
	 * <p>メソッド名称： PATHCONF処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handlePathConf(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			response.writeInt( NfsStatus.ACCES) ;
			writePostOpAttr( response, path) ;
			return ;
		}

		response.writeInt( NfsStatus.OK) ;
		writePostOpAttr( response, path) ;
		response.writeInt( 1024) ;
		response.writeInt( MAX_NAME_BYTES) ;
		response.writeBoolean( true) ;
		response.writeBoolean( true) ;
		response.writeBoolean( true) ;
		response.writeBoolean( true) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * COMMITを処理します。<br><br>
	 *
	 * <p>メソッド名称： COMMIT処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleCommit(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		arguments.readLong() ;
		arguments.readInt() ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.STALE) ;
			writeWccData( response, path, path) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			response.writeInt( NfsStatus.ACCES) ;
			writeWccData( response, path, path) ;
			return ;
		}

		response.writeInt( NfsStatus.OK) ;
		writeWccData( response, path, path) ;
		response.writeFixedOpaque( WRITE_VERIFIER) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 操作対象パスを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 操作対象パス読込</p>
	 *
	 * @param arguments	引数
	 * @return 解決済みパス
	 */
	//--------------------------------------------------------------------------
	private ResolvedPath readOperationPath(XdrReader arguments) {
		FileHandle directoryHandle = readHandle( arguments) ;
		String name = arguments.readString( filenameCharset) ;
		Path directory = handleTable.getPath( directoryHandle) ;

		// ディレクトリハンドルが不明な場合
		if( directory == null) {
			return ResolvedPath.status( NfsStatus.STALE, null, null) ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( directory)) {
			logAccessDenied( "LOOKUP", directory, "client-denied name=" + name) ;
			return ResolvedPath.status( NfsStatus.ACCES, directory, null) ;
		}

		// ディレクトリではない場合
		if( !Files.isDirectory( directory, LinkOption.NOFOLLOW_LINKS)) {
			return ResolvedPath.status( NfsStatus.NOTDIR, directory, null) ;
		}

		// 名前が不正な場合
		if( !isValidLookupName( name)) {
			return ResolvedPath.status( NfsStatus.ACCES, directory, null) ;
		}

		try {
			Path child = directory.resolve( name).toAbsolutePath().normalize() ;

			// 公開ルート外の場合
			if( !handleTable.isSameExport( directory, child)) {
				return ResolvedPath.status( NfsStatus.ACCES, directory, child) ;
			}

			return ResolvedPath.path( directory, child) ;
		} catch( InvalidPathException ipex) {
			return ResolvedPath.status( NfsStatus.ACCES, directory, null) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル読込</p>
	 *
	 * @param arguments	引数
	 * @return ファイルハンドル
	 */
	//--------------------------------------------------------------------------
	private FileHandle readHandle(XdrReader arguments) {
		return new FileHandle( arguments.readOpaque()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル書込</p>
	 *
	 * @param response	応答
	 * @param handle	ファイルハンドル
	 */
	//--------------------------------------------------------------------------
	private void writeHandle(XdrWriter response, FileHandle handle) {
		response.writeOpaque( handle.getValue()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 作成結果を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 作成結果書込</p>
	 *
	 * @param response	応答
	 * @param status	ステータス
	 * @param path		作成対象パス
	 * @param directory	親ディレクトリ
	 * @param handle	ファイルハンドル
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writeCreateResult(XdrWriter response, int status, Path path, Path directory, FileHandle handle) throws IOException {
		response.writeInt( status) ;

		// 正常終了の場合
		if( status == NfsStatus.OK) {
			response.writeBoolean( true) ;
			writeHandle( response, handle) ;
			writePostOpAttr( response, path) ;
		}

		writeWccData( response, directory, directory) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * post_op_attrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： post_op_attr書込</p>
	 *
	 * @param response	応答
	 * @param path		パス
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writePostOpAttr(XdrWriter response, Path path) throws IOException {
		// パスが存在しない場合
		if( path == null || !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeBoolean( false) ;
			return ;
		}

		response.writeBoolean( true) ;
		writeAttributes( response, path, handleTable.getOrCreate( path)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * wcc_dataを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： wcc_data書込</p>
	 *
	 * @param response	応答
	 * @param before	更新前パス
	 * @param after		更新後パス
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writeWccData(XdrWriter response, Path before, Path after) throws IOException {
		writePreOpAttr( response, before) ;
		writePostOpAttr( response, after) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * pre_op_attrを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： pre_op_attr書込</p>
	 *
	 * @param response	応答
	 * @param path		パス
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writePreOpAttr(XdrWriter response, Path path) throws IOException {
		// パスが存在しない場合
		if( path == null || !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeBoolean( false) ;
			return ;
		}

		BasicFileAttributes attributes = Files.readAttributes( path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS) ;
		response.writeBoolean( true) ;
		response.writeLong( attributes.size()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv3属性を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFSv3属性書込</p>
	 *
	 * @param response	応答
	 * @param path		パス
	 * @param handle	ファイルハンドル
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writeAttributes(XdrWriter response, Path path, FileHandle handle) throws IOException {
		BasicFileAttributes attributes = Files.readAttributes( path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS) ;
		boolean directory = attributes.isDirectory() ;
		boolean symbolicLink = attributes.isSymbolicLink() ;
		int type = NF3REG ;
		int mode = 0100000 | config.getFileMode() ;

		// ディレクトリの場合
		if( directory) {
			type = NF3DIR ;
			mode = 0040000 | config.getDirectoryMode() ;
		}
		// シンボリックリンクの場合
		else if( symbolicLink) {
			type = NF3LNK ;
			mode = 0120000 | config.getFileMode() ;
		}

		// 読込専用属性が設定されている場合
		if( isReadOnlyPath( path)) {
			mode &= ~MODE_WRITE_BITS ;
		}

		long size = attributes.size() ;
		long used = size == 0 ? 0 : ((size + config.getBlockSize() - 1L) / config.getBlockSize()) * config.getBlockSize() ;
		response.writeInt( type) ;
		response.writeUnsignedInt( mode) ;
		response.writeUnsignedInt( readLinkCount( path, directory)) ;
		response.writeUnsignedInt( config.getUid()) ;
		response.writeUnsignedInt( config.getGid()) ;
		response.writeLong( size) ;
		response.writeLong( used) ;
		response.writeUnsignedInt( 0) ;
		response.writeUnsignedInt( 0) ;
		response.writeLong( Integer.toUnsignedLong( handleTable.getRootPath( path).toString().hashCode())) ;
		response.writeLong( Integer.toUnsignedLong( handleTable.getFileId( handle))) ;
		writeTime( response, attributes.lastAccessTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * SETATTR属性を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： SETATTR属性読込</p>
	 *
	 * @param arguments	引数
	 * @return SETATTR属性
	 */
	//--------------------------------------------------------------------------
	private SetAttributes readSetAttributes(XdrReader arguments) {
		Integer mode = readOptionalUnsignedInt( arguments) ;
		Integer uid = readOptionalUnsignedInt( arguments) ;
		Integer gid = readOptionalUnsignedInt( arguments) ;
		Long size = readOptionalLong( arguments) ;
		FileTime accessTime = readOptionalSetTime( arguments) ;
		FileTime modifiedTime = readOptionalSetTime( arguments) ;
		return new SetAttributes( mode, uid, gid, size, accessTime, modifiedTime) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 任意uint32を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 任意uint32読込</p>
	 *
	 * @param arguments	引数
	 * @return 値
	 */
	//--------------------------------------------------------------------------
	private Integer readOptionalUnsignedInt(XdrReader arguments) {
		// 値が指定されていない場合
		if( !arguments.readBoolean()) {
			return null ;
		}

		return Integer.valueOf( arguments.readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 任意uint64を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 任意uint64読込</p>
	 *
	 * @param arguments	引数
	 * @return 値
	 */
	//--------------------------------------------------------------------------
	private Long readOptionalLong(XdrReader arguments) {
		// 値が指定されていない場合
		if( !arguments.readBoolean()) {
			return null ;
		}

		return Long.valueOf( arguments.readLong()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 任意時刻を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 任意時刻読込</p>
	 *
	 * @param arguments	引数
	 * @return 時刻
	 */
	//--------------------------------------------------------------------------
	private FileTime readOptionalSetTime(XdrReader arguments) {
		int mode = arguments.readInt() ;

		// サーバー時刻指定の場合
		if( mode == TIME_SET_TO_SERVER) {
			return FileTime.fromMillis( System.currentTimeMillis()) ;
		}
		// クライアント時刻指定の場合
		else if( mode == TIME_SET_TO_CLIENT) {
			return readTime( arguments) ;
		}

		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 属性を適用します。<br><br>
	 *
	 * <p>メソッド名称： 属性適用</p>
	 *
	 * @param path			対象パス
	 * @param attributes	属性
	 * @param allowSize		サイズ変更許可
	 * @throws IOException 反映異常
	 */
	//--------------------------------------------------------------------------
	private void applySetAttributes(Path path, SetAttributes attributes, boolean allowSize) throws IOException {
		// サイズ指定がある場合
		if( attributes.hasSize() && allowSize) {
			try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "rw")) {
				file.setLength( attributes.getSize()) ;
				file.getFD().sync() ;
			}
		}

		// モード指定がある場合
		if( attributes.hasMode()) {
			applyWritableMode( path, attributes.getMode()) ;
		}

		BasicFileAttributeView view = Files.getFileAttributeView( path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

		// 時刻指定がある場合
		if( attributes.getAccessTime() != null || attributes.getModifiedTime() != null) {
			view.setTimes( attributes.getModifiedTime(), attributes.getAccessTime(), null) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 書込モードを反映します。<br><br>
	 *
	 * <p>メソッド名称： 書込モード反映</p>
	 *
	 * @param path	対象パス
	 * @param mode	モード
	 * @throws IOException 反映異常
	 */
	//--------------------------------------------------------------------------
	private void applyWritableMode(Path path, int mode) throws IOException {
		boolean writable = (mode & MODE_WRITE_BITS) != 0 ;

		try {
			DosFileAttributeView view = Files.getFileAttributeView( path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

			// DOS属性が利用できる場合
			if( view != null) {
				view.setReadOnly( !writable) ;
				return ;
			}
		} catch( UnsupportedOperationException uoex) {
			// DOS属性が利用できない場合はFile APIへフォールバックする
		}

		path.toFile().setWritable( writable, false) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ACCESS許可値を計算します。<br><br>
	 *
	 * <p>メソッド名称： ACCESS許可値計算</p>
	 *
	 * @param path	パス
	 * @return ACCESS許可値
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private int calculateAccess(Path path) throws IOException {
		int allowed = 0 ;

		// 読込可能な場合
		if( Files.isReadable( path)) {
			allowed |= ACCESS_READ ;
		}

		// ディレクトリの場合
		if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS)) {
			allowed |= ACCESS_LOOKUP | ACCESS_EXECUTE ;
		}

		// 実行可能な場合
		if( Files.isExecutable( path)) {
			allowed |= ACCESS_EXECUTE ;
		}

		// 書込可能な場合
		if( isWritable( path) && Files.isWritable( path) && !isReadOnlyPath( path)) {
			allowed |= ACCESS_MODIFY | ACCESS_EXTEND | ACCESS_DELETE ;
		}

		return allowed ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリを一覧します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリ一覧</p>
	 *
	 * @param directory	ディレクトリ
	 * @return エントリ一覧
	 * @throws IOException 読込異常
	 */
	//--------------------------------------------------------------------------
	private List<DirectoryEntry> listDirectory(Path directory) throws IOException {
		List<DirectoryEntry> entries = new ArrayList<DirectoryEntry>() ;

		try( var stream = Files.list( directory)) {
			List<Path> paths = stream.sorted( Comparator.comparing( path -> path.getFileName().toString())).toList() ;
			int index = 1 ;

			// ディレクトリエントリを収集する
			for( Path child : paths) {
				FileHandle handle = handleTable.getOrCreate( child) ;
				entries.add( new DirectoryEntry(
						child.getFileName().toString(),
						child,
						READDIR_COOKIE_BASE + index,
						Integer.toUnsignedLong( handleTable.getFileId( handle)))) ;
				index++ ;
			}
		}

		return entries ;
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアント許可を確認します。<br><br>
	 *
	 * <p>メソッド名称： クライアント許可確認</p>
	 *
	 * @param path	対象パス
	 * @return true:許可 false:拒否
	 */
	//--------------------------------------------------------------------------
	private boolean isClientAllowed(Path path) {
		NfsExport export = handleTable.getExport( path) ;
		return export != null && export.allowsClient( RpcRequestContext.current().getClientAddress()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 書込可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 書込可否取得</p>
	 *
	 * @param path	対象パス
	 * @return true:書込可 false:書込不可
	 */
	//--------------------------------------------------------------------------
	private boolean isWritable(Path path) {
		return handleTable.isWritable( path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * LOOKUP名を検証します。<br><br>
	 *
	 * <p>メソッド名称： LOOKUP名検証</p>
	 *
	 * @param name	名前
	 * @return true:正常 false:不正
	 */
	//--------------------------------------------------------------------------
	private boolean isValidLookupName(String name) {
		// 名前が空の場合
		if( name == null || name.isEmpty()) {
			return false ;
		}

		// パス区切りを含む場合
		if( name.contains( "/" ) || name.contains( "\\")) {
			return false ;
		}

		// 親ディレクトリ参照の場合
		if( "..".equals( name)) {
			return false ;
		}

		return name.getBytes( filenameCharset).length <= MAX_NAME_BYTES ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 読込専用属性を取得します。<br><br>
	 *
	 * <p>メソッド名称： 読込専用属性取得</p>
	 *
	 * @param path	対象パス
	 * @return true:読込専用 false:書込可能
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private boolean isReadOnlyPath(Path path) throws IOException {
		try {
			DosFileAttributes attributes = Files.readAttributes( path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS) ;
			return attributes.isReadOnly() ;
		} catch( UnsupportedOperationException uoex) {
			return !Files.isWritable( path) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * リンク数を取得します。<br><br>
	 *
	 * <p>メソッド名称： リンク数取得</p>
	 *
	 * @param path		対象パス
	 * @param directory	ディレクトリ有無
	 * @return リンク数
	 * @throws IOException 読込異常
	 */
	//--------------------------------------------------------------------------
	private long readLinkCount(Path path, boolean directory) throws IOException {
		try {
			Object value = Files.getAttribute( path, "unix:nlink", LinkOption.NOFOLLOW_LINKS) ;

			// 数値属性の場合
			if( value instanceof Number number) {
				return Math.max( 1L, number.longValue()) ;
			}
		} catch( UnsupportedOperationException | IllegalArgumentException uoex) {
			// Windowsではunix:nlinkが利用できないため、近似値へフォールバックする
		}

		// ディレクトリの場合
		if( directory) {
			return 2L ;
		}

		return 1L ;
	}

	//--------------------------------------------------------------------------
	/**
	 * IO例外をNFSステータスへ変換します。<br><br>
	 *
	 * <p>メソッド名称： IO例外変換</p>
	 *
	 * @param ioex	IO例外
	 * @return NFSステータス
	 */
	//--------------------------------------------------------------------------
	private int mapIoStatus(IOException ioex) {
		// 対象が存在しない場合
		if( ioex instanceof NoSuchFileException) {
			return NfsStatus.NOENT ;
		}

		// アクセス拒否の場合
		if( ioex instanceof AccessDeniedException) {
			return NfsStatus.ACCES ;
		}

		// 対象が存在済みの場合
		if( ioex instanceof FileAlreadyExistsException) {
			return NfsStatus.EXIST ;
		}

		// ディレクトリが空ではない場合
		if( ioex instanceof DirectoryNotEmptyException) {
			return NfsStatus.NOTEMPTY ;
		}

		return NfsStatus.IO ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFS時刻を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： NFS時刻読込</p>
	 *
	 * @param reader	XDR読込
	 * @return ファイル時刻
	 */
	//--------------------------------------------------------------------------
	private FileTime readTime(XdrReader reader) {
		long seconds = reader.readUnsignedInt() ;
		long nanoseconds = reader.readUnsignedInt() ;
		return FileTime.from( seconds * 1000000000L + nanoseconds, TimeUnit.NANOSECONDS) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFS時刻を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFS時刻書込</p>
	 *
	 * @param response	応答
	 * @param time		時刻
	 */
	//--------------------------------------------------------------------------
	private void writeTime(XdrWriter response, FileTime time) {
		long totalNanoseconds = time.to( TimeUnit.NANOSECONDS) ;
		long seconds = totalNanoseconds / 1000000000L ;
		long nanoseconds = totalNanoseconds % 1000000000L ;
		writeTimeValue( response, seconds, nanoseconds) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFS時刻値を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFS時刻値書込</p>
	 *
	 * @param response	応答
	 * @param seconds	秒
	 * @param nanos		nano秒
	 */
	//--------------------------------------------------------------------------
	private void writeTimeValue(XdrWriter response, long seconds, long nanos) {
		response.writeUnsignedInt( seconds) ;
		response.writeUnsignedInt( nanos) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * アクセス拒否ログを出力します。<br><br>
	 *
	 * <p>メソッド名称： アクセス拒否ログ出力</p>
	 *
	 * @param operation	操作名
	 * @param path		対象パス
	 * @param detail	詳細
	 */
	//--------------------------------------------------------------------------
	private void logAccessDenied(String operation, Path path, String detail) {
		logMutation( operation, path, NfsStatus.ACCES, detail) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 変更操作ログを出力します。<br><br>
	 *
	 * <p>メソッド名称： 変更操作ログ出力</p>
	 *
	 * @param operation	操作名
	 * @param path		対象パス
	 * @param status	ステータス
	 * @param detail	詳細
	 */
	//--------------------------------------------------------------------------
	private void logMutation(String operation, Path path, int status, String detail) {
		RpcRequestContext context = RpcRequestContext.current() ;
		StringBuilder message = new StringBuilder() ;
		message.append( "NFSv3 " ).append( operation)
				.append( " client=" ).append( context.getClientAddress())
				.append( " xid=" ).append( context.formatXid())
				.append( " status=" ).append( status) ;

		// パスが存在する場合
		if( path != null) {
			message.append( " path=" ).append( path) ;
		}

		// 詳細が存在する場合
		if( detail != null && !detail.isBlank()) {
			message.append( " " ).append( detail) ;
		}

		ServerLog.info( message.toString()) ;
	}

	//------------------------------------------------------------------------------
	/**
	 * SETATTR属性クラスです。<br><br>
	 *
	 * <p>クラス名称： SETATTR属性</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class SetAttributes {
		/** モード */
		private final Integer			mode ;

		/** UID */
		private final Integer			uid ;

		/** GID */
		private final Integer			gid ;

		/** サイズ */
		private final Long				size ;

		/** アクセス時刻 */
		private final FileTime			accessTime ;

		/** 更新時刻 */
		private final FileTime			modifiedTime ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param mode			モード
		 * @param uid			UID
		 * @param gid			GID
		 * @param size			サイズ
		 * @param accessTime	アクセス時刻
		 * @param modifiedTime	更新時刻
		 */
		//----------------------------------------------------------------------
		SetAttributes(Integer mode, Integer uid, Integer gid, Long size, FileTime accessTime, FileTime modifiedTime) {
			this.mode = mode ;
			this.uid = uid ;
			this.gid = gid ;
			this.size = size ;
			this.accessTime = accessTime ;
			this.modifiedTime = modifiedTime ;
		}

		//----------------------------------------------------------------------
		/**
		 * モード指定有無を取得します。<br><br>
		 *
		 * <p>メソッド名称： モード指定有無取得</p>
		 *
		 * @return true:指定あり false:指定なし
		 */
		//----------------------------------------------------------------------
		boolean hasMode() {
			return mode != null ;
		}

		//----------------------------------------------------------------------
		/**
		 * サイズ指定有無を取得します。<br><br>
		 *
		 * <p>メソッド名称： サイズ指定有無取得</p>
		 *
		 * @return true:指定あり false:指定なし
		 */
		//----------------------------------------------------------------------
		boolean hasSize() {
			return size != null ;
		}

		//----------------------------------------------------------------------
		/**
		 * モードを取得します。<br><br>
		 *
		 * <p>メソッド名称： モード取得</p>
		 *
		 * @return モード
		 */
		//----------------------------------------------------------------------
		int getMode() {
			return mode.intValue() ;
		}

		//----------------------------------------------------------------------
		/**
		 * サイズを取得します。<br><br>
		 *
		 * <p>メソッド名称： サイズ取得</p>
		 *
		 * @return サイズ
		 */
		//----------------------------------------------------------------------
		long getSize() {
			return size.longValue() ;
		}

		//----------------------------------------------------------------------
		/**
		 * アクセス時刻を取得します。<br><br>
		 *
		 * <p>メソッド名称： アクセス時刻取得</p>
		 *
		 * @return アクセス時刻
		 */
		//----------------------------------------------------------------------
		FileTime getAccessTime() {
			return accessTime ;
		}

		//----------------------------------------------------------------------
		/**
		 * 更新時刻を取得します。<br><br>
		 *
		 * <p>メソッド名称： 更新時刻取得</p>
		 *
		 * @return 更新時刻
		 */
		//----------------------------------------------------------------------
		FileTime getModifiedTime() {
			return modifiedTime ;
		}

		//----------------------------------------------------------------------
		/**
		 * ログ用説明を取得します。<br><br>
		 *
		 * <p>メソッド名称： ログ用説明取得</p>
		 *
		 * @return ログ用説明
		 */
		//----------------------------------------------------------------------
		String describe() {
			List<String> values = new ArrayList<String>() ;

			// モード指定がある場合
			if( mode != null) {
				values.add( "mode=" + Integer.toOctalString( mode.intValue())) ;
			}

			// UID指定がある場合
			if( uid != null) {
				values.add( "uid=" + Integer.toUnsignedLong( uid.intValue())) ;
			}

			// GID指定がある場合
			if( gid != null) {
				values.add( "gid=" + Integer.toUnsignedLong( gid.intValue())) ;
			}

			// サイズ指定がある場合
			if( size != null) {
				values.add( "size=" + size) ;
			}

			// アクセス時刻指定がある場合
			if( accessTime != null) {
				values.add( "atime=" + accessTime) ;
			}

			// 更新時刻指定がある場合
			if( modifiedTime != null) {
				values.add( "mtime=" + modifiedTime) ;
			}

			return String.join( " ", values) ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 解決済みパスクラスです。<br><br>
	 *
	 * <p>クラス名称： 解決済みパス</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class ResolvedPath {
		/** ステータス */
		private final int				status ;

		/** 親ディレクトリ */
		private final Path				directory ;

		/** パス */
		private final Path				path ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param status		ステータス
		 * @param directory	親ディレクトリ
		 * @param path		パス
		 */
		//----------------------------------------------------------------------
		private ResolvedPath(int status, Path directory, Path path) {
			this.status = status ;
			this.directory = directory ;
			this.path = path ;
		}

		//----------------------------------------------------------------------
		/**
		 * 正常パスを生成します。<br><br>
		 *
		 * <p>メソッド名称： 正常パス生成</p>
		 *
		 * @param directory	親ディレクトリ
		 * @param path		パス
		 * @return 解決済みパス
		 */
		//----------------------------------------------------------------------
		static ResolvedPath path(Path directory, Path path) {
			return new ResolvedPath( NfsStatus.OK, directory, path) ;
		}

		//----------------------------------------------------------------------
		/**
		 * ステータスを生成します。<br><br>
		 *
		 * <p>メソッド名称： ステータス生成</p>
		 *
		 * @param status		ステータス
		 * @param directory	親ディレクトリ
		 * @param path		パス
		 * @return 解決済みパス
		 */
		//----------------------------------------------------------------------
		static ResolvedPath status(int status, Path directory, Path path) {
			return new ResolvedPath( status, directory, path) ;
		}

		//----------------------------------------------------------------------
		/**
		 * 正常有無を取得します。<br><br>
		 *
		 * <p>メソッド名称： 正常有無取得</p>
		 *
		 * @return true:正常 false:異常
		 */
		//----------------------------------------------------------------------
		boolean isOk() {
			return status == NfsStatus.OK ;
		}

		//----------------------------------------------------------------------
		/**
		 * ステータスを取得します。<br><br>
		 *
		 * <p>メソッド名称： ステータス取得</p>
		 *
		 * @return ステータス
		 */
		//----------------------------------------------------------------------
		int getStatus() {
			return status ;
		}

		//----------------------------------------------------------------------
		/**
		 * 親ディレクトリを取得します。<br><br>
		 *
		 * <p>メソッド名称： 親ディレクトリ取得</p>
		 *
		 * @return 親ディレクトリ
		 */
		//----------------------------------------------------------------------
		Path getDirectory() {
			return directory ;
		}

		//----------------------------------------------------------------------
		/**
		 * パスを取得します。<br><br>
		 *
		 * <p>メソッド名称： パス取得</p>
		 *
		 * @return パス
		 */
		//----------------------------------------------------------------------
		Path getPath() {
			return path ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * ディレクトリエントリクラスです。<br><br>
	 *
	 * <p>クラス名称： ディレクトリエントリ</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class DirectoryEntry {
		/** 名前 */
		private final String				name ;

		/** パス */
		private final Path				path ;

		/** cookie */
		private final long				cookie ;

		/** file id */
		private final long				fileId ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param name	名前
		 * @param path	パス
		 * @param cookie cookie
		 * @param fileId file id
		 */
		//----------------------------------------------------------------------
		DirectoryEntry(String name, Path path, long cookie, long fileId) {
			this.name = name ;
			this.path = path ;
			this.cookie = cookie ;
			this.fileId = fileId ;
		}

		//----------------------------------------------------------------------
		/**
		 * 名前を取得します。<br><br>
		 *
		 * <p>メソッド名称： 名前取得</p>
		 *
		 * @return 名前
		 */
		//----------------------------------------------------------------------
		String getName() {
			return name ;
		}

		//----------------------------------------------------------------------
		/**
		 * パスを取得します。<br><br>
		 *
		 * <p>メソッド名称： パス取得</p>
		 *
		 * @return パス
		 */
		//----------------------------------------------------------------------
		Path getPath() {
			return path ;
		}

		//----------------------------------------------------------------------
		/**
		 * cookieを取得します。<br><br>
		 *
		 * <p>メソッド名称： cookie取得</p>
		 *
		 * @return cookie
		 */
		//----------------------------------------------------------------------
		long getCookie() {
			return cookie ;
		}

		//----------------------------------------------------------------------
		/**
		 * file idを取得します。<br><br>
		 *
		 * <p>メソッド名称： file id取得</p>
		 *
		 * @return file id
		 */
		//----------------------------------------------------------------------
		long getFileId() {
			return fileId ;
		}
	}
}
