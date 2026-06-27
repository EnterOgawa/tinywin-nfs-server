package jp.co.enterogawa.nfs.rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

	/** ワーカー */
	private ThreadPoolExecutor			workerPool ;

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
		workerPool = createWorkerPool() ;
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

		// ワーカーが存在する場合
		if( workerPool != null) {
			workerPool.shutdownNow() ;
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
				dispatchPacket( packet) ;
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
	 * RPCパケット処理をワーカーへ渡します。<br><br>
	 *
	 * <p>メソッド名称： RPCパケット振分</p>
	 *
	 * @param packet	受信パケット
	 */
	//--------------------------------------------------------------------------
	private void dispatchPacket(DatagramPacket packet) {
		byte[] data = Arrays.copyOf( packet.getData(), packet.getLength()) ;
		RpcDatagram datagram = new RpcDatagram( data, packet.getAddress(), packet.getPort()) ;

		// ワーカーが無効の場合
		if( workerPool == null) {
			handlePacketQuietly( datagram) ;
			return ;
		}

		workerPool.execute( () -> handlePacketQuietly( datagram)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCパケットを処理します。<br><br>
	 *
	 * <p>メソッド名称： RPCパケット静的処理</p>
	 *
	 * @param datagram	受信データ
	 */
	//--------------------------------------------------------------------------
	private void handlePacketQuietly(RpcDatagram datagram) {
		try {
			handlePacket( datagram) ;
		} catch( SocketException sex) {
			// 停止中の場合
			if( running) {
				sex.printStackTrace() ;
			}
		} catch( Exception ex) {
			ex.printStackTrace() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCパケットを処理します。<br><br>
	 *
	 * <p>メソッド名称： RPCパケット処理</p>
	 *
	 * @param datagram	受信データ
	 * @throws IOException 送信異常
	 */
	//--------------------------------------------------------------------------
	private void handlePacket(RpcDatagram datagram) throws IOException {
		byte[] response = handler.handle(
				datagram.getData(),
				datagram.getData().length,
				datagram.getAddress().getHostAddress(),
				datagram.getPort()) ;
		DatagramPacket responsePacket = new DatagramPacket( response, response.length, datagram.getAddress(), datagram.getPort()) ;
		socket.send( responsePacket) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ワーカープールを生成します。<br><br>
	 *
	 * <p>メソッド名称： ワーカープール生成</p>
	 *
	 * @return ワーカープール
	 */
	//--------------------------------------------------------------------------
	private ThreadPoolExecutor createWorkerPool() {
		int workers = Math.max( 1, Integer.getInteger( "tinywin.nfs.udp.workers", defaultWorkerCount()).intValue()) ;
		AtomicInteger sequence = new AtomicInteger( 1) ;
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				workers,
				workers,
				0L,
				TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>( 1024),
				runnable -> {
					Thread worker = new Thread( runnable, name + "-worker-" + sequence.getAndIncrement()) ;
					worker.setDaemon( true) ;
					return worker ;
				},
				new ThreadPoolExecutor.CallerRunsPolicy()) ;
		executor.prestartAllCoreThreads() ;
		return executor ;
	}

	//--------------------------------------------------------------------------
	/**
	 * デフォルトワーカー数を取得します。<br><br>
	 *
	 * <p>メソッド名称： デフォルトワーカー数取得</p>
	 *
	 * @return デフォルトワーカー数
	 */
	//--------------------------------------------------------------------------
	private int defaultWorkerCount() {
		return Math.min( 8, Math.max( 2, Runtime.getRuntime().availableProcessors())) ;
	}

	//------------------------------------------------------------------------------
	/**
	 * RPC Datagram保持クラスです。<br><br>
	 *
	 * <p>クラス名称： RPC Datagram保持</p>
	 */
	//------------------------------------------------------------------------------
	private static class RpcDatagram {
		/** データ */
		private final byte[]				data ;

		/** アドレス */
		private final InetAddress			address ;

		/** ポート */
		private final int					port ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param data		データ
		 * @param address	アドレス
		 * @param port		ポート
		 */
		//----------------------------------------------------------------------
		RpcDatagram(byte[] data, InetAddress address, int port) {
			this.data = data ;
			this.address = address ;
			this.port = port ;
		}

		//----------------------------------------------------------------------
		/**
		 * データを取得します。<br><br>
		 *
		 * <p>メソッド名称： データ取得</p>
		 *
		 * @return データ
		 */
		//----------------------------------------------------------------------
		byte[] getData() {
			return data ;
		}

		//----------------------------------------------------------------------
		/**
		 * アドレスを取得します。<br><br>
		 *
		 * <p>メソッド名称： アドレス取得</p>
		 *
		 * @return アドレス
		 */
		//----------------------------------------------------------------------
		InetAddress getAddress() {
			return address ;
		}

		//----------------------------------------------------------------------
		/**
		 * ポートを取得します。<br><br>
		 *
		 * <p>メソッド名称： ポート取得</p>
		 *
		 * @return ポート
		 */
		//----------------------------------------------------------------------
		int getPort() {
			return port ;
		}
	}
}
