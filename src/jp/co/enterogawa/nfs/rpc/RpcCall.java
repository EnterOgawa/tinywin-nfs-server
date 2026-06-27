package jp.co.enterogawa.nfs.rpc;

import jp.co.enterogawa.nfs.xdr.XdrReader;

//------------------------------------------------------------------------------
/**
 * RPC呼出クラスです。<br><br>
 *
 * <p>クラス名称： RPC呼出</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class RpcCall {
	//	内部定義	------------------------------------------------------------
	/** XID */
	private final int					xid ;

	/** Program */
	private final int					program ;

	/** Version */
	private final int					version ;

	/** Procedure */
	private final int					procedure ;

	/** Credential */
	private final RpcCredential			credential ;

	/** Verifier */
	private final RpcCredential			verifier ;

	/** 引数 */
	private final XdrReader				arguments ;

	/** 引数バイト列 */
	private final byte[]					argumentBytes ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param xid			XID
	 * @param program		Program
	 * @param version		Version
	 * @param procedure		Procedure
	 * @param credential	Credential
	 * @param verifier		Verifier
	 * @param arguments		引数
	 * @param argumentBytes	引数バイト列
	 */
	//--------------------------------------------------------------------------
	private RpcCall(int xid, int program, int version, int procedure, RpcCredential credential, RpcCredential verifier, XdrReader arguments, byte[] argumentBytes) {
		this.xid = xid ;
		this.program = program ;
		this.version = version ;
		this.procedure = procedure ;
		this.credential = credential ;
		this.verifier = verifier ;
		this.arguments = arguments ;
		this.argumentBytes = argumentBytes ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出読込</p>
	 *
	 * @param packet	パケット
	 * @param length	有効長
	 * @return RPC呼出
	 */
	//--------------------------------------------------------------------------
	public static RpcCall read(byte[] packet, int length) {
		XdrReader reader = new XdrReader( packet, length) ;
		int xid = reader.readInt() ;
		int messageType = reader.readInt() ;

		// CALL以外の場合
		if( messageType != RpcConstants.MSG_CALL) {
			throw new IllegalArgumentException( "RPC message is not CALL.") ;
		}

		int rpcVersion = reader.readInt() ;

		// RPCバージョンが一致しない場合
		if( rpcVersion != RpcConstants.RPC_VERSION) {
			throw new IllegalArgumentException( "Unsupported RPC version.") ;
		}

		int program = reader.readInt() ;
		int version = reader.readInt() ;
		int procedure = reader.readInt() ;
		RpcCredential credential = readCredential( reader) ;
		RpcCredential verifier = readCredential( reader) ;
		byte[] argumentBytes = reader.readRemaining() ;
		XdrReader arguments = new XdrReader( argumentBytes) ;

		return new RpcCall( xid, program, version, procedure, credential, verifier, arguments, argumentBytes) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * XIDのみを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： XID読込</p>
	 *
	 * @param packet	パケット
	 * @param length	有効長
	 * @return XID
	 */
	//--------------------------------------------------------------------------
	public static int readXid(byte[] packet, int length) {
		// XIDが存在しない場合
		if( length < 4) {
			return 0 ;
		}

		return ((packet[0] & 0xff) << 24)
				| ((packet[1] & 0xff) << 16)
				| ((packet[2] & 0xff) << 8)
				| (packet[3] & 0xff) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 認証情報を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 認証情報読込</p>
	 *
	 * @param reader	XDR読込
	 * @return 認証情報
	 */
	//--------------------------------------------------------------------------
	private static RpcCredential readCredential(XdrReader reader) {
		int flavor = reader.readInt() ;
		byte[] body = reader.readOpaque() ;
		return new RpcCredential( flavor, body) ;
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
	 * Credentialを取得します。<br><br>
	 *
	 * <p>メソッド名称： Credential取得</p>
	 *
	 * @return Credential
	 */
	//--------------------------------------------------------------------------
	public RpcCredential getCredential() {
		return credential ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Verifierを取得します。<br><br>
	 *
	 * <p>メソッド名称： Verifier取得</p>
	 *
	 * @return Verifier
	 */
	//--------------------------------------------------------------------------
	public RpcCredential getVerifier() {
		return verifier ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 引数を取得します。<br><br>
	 *
	 * <p>メソッド名称： 引数取得</p>
	 *
	 * @return 引数
	 */
	//--------------------------------------------------------------------------
	public XdrReader getArguments() {
		return arguments ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 引数バイト列を取得します。<br><br>
	 *
	 * <p>メソッド名称： 引数バイト列取得</p>
	 *
	 * @return 引数バイト列
	 */
	//--------------------------------------------------------------------------
	public byte[] getArgumentBytes() {
		return argumentBytes.clone() ;
	}
}
