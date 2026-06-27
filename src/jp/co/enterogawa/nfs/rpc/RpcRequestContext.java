package jp.co.enterogawa.nfs.rpc;

//------------------------------------------------------------------------------
/**
 * RPC要求コンテキストクラスです。<br><br>
 *
 * <p>クラス名称： RPC要求コンテキスト</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class RpcRequestContext {
	//	定数定義	------------------------------------------------------------
	/** ローカルテスト用コンテキスト */
	public static final RpcRequestContext LOCAL = new RpcRequestContext(
			"127.0.0.1",
			0,
			"local",
			0,
			0,
			0,
			0) ;

	/** 現在のコンテキスト */
	private static final ThreadLocal<RpcRequestContext> CURRENT = new ThreadLocal<RpcRequestContext>() ;

	//	内部定義	------------------------------------------------------------
	/** クライアントアドレス */
	private final String					clientAddress ;

	/** クライアントポート */
	private final int					clientPort ;

	/** サーバー名 */
	private final String					serverName ;

	/** XID */
	private final int					xid ;

	/** Program */
	private final int					program ;

	/** Version */
	private final int					version ;

	/** Procedure */
	private final int					procedure ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param clientAddress	クライアントアドレス
	 * @param clientPort		クライアントポート
	 * @param serverName		サーバー名
	 * @param xid			XID
	 * @param program		Program
	 * @param version		Version
	 * @param procedure		Procedure
	 */
	//--------------------------------------------------------------------------
	public RpcRequestContext(String clientAddress, int clientPort, String serverName, int xid, int program, int version, int procedure) {
		this.clientAddress = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress.trim() ;
		this.clientPort = clientPort ;
		this.serverName = serverName == null || serverName.isBlank() ? "unknown" : serverName.trim() ;
		this.xid = xid ;
		this.program = program ;
		this.version = version ;
		this.procedure = procedure ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 現在のコンテキストを取得します。<br><br>
	 *
	 * <p>メソッド名称： 現在コンテキスト取得</p>
	 *
	 * @return コンテキスト
	 */
	//--------------------------------------------------------------------------
	public static RpcRequestContext current() {
		RpcRequestContext context = CURRENT.get() ;

		// コンテキストが未設定の場合
		if( context == null) {
			return LOCAL ;
		}

		return context ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 現在のコンテキストを設定します。<br><br>
	 *
	 * <p>メソッド名称： 現在コンテキスト設定</p>
	 *
	 * @param context	コンテキスト
	 * @return 直前のコンテキスト
	 */
	//--------------------------------------------------------------------------
	public static RpcRequestContext setCurrent(RpcRequestContext context) {
		RpcRequestContext previous = CURRENT.get() ;
		CURRENT.set( context == null ? LOCAL : context) ;
		return previous ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 現在のコンテキストを復元します。<br><br>
	 *
	 * <p>メソッド名称： 現在コンテキスト復元</p>
	 *
	 * @param previous	直前のコンテキスト
	 */
	//--------------------------------------------------------------------------
	public static void restore(RpcRequestContext previous) {
		// 直前のコンテキストが存在しない場合
		if( previous == null) {
			CURRENT.remove() ;
			return ;
		}

		CURRENT.set( previous) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアントアドレスを取得します。<br><br>
	 *
	 * <p>メソッド名称： クライアントアドレス取得</p>
	 *
	 * @return クライアントアドレス
	 */
	//--------------------------------------------------------------------------
	public String getClientAddress() {
		return clientAddress ;
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアントポートを取得します。<br><br>
	 *
	 * <p>メソッド名称： クライアントポート取得</p>
	 *
	 * @return クライアントポート
	 */
	//--------------------------------------------------------------------------
	public int getClientPort() {
		return clientPort ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバー名を取得します。<br><br>
	 *
	 * <p>メソッド名称： サーバー名取得</p>
	 *
	 * @return サーバー名
	 */
	//--------------------------------------------------------------------------
	public String getServerName() {
		return serverName ;
	}

	//--------------------------------------------------------------------------
	/**
	 * XIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： XID取得</p>
	 *
	 * @return XID
	 */
	//--------------------------------------------------------------------------
	public int getXid() {
		return xid ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Programを取得します。<br><br>
	 *
	 * <p>メソッド名称： Program取得</p>
	 *
	 * @return Program
	 */
	//--------------------------------------------------------------------------
	public int getProgram() {
		return program ;
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
	public int getVersion() {
		return version ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Procedureを取得します。<br><br>
	 *
	 * <p>メソッド名称： Procedure取得</p>
	 *
	 * @return Procedure
	 */
	//--------------------------------------------------------------------------
	public int getProcedure() {
		return procedure ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログ用XIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： ログ用XID取得</p>
	 *
	 * @return XID文字列
	 */
	//--------------------------------------------------------------------------
	public String formatXid() {
		return String.format( "0x%08x", xid) ;
	}
}
