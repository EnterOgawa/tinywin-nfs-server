package jp.co.enterogawa.nfs;

import java.nio.file.Files;
import java.nio.file.Path;

import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.config.TinyWinNfsPaths;
import jp.co.enterogawa.nfs.server.NfsServer;

//------------------------------------------------------------------------------
/**
 * NFSサーバーメインクラスです。<br><br>
 *
 * <p>クラス名称： NFSサーバーメインクラス</p>
 *
 * <p>著作権： Copyright (c) 2026 EnterOgawa. All Rights Reserved.</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsServerMain {
	//--------------------------------------------------------------------------
	/**
	 * アプリケーションを開始します。<br><br>
	 *
	 * <p>メソッド名称： アプリケーション開始</p>
	 *
	 * @param args	起動引数
	 * @throws Exception 起動異常
	 */
	//--------------------------------------------------------------------------
	public static void main(String[] args) throws Exception {
		Path applicationRoot = Path.of( System.getProperty( "user.dir")).toAbsolutePath().normalize() ;
		Path localConfigPath = applicationRoot.resolve( "conf").resolve( "nfs-server.properties") ;
		Path configPath = TinyWinNfsPaths.getConfigPath( applicationRoot) ;

		// ローカル設定ファイルが存在する場合
		if( Files.exists( localConfigPath)) {
			configPath = localConfigPath ;
		}

		// 設定ファイル指定がある場合
		if( args.length > 0) {
			configPath = Path.of( args[0]) ;
		}

		NfsServerConfig config = NfsServerConfig.load( configPath) ;
		NfsServer server = new NfsServer( config) ;

		Runtime.getRuntime().addShutdownHook( new Thread( () -> {
			server.stop() ;
		}, "nfs-shutdown")) ;

		server.start() ;
		Thread.currentThread().join() ;
	}
}
