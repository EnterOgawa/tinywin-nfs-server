package jp.co.enterogawa.nfs.program;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jp.co.enterogawa.nfs.config.NfsExport;
import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.io.WriteFileCache;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcProgram;
import jp.co.enterogawa.nfs.rpc.RpcRequestContext;
import jp.co.enterogawa.nfs.util.ServerLog;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * NFS Version 2プログラムクラスです。<br><br>
 *
 * <p>クラス名称： NFS Version 2プログラム</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsV2Program implements RpcProgram {
	//	定数定義	------------------------------------------------------------
	/** Version */
	private static final int				VERSION = 2 ;

	/** NFSv2最大ファイル名バイト数 */
	private static final int				MAX_NAME_BYTES = 255 ;

	/** NFSv2最大パスバイト数 */
	private static final int				MAX_PATH_BYTES = 1024 ;

	/** NFSv2最大WRITEデータバイト数 */
	private static final int				MAX_WRITE_BYTES = 8192 ;

	/** 書込権限ビット */
	private static final int				MODE_WRITE_BITS = 0222 ;

	/** READDIR通常cookie基底値 */
	private static final int				READDIR_COOKIE_BASE = 0x10000 ;

	/** NFSPROC_NULL */
	private static final int				PROC_NULL = 0 ;

	/** NFSPROC_GETATTR */
	private static final int				PROC_GETATTR = 1 ;

	/** NFSPROC_SETATTR */
	private static final int				PROC_SETATTR = 2 ;

	/** NFSPROC_ROOT */
	private static final int				PROC_ROOT = 3 ;

	/** NFSPROC_LOOKUP */
	private static final int				PROC_LOOKUP = 4 ;

	/** NFSPROC_READLINK */
	private static final int				PROC_READLINK = 5 ;

	/** NFSPROC_READ */
	private static final int				PROC_READ = 6 ;

	/** NFSPROC_WRITECACHE */
	private static final int				PROC_WRITECACHE = 7 ;

	/** NFSPROC_WRITE */
	private static final int				PROC_WRITE = 8 ;

	/** NFSPROC_CREATE */
	private static final int				PROC_CREATE = 9 ;

	/** NFSPROC_REMOVE */
	private static final int				PROC_REMOVE = 10 ;

	/** NFSPROC_RENAME */
	private static final int				PROC_RENAME = 11 ;

	/** NFSPROC_LINK */
	private static final int				PROC_LINK = 12 ;

	/** NFSPROC_SYMLINK */
	private static final int				PROC_SYMLINK = 13 ;

	/** NFSPROC_MKDIR */
	private static final int				PROC_MKDIR = 14 ;

	/** NFSPROC_RMDIR */
	private static final int				PROC_RMDIR = 15 ;

	/** NFSPROC_READDIR */
	private static final int				PROC_READDIR = 16 ;

	/** NFSPROC_STATFS */
	private static final int				PROC_STATFS = 17 ;

	/** 通常ファイル */
	private static final int				NFREG = 1 ;

	/** ディレクトリ */
	private static final int				NFDIR = 2 ;

	/** シンボリックリンク */
	private static final int				NFLNK = 5 ;

	//	内部定義	------------------------------------------------------------
	/** 設定 */
	private final NfsServerConfig		config ;

	/** ファイルハンドル管理 */
	private final FileHandleTable		handleTable ;

	/** ファイル名文字コード */
	private final Charset				filenameCharset ;

	/** 書込ファイルキャッシュ */
	private final WriteFileCache			writeFileCache ;

	/** NFS経由hard linkグループ */
	private final Map<Path, HardLinkGroup> hardLinkGroups = new HashMap<Path, HardLinkGroup>() ;

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
	public NfsV2Program(NfsServerConfig config, FileHandleTable handleTable) {
		this( config, handleTable, new WriteFileCache( config)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param config			設定
	 * @param handleTable		ファイルハンドル管理
	 * @param writeFileCache	書込ファイルキャッシュ
	 */
	//--------------------------------------------------------------------------
	public NfsV2Program(NfsServerConfig config, FileHandleTable handleTable, WriteFileCache writeFileCache) {
		this.config = config ;
		this.handleTable = handleTable ;
		this.writeFileCache = writeFileCache ;
		filenameCharset = config.getFilenameCharset() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * リソースを閉じます。<br><br>
	 *
	 * <p>メソッド名称： リソースクローズ</p>
	 *
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	public void close() throws IOException {
		writeFileCache.close() ;
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
			case PROC_ROOT:
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_GETATTR:
				handleGetAttr( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_LOOKUP:
				handleLookup( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READLINK:
				handleReadLink( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READ:
				handleRead( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_WRITECACHE:
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_READDIR:
				handleReadDir( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_STATFS:
				handleStatFs( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_SETATTR:
				handleSetAttr( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_WRITE:
				handleWrite( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_CREATE:
				handleCreate( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_REMOVE:
				handleRemove( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_RENAME:
				handleRename( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_LINK:
				handleLink( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_SYMLINK:
				handleSymlink( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_MKDIR:
				handleMkdir( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			case PROC_RMDIR:
				handleRmdir( call.getArguments(), response) ;
				return RpcConstants.ACCEPT_SUCCESS ;

			default:
				return RpcConstants.ACCEPT_PROC_UNAVAIL ;
			}
		} catch( IllegalArgumentException iaex) {
			if( call.getProcedure() == PROC_WRITE) {
				logWriteParseError( call, iaex) ;
			}
			response.writeInt( NfsStatus.INVAL) ;
			return RpcConstants.ACCEPT_SUCCESS ;
		} catch( IOException ioex) {
			response.writeInt( NfsStatus.IO) ;
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
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "GETATTR", path, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		writeStatusAttr( response, path, handle) ;
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
		FileHandle directoryHandle = readHandle( arguments) ;
		String name = readName( arguments) ;
		Path directory = handleTable.getPath( directoryHandle) ;

		// ディレクトリハンドルが不明な場合
		if( directory == null) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( directory)) {
			logAccessDenied( "LOOKUP", directory, "client-denied name=" + name) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// ディレクトリではない場合
		if( !Files.isDirectory( directory, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOTDIR) ;
			return ;
		}

		// 名前が不正な場合
		if( !isValidLookupName( name)) {
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		Path child = null ;

		try {
			child = resolveLookupPath( directory, name) ;
		} catch( InvalidPathException ipex) {
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 公開ルート外の場合
		if( !isInSameExportRoot( directory, child)) {
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 子パスが存在しない場合
		if( !Files.exists( child, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		FileHandle childHandle = handleTable.getOrCreate( child) ;
		response.writeInt( NfsStatus.OK) ;
		response.writeFixedOpaque( childHandle.getValue()) ;
		writeAttributes( response, child, childHandle) ;
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
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "READLINK", path, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// シンボリックリンクではない場合
		if( !Files.isSymbolicLink( path)) {
			response.writeInt( NfsStatus.INVAL) ;
			return ;
		}

		Path target = Files.readSymbolicLink( path) ;
		response.writeInt( NfsStatus.OK) ;
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
		long offset = arguments.readUnsignedInt() ;
		int count = arguments.readInt() ;
		arguments.readInt() ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "READ", path, "client-denied offset=" + offset + " bytes=" + count) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 通常ファイルではない場合
		if( !Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.ISDIR) ;
			return ;
		}

		int readLength = Math.max( 0, Math.min( count, config.getReadSize())) ;
		byte[] data = new byte[readLength] ;
		int actualLength = 0 ;

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
		writeAttributes( response, path, handle) ;
		response.writeOpaque( actualData) ;
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
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			logMutation( "SETATTR", path, NfsStatus.NOENT, "unknown-handle" ) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logMutation( "SETATTR", path, NfsStatus.ACCES, "client-denied" ) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "SETATTR", path, NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		try {
			writeFileCache.close( path) ;
			int status = applySetAttributes( path, attributes, true) ;

			// 属性反映が失敗した場合
			if( status != NfsStatus.OK) {
				logMutation( "SETATTR", path, status, attributes.describe()) ;
				response.writeInt( status) ;
				return ;
			}
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "SETATTR", path, status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			return ;
		}

		logMutation( "SETATTR", path, NfsStatus.OK, attributes.describe()) ;
		writeStatusAttr( response, path, handle) ;
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
		arguments.readUnsignedInt() ;
		long offset = arguments.readUnsignedInt() ;
		long totalCount = arguments.readUnsignedInt() ;
		byte[] data = readWriteData( arguments, totalCount) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			logMutation( "WRITE", path, NfsStatus.NOENT, "unknown-handle offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logMutation( "WRITE", path, NfsStatus.ACCES, "client-denied offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "WRITE", path, NfsStatus.ROFS, "read-only offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 通常ファイルではない場合
		if( !Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "WRITE", path, NfsStatus.ISDIR, "not-regular offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.ISDIR) ;
			return ;
		}

		try {
			writeFileCache.write( path, offset, data, data.length, config.isWriteSync()) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "WRITE", path, status, ioex.getClass().getSimpleName() + " offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( status) ;
			return ;
		}

		logMutation( "WRITE", path, NfsStatus.OK, "offset=" + offset + " bytes=" + data.length) ;
		writeStatusAttr( response, path, handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * WRITEデータを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： WRITEデータ読込</p>
	 *
	 * @param arguments	引数
	 * @param totalCount	WRITE totalcount
	 * @return WRITEデータ
	 */
	//--------------------------------------------------------------------------
	private byte[] readWriteData(XdrReader arguments, long totalCount) {
		if( isValidWriteCount( totalCount)) {
			int count = (int)totalCount ;

			// QNX 4.25はNFSv2 WRITEのdataを長さなし固定長opaqueとして送る場合がある。
			if( arguments.remainingLength() == getPaddedLength( count)) {
				return arguments.readFixedOpaque( count) ;
			}
		}

		int position = arguments.getPosition() ;
		try {
			return arguments.readOpaque() ;
		} catch( IllegalArgumentException iaex) {
			arguments.setPosition( position) ;

			byte[] unpaddedData = readUnpaddedOpaqueData( arguments) ;

			// QNX 4.25はNFSv2 WRITEのdataを長さ付きだがpaddingなしで送る場合がある。
			if( unpaddedData != null) {
				return unpaddedData ;
			}

			// data lengthが付かない古いクライアント向けの限定的な互換処理
			if( isValidWriteCount( totalCount) && arguments.remainingLength() == getPaddedLength( (int)totalCount)) {
				return arguments.readFixedOpaque( (int)totalCount) ;
			}

			throw iaex ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * パディングなしopaqueデータを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： パディングなしopaqueデータ読込</p>
	 *
	 * @param arguments	引数
	 * @return データ。対象外の場合はnull
	 */
	//--------------------------------------------------------------------------
	private byte[] readUnpaddedOpaqueData(XdrReader arguments) {
		int position = arguments.getPosition() ;

		// lengthフィールドが存在しない場合
		if( arguments.remainingLength() < Integer.BYTES) {
			arguments.setPosition( position) ;
			return null ;
		}

		long dataLength = arguments.readUnsignedInt() ;

		// lengthが不正な場合
		if( !isValidWriteCount( dataLength)) {
			arguments.setPosition( position) ;
			return null ;
		}

		// paddingなしのデータ長と一致する場合
		if( arguments.remainingLength() == (int)dataLength) {
			return arguments.readFixedOpaqueWithoutPadding( (int)dataLength) ;
		}

		arguments.setPosition( position) ;
		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * WRITE解析エラーを出力します。<br><br>
	 *
	 * <p>メソッド名称： WRITE解析エラー出力</p>
	 *
	 * @param call		RPC呼出
	 * @param cause		原因
	 */
	//--------------------------------------------------------------------------
	private void logWriteParseError(RpcCall call, IllegalArgumentException cause) {
		RpcRequestContext context = RpcRequestContext.current() ;
		byte[] arguments = call.getArgumentBytes() ;
		StringBuilder message = new StringBuilder() ;
		message.append( "NFS WRITE parse-error" )
				.append( " client=" ).append( context.getClientAddress())
				.append( " xid=" ).append( context.formatXid())
				.append( " program=" ).append( call.getProgram())
				.append( " version=" ).append( call.getVersion())
				.append( " procedure=" ).append( call.getProcedure())
				.append( " status=" ).append( NfsStatus.INVAL)
				.append( " reason=" ).append( cause.getMessage())
				.append( " argsLength=" ).append( arguments.length)
				.append( " fields=" ).append( formatWriteArgumentFields( arguments)) ;

		// デバッグログの場合のみファイル内容を含み得るHEXを出力する
		if( ServerLog.isDebugEnabled()) {
			message.append( " argsHex=" ).append( formatHex( arguments, 128)) ;
		}

		ServerLog.info( message.toString()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * WRITE引数フィールドを整形します。<br><br>
	 *
	 * <p>メソッド名称： WRITE引数フィールド整形</p>
	 *
	 * @param arguments	引数
	 * @return フィールド文字列
	 */
	//--------------------------------------------------------------------------
	private String formatWriteArgumentFields(byte[] arguments) {
		StringBuilder fields = new StringBuilder() ;
		appendUnsignedField( fields, "begin", arguments, FileHandle.LENGTH) ;
		appendUnsignedField( fields, "offset", arguments, FileHandle.LENGTH + Integer.BYTES) ;
		appendUnsignedField( fields, "total", arguments, FileHandle.LENGTH + Integer.BYTES * 2) ;
		appendUnsignedField( fields, "dataLength", arguments, FileHandle.LENGTH + Integer.BYTES * 3) ;
		return fields.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 符号なしフィールドを追加します。<br><br>
	 *
	 * <p>メソッド名称： 符号なしフィールド追加</p>
	 *
	 * @param builder	文字列
	 * @param name		名前
	 * @param data		データ
	 * @param offset	オフセット
	 */
	//--------------------------------------------------------------------------
	private void appendUnsignedField(StringBuilder builder, String name, byte[] data, int offset) {
		if( builder.length() > 0) {
			builder.append( "," ) ;
		}

		builder.append( name ).append( "=" ) ;

		if( data.length < offset + Integer.BYTES) {
			builder.append( "missing" ) ;
			return ;
		}

		builder.append( Integer.toUnsignedLong( readInt( data, offset))) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 32bit整数を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 32bit整数読込</p>
	 *
	 * @param data		データ
	 * @param offset	オフセット
	 * @return 32bit整数
	 */
	//--------------------------------------------------------------------------
	private int readInt(byte[] data, int offset) {
		return ((data[offset] & 0xff) << 24)
				| ((data[offset + 1] & 0xff) << 16)
				| ((data[offset + 2] & 0xff) << 8)
				| (data[offset + 3] & 0xff) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * HEX文字列を整形します。<br><br>
	 *
	 * <p>メソッド名称： HEX文字列整形</p>
	 *
	 * @param data		データ
	 * @param maxBytes	最大バイト数
	 * @return HEX文字列
	 */
	//--------------------------------------------------------------------------
	private String formatHex(byte[] data, int maxBytes) {
		StringBuilder hex = new StringBuilder() ;
		int length = Math.min( data.length, maxBytes) ;

		for( int i = 0; i < length; i++) {
			if( i > 0) {
				hex.append( ' ' ) ;
			}

			hex.append( String.format( "%02x", data[i] & 0xff)) ;
		}

		if( data.length > maxBytes) {
			hex.append( " ..." ) ;
		}

		return hex.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * WRITE countが有効か確認します。<br><br>
	 *
	 * <p>メソッド名称： WRITE count有効確認</p>
	 *
	 * @param count	WRITE count
	 * @return true:有効 false:無効
	 */
	//--------------------------------------------------------------------------
	private boolean isValidWriteCount(long count) {
		return 0 <= count && count <= MAX_WRITE_BYTES ;
	}

	//--------------------------------------------------------------------------
	/**
	 * XDRパディング込みの長さを取得します。<br><br>
	 *
	 * <p>メソッド名称： XDRパディング長取得</p>
	 *
	 * @param length	実データ長
	 * @return パディング込み長
	 */
	//--------------------------------------------------------------------------
	private int getPaddedLength(int length) {
		return length + (4 - (length % 4)) % 4 ;
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
		SetAttributes attributes = readSetAttributes( arguments) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			logMutation( "CREATE", target.getPath(), target.getStatus(), "invalid-target" ) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( target.getPath())) {
			logMutation( "CREATE", target.getPath(), NfsStatus.ACCES, "client-denied" ) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			logMutation( "CREATE", target.getPath(), NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 対象ファイルが既に存在する場合
		if( Files.exists( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "CREATE", target.getPath(), NfsStatus.EXIST, "exists" ) ;
			response.writeInt( NfsStatus.EXIST) ;
			return ;
		}

		try {
			Files.createFile( target.getPath()) ;
			int status = applySetAttributes( target.getPath(), attributes, true) ;

			// 属性反映が失敗した場合
			if( status != NfsStatus.OK) {
				logMutation( "CREATE", target.getPath(), status, "setattr" ) ;
				response.writeInt( status) ;
				return ;
			}
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "CREATE", target.getPath(), status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			return ;
		}

		logMutation( "CREATE", target.getPath(), NfsStatus.OK, "" ) ;
		FileHandle handle = handleTable.getOrCreate( target.getPath()) ;
		writeDiropResult( response, target.getPath(), handle) ;
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
			logMutation( "REMOVE", target.getPath(), target.getStatus(), "invalid-target" ) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( target.getPath())) {
			logMutation( "REMOVE", target.getPath(), NfsStatus.ACCES, "client-denied" ) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			logMutation( "REMOVE", target.getPath(), NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 対象ファイルが存在しない場合
		if( !Files.exists( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "REMOVE", target.getPath(), NfsStatus.NOENT, "missing" ) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		boolean directoryTarget = Files.isDirectory( target.getPath(), LinkOption.NOFOLLOW_LINKS) ;

		try {
			closeCachedPath( target.getPath()) ;
			Files.delete( target.getPath()) ;
			forgetHardLink( target.getPath()) ;
			handleTable.forget( target.getPath()) ;
			logMutation( "REMOVE", target.getPath(), NfsStatus.OK, directoryTarget ? "directory-compatible" : "" ) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( IOException ioex) {
			// QNX 4.xはディレクトリ削除時に .nfsX* へリネームしてからREMOVEする場合がある
			if( directoryTarget && ioex instanceof DirectoryNotEmptyException && isQnxSillyRenamePath( target.getPath())) {
				try {
					writeFileCache.closeTree( target.getPath()) ;
					deleteDirectoryTree( target.getPath()) ;
					handleTable.forgetTree( target.getPath()) ;
					logMutation( "REMOVE", target.getPath(), NfsStatus.OK, "qnx-silly-rename-recursive" ) ;
					response.writeInt( NfsStatus.OK) ;
				} catch( IOException deleteEx) {
					int deleteStatus = mapIoStatus( deleteEx) ;
					logMutation( "REMOVE", target.getPath(), deleteStatus, "qnx-silly-rename-recursive " + deleteEx.getClass().getSimpleName()) ;
					response.writeInt( deleteStatus) ;
				}

				return ;
			}

			int status = mapIoStatus( ioex) ;
			logMutation( "REMOVE", target.getPath(), status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
		}
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
			logMutation( "RENAME", source.getPath(), source.getStatus(), "invalid-source target=" + target.getPath()) ;
			response.writeInt( source.getStatus()) ;
			return ;
		}

		// 移動先が不正な場合
		if( !target.isOk()) {
			logMutation( "RENAME", source.getPath(), target.getStatus(), "invalid-target target=" + target.getPath()) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( source.getPath()) || !isClientAllowed( target.getPath())) {
			logMutation( "RENAME", source.getPath(), NfsStatus.ACCES, "client-denied target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( source.getPath()) || !isWritable( target.getPath())) {
			logMutation( "RENAME", source.getPath(), NfsStatus.ROFS, "read-only target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 公開ルートを跨ぐ場合
		if( !handleTable.isSameExport( source.getPath(), target.getPath())) {
			logMutation( "RENAME", source.getPath(), NfsStatus.ACCES, "cross-export target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 移動元が存在しない場合
		if( !Files.exists( source.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "RENAME", source.getPath(), NfsStatus.NOENT, "missing target=" + target.getPath()) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		try {
			closeCachedPath( source.getPath()) ;
			closeCachedPath( target.getPath()) ;
			Files.move( source.getPath(), target.getPath(), StandardCopyOption.REPLACE_EXISTING) ;
			moveHardLink( source.getPath(), target.getPath()) ;
			handleTable.move( source.getPath(), target.getPath()) ;
			logMutation( "RENAME", source.getPath(), NfsStatus.OK, "target=" + target.getPath()) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "RENAME", source.getPath(), status, ioex.getClass().getSimpleName() + " target=" + target.getPath()) ;
			response.writeInt( status) ;
		}
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
			logMutation( "LINK", source, NfsStatus.NOENT, "unknown-source" ) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// 移動先が不正な場合
		if( !target.isOk()) {
			logMutation( "LINK", source, target.getStatus(), "invalid-target target=" + target.getPath()) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( source) || !isClientAllowed( target.getPath())) {
			logMutation( "LINK", source, NfsStatus.ACCES, "client-denied target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 公開ルートを跨ぐ場合
		if( !handleTable.isSameExport( source, target.getPath())) {
			logMutation( "LINK", source, NfsStatus.ACCES, "cross-export target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			logMutation( "LINK", target.getPath(), NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 移動元が存在しない場合
		if( !Files.exists( source, LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "LINK", source, NfsStatus.NOENT, "missing" ) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// ディレクトリへのハードリンクの場合
		if( Files.isDirectory( source, LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "LINK", source, NfsStatus.ISDIR, "directory" ) ;
			response.writeInt( NfsStatus.ISDIR) ;
			return ;
		}

		// 移動先が存在済みの場合
		if( Files.exists( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "LINK", target.getPath(), NfsStatus.EXIST, "exists" ) ;
			response.writeInt( NfsStatus.EXIST) ;
			return ;
		}

		try {
			Files.createLink( target.getPath(), source) ;
			trackHardLink( source, target.getPath()) ;
			handleTable.getOrCreate( target.getPath()) ;
			logMutation( "LINK", source, NfsStatus.OK, "target=" + target.getPath()) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( SecurityException sex) {
			logMutation( "LINK", source, NfsStatus.ACCES, sex.getClass().getSimpleName() + " target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ACCES) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "LINK", source, status, ioex.getClass().getSimpleName() + " target=" + target.getPath()) ;
			response.writeInt( status) ;
		}
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
		String linkTarget = readPath( arguments) ;
		SetAttributes attributes = readSetAttributes( arguments) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			logMutation( "SYMLINK", target.getPath(), target.getStatus(), "invalid-target" ) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( target.getPath())) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.ACCES, "client-denied" ) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// リンク先パスが長すぎる場合
		if( linkTarget.getBytes( filenameCharset).length > MAX_PATH_BYTES) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.NAMETOOLONG, "target-too-long" ) ;
			response.writeInt( NfsStatus.NAMETOOLONG) ;
			return ;
		}

		// 対象が存在済みの場合
		if( Files.exists( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.EXIST, "exists" ) ;
			response.writeInt( NfsStatus.EXIST) ;
			return ;
		}

		try {
			Files.createSymbolicLink( target.getPath(), Path.of( linkTarget)) ;
			int status = applySetAttributes( target.getPath(), attributes, false) ;

			// 属性反映が失敗した場合
			if( status != NfsStatus.OK) {
				Files.deleteIfExists( target.getPath()) ;
				logMutation( "SYMLINK", target.getPath(), status, "setattr" ) ;
				response.writeInt( status) ;
				return ;
			}

			handleTable.getOrCreate( target.getPath()) ;
			logMutation( "SYMLINK", target.getPath(), NfsStatus.OK, "target=" + linkTarget) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( UnsupportedOperationException uoex) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.PERM, uoex.getClass().getSimpleName()) ;
			response.writeInt( NfsStatus.PERM) ;
		} catch( InvalidPathException ipex) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.ACCES, ipex.getClass().getSimpleName()) ;
			response.writeInt( NfsStatus.ACCES) ;
		} catch( SecurityException sex) {
			logMutation( "SYMLINK", target.getPath(), NfsStatus.ACCES, sex.getClass().getSimpleName()) ;
			response.writeInt( NfsStatus.ACCES) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "SYMLINK", target.getPath(), status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
		}
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
			logMutation( "MKDIR", target.getPath(), target.getStatus(), "invalid-target" ) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( target.getPath())) {
			logMutation( "MKDIR", target.getPath(), NfsStatus.ACCES, "client-denied" ) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			logMutation( "MKDIR", target.getPath(), NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		try {
			Files.createDirectory( target.getPath()) ;
			int status = applySetAttributes( target.getPath(), attributes, false) ;

			// 属性反映が失敗した場合
			if( status != NfsStatus.OK) {
				logMutation( "MKDIR", target.getPath(), status, "setattr" ) ;
				response.writeInt( status) ;
				return ;
			}
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "MKDIR", target.getPath(), status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			return ;
		}

		logMutation( "MKDIR", target.getPath(), NfsStatus.OK, "" ) ;
		FileHandle handle = handleTable.getOrCreate( target.getPath()) ;
		writeDiropResult( response, target.getPath(), handle) ;
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
			logMutation( "RMDIR", target.getPath(), target.getStatus(), "invalid-target" ) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( target.getPath())) {
			logMutation( "RMDIR", target.getPath(), NfsStatus.ACCES, "client-denied" ) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			logMutation( "RMDIR", target.getPath(), NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 対象がディレクトリではない場合
		if( !Files.isDirectory( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "RMDIR", target.getPath(), NfsStatus.NOTDIR, "not-directory" ) ;
			response.writeInt( NfsStatus.NOTDIR) ;
			return ;
		}

		try {
			Files.delete( target.getPath()) ;
			handleTable.forget( target.getPath()) ;
			logMutation( "RMDIR", target.getPath(), NfsStatus.OK, "" ) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "RMDIR", target.getPath(), status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIRを処理します。<br><br>
	 *
	 * <p>メソッド名称： READDIR処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleReadDir(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		int cookie = arguments.readInt() ;
		int count = arguments.readInt() ;
		Path directory = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( directory == null) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( directory)) {
			logAccessDenied( "READDIR", directory, "client-denied cookie=" + cookie) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// ディレクトリではない場合
		if( !Files.isDirectory( directory, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOTDIR) ;
			return ;
		}

		List<DirectoryEntry> entries = listDirectory( directory) ;
		int index = findReadDirStartIndex( entries, cookie) ;
		int writtenBytes = 0 ;
		response.writeInt( NfsStatus.OK) ;

		// ディレクトリエントリを応答サイズ範囲で書き込む
		for( int i = index; i < entries.size(); i++) {
			DirectoryEntry entry = entries.get( i) ;
			String name = entry.getName() ;
			int estimatedBytes = 4 + 4 + 4 + name.getBytes( filenameCharset).length + 4 ;

			// 指定サイズを超過する場合
			if( writtenBytes > 0 && writtenBytes + estimatedBytes > count) {
				response.writeBoolean( false) ;
				response.writeBoolean( false) ;
				return ;
			}

			FileHandle entryHandle = handleTable.getOrCreate( entry.getPath()) ;
			response.writeBoolean( true) ;
			response.writeUnsignedInt( Integer.toUnsignedLong( handleTable.getFileId( entryHandle))) ;
			response.writeString( name, filenameCharset) ;
			response.writeUnsignedInt( Integer.toUnsignedLong( entry.getCookie())) ;
			writtenBytes += estimatedBytes ;
		}

			response.writeBoolean( false) ;
			response.writeBoolean( true) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIR開始位置を取得します。<br><br>
	 *
	 * <p>メソッド名称： READDIR開始位置取得</p>
	 *
	 * @param entries	エントリ一覧
	 * @param cookie	cookie
	 * @return 開始位置
	 */
	//--------------------------------------------------------------------------
	private int findReadDirStartIndex(List<DirectoryEntry> entries, int cookie) {
		// 先頭cookieの場合
		if( cookie == 0) {
			return 0 ;
		}

		// 前回cookieの次位置を検索する
		for( int i = 0; i < entries.size(); i++) {
			// cookieが一致する場合
			if( entries.get( i).getCookie() == cookie) {
				return i + 1 ;
			}
		}

		return entries.size() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイル名を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイル名読込</p>
	 *
	 * @param arguments	引数
	 * @return ファイル名
	 */
	//--------------------------------------------------------------------------
	private String readName(XdrReader arguments) {
		return arguments.readString( filenameCharset) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パス文字列を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： パス文字列読込</p>
	 *
	 * @param arguments	引数
	 * @return パス文字列
	 */
	//--------------------------------------------------------------------------
	private String readPath(XdrReader arguments) {
		return arguments.readString( filenameCharset) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * LOOKUP対象パスを解決します。<br><br>
	 *
	 * <p>メソッド名称： LOOKUP対象パス解決</p>
	 *
	 * @param directory	ディレクトリ
	 * @param name		名前
	 * @return 対象パス
	 */
	//--------------------------------------------------------------------------
	private Path resolveLookupPath(Path directory, String name) {
		// カレントディレクトリの場合
		if( ".".equals( name)) {
			return directory ;
		}

		// 親ディレクトリの場合
		if( "..".equals( name)) {
			Path parent = directory.getParent() ;

			// 親が存在しない、または公開ルート外の場合
			Path rootPath = handleTable.getRootPath( directory) ;

			// 親が存在しない、または公開ルート外の場合
			if( parent == null || !parent.toAbsolutePath().normalize().startsWith( rootPath)) {
				return rootPath ;
			}

			return parent.toAbsolutePath().normalize() ;
		}

		return directory.resolve( name).normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * LOOKUP名の妥当性を確認します。<br><br>
	 *
	 * <p>メソッド名称： LOOKUP名妥当性確認</p>
	 *
	 * @param name	名前
	 * @return true:正常 false:不正
	 */
	//--------------------------------------------------------------------------
	private boolean isValidLookupName(String name) {
		// カレントディレクトリの場合
		if( ".".equals( name)) {
			return true ;
		}

		// 親ディレクトリの場合
		if( "..".equals( name)) {
			return true ;
		}

		// 空文字の場合
		if( name == null || name.isEmpty()) {
			return false ;
		}

		// NFSv2ファイル名長を超過する場合
		if( name.getBytes( filenameCharset).length > MAX_NAME_BYTES) {
			return false ;
		}

		// パス区切りを含む場合
		if( name.indexOf( '/') != -1 || name.indexOf( '\\') != -1) {
			return false ;
		}

		// Windowsファイル名として不正な場合
		if( hasInvalidWindowsNameCharacter( name)) {
			return false ;
		}

		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Windowsファイル名として不正な文字を含むか確認します。<br><br>
	 *
	 * <p>メソッド名称： Windowsファイル名不正文字確認</p>
	 *
	 * @param name	名前
	 * @return true:不正文字あり false:不正文字なし
	 */
	//--------------------------------------------------------------------------
	private boolean hasInvalidWindowsNameCharacter(String name) {
		// ファイル名を1文字ずつ検証する
		for( int i = 0; i < name.length(); i++) {
			char value = name.charAt( i) ;

			// 制御文字またはWindows予約文字の場合
			if( value < 0x20 || value == '<' || value == '>' || value == ':' || value == '"' || value == '|' || value == '?' || value == '*') {
				return true ;
			}
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開ルート内かを確認します。<br><br>
	 *
	 * <p>メソッド名称： 公開ルート内確認</p>
	 *
	 * @param path	パス
	 * @return true:公開ルート内 false:公開ルート外
	 */
	//--------------------------------------------------------------------------
	private boolean isInExportRoot(Path path) {
		return handleTable.isInExportRoot( path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 同一公開ルート内かを確認します。<br><br>
	 *
	 * <p>メソッド名称： 同一公開ルート内確認</p>
	 *
	 * @param base		基準パス
	 * @param target	対象パス
	 * @return true:同一公開ルート内 false:公開ルート外
	 */
	//--------------------------------------------------------------------------
	private boolean isInSameExportRoot(Path base, Path target) {
		return handleTable.isSameExport( base, target) && isInExportRoot( target) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 書込可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 書込可否取得</p>
	 *
	 * @param path	パス
	 * @return true:書込可 false:読込専用
	 */
	//--------------------------------------------------------------------------
	private boolean isWritable(Path path) {
		return handleTable.isWritable( path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアント許可可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： クライアント許可可否取得</p>
	 *
	 * @param path	対象パス
	 * @return true:許可 false:拒否
	 */
	//--------------------------------------------------------------------------
	private boolean isClientAllowed(Path path) {
		NfsExport export = handleTable.getExport( path) ;

		// 公開定義が取得できない場合
		if( export == null) {
			return false ;
		}

		return export.allowsClient( RpcRequestContext.current().getClientAddress()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリ操作対象を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリ操作対象読込</p>
	 *
	 * @param arguments	引数
	 * @return 操作対象
	 */
	//--------------------------------------------------------------------------
	private ResolvedPath readOperationPath(XdrReader arguments) {
		FileHandle directoryHandle = readHandle( arguments) ;
		String name = readName( arguments) ;
		Path directory = handleTable.getPath( directoryHandle) ;

		// ディレクトリハンドルが不明な場合
		if( directory == null) {
			return ResolvedPath.status( NfsStatus.NOENT) ;
		}

		// ディレクトリではない場合
		if( !Files.isDirectory( directory, LinkOption.NOFOLLOW_LINKS)) {
			return ResolvedPath.status( NfsStatus.NOTDIR) ;
		}

		// 名前が不正な場合
		if( !isValidLookupName( name)) {
			return ResolvedPath.status( NfsStatus.ACCES) ;
		}

		Path target = null ;

		try {
			target = resolveLookupPath( directory, name) ;
		} catch( InvalidPathException ipex) {
			return ResolvedPath.status( NfsStatus.ACCES) ;
		}

		// 公開ルート外の場合
		if( !isInSameExportRoot( directory, target)) {
			return ResolvedPath.status( NfsStatus.ACCES) ;
		}

		return ResolvedPath.path( target) ;
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
		int mode = arguments.readInt() ;
		int uid = arguments.readInt() ;
		int gid = arguments.readInt() ;
		int size = arguments.readInt() ;
		int atimeSeconds = arguments.readInt() ;
		int atimeMicros = arguments.readInt() ;
		int mtimeSeconds = arguments.readInt() ;
		int mtimeMicros = arguments.readInt() ;
		return new SetAttributes(
				mode,
				uid,
				gid,
				size,
				toFileTime( atimeSeconds, atimeMicros),
				toFileTime( mtimeSeconds, mtimeMicros)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * SETATTR属性を反映します。<br><br>
	 *
	 * <p>メソッド名称： SETATTR属性反映</p>
	 *
	 * @param path			対象パス
	 * @param attributes	属性
	 * @param allowSize		サイズ変更可否
	 * @return NFSステータス
	 * @throws IOException 反映異常
	 */
	//--------------------------------------------------------------------------
	private int applySetAttributes(Path path, SetAttributes attributes, boolean allowSize) throws IOException {
		// サイズ指定がある場合
		if( attributes.hasSize()) {
			// サイズ変更不可の場合
			if( !allowSize || !Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS)) {
				return NfsStatus.INVAL ;
			}

			try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "rw")) {
				file.setLength( attributes.getSize()) ;
				file.getFD().sync() ;
			}
		}

		// モード指定がある場合
		if( attributes.hasMode()) {
			applyMode( path, attributes.getMode()) ;
		}

		BasicFileAttributeView view = Files.getFileAttributeView( path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

		// 時刻属性が設定可能な場合
		if( view != null && (attributes.getAccessTime() != null || attributes.getModifiedTime() != null)) {
			view.setTimes( attributes.getModifiedTime(), attributes.getAccessTime(), null) ;
		}

		return NfsStatus.OK ;
	}

	//--------------------------------------------------------------------------
	/**
	 * モードを反映します。<br><br>
	 *
	 * <p>メソッド名称： モード反映</p>
	 *
	 * @param path	対象パス
	 * @param mode	モード
	 * @throws IOException 反映異常
	 */
	//--------------------------------------------------------------------------
	private void applyMode(Path path, int mode) throws IOException {
		boolean readOnly = (mode & MODE_WRITE_BITS) == 0 ;
		DosFileAttributeView dosView = Files.getFileAttributeView( path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

		// DOS属性が利用可能な場合
		if( dosView != null) {
			dosView.setReadOnly( readOnly) ;
			return ;
		}

		// Java標準の書込属性だけが利用可能な場合
		if( !path.toFile().setWritable( !readOnly, false)) {
			throw new AccessDeniedException( path.toString()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * NFS時刻を変換します。<br><br>
	 *
	 * <p>メソッド名称： NFS時刻変換</p>
	 *
	 * @param seconds	秒
	 * @param micros	マイクロ秒
	 * @return ファイル時刻
	 */
	//--------------------------------------------------------------------------
	private FileTime toFileTime(int seconds, int micros) {
		// 未指定の場合
		if( seconds == -1 || micros == -1) {
			return null ;
		}

		long millis = Integer.toUnsignedLong( seconds) * 1000L + Integer.toUnsignedLong( micros) / 1000L ;
		return FileTime.fromMillis( millis) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * IO異常をNFSステータスへ変換します。<br><br>
	 *
	 * <p>メソッド名称： IO異常ステータス変換</p>
	 *
	 * @param ioex	IO異常
	 * @return NFSステータス
	 */
	//--------------------------------------------------------------------------
	private int mapIoStatus(IOException ioex) {
		// 対象が存在しない場合
		if( ioex instanceof NoSuchFileException) {
			return NfsStatus.NOENT ;
		}

		// 対象が存在済みの場合
		if( ioex instanceof FileAlreadyExistsException) {
			return NfsStatus.EXIST ;
		}

		// ディレクトリが空ではない場合
		if( ioex instanceof DirectoryNotEmptyException) {
			return NfsStatus.NOTEMPTY ;
		}

		// アクセス拒否の場合
		if( ioex instanceof AccessDeniedException) {
			return NfsStatus.ACCES ;
		}

		return NfsStatus.IO ;
	}

	//--------------------------------------------------------------------------
	/**
	 * QNXの削除用一時リネームパスかを確認します。<br><br>
	 *
	 * <p>メソッド名称： QNX一時リネームパス確認</p>
	 *
	 * @param path	パス
	 * @return true:QNX一時リネームパス false:通常パス
	 */
	//--------------------------------------------------------------------------
	private boolean isQnxSillyRenamePath(Path path) {
		Path fileName = path.getFileName() ;

		// ファイル名が取得できない場合
		if( fileName == null) {
			return false ;
		}

		String name = fileName.toString() ;
		return name.length() > 5 && name.startsWith( ".nfsX") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリツリーを削除します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリツリー削除</p>
	 *
	 * @param path	対象パス
	 * @throws IOException 削除異常
	 */
	//--------------------------------------------------------------------------
	private void deleteDirectoryTree(Path path) throws IOException {
		Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				deleteFileSystemEntry( file) ;
				return FileVisitResult.CONTINUE ;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
				// 子要素の処理で異常が発生した場合
				if( exception != null) {
					throw exception ;
				}

				deleteFileSystemEntry( directory) ;
				return FileVisitResult.CONTINUE ;
			}
		}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルシステム要素を削除します。<br><br>
	 *
	 * <p>メソッド名称： ファイルシステム要素削除</p>
	 *
	 * @param path	対象パス
	 * @throws IOException 削除異常
	 */
	//--------------------------------------------------------------------------
	private void deleteFileSystemEntry(Path path) throws IOException {
		try {
			prepareDelete( path) ;
			Files.delete( path) ;
		} catch( AccessDeniedException adex) {
			prepareDelete( path) ;
			Files.delete( path) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 対象パスの書込キャッシュを閉じます。<br><br>
	 *
	 * <p>メソッド名称： 書込キャッシュクローズ</p>
	 *
	 * @param path	対象パス
	 * @throws IOException クローズ異常
	 */
	//--------------------------------------------------------------------------
	private void closeCachedPath(Path path) throws IOException {
		// ディレクトリの場合
		if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS)) {
			writeFileCache.closeTree( path) ;
			return ;
		}

		writeFileCache.close( path) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 削除前にWindows属性を解除します。<br><br>
	 *
	 * <p>メソッド名称： 削除前属性解除</p>
	 *
	 * @param path	対象パス
	 * @throws IOException 属性更新異常
	 */
	//--------------------------------------------------------------------------
	private void prepareDelete(Path path) throws IOException {
		DosFileAttributeView view = Files.getFileAttributeView( path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

		// DOS属性を扱えないファイルシステムの場合
		if( view == null) {
			return ;
		}

		view.setReadOnly( false) ;
		view.setHidden( false) ;
		view.setSystem( false) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 変更系操作をログ出力します。<br><br>
	 *
	 * <p>メソッド名称： 変更系操作ログ出力</p>
	 *
	 * @param operation	操作名
	 * @param path		対象パス
	 * @param status	NFSステータス
	 * @param detail	詳細
	 */
	//--------------------------------------------------------------------------
	private void logMutation(String operation, Path path, int status, String detail) {
		// 高頻度の成功ログを抑制する場合
		if( !ServerLog.shouldLogOperation( operation, status)) {
			return ;
		}

		RpcRequestContext context = RpcRequestContext.current() ;
		StringBuilder message = new StringBuilder() ;
		message.append( "NFS " ).append( operation )
				.append( " client=" ).append( context.getClientAddress() )
				.append( " xid=" ).append( context.formatXid() )
				.append( " program=" ).append( context.getProgram() )
				.append( " version=" ).append( context.getVersion() )
				.append( " procedure=" ).append( context.getProcedure() )
				.append( " status=" ).append( status) ;

		// パスが存在する場合
		if( path != null) {
			message.append( " path=").append( path) ;
		}

		// 詳細が存在する場合
		if( detail != null && !detail.isBlank()) {
			message.append( " " ).append( detail) ;
		}

		ServerLog.info( message.toString()) ;
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
	 * STATFSを処理します。<br><br>
	 *
	 * <p>メソッド名称： STATFS処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void handleStatFs(XdrReader arguments, XdrWriter response) throws IOException {
		FileHandle handle = readHandle( arguments) ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !isClientAllowed( path)) {
			logAccessDenied( "STATFS", path, "client-denied") ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		FileStore store = Files.getFileStore( path) ;
		long blockSize = readBlockSize( store) ;
		long total = store.getTotalSpace() / blockSize ;
		long free = store.getUnallocatedSpace() / blockSize ;
		long usable = store.getUsableSpace() / blockSize ;
		response.writeInt( NfsStatus.OK) ;
		response.writeInt( config.getReadSize()) ;
		response.writeUnsignedInt( clampUnsignedInt( blockSize)) ;
		response.writeUnsignedInt( clampUnsignedInt( total)) ;
		response.writeUnsignedInt( clampUnsignedInt( free)) ;
		response.writeUnsignedInt( clampUnsignedInt( usable)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルシステムブロックサイズを取得します。<br><br>
	 *
	 * <p>メソッド名称： ファイルシステムブロックサイズ取得</p>
	 *
	 * @param store	ファイルストア
	 * @return ブロックサイズ
	 */
	//--------------------------------------------------------------------------
	private long readBlockSize(FileStore store) {
		try {
			long value = store.getBlockSize() ;

			// 正の値の場合
			if( value > 0) {
				return value ;
			}
		} catch( IOException | UnsupportedOperationException ex) {
			// 取得できない環境では設定値を利用する
		}

		return config.getBlockSize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリを一覧します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリ一覧</p>
	 *
	 * @param directory	ディレクトリ
	 * @return ディレクトリエントリ
	 * @throws IOException 読込異常
	 */
	//--------------------------------------------------------------------------
	private List<DirectoryEntry> listDirectory(Path directory) throws IOException {
		List<DirectoryEntry> entries = new ArrayList<DirectoryEntry>() ;
		entries.add( new DirectoryEntry( ".", directory, 1)) ;
		entries.add( new DirectoryEntry( "..", resolveLookupPath( directory, ".."), 2)) ;

		try( var stream = Files.list( directory)) {
			stream.sorted( Comparator.comparing( path -> path.getFileName().toString().toLowerCase( Locale.ROOT)))
					.forEach( path -> entries.add( new DirectoryEntry( path.getFileName().toString(), path, createReadDirCookie( path.getFileName().toString(), entries))) ) ;
		}

		return entries ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIR cookieを作成します。<br><br>
	 *
	 * <p>メソッド名称： READDIR cookie作成</p>
	 *
	 * @param name		名前
	 * @param entries	作成済みエントリ
	 * @return cookie
	 */
	//--------------------------------------------------------------------------
	private int createReadDirCookie(String name, List<DirectoryEntry> entries) {
		int cookie = READDIR_COOKIE_BASE + (name.hashCode() & 0x3fffffff) ;

		// cookie衝突を回避する
		while( containsReadDirCookie( entries, cookie)) {
			cookie++ ;
		}

		return cookie ;
	}

	//--------------------------------------------------------------------------
	/**
	 * READDIR cookieが存在するか確認します。<br><br>
	 *
	 * <p>メソッド名称： READDIR cookie存在確認</p>
	 *
	 * @param entries	エントリ
	 * @param cookie	cookie
	 * @return true:存在あり false:存在なし
	 */
	//--------------------------------------------------------------------------
	private boolean containsReadDirCookie(List<DirectoryEntry> entries, int cookie) {
		// エントリを検索する
		for( DirectoryEntry entry : entries) {
			// cookieが一致する場合
			if( entry.getCookie() == cookie) {
				return true ;
			}
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルハンドルを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： ファイルハンドル読込</p>
	 *
	 * @param reader	XDR読込
	 * @return ファイルハンドル
	 */
	//--------------------------------------------------------------------------
	private FileHandle readHandle(XdrReader reader) {
		return new FileHandle( reader.readFixedOpaque( FileHandle.LENGTH)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ステータスと属性を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ステータス属性書込</p>
	 *
	 * @param response	応答
	 * @param path		パス
	 * @param handle	ファイルハンドル
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writeStatusAttr(XdrWriter response, Path path, FileHandle handle) throws IOException {
		// パスが存在しない場合
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		response.writeInt( NfsStatus.OK) ;
		writeAttributes( response, path, handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリ操作結果を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリ操作結果書込</p>
	 *
	 * @param response	応答
	 * @param path		パス
	 * @param handle	ファイルハンドル
	 * @throws IOException 属性取得異常
	 */
	//--------------------------------------------------------------------------
	private void writeDiropResult(XdrWriter response, Path path, FileHandle handle) throws IOException {
		// パスが存在しない場合
		if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		response.writeInt( NfsStatus.OK) ;
		response.writeFixedOpaque( handle.getValue()) ;
		writeAttributes( response, path, handle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * NFSv2属性を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： NFSv2属性書込</p>
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
		int type = NFREG ;
		int mode = 0100000 | config.getFileMode() ;

		// ディレクトリの場合
		if( directory) {
			type = NFDIR ;
			mode = 0040000 | config.getDirectoryMode() ;
		}
		// シンボリックリンクの場合
		else if( symbolicLink) {
			type = NFLNK ;
			mode = 0120000 | config.getFileMode() ;
		}

		// 読込専用属性が設定されている場合
		if( isReadOnlyPath( path)) {
			mode &= ~MODE_WRITE_BITS ;
		}

		long size = attributes.size() ;
		long blocks = size == 0 ? 0 : (size + config.getBlockSize() - 1L) / config.getBlockSize() ;

		response.writeInt( type) ;
		response.writeUnsignedInt( mode) ;
		response.writeUnsignedInt( readLinkCount( path, directory)) ;
		response.writeUnsignedInt( resolveAttributeUid()) ;
		response.writeUnsignedInt( resolveAttributeGid()) ;
		response.writeUnsignedInt( clampUnsignedInt( size)) ;
		response.writeUnsignedInt( config.getBlockSize()) ;
		response.writeUnsignedInt( 0) ;
		response.writeUnsignedInt( clampUnsignedInt( blocks)) ;
		response.writeUnsignedInt( Integer.toUnsignedLong( handleTable.getRootPath( path).toString().hashCode())) ;
		response.writeUnsignedInt( Integer.toUnsignedLong( handleTable.getFileId( handle))) ;
		writeTime( response, attributes.lastAccessTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 応答属性UIDを解決します。<br><br>
	 *
	 * <p>メソッド名称： 応答属性UID解決</p>
	 *
	 * @return UID
	 */
	//--------------------------------------------------------------------------
	private int resolveAttributeUid() {
		// クライアント資格情報を反映しない場合
		if( !config.isClientIdentityEnabled()) {
			return config.getUid() ;
		}

		return RpcRequestContext.current().resolveUid( config.getUid()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 応答属性GIDを解決します。<br><br>
	 *
	 * <p>メソッド名称： 応答属性GID解決</p>
	 *
	 * @return GID
	 */
	//--------------------------------------------------------------------------
	private int resolveAttributeGid() {
		// クライアント資格情報を反映しない場合
		if( !config.isClientIdentityEnabled()) {
			return config.getGid() ;
		}

		return RpcRequestContext.current().resolveGid( config.getGid()) ;
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
	 * @throws IOException 属性取得異常
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

		return Math.max( 1L, getTrackedHardLinkCount( path)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * hard linkを追跡します。<br><br>
	 *
	 * <p>メソッド名称： hard link追跡</p>
	 *
	 * @param source	リンク元
	 * @param target	リンク先
	 */
	//--------------------------------------------------------------------------
	private synchronized void trackHardLink(Path source, Path target) {
		Path sourceKey = hardLinkKey( source) ;
		Path targetKey = hardLinkKey( target) ;
		HardLinkGroup group = hardLinkGroups.get( sourceKey) ;

		// 既存グループが存在しない場合
		if( group == null) {
			group = new HardLinkGroup() ;
			group.add( sourceKey) ;
			hardLinkGroups.put( sourceKey, group) ;
		}

		group.add( targetKey) ;
		hardLinkGroups.put( targetKey, group) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * hard link追跡を忘却します。<br><br>
	 *
	 * <p>メソッド名称： hard link追跡忘却</p>
	 *
	 * @param path	対象パス
	 */
	//--------------------------------------------------------------------------
	private synchronized void forgetHardLink(Path path) {
		Path key = hardLinkKey( path) ;
		HardLinkGroup group = hardLinkGroups.remove( key) ;

		// グループが存在しない場合
		if( group == null) {
			return ;
		}

		group.remove( key) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * hard link追跡パスを移動します。<br><br>
	 *
	 * <p>メソッド名称： hard link追跡パス移動</p>
	 *
	 * @param source	移動元
	 * @param target	移動先
	 */
	//--------------------------------------------------------------------------
	private synchronized void moveHardLink(Path source, Path target) {
		Path sourceKey = hardLinkKey( source) ;
		Path targetKey = hardLinkKey( target) ;
		HardLinkGroup group = hardLinkGroups.remove( sourceKey) ;

		// 上書き先の既存追跡を削除する
		forgetHardLink( target) ;

		// グループが存在しない場合
		if( group == null) {
			return ;
		}

		group.remove( sourceKey) ;
		group.add( targetKey) ;
		hardLinkGroups.put( targetKey, group) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 追跡中のhard link数を取得します。<br><br>
	 *
	 * <p>メソッド名称： 追跡hard link数取得</p>
	 *
	 * @param path	対象パス
	 * @return hard link数
	 */
	//--------------------------------------------------------------------------
	private synchronized long getTrackedHardLinkCount(Path path) {
		HardLinkGroup group = hardLinkGroups.get( hardLinkKey( path)) ;

		// グループが存在しない場合
		if( group == null) {
			return 1L ;
		}

		return group.size() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * hard link追跡キーを取得します。<br><br>
	 *
	 * <p>メソッド名称： hard link追跡キー取得</p>
	 *
	 * @param path	対象パス
	 * @return 追跡キー
	 */
	//--------------------------------------------------------------------------
	private Path hardLinkKey(Path path) {
		return path.toAbsolutePath().normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 符号なし32bit範囲に丸めます。<br><br>
	 *
	 * <p>メソッド名称： 符号なし32bit範囲丸め</p>
	 *
	 * @param value	値
	 * @return 丸め後の値
	 */
	//--------------------------------------------------------------------------
	private long clampUnsignedInt(long value) {
		return Math.max( 0L, Math.min( value, 0xffffffffL)) ;
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
		long totalMicroseconds = time.to( TimeUnit.MICROSECONDS) ;
		long seconds = totalMicroseconds / 1000000L ;
		long microseconds = totalMicroseconds % 1000000L ;
		response.writeUnsignedInt( seconds) ;
		response.writeUnsignedInt( microseconds) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * SETATTR属性クラスです。<br><br>
	 *
	 * <p>クラス名称： SETATTR属性</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//--------------------------------------------------------------------------
	private static class SetAttributes {
		//	内部定義	--------------------------------------------------------
		/** モード */
		private final int				mode ;

		/** UID */
		private final int				uid ;

		/** GID */
		private final int				gid ;

		/** サイズ */
		private final int				size ;

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
		SetAttributes(int mode, int uid, int gid, int size, FileTime accessTime, FileTime modifiedTime) {
			this.mode = mode ;
			this.uid = uid ;
			this.gid = gid ;
			this.size = size ;
			this.accessTime = accessTime ;
			this.modifiedTime = modifiedTime ;
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
			return size != -1 ;
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
			return mode != -1 ;
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
			return mode ;
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
			return Integer.toUnsignedLong( size) ;
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
			if( hasMode()) {
				values.add( "mode=" + Integer.toOctalString( mode)) ;
			}

			// UID指定がある場合
			if( uid != -1) {
				values.add( "uid=" + Integer.toUnsignedLong( uid)) ;
			}

			// GID指定がある場合
			if( gid != -1) {
				values.add( "gid=" + Integer.toUnsignedLong( gid)) ;
			}

			// サイズ指定がある場合
			if( hasSize()) {
				values.add( "size=" + getSize()) ;
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

	//--------------------------------------------------------------------------
	/**
	 * 解決済みパスクラスです。<br><br>
	 *
	 * <p>クラス名称： 解決済みパス</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//--------------------------------------------------------------------------
	private static class ResolvedPath {
		//	内部定義	--------------------------------------------------------
		/** ステータス */
		private final int				status ;

		/** パス */
		private final Path				path ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param status	ステータス
		 * @param path		パス
		 */
		//----------------------------------------------------------------------
		private ResolvedPath(int status, Path path) {
			this.status = status ;
			this.path = path ;
		}

		//----------------------------------------------------------------------
		/**
		 * 正常パスを生成します。<br><br>
		 *
		 * <p>メソッド名称： 正常パス生成</p>
		 *
		 * @param path	パス
		 * @return 解決済みパス
		 */
		//----------------------------------------------------------------------
		static ResolvedPath path(Path path) {
			return new ResolvedPath( NfsStatus.OK, path) ;
		}

		//----------------------------------------------------------------------
		/**
		 * ステータスを生成します。<br><br>
		 *
		 * <p>メソッド名称： ステータス生成</p>
		 *
		 * @param status	ステータス
		 * @return 解決済みパス
		 */
		//----------------------------------------------------------------------
		static ResolvedPath status(int status) {
			return new ResolvedPath( status, null) ;
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

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリエントリクラスです。<br><br>
	 *
	 * <p>クラス名称： ディレクトリエントリ</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//--------------------------------------------------------------------------
	private static class DirectoryEntry {
		//	内部定義	--------------------------------------------------------
		/** 名前 */
		private final String				name ;

		/** パス */
		private final Path				path ;

		/** cookie */
		private final int				cookie ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param name		名前
		 * @param path		パス
		 * @param cookie	cookie
		 */
		//----------------------------------------------------------------------
		DirectoryEntry(String name, Path path, int cookie) {
			this.name = name ;
			this.path = path ;
			this.cookie = cookie ;
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
		int getCookie() {
			return cookie ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * hard linkグループクラスです。<br><br>
	 *
	 * <p>クラス名称： hard linkグループ</p>
	 */
	//------------------------------------------------------------------------------
	private static class HardLinkGroup {
		/** パス */
		private final Set<Path>			paths = new HashSet<Path>() ;

		//----------------------------------------------------------------------
		/**
		 * パスを追加します。<br><br>
		 *
		 * <p>メソッド名称： パス追加</p>
		 *
		 * @param path	パス
		 */
		//----------------------------------------------------------------------
		void add(Path path) {
			paths.add( path) ;
		}

		//----------------------------------------------------------------------
		/**
		 * パスを削除します。<br><br>
		 *
		 * <p>メソッド名称： パス削除</p>
		 *
		 * @param path	パス
		 */
		//----------------------------------------------------------------------
		void remove(Path path) {
			paths.remove( path) ;
		}

		//----------------------------------------------------------------------
		/**
		 * パス数を取得します。<br><br>
		 *
		 * <p>メソッド名称： パス数取得</p>
		 *
		 * @return パス数
		 */
		//----------------------------------------------------------------------
		long size() {
			return paths.size() ;
		}
	}
}
