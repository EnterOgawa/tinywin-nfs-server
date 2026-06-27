package jp.co.enterogawa.nfs.rpc;

import jp.co.enterogawa.nfs.xdr.XdrReader;

//------------------------------------------------------------------------------
/**
 * RPC認証情報クラスです。<br><br>
 *
 * <p>クラス名称： RPC認証情報</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class RpcCredential {
	//	内部定義	------------------------------------------------------------
	/** 認証方式 */
	private final int					flavor ;

	/** 認証データ */
	private final byte[]				body ;

	/** AUTH_SYSホスト名 */
	private final String				authSysMachineName ;

	/** AUTH_SYS UID */
	private final Integer				authSysUid ;

	/** AUTH_SYS GID */
	private final Integer				authSysGid ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param flavor	認証方式
	 * @param body		認証データ
	 */
	//--------------------------------------------------------------------------
	public RpcCredential(int flavor, byte[] body) {
		this.flavor = flavor ;
		this.body = body.clone() ;
		AuthSysFields authSysFields = readAuthSysFields( flavor, this.body) ;
		authSysMachineName = authSysFields.machineName ;
		authSysUid = authSysFields.uid ;
		authSysGid = authSysFields.gid ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 認証方式を取得します。<br><br>
	 *
	 * <p>メソッド名称： 認証方式取得</p>
	 *
	 * @return 認証方式
	 */
	//--------------------------------------------------------------------------
	public int getFlavor() {
		return flavor ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 認証データを取得します。<br><br>
	 *
	 * <p>メソッド名称： 認証データ取得</p>
	 *
	 * @return 認証データ
	 */
	//--------------------------------------------------------------------------
	public byte[] getBody() {
		return body.clone() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS認証情報かどうかを取得します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS認証情報判定</p>
	 *
	 * @return true:AUTH_SYS false:その他
	 */
	//--------------------------------------------------------------------------
	public boolean isAuthSys() {
		return authSysUid != null && authSysGid != null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYSホスト名を取得します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYSホスト名取得</p>
	 *
	 * @return ホスト名
	 */
	//--------------------------------------------------------------------------
	public String getAuthSysMachineName() {
		return authSysMachineName ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS UIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS UID取得</p>
	 *
	 * @return UID
	 */
	//--------------------------------------------------------------------------
	public Integer getAuthSysUid() {
		return authSysUid ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS GIDを取得します。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS GID取得</p>
	 *
	 * @return GID
	 */
	//--------------------------------------------------------------------------
	public Integer getAuthSysGid() {
		return authSysGid ;
	}

	//--------------------------------------------------------------------------
	/**
	 * AUTH_SYS認証情報を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： AUTH_SYS認証情報読込</p>
	 *
	 * @param flavor	認証方式
	 * @param body		認証データ
	 * @return AUTH_SYSフィールド
	 */
	//--------------------------------------------------------------------------
	private AuthSysFields readAuthSysFields(int flavor, byte[] body) {
		// AUTH_SYS以外の場合
		if( flavor != RpcConstants.AUTH_SYS) {
			return AuthSysFields.EMPTY ;
		}

		try {
			XdrReader reader = new XdrReader( body) ;
			reader.readInt() ;
			String machineName = reader.readString() ;
			int uid = reader.readInt() ;
			int gid = reader.readInt() ;
			return new AuthSysFields( machineName, uid, gid) ;
		} catch( RuntimeException rex) {
			return AuthSysFields.EMPTY ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * AUTH_SYSフィールドクラスです。<br><br>
	 *
	 * <p>クラス名称： AUTH_SYSフィールド</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class AuthSysFields {
		//	定数定義	--------------------------------------------------------
		/** 空フィールド */
		private static final AuthSysFields	EMPTY = new AuthSysFields( null, null, null) ;

		//	内部定義	--------------------------------------------------------
		/** ホスト名 */
		private final String					machineName ;

		/** UID */
		private final Integer				uid ;

		/** GID */
		private final Integer				gid ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param machineName	ホスト名
		 * @param uid			UID
		 * @param gid			GID
		 */
		//----------------------------------------------------------------------
		AuthSysFields(String machineName, Integer uid, Integer gid) {
			this.machineName = machineName ;
			this.uid = uid ;
			this.gid = gid ;
		}
	}
}
