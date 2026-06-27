package jp.co.enterogawa.nfs.rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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
public class UdpRpcServer implements RpcServer {
	//	定数定義	------------------------------------------------------------
	/** UDP受信バッファサイズ */
	private static final int				BUFFER_SIZE = 65535 ;

	//	内部定義	------------------------------------------------------------
	/** サーバー名 */
	private final String					name ;

	/** ポート */
	private final int					port ;

	/** RPC呼出処理 */
	private final RpcCallHandler			handler ;

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
		handler = new RpcCallHandler( name, program) ;
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
		byte[] response = handler.handle(
				packet.getData(),
				packet.getLength(),
				packet.getAddress().getHostAddress(),
				packet.getPort()) ;
		DatagramPacket responsePacket = new DatagramPacket( response, response.length, packet.getAddress(), packet.getPort()) ;
		socket.send( responsePacket) ;
	}
}
