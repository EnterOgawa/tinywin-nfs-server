package jp.co.enterogawa.nfs.program;

//------------------------------------------------------------------------------
/**
 * NFSステータス定数クラスです。<br><br>
 *
 * <p>クラス名称： NFSステータス定数</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public final class NfsStatus {
	//	定数定義	------------------------------------------------------------
	/** 正常 */
	public static final int				OK = 0 ;

	/** 権限なし */
	public static final int				PERM = 1 ;

	/** 存在なし */
	public static final int				NOENT = 2 ;

	/** IO異常 */
	public static final int				IO = 5 ;

	/** アクセス拒否 */
	public static final int				ACCES = 13 ;

	/** 存在済み */
	public static final int				EXIST = 17 ;

	/** ディレクトリではない */
	public static final int				NOTDIR = 20 ;

	/** ディレクトリ */
	public static final int				ISDIR = 21 ;

	/** 不正値 */
	public static final int				INVAL = 22 ;

	/** 読込専用ファイルシステム */
	public static final int				ROFS = 30 ;

	/** 名前が長すぎる */
	public static final int				NAMETOOLONG = 63 ;

	/** ディレクトリが空ではない */
	public static final int				NOTEMPTY = 66 ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンス化を禁止します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	private NfsStatus() {
	}
}
