package jp.co.enterogawa.nfs.rpc;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

//------------------------------------------------------------------------------
/**
 * TCP RPCサーバークラスです。<br><br>
 *
 * <p>クラス名称： TCP RPCサーバー</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class TcpRpcServer implements RpcServer {
	//	定数定義	------------------------------------------------------------
	/** LAST fragmentフラグ */
	private static final int				LAST_FRAGMENT = 0x80000000 ;

	/** fragment長マスク */
	private static final int				FRAGMENT_LENGTH_MASK = 0x7fffffff ;

	/** 最大RPC recordサイズ */
	private static final int				MAX_RECORD_SIZE = 16 * 1024 * 1024 ;

	/** デフォルトクライアントタイムアウト */
	private static final int				DEFAULT_CLIENT_TIMEOUT_MILLIS = 30000 ;

	//	内部定義	------------------------------------------------------------
	/** サーバー名 */
	private final String					name ;

	/** ポート */
	private final int					port ;

	/** RPC呼出処理 */
	private final RpcCallHandler			handler ;

	/** クライアントタイムアウト */
	private final int					clientTimeoutMillis ;

	/** 実行状態 */
	private volatile boolean				running ;

	/** サーバーソケット */
	private ServerSocket				serverSocket ;

	/** acceptスレッド */
	private Thread						thread ;

	/** 接続中ソケット */
	private final Set<Socket>			connections = Collections.synchronizedSet( new HashSet<Socket>()) ;

	/** クライアントスレッド連番 */
	private final AtomicInteger			clientSequence = new AtomicInteger( 1) ;

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
	public TcpRpcServer(String name, int port, RpcProgram program) {
		this( name, port, program, DEFAULT_CLIENT_TIMEOUT_MILLIS) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param name					サーバー名
	 * @param port					ポート
	 * @param program				RPCプログラム
	 * @param clientTimeoutMillis	クライアントタイムアウト
	 */
	//--------------------------------------------------------------------------
	public TcpRpcServer(String name, int port, RpcProgram program, int clientTimeoutMillis) {
		this.name = name ;
		this.port = port ;
		handler = new RpcCallHandler( name, program) ;
		this.clientTimeoutMillis = Math.max( 1, clientTimeoutMillis) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーを開始します。<br><br>
	 *
	 * <p>メソッド名称： サーバー開始</p>
	 *
	 * @throws IOException 開始異常
	 */
	//--------------------------------------------------------------------------
	@Override
	public void start() throws IOException {
		// 既に開始済みの場合
		if( running) {
			return ;
		}

		ServerSocket socket = new ServerSocket() ;
		socket.setReuseAddress( true) ;
		socket.bind( new InetSocketAddress( port)) ;
		serverSocket = socket ;
		running = true ;
		thread = new Thread( this::acceptLoop, name) ;
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
	@Override
	public void stop() {
		running = false ;

		// サーバーソケットが存在する場合
		if( serverSocket != null) {
			try {
				serverSocket.close() ;
			} catch( IOException ioex) {
				ioex.printStackTrace() ;
			}
		}

		synchronized( connections) {
			// 接続中ソケットを閉じる
			for( Socket socket : connections) {
				try {
					socket.close() ;
				} catch( IOException ioex) {
					ioex.printStackTrace() ;
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * acceptループを処理します。<br><br>
	 *
	 * <p>メソッド名称： acceptループ処理</p>
	 */
	//--------------------------------------------------------------------------
	private void acceptLoop() {
		while( running) {
			try {
				Socket client = serverSocket.accept() ;
				startClientThread( client) ;
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
	 * クライアント処理スレッドを開始します。<br><br>
	 *
	 * <p>メソッド名称： クライアント処理スレッド開始</p>
	 *
	 * @param client	クライアントソケット
	 */
	//--------------------------------------------------------------------------
	private void startClientThread(Socket client) {
		Thread clientThread = new Thread( () -> handleClient( client), name + "-client-" + clientSequence.getAndIncrement()) ;
		clientThread.setDaemon( true) ;
		clientThread.start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアント接続を処理します。<br><br>
	 *
	 * <p>メソッド名称： クライアント接続処理</p>
	 *
	 * @param client	クライアントソケット
	 */
	//--------------------------------------------------------------------------
	private void handleClient(Socket client) {
		connections.add( client) ;

		try( Socket socket = client) {
			socket.setSoTimeout( clientTimeoutMillis) ;
			InputStream input = socket.getInputStream() ;
			OutputStream output = socket.getOutputStream() ;

			while( running && !socket.isClosed()) {
				byte[] request = readRecord( input) ;

				// クライアントが正常切断した場合
				if( request == null) {
					return ;
				}

				byte[] response = handler.handle(
						request,
						request.length,
						socket.getInetAddress().getHostAddress(),
						socket.getPort()) ;
				writeRecord( output, response) ;
			}
		} catch( SocketException sex) {
			// 停止中の場合
			if( running) {
				sex.printStackTrace() ;
			}
		} catch( Exception ex) {
			ex.printStackTrace() ;
		} finally {
			connections.remove( client) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPC recordを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC record読込</p>
	 *
	 * @param input	入力
	 * @return record。正常切断時はnull。
	 * @throws IOException 読込異常
	 */
	//--------------------------------------------------------------------------
	private byte[] readRecord(InputStream input) throws IOException {
		DataInputStream dataInput = new DataInputStream( input) ;
		ByteArrayOutputStream record = new ByteArrayOutputStream() ;
		boolean last = false ;

		while( !last) {
			int header ;

			try {
				header = dataInput.readInt() ;
			} catch( EOFException eofex) {
				// record途中ではない場合
				if( record.size() == 0) {
					return null ;
				}

				throw eofex ;
			}

			last = (header & LAST_FRAGMENT) != 0 ;
			int length = header & FRAGMENT_LENGTH_MASK ;

			// fragment長が不正な場合
			if( length <= 0) {
				throw new IOException( "Invalid TCP RPC fragment length: " + length) ;
			}

			// record長が上限を超える場合
			if( record.size() + length > MAX_RECORD_SIZE) {
				throw new IOException( "TCP RPC record is too large.") ;
			}

			byte[] fragment = dataInput.readNBytes( length) ;

			// fragmentが途中で終端した場合
			if( fragment.length != length) {
				throw new EOFException( "Unexpected EOF in TCP RPC fragment.") ;
			}

			record.writeBytes( fragment) ;
		}

		return record.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * TCP RPC recordを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： TCP RPC record書込</p>
	 *
	 * @param output	出力
	 * @param record	record
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private void writeRecord(OutputStream output, byte[] record) throws IOException {
		DataOutputStream dataOutput = new DataOutputStream( output) ;
		dataOutput.writeInt( LAST_FRAGMENT | record.length) ;
		dataOutput.write( record) ;
		dataOutput.flush() ;
	}
}
