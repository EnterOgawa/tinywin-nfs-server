package jp.co.enterogawa.nfs.rpc;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * RPCプログラムのVersion別ルーターです。<br><br>
 *
 * <p>クラス名称： RPCプログラムルーター</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class RpcProgramRouter implements RpcProgram {
	//	内部定義	------------------------------------------------------------
	/** Program番号 */
	private final int					programNumber ;

	/** Version別プログラム */
	private final Map<Integer, RpcProgram> programs = new LinkedHashMap<Integer, RpcProgram>() ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param programNumber	Program番号
	 */
	//--------------------------------------------------------------------------
	public RpcProgramRouter(int programNumber) {
		this.programNumber = programNumber ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Programを追加します。<br><br>
	 *
	 * <p>メソッド名称： Program追加</p>
	 *
	 * @param program	Program
	 * @return このインスタンス
	 */
	//--------------------------------------------------------------------------
	public RpcProgramRouter add(RpcProgram program) {
		// Program番号が異なる場合
		if( program.getProgramNumber() != programNumber) {
			throw new IllegalArgumentException( "Program number mismatch.") ;
		}

		programs.put( program.getVersion(), program) ;
		return this ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Program番号を取得します。<br><br>
	 *
	 * <p>メソッド名称： Program番号取得</p>
	 *
	 * @return Program番号
	 */
	//--------------------------------------------------------------------------
	@Override
	public int getProgramNumber() {
		return programNumber ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Versionを取得します。<br><br>
	 *
	 * <p>メソッド名称： Version取得</p>
	 *
	 * @return 代表Version
	 */
	//--------------------------------------------------------------------------
	@Override
	public int getVersion() {
		return programs.keySet().iterator().next() ;
	}

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
	@Override
	public boolean supportsVersion(int version) {
		return programs.containsKey( version) ;
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
	@Override
	public int handle(RpcCall call, XdrWriter response) throws IOException {
		RpcProgram program = programs.get( call.getVersion()) ;

		// Versionが未対応の場合
		if( program == null) {
			return RpcConstants.ACCEPT_PROG_UNAVAIL ;
		}

		return program.handle( call, response) ;
	}
}
