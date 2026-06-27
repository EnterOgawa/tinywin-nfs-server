package jp.co.enterogawa.nfs.rpc;

//------------------------------------------------------------------------------
/**
 * RPC定数クラスです。<br><br>
 *
 * <p>クラス名称： RPC定数</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class RpcConstants {
	//	定数定義	------------------------------------------------------------
	/** RPCバージョン */
	public static final int				RPC_VERSION = 2 ;

	/** CALLメッセージ */
	public static final int				MSG_CALL = 0 ;

	/** REPLYメッセージ */
	public static final int				MSG_REPLY = 1 ;

	/** ACCEPTED */
	public static final int				REPLY_STAT_ACCEPTED = 0 ;

	/** SUCCESS */
	public static final int				ACCEPT_SUCCESS = 0 ;

	/** PROG_UNAVAIL */
	public static final int				ACCEPT_PROG_UNAVAIL = 1 ;

	/** PROC_UNAVAIL */
	public static final int				ACCEPT_PROC_UNAVAIL = 3 ;

	/** GARBAGE_ARGS */
	public static final int				ACCEPT_GARBAGE_ARGS = 4 ;

	/** AUTH_NONE */
	public static final int				AUTH_NONE = 0 ;

	/** AUTH_SYS */
	public static final int				AUTH_SYS = 1 ;

	/** IPPROTO_TCP */
	public static final int				IPPROTO_TCP = 6 ;

	/** IPPROTO_UDP */
	public static final int				IPPROTO_UDP = 17 ;

	/** Portmap program */
	public static final int				PROGRAM_PORTMAP = 100000 ;

	/** NFS program */
	public static final int				PROGRAM_NFS = 100003 ;

	/** Mount program */
	public static final int				PROGRAM_MOUNT = 100005 ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を禁止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private RpcConstants() {
	}
}
