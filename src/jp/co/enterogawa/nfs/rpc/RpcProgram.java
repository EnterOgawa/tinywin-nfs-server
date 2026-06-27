package jp.co.enterogawa.nfs.rpc;

import java.io.IOException;

import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * RPCプログラムインターフェースです。<br><br>
 *
 * <p>クラス名称： RPCプログラム</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public interface RpcProgram {
	//--------------------------------------------------------------------------
	/**
	 * Program番号を取得します。<br><br>
	 *
	 * <p>メソッド名称： Program番号取得</p>
	 *
	 * @return Program番号
	 */
	//--------------------------------------------------------------------------
	int getProgramNumber() ;

	//--------------------------------------------------------------------------
	/**
	 * Versionを取得します。<br><br>
	 *
	 * <p>メソッド名称： Version取得</p>
	 *
	 * @return Version
	 */
	//--------------------------------------------------------------------------
	int getVersion() ;

	//--------------------------------------------------------------------------
	/**
	 * Version対応可否を確認します。<br><br>
	 *
	 * <p>メソッド名称： Version対応可否確認</p>
	 *
	 * @param version	Version
	 * @return true:対応 false:未対応
	 */
	//--------------------------------------------------------------------------
	default boolean supportsVersion(int version) {
		return getVersion() == version ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出処理</p>
	 *
	 * @param call		RPC呼出
	 * @param response	応答本文
	 * @return ACCEPTステータス
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	int handle(RpcCall call, XdrWriter response) throws IOException ;

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出処理</p>
	 *
	 * @param call		RPC呼出
	 * @param context	要求コンテキスト
	 * @param response	応答本文
	 * @return ACCEPTステータス
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	default int handle(RpcCall call, RpcRequestContext context, XdrWriter response) throws IOException {
		RpcRequestContext previous = RpcRequestContext.setCurrent( context) ;

		try {
			return handle( call, response) ;
		} finally {
			RpcRequestContext.restore( previous) ;
		}
	}
}
