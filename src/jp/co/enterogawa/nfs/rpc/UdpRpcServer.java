package jp.co.enterogawa.nfs.rpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import jp.co.enterogawa.nfs.util.ServerLog;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * UDP RPCサーバークラスです。<br><br>
 *
 * <p>クラス名称： UDP RPCサーバー</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class UdpRpcServer {
	//	定数定義	------------------------------------------------------------
	/** UDP受信バッファサイズ */
	private static final int				BUFFER_SIZE = 65535 ;

	//	内部定義	------------------------------------------------------------
	/** サーバー名 */
	private final String					name ;

	/** ポート */
	private final int					port ;

	/** RPCプログラム */
	private final RpcProgram				program ;

	/** 実行状態 */
	private volatile boolean				running ;

	/** ソケット */
	private DatagramSocket				socket ;

	/** 受信スレッド */
	private Thread						thread ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param name		サーバー名
	 * @param port		ポート
	 * @param program	RPCプログラム
	 */
	//--------------------------------------------------------------------------
	public UdpRpcServer(String name, int port, RpcProgram program) {
		this.name = name ;
		this.port = port ;
		this.program = program ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーを開始します。<br><br>
	 *
	 * <p>メソッド名称： サーバー開始</p>
	 *
	 * @throws SocketException ソケット異常
	 */
	//--------------------------------------------------------------------------
	public void start() throws SocketException {
		// 既に開始済みの場合
		if( running) {
			return ;
		}

		socket = new DatagramSocket( port) ;
		running = true ;
		thread = new Thread( this::receiveLoop, name) ;
		thread.setDaemon( false) ;
		thread.start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーを停止します。<br><br>
	 *
	 * <p>メソッド名称： サーバー停止</p>
	 */
	//--------------------------------------------------------------------------
	public void stop() {
		running = false ;

		// ソケットが存在する場合
		if( socket != null) {
			socket.close() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 受信ループを処理します。<br><br>
	 *
	 * <p>メソッド名称： 受信ループ処理</p>
	 */
	//--------------------------------------------------------------------------
	private void receiveLoop() {
		byte[] buffer = new byte[BUFFER_SIZE] ;

		while( running) {
			DatagramPacket packet = new DatagramPacket( buffer, buffer.length) ;

			try {
				socket.receive( packet) ;
				handlePacket( packet) ;
			} catch( SocketException sex) {
				// 停止中の場合
				if( !running) {
					return ;
				}

				sex.printStackTrace() ;
			} catch( Exception ex) {
				ex.printStackTrace() ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCパケットを処理します。<br><br>
	 *
	 * <p>メソッド名称： RPCパケット処理</p>
	 *
	 * @param packet	受信パケット
	 * @throws IOException 送信異常
	 */
	//--------------------------------------------------------------------------
	private void handlePacket(DatagramPacket packet) throws IOException {
		int xid = RpcCall.readXid( packet.getData(), packet.getLength()) ;
		byte[] response = createResponse( packet, xid) ;
		DatagramPacket responsePacket = new DatagramPacket( response, response.length, packet.getAddress(), packet.getPort()) ;
		socket.send( responsePacket) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC応答を作成します。<br><br>
	 *
	 * <p>メソッド名称： RPC応答作成</p>
	 *
	 * @param packet	受信パケット
	 * @param xid		XID
	 * @return RPC応答
	 */
	//--------------------------------------------------------------------------
	private byte[] createResponse(DatagramPacket packet, int xid) {
		try {
			RpcCall call = RpcCall.read( packet.getData(), packet.getLength()) ;
			RpcRequestContext context = new RpcRequestContext(
					packet.getAddress().getHostAddress(),
					packet.getPort(),
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

			// 通常READ成功ログを抑制できる場合
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
					+ " client=" + packet.getAddress().getHostAddress() + ":" + packet.getPort()
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
