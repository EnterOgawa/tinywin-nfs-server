package jp.co.enterogawa.nfs.config;

import java.nio.file.Path;
import java.util.List;

//------------------------------------------------------------------------------
/**
 * NFS公開定義クラスです。<br><br>
 *
 * <p>クラス名称： NFS公開定義</p>
 *
 * <p>著作権： Copyright (c) 2026 EnterOgawa. All Rights Reserved.</p>
 *
 * <p>更新履歴：
 * <pre>
 *     VerNo.         author        update     comment
 *     Ver.01.00.00   Shunji Ogawa  2026/09/02 新規作成
 * </pre>
 * </p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class NfsExport {
	//	内部定義	------------------------------------------------------------
	/** 公開名 */
	private final String					name ;

	/** 公開パス */
	private final Path					path ;

	/** 公開書込可否 */
	private final boolean				writable ;

	/** 許可クライアント */
	private final List<String>			allowedClients ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param name	公開名
	 * @param path	公開パス
	 * @param writable	書込可否
	 */
	//--------------------------------------------------------------------------
	public NfsExport( String name, Path path, boolean writable) {
		this( name, path, writable, List.of()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param name	公開名
	 * @param path	公開パス
	 * @param writable	書込可否
	 * @param allowedClients	許可クライアント
	 */
	//--------------------------------------------------------------------------
	public NfsExport( String name, Path path, boolean writable, List<String> allowedClients) {
		this.name = name == null ? "" : name.trim() ;
		this.path = path.toAbsolutePath().normalize() ;
		this.writable = writable ;
		this.allowedClients = allowedClients == null ? List.of() : List.copyOf( allowedClients) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開名を取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開名取得</p>
	 *
	 * @return	公開名
	 */
	//--------------------------------------------------------------------------
	public String getName() {
		return name ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開パスを取得します。<br><br>
	 *
	 * <p>メソッド名称： 公開パス取得</p>
	 *
	 * @return	公開パス
	 */
	//--------------------------------------------------------------------------
	public Path getPath() {
		return path ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 書込可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： 書込可否取得</p>
	 *
	 * @return	判定結果<ul>
	 * 			false	: 読込専用<br>
	 * 			true	: 書込可</ul>
	 */
	//--------------------------------------------------------------------------
	public boolean isWritable() {
		return writable ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 許可クライアントを取得します。<br><br>
	 *
	 * <p>メソッド名称： 許可クライアント取得</p>
	 *
	 * @return	許可クライアント
	 */
	//--------------------------------------------------------------------------
	public List<String> getAllowedClients() {
		return allowedClients ;
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアント許可可否を取得します。<br><br>
	 *
	 * <p>メソッド名称： クライアント許可可否取得</p>
	 *
	 * @param clientAddress	クライアントアドレス
	 * @return	判定結果<ul>
	 * 			false	: 拒否<br>
	 * 			true	: 許可</ul>
	 */
	//--------------------------------------------------------------------------
	public boolean allowsClient( String clientAddress) {
		// 許可クライアントが未設定の場合
		if( allowedClients.isEmpty()) {
			return true ;
		}

		return allowedClients.contains( clientAddress) ;
	}
}
