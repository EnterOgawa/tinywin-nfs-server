package jp.co.enterogawa.nfs.program;

import java.io.IOException;

import jp.co.enterogawa.nfs.config.NfsExport;
import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcProgram;
import jp.co.enterogawa.nfs.rpc.RpcRequestContext;
import jp.co.enterogawa.nfs.util.ServerLog;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * MOUNT Version 1プログラムクラスです。<br><br>
 *
 * <p>クラス名称： MOUNT Version 1プログラム</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class MountV1Program implements RpcProgram {
	//	定数定義	------------------------------------------------------------
	/** Version */
	private static final int				VERSION = 1 ;

	/** MOUNTPROC_NULL */
	private static final int				PROC_NULL = 0 ;

	/** MOUNTPROC_MNT */
	private static final int				PROC_MNT = 1 ;

	/** MOUNTPROC_DUMP */
	private static final int				PROC_DUMP = 2 ;

	/** MOUNTPROC_UMNT */
	private static final int				PROC_UMNT = 3 ;

	/** MOUNTPROC_UMNTALL */
	private static final int				PROC_UMNTALL = 4 ;

	/** MOUNTPROC_EXPORT */
	private static final int				PROC_EXPORT = 5 ;

	/** MOUNT成功 */
	private static final int				MNT_OK = 0 ;

	/** アクセス拒否 */
	private static final int				MNT_EACCES = 13 ;

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
	public MountV1Program(NfsServerConfig config, FileHandleTable handleTable) {
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
		return RpcConstants.PROGRAM_MOUNT ;
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
	 * Version対応可否を確認します。<br><br>
	 *
	 * <p>メソッド名称： Version対応可否確認</p>
	 *
	 * @param version	Version
	 * @return true:対応 false:未対応
	 */
	//--------------------------------------------------------------------------
	@Override
	public boolean supportsVersion(int version) {
		return version == 1 || version == 2 || version == 3 ;
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
		switch( call.getProcedure()) {
		case PROC_NULL:
			return RpcConstants.ACCEPT_SUCCESS ;

		case PROC_MNT:
			handleMount( call, response) ;
			return RpcConstants.ACCEPT_SUCCESS ;

		case PROC_DUMP:
			response.writeBoolean( false) ;
			return RpcConstants.ACCEPT_SUCCESS ;

		case PROC_UMNT:
		case PROC_UMNTALL:
			return RpcConstants.ACCEPT_SUCCESS ;

		case PROC_EXPORT:
			handleExport( response) ;
			return RpcConstants.ACCEPT_SUCCESS ;

		default:
			return RpcConstants.ACCEPT_PROC_UNAVAIL ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * MOUNTを処理します。<br><br>
	 *
	 * <p>メソッド名称： MOUNT処理</p>
	 *
	 * @param call		RPC呼出
	 * @param response	応答
	 */
	//--------------------------------------------------------------------------
	private void handleMount(RpcCall call, XdrWriter response) {
		String path = call.getArguments().readString() ;
		NfsExport export = config.findExport( path) ;

		// 公開名が一致しない場合
		if( export == null) {
			logMount( path, MNT_EACCES, "unknown-export") ;
			response.writeInt( MNT_EACCES) ;
			return ;
		}

		// クライアントが許可されていない場合
		if( !export.allowsClient( RpcRequestContext.current().getClientAddress())) {
			logMount( path, MNT_EACCES, "client-denied") ;
			response.writeInt( MNT_EACCES) ;
			return ;
		}

		logMount( path, MNT_OK, "" ) ;
		response.writeInt( MNT_OK) ;

		// MOUNT v3の場合
		if( call.getVersion() == 3) {
			response.writeOpaque( handleTable.getRootHandle( export.getName()).getValue()) ;
			response.writeInt( 2) ;
			response.writeInt( RpcConstants.AUTH_NONE) ;
			response.writeInt( RpcConstants.AUTH_SYS) ;
			return ;
		}

		response.writeFixedOpaque( handleTable.getRootHandle( export.getName()).getValue()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * MOUNTログを出力します。<br><br>
	 *
	 * <p>メソッド名称： MOUNTログ出力</p>
	 *
	 * @param path		公開名
	 * @param status	ステータス
	 * @param detail	詳細
	 */
	//--------------------------------------------------------------------------
	private void logMount(String path, int status, String detail) {
		RpcRequestContext context = RpcRequestContext.current() ;
		StringBuilder message = new StringBuilder() ;
		message.append( "MOUNT MNT" )
				.append( " client=" ).append( context.getClientAddress() )
				.append( " xid=" ).append( context.formatXid() )
				.append( " program=" ).append( context.getProgram() )
				.append( " version=" ).append( context.getVersion() )
				.append( " procedure=" ).append( context.getProcedure() )
				.append( " status=" ).append( status )
				.append( " export=" ).append( path ) ;

		// 詳細が存在する場合
		if( detail != null && !detail.isBlank()) {
			message.append( " " ).append( detail) ;
		}

		ServerLog.info( message.toString()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * EXPORTを処理します。<br><br>
	 *
	 * <p>メソッド名称： EXPORT処理</p>
	 *
	 * @param response	応答
	 */
	//--------------------------------------------------------------------------
	private void handleExport(XdrWriter response) {
		// 公開定義を出力する
		for( NfsExport export : config.getExports()) {
			response.writeBoolean( true) ;
			response.writeString( export.getName()) ;
			response.writeBoolean( false) ;
		}

		response.writeBoolean( false) ;
	}
}
