package jp.co.enterogawa.nfs.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;

import jp.co.enterogawa.nfs.config.NfsExport;
import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.export.NfsModeStore;
import jp.co.enterogawa.nfs.io.WriteFileCache;
import jp.co.enterogawa.nfs.program.MountV1Program;
import jp.co.enterogawa.nfs.program.NfsV2Program;
import jp.co.enterogawa.nfs.program.NfsV3Program;
import jp.co.enterogawa.nfs.program.PortmapV2Program;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcProgramRouter;
import jp.co.enterogawa.nfs.rpc.RpcServer;
import jp.co.enterogawa.nfs.rpc.TcpRpcServer;
import jp.co.enterogawa.nfs.rpc.UdpRpcServer;

//------------------------------------------------------------------------------
/**
 * NFSサーバークラスです。<br><br>
 *
 * <p>クラス名称： NFSサーバー</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsServer {
	//	内部定義	------------------------------------------------------------
	/** 設定 */
	private final NfsServerConfig		config ;

	/** RPCサーバーリスト */
	private final List<RpcServer>		servers = new ArrayList<RpcServer>() ;

	/** ファイルハンドル管理 */
	private final FileHandleTable		handleTable ;

	/** NFSモード管理 */
	private final NfsModeStore			modeStore ;

	/** 書込ファイルキャッシュ */
	private final WriteFileCache			writeFileCache ;

	/** 実行状態 */
	private boolean						started ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param config	設定
	 */
	//--------------------------------------------------------------------------
	public NfsServer(NfsServerConfig config) {
		this.config = config ;
		handleTable = new FileHandleTable( config.getExports()) ;
		modeStore = new NfsModeStore( config.getExports()) ;
		writeFileCache = new WriteFileCache( config) ;
		PortmapV2Program portmapProgram = new PortmapV2Program( config.getPortmapPort(), config.getNfsPort(), config.getMountPort()) ;
		MountV1Program mountProgram = new MountV1Program( config, handleTable) ;
		RpcProgramRouter nfsProgram = new RpcProgramRouter( RpcConstants.PROGRAM_NFS)
				.add( new NfsV2Program( config, handleTable, writeFileCache, modeStore))
				.add( new NfsV3Program( config, handleTable, writeFileCache, modeStore)) ;
		servers.add( new UdpRpcServer(
				"nfs-portmap-udp",
				config.getPortmapPort(),
				portmapProgram,
				config.getRpcUdpWorkers(),
				config.getRpcUdpQueueSize())) ;
		servers.add( new TcpRpcServer(
				"nfs-portmap-tcp",
				config.getPortmapPort(),
				portmapProgram,
				config.getRpcTcpTimeoutMillis())) ;
		servers.add( new UdpRpcServer(
				"nfs-mount-udp",
				config.getMountPort(),
				mountProgram,
				config.getRpcUdpWorkers(),
				config.getRpcUdpQueueSize())) ;
		servers.add( new TcpRpcServer(
				"nfs-mount-tcp",
				config.getMountPort(),
				mountProgram,
				config.getRpcTcpTimeoutMillis())) ;
		servers.add( new UdpRpcServer(
				"nfs-udp",
				config.getNfsPort(),
				nfsProgram,
				config.getRpcUdpWorkers(),
				config.getRpcUdpQueueSize())) ;
		servers.add( new TcpRpcServer(
				"nfs-tcp",
				config.getNfsPort(),
				nfsProgram,
				config.getRpcTcpTimeoutMillis())) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーを開始します。<br><br>
	 *
	 * <p>メソッド名称： サーバー開始</p>
	 *
	 * @throws IOException ソケット異常
	 */
	//--------------------------------------------------------------------------
	public synchronized void start() throws IOException {
		// 既に開始済みの場合
		if( started) {
			return ;
		}

		validateExportPath() ;

		try {
			// RPCサーバーを開始する
			for( RpcServer server : servers) {
				server.start() ;
			}
		} catch( IOException | RuntimeException ex) {
			stopServers() ;
			throw ex ;
		}

		started = true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーを停止します。<br><br>
	 *
	 * <p>メソッド名称： サーバー停止</p>
	 */
	//--------------------------------------------------------------------------
	public synchronized void stop() {
		// 未開始の場合
		if( !started) {
			return ;
		}

		stopServers() ;
		try {
			writeFileCache.close() ;
		} catch( IOException ioex) {
			ioex.printStackTrace() ;
		}
		handleTable.flush() ;
		modeStore.flush() ;

		started = false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCサーバーを停止します。<br><br>
	 *
	 * <p>メソッド名称： RPCサーバー停止</p>
	 */
	//--------------------------------------------------------------------------
	private void stopServers() {
		// RPCサーバーを停止する
		for( RpcServer server : servers) {
			server.stop() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定を取得します。<br><br>
	 *
	 * <p>メソッド名称： 設定取得</p>
	 *
	 * @return 設定
	 */
	//--------------------------------------------------------------------------
	public NfsServerConfig getConfig() {
		return config ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開パスを検証します。<br><br>
	 *
	 * <p>メソッド名称： 公開パス検証</p>
	 */
	//--------------------------------------------------------------------------
	private void validateExportPath() {
		// 公開パスを検証する
		for( NfsExport export : config.getExports()) {
			// 公開パスがディレクトリではない場合
			if( !Files.isDirectory( export.getPath(), LinkOption.NOFOLLOW_LINKS)) {
				throw new IllegalStateException( "Export path is not a directory: " + export.getPath()) ;
			}

			// 書込可能設定だがフォルダへ書き込めない場合
			if( export.isWritable() && !Files.isWritable( export.getPath())) {
				throw new IllegalStateException( "Export path is not writable: " + export.getPath()) ;
			}
		}
	}
}
