package jp.co.enterogawa.nfs.program;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcProgram;
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
		this.config = config ;
		this.handleTable = handleTable ;
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
		String name = arguments.readString() ;
		Path directory = handleTable.getPath( directoryHandle) ;

		// ディレクトリハンドルが不明な場合
		if( directory == null) {
			response.writeInt( NfsStatus.NOENT) ;
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

		Path child = resolveLookupPath( directory, name) ;

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

		// シンボリックリンクではない場合
		if( !Files.isSymbolicLink( path)) {
			response.writeInt( NfsStatus.INVAL) ;
			return ;
		}

		Path target = Files.readSymbolicLink( path) ;
		response.writeInt( NfsStatus.OK) ;
		response.writeString( target.toString()) ;
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

		// 書込不可の場合
		if( !isWritable( path)) {
			logMutation( "SETATTR", path, NfsStatus.ROFS, "read-only" ) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		try {
			int status = applySetAttributes( path, attributes, true) ;

			// 属性反映が失敗した場合
			if( status != NfsStatus.OK) {
				logMutation( "SETATTR", path, status, "size=" + attributes.getSize()) ;
				response.writeInt( status) ;
				return ;
			}
		} catch( IOException ioex) {
			int status = mapIoStatus( ioex) ;
			logMutation( "SETATTR", path, status, ioex.getClass().getSimpleName()) ;
			response.writeInt( status) ;
			return ;
		}

		logMutation( "SETATTR", path, NfsStatus.OK, "size=" + attributes.getSize()) ;
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
		arguments.readUnsignedInt() ;
		byte[] data = arguments.readOpaque() ;
		Path path = handleTable.getPath( handle) ;

		// ファイルハンドルが不明な場合
		if( path == null) {
			logMutation( "WRITE", path, NfsStatus.NOENT, "unknown-handle offset=" + offset + " bytes=" + data.length) ;
			response.writeInt( NfsStatus.NOENT) ;
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

		try( RandomAccessFile file = new RandomAccessFile( path.toFile(), "rw")) {
			file.seek( offset) ;
			file.write( data) ;
			file.getFD().sync() ;
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

		// 対象がディレクトリの場合
		if( Files.isDirectory( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "REMOVE", target.getPath(), NfsStatus.ISDIR, "directory" ) ;
			response.writeInt( NfsStatus.ISDIR) ;
			return ;
		}

		try {
			Files.delete( target.getPath()) ;
			handleTable.forget( target.getPath()) ;
			logMutation( "REMOVE", target.getPath(), NfsStatus.OK, "" ) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( IOException ioex) {
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

		// 公開ルートを跨ぐ場合
		if( !handleTable.isSameExport( source.getPath(), target.getPath())) {
			logMutation( "RENAME", source.getPath(), NfsStatus.ACCES, "cross-export target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( source.getPath()) || !isWritable( target.getPath())) {
			logMutation( "RENAME", source.getPath(), NfsStatus.ROFS, "read-only target=" + target.getPath()) ;
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 移動先が不正な場合
		if( !target.isOk()) {
			logMutation( "RENAME", source.getPath(), target.getStatus(), "invalid-target target=" + target.getPath()) ;
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// 移動元が存在しない場合
		if( !Files.exists( source.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			logMutation( "RENAME", source.getPath(), NfsStatus.NOENT, "missing target=" + target.getPath()) ;
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		try {
			Files.move( source.getPath(), target.getPath(), StandardCopyOption.REPLACE_EXISTING) ;
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
			response.writeInt( NfsStatus.NOENT) ;
			return ;
		}

		// 移動先が不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// 公開ルートを跨ぐ場合
		if( !handleTable.isSameExport( source, target.getPath())) {
			response.writeInt( NfsStatus.ACCES) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		try {
			Files.createLink( target.getPath(), source) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( IOException ioex) {
			response.writeInt( mapIoStatus( ioex)) ;
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
		String linkTarget = arguments.readString() ;
		readSetAttributes( arguments) ;

		// 対象パスが不正な場合
		if( !target.isOk()) {
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		try {
			Files.createSymbolicLink( target.getPath(), Path.of( linkTarget)) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( UnsupportedOperationException uoex) {
			response.writeInt( NfsStatus.PERM) ;
		} catch( IOException ioex) {
			response.writeInt( mapIoStatus( ioex)) ;
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
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		try {
			Files.createDirectory( target.getPath()) ;
			int status = applySetAttributes( target.getPath(), attributes, false) ;

			// 属性反映が失敗した場合
			if( status != NfsStatus.OK) {
				response.writeInt( status) ;
				return ;
			}
		} catch( IOException ioex) {
			response.writeInt( mapIoStatus( ioex)) ;
			return ;
		}

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
			response.writeInt( target.getStatus()) ;
			return ;
		}

		// 書込不可の場合
		if( !isWritable( target.getPath())) {
			response.writeInt( NfsStatus.ROFS) ;
			return ;
		}

		// 対象がディレクトリではない場合
		if( !Files.isDirectory( target.getPath(), LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOTDIR) ;
			return ;
		}

		try {
			Files.delete( target.getPath()) ;
			handleTable.forget( target.getPath()) ;
			response.writeInt( NfsStatus.OK) ;
		} catch( IOException ioex) {
			response.writeInt( mapIoStatus( ioex)) ;
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

		// ディレクトリではない場合
		if( !Files.isDirectory( directory, LinkOption.NOFOLLOW_LINKS)) {
			response.writeInt( NfsStatus.NOTDIR) ;
			return ;
		}

		List<DirectoryEntry> entries = listDirectory( directory) ;
		int index = Math.max( 0, cookie) ;
		int writtenBytes = 0 ;
		response.writeInt( NfsStatus.OK) ;

		// ディレクトリエントリを応答サイズ範囲で書き込む
		for( int i = index; i < entries.size(); i++) {
			DirectoryEntry entry = entries.get( i) ;
			String name = entry.getName() ;
			int estimatedBytes = 4 + 4 + 4 + name.length() + 4 ;

			// 指定サイズを超過する場合
			if( writtenBytes > 0 && writtenBytes + estimatedBytes > count) {
				response.writeBoolean( false) ;
				response.writeBoolean( false) ;
				return ;
			}

			FileHandle entryHandle = handleTable.getOrCreate( entry.getPath()) ;
			response.writeBoolean( true) ;
			response.writeUnsignedInt( Integer.toUnsignedLong( handleTable.getFileId( entryHandle))) ;
			response.writeString( name) ;
			response.writeUnsignedInt( i + 1L) ;
			writtenBytes += estimatedBytes ;
		}

		response.writeBoolean( false) ;
		response.writeBoolean( true) ;
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

		// パス区切りを含む場合
		if( name.indexOf( '/') != -1 || name.indexOf( '\\') != -1) {
			return false ;
		}

		return true ;
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
		String name = arguments.readString() ;
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

		Path target = resolveLookupPath( directory, name) ;

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

		BasicFileAttributeView view = Files.getFileAttributeView( path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) ;

		// 時刻属性が設定可能な場合
		if( view != null && (attributes.getAccessTime() != null || attributes.getModifiedTime() != null)) {
			view.setTimes( attributes.getModifiedTime(), attributes.getAccessTime(), null) ;
		}

		return NfsStatus.OK ;
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
		StringBuilder message = new StringBuilder() ;
		message.append( "NFS ").append( operation).append( " status=").append( status) ;

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

		long total = Files.getFileStore( path).getTotalSpace() / config.getBlockSize() ;
		long free = Files.getFileStore( path).getUsableSpace() / config.getBlockSize() ;
		response.writeInt( NfsStatus.OK) ;
		response.writeInt( config.getReadSize()) ;
		response.writeInt( config.getBlockSize()) ;
		response.writeUnsignedInt( total) ;
		response.writeUnsignedInt( free) ;
		response.writeUnsignedInt( free) ;
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
		entries.add( new DirectoryEntry( ".", directory)) ;
		entries.add( new DirectoryEntry( "..", resolveLookupPath( directory, ".."))) ;

		try( var stream = Files.list( directory)) {
			stream.sorted( Comparator.comparing( path -> path.getFileName().toString().toLowerCase()))
					.forEach( path -> entries.add( new DirectoryEntry( path.getFileName().toString(), path))) ;
		}

		return entries ;
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

		long size = attributes.size() ;
		long blocks = Math.max( 1L, (size + config.getBlockSize() - 1L) / config.getBlockSize()) ;

		response.writeInt( type) ;
		response.writeUnsignedInt( mode) ;
		response.writeUnsignedInt( directory ? 2 : 1) ;
		response.writeUnsignedInt( config.getUid()) ;
		response.writeUnsignedInt( config.getGid()) ;
		response.writeUnsignedInt( size) ;
		response.writeUnsignedInt( config.getBlockSize()) ;
		response.writeUnsignedInt( 0) ;
		response.writeUnsignedInt( blocks) ;
		response.writeUnsignedInt( 1) ;
		response.writeUnsignedInt( Integer.toUnsignedLong( handleTable.getFileId( handle))) ;
		writeTime( response, attributes.lastAccessTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
		writeTime( response, attributes.lastModifiedTime()) ;
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

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param name	名前
		 * @param path	パス
		 */
		//----------------------------------------------------------------------
		DirectoryEntry(String name, Path path) {
			this.name = name ;
			this.path = path ;
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
	}
}
