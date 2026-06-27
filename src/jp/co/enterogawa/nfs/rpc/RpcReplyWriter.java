package jp.co.enterogawa.nfs.rpc;

import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * RPC応答書込クラスです。<br><br>
 *
 * <p>クラス名称： RPC応答書込</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class RpcReplyWriter {
	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を禁止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private RpcReplyWriter() {
	}

	//--------------------------------------------------------------------------
	/**
	 * ACCEPT応答を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： ACCEPT応答書込</p>
	 *
	 * @param xid			XID
	 * @param acceptStatus	ACCEPTステータス
	 * @param body			本文
	 * @return 応答データ
	 */
	//--------------------------------------------------------------------------
	public static byte[] accepted(int xid, int acceptStatus, byte[] body) {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( xid) ;
		writer.writeInt( RpcConstants.MSG_REPLY) ;
		writer.writeInt( RpcConstants.REPLY_STAT_ACCEPTED) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeInt( acceptStatus) ;

		// 本文がある場合
		if( body.length > 0) {
			writer.writeFixedOpaque( body) ;
		}

		return writer.toByteArray() ;
	}
}
