package jp.co.enterogawa.nfs.server;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import jp.co.enterogawa.nfs.config.NfsExport;
import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.export.FileHandleTable;
import jp.co.enterogawa.nfs.program.MountV1Program;
import jp.co.enterogawa.nfs.program.NfsV2Program;
import jp.co.enterogawa.nfs.program.PortmapV2Program;
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
	private final List<UdpRpcServer>		servers = new ArrayList<UdpRpcServer>() ;

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
		FileHandleTable handleTable = new FileHandleTable( config.getExports()) ;
		servers.add( new UdpRpcServer(
				"nfs-portmap",
				config.getPortmapPort(),
				new PortmapV2Program( config.getPortmapPort(), config.getNfsPort(), config.getMountPort()))) ;
		servers.add( new UdpRpcServer(
				"nfs-mount-v1",
				config.getMountPort(),
				new MountV1Program( config, handleTable))) ;
		servers.add( new UdpRpcServer(
				"nfs-v2",
				config.getNfsPort(),
				new NfsV2Program( config, handleTable))) ;
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
	public synchronized void start() throws SocketException {
		// 既に開始済みの場合
		if( started) {
			return ;
		}

		validateExportPath() ;

		// RPCサーバーを開始する
		for( UdpRpcServer server : servers) {
			server.start() ;
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

		// RPCサーバーを停止する
		for( UdpRpcServer server : servers) {
			server.stop() ;
		}

		started = false ;
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
