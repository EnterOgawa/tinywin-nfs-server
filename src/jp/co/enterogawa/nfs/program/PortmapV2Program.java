package jp.co.enterogawa.nfs.program;

import java.io.IOException;

import jp.co.enterogawa.nfs.rpc.RpcCall;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.rpc.RpcProgram;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * Portmap Version 2プログラムクラスです。<br><br>
 *
 * <p>クラス名称： Portmap Version 2プログラム</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class PortmapV2Program implements RpcProgram {
	//	定数定義	------------------------------------------------------------
	/** Version */
	private static final int				VERSION = 2 ;

	/** PMAPPROC_NULL */
	private static final int				PROC_NULL = 0 ;

	/** PMAPPROC_GETPORT */
	private static final int				PROC_GETPORT = 3 ;

	/** PMAPPROC_DUMP */
	private static final int				PROC_DUMP = 4 ;

	//	内部定義	------------------------------------------------------------
	/** Portmapポート */
	private final int					portmapPort ;

	/** NFSポート */
	private final int					nfsPort ;

	/** MOUNTポート */
	private final int					mountPort ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param portmapPort	Portmapポート
	 * @param nfsPort		NFSポート
	 * @param mountPort		MOUNTポート
	 */
	//--------------------------------------------------------------------------
	public PortmapV2Program(int portmapPort, int nfsPort, int mountPort) {
		this.portmapPort = portmapPort ;
		this.nfsPort = nfsPort ;
		this.mountPort = mountPort ;
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
		return RpcConstants.PROGRAM_PORTMAP ;
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
	@Override
	public int getVersion() {
		return VERSION ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC呼出を処理します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出処理</p>
	 *
	 * @param call		RPC呼出
	 * @param response	応答
	 * @return ACCEPTステータス
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	@Override
	public int handle(RpcCall call, XdrWriter response) throws IOException {
		switch( call.getProcedure()) {
		case PROC_NULL:
			return RpcConstants.ACCEPT_SUCCESS ;

		case PROC_GETPORT:
			handleGetPort( call.getArguments(), response) ;
			return RpcConstants.ACCEPT_SUCCESS ;

		case PROC_DUMP:
			handleDump( response) ;
			return RpcConstants.ACCEPT_SUCCESS ;

		default:
			return RpcConstants.ACCEPT_PROC_UNAVAIL ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * GETPORTを処理します。<br><br>
	 *
	 * <p>メソッド名称： GETPORT処理</p>
	 *
	 * @param arguments	引数
	 * @param response	応答
	 */
	//--------------------------------------------------------------------------
	private void handleGetPort(XdrReader arguments, XdrWriter response) {
		int program = arguments.readInt() ;
		int version = arguments.readInt() ;
		int protocol = arguments.readInt() ;
		arguments.readInt() ;

		int port = 0 ;

		// TCP/UDP要求の場合
		if( protocol == RpcConstants.IPPROTO_UDP || protocol == RpcConstants.IPPROTO_TCP) {
			// NFS要求の場合
			if( program == RpcConstants.PROGRAM_NFS && (version == 2 || version == 3)) {
				port = nfsPort ;
			}
			// MOUNT要求の場合
			else if( program == RpcConstants.PROGRAM_MOUNT && (version == 1 || version == 2 || version == 3)) {
				port = mountPort ;
			}
			// Portmap v2要求の場合
			else if( program == RpcConstants.PROGRAM_PORTMAP && version == VERSION) {
				port = portmapPort ;
			}
		}

		System.out.println(
				"PORTMAP GETPORT requestProgram="
				+ program
				+ " requestVersion="
				+ version
				+ " protocol="
				+ protocol
				+ " resultPort="
				+ port) ;
		response.writeInt( port) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * DUMPを処理します。<br><br>
	 *
	 * <p>メソッド名称： DUMP処理</p>
	 *
	 * @param response	応答
	 */
	//--------------------------------------------------------------------------
	private void handleDump(XdrWriter response) {
		writeMapping( response, RpcConstants.PROGRAM_PORTMAP, VERSION, RpcConstants.IPPROTO_UDP, portmapPort) ;
		writeMapping( response, RpcConstants.PROGRAM_PORTMAP, VERSION, RpcConstants.IPPROTO_TCP, portmapPort) ;
		writeMapping( response, RpcConstants.PROGRAM_NFS, 2, RpcConstants.IPPROTO_UDP, nfsPort) ;
		writeMapping( response, RpcConstants.PROGRAM_NFS, 2, RpcConstants.IPPROTO_TCP, nfsPort) ;
		writeMapping( response, RpcConstants.PROGRAM_NFS, 3, RpcConstants.IPPROTO_UDP, nfsPort) ;
		writeMapping( response, RpcConstants.PROGRAM_NFS, 3, RpcConstants.IPPROTO_TCP, nfsPort) ;
		writeMapping( response, RpcConstants.PROGRAM_MOUNT, 1, RpcConstants.IPPROTO_UDP, mountPort) ;
		writeMapping( response, RpcConstants.PROGRAM_MOUNT, 1, RpcConstants.IPPROTO_TCP, mountPort) ;
		writeMapping( response, RpcConstants.PROGRAM_MOUNT, 2, RpcConstants.IPPROTO_UDP, mountPort) ;
		writeMapping( response, RpcConstants.PROGRAM_MOUNT, 2, RpcConstants.IPPROTO_TCP, mountPort) ;
		writeMapping( response, RpcConstants.PROGRAM_MOUNT, 3, RpcConstants.IPPROTO_UDP, mountPort) ;
		writeMapping( response, RpcConstants.PROGRAM_MOUNT, 3, RpcConstants.IPPROTO_TCP, mountPort) ;
		response.writeBoolean( false) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * マッピングを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： マッピング書込</p>
	 *
	 * @param response	応答
	 * @param program	Program
	 * @param version	Version
	 * @param protocol	Protocol
	 * @param port		Port
	 */
	//--------------------------------------------------------------------------
	private void writeMapping(XdrWriter response, int program, int version, int protocol, int port) {
		response.writeBoolean( true) ;
		response.writeInt( program) ;
		response.writeInt( version) ;
		response.writeInt( protocol) ;
		response.writeInt( port) ;
	}
}
