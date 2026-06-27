package jp.co.enterogawa.nfs.rpc;

import java.nio.ByteBuffer;

import jp.co.enterogawa.nfs.util.ServerLog;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * RPC呼出処理クラスです。<br><br>
 *
 * <p>クラス名称： RPC呼出処理</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class RpcCallHandler {
	//	内部定義	------------------------------------------------------------
	/** サーバー名 */
	private final String					name ;

	/** RPCプログラム */
	private final RpcProgram				program ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param name		サーバー名
	 * @param program	RPCプログラム
	 */
	//--------------------------------------------------------------------------
	public RpcCallHandler(String name, RpcProgram program) {
		this.name = name ;
		this.program = program ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC要求を処理します。<br><br>
	 *
	 * <p>メソッド名称： RPC要求処理</p>
	 *
	 * @param request		要求
	 * @param length		有効長
	 * @param clientAddress	クライアントアドレス
	 * @param clientPort	クライアントポート
	 * @return RPC応答
	 */
	//--------------------------------------------------------------------------
	public byte[] handle(byte[] request, int length, String clientAddress, int clientPort) {
		int xid = RpcCall.readXid( request, length) ;

		try {
			RpcCall call = RpcCall.read( request, length) ;
			RpcRequestContext context = new RpcRequestContext(
					clientAddress,
					clientPort,
					name,
					call.getXid(),
					call.getProgram(),
					call.getVersion(),
					call.getProcedure()) ;
			XdrWriter body = new XdrWriter() ;
			int acceptStatus = RpcConstants.ACCEPT_SUCCESS ;

			// Programが一致しない場合
			if( call.getProgram() != program.getProgramNumber()) {
				acceptStatus = RpcConstants.ACCEPT_PROG_UNAVAIL ;
			}
			// Versionが未対応の場合
			else if( !program.supportsVersion( call.getVersion())) {
				acceptStatus = RpcConstants.ACCEPT_PROG_UNAVAIL ;
			}
			else {
				acceptStatus = program.handle( call, context, body) ;
			}

			byte[] bodyBytes = body.toByteArray() ;
			Integer resultStatus = null ;

			// NFS/MOUNTの結果ステータスが存在する場合
			if( acceptStatus == RpcConstants.ACCEPT_SUCCESS && bodyBytes.length >= Integer.BYTES) {
				resultStatus = ByteBuffer.wrap( bodyBytes).getInt() ;
			}

			// 要求ログを出力する場合
			if( shouldLogRequest( call, acceptStatus, resultStatus)) {
				ServerLog.info(
						"RPC"
						+ " client=" + context.getClientAddress() + ":" + context.getClientPort()
						+ " server=" + name
						+ " xid=" + context.formatXid()
						+ " program=" + call.getProgram()
						+ " version=" + call.getVersion()
						+ " procedure=" + call.getProcedure()
						+ " accept=" + acceptStatus
						+ formatResultStatus( resultStatus)) ;
			}

			return RpcReplyWriter.accepted( call.getXid(), acceptStatus, bodyBytes) ;
		} catch( Exception ex) {
			ServerLog.info(
					"RPC"
					+ " client=" + clientAddress + ":" + clientPort
					+ " server=" + name
					+ " xid=" + String.format( "0x%08x", xid)
					+ " parse-error="
					+ ex.getClass().getSimpleName()
					+ ":" + ex.getMessage()) ;
			ex.printStackTrace() ;
			return RpcReplyWriter.accepted( xid, RpcConstants.ACCEPT_GARBAGE_ARGS, new byte[0]) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 要求ログ出力要否を判定します。<br><br>
	 *
	 * <p>メソッド名称： 要求ログ出力要否判定</p>
	 *
	 * @param call			RPC呼出
	 * @param acceptStatus	ACCEPTステータス
	 * @param resultStatus	結果ステータス
	 * @return true:出力 false:抑制
	 */
	//--------------------------------------------------------------------------
	private boolean shouldLogRequest(RpcCall call, int acceptStatus, Integer resultStatus) {
		// デバッグログが有効な場合
		if( ServerLog.isDebugEnabled()) {
			return true ;
		}

		boolean successfulRead = call.getProgram() == RpcConstants.PROGRAM_NFS
				&& (call.getVersion() == 2 || call.getVersion() == 3)
				&& call.getProcedure() == 6
				&& acceptStatus == RpcConstants.ACCEPT_SUCCESS
				&& resultStatus != null
				&& resultStatus.intValue() == 0 ;

		return !successfulRead ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 結果ステータスを整形します。<br><br>
	 *
	 * <p>メソッド名称： 結果ステータス整形</p>
	 *
	 * @param resultStatus	結果ステータス
	 * @return 結果ステータス文字列
	 */
	//--------------------------------------------------------------------------
	private String formatResultStatus(Integer resultStatus) {
		// 結果ステータスが存在しない場合
		if( resultStatus == null) {
			return "" ;
		}

		return " status=" + resultStatus ;
	}
}
