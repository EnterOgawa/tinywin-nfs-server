package jp.co.enterogawa.nfs.xdr;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.Arrays;

//------------------------------------------------------------------------------
/**
 * XDR読込クラスです。<br><br>
 *
 * <p>クラス名称： XDR読込</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class XdrReader {
	//	内部定義	------------------------------------------------------------
	/** 入力データ */
	private final byte[]				data ;

	/** 読込位置 */
	private int						position ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param data		入力データ
	 * @param length	有効長
	 */
	//--------------------------------------------------------------------------
	public XdrReader(byte[] data, int length) {
		this.data = Arrays.copyOf( data, length) ;
		position = 0 ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param data	入力データ
	 */
	//--------------------------------------------------------------------------
	public XdrReader(byte[] data) {
		this( data, data.length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 32bit整数を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 32bit整数読込</p>
	 *
	 * @return 32bit整数
	 */
	//--------------------------------------------------------------------------
	public int readInt() {
		require( 4) ;
		int value = ((data[position] & 0xff) << 24)
				| ((data[position + 1] & 0xff) << 16)
				| ((data[position + 2] & 0xff) << 8)
				| (data[position + 3] & 0xff) ;
		position += 4 ;
		return value ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 符号なし32bit整数を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 符号なし32bit整数読込</p>
	 *
	 * @return 符号なし32bit整数
	 */
	//--------------------------------------------------------------------------
	public long readUnsignedInt() {
		return Integer.toUnsignedLong( readInt()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 64bit整数を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 64bit整数読込</p>
	 *
	 * @return 64bit整数
	 */
	//--------------------------------------------------------------------------
	public long readLong() {
		long high = readUnsignedInt() ;
		long low = readUnsignedInt() ;
		return (high << 32) | low ;
	}

	//--------------------------------------------------------------------------
	/**
	 * boolean値を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： boolean値読込</p>
	 *
	 * @return boolean値
	 */
	//--------------------------------------------------------------------------
	public boolean readBoolean() {
		return readInt() != 0 ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 可変長バイト列を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 可変長バイト列読込</p>
	 *
	 * @return バイト列
	 */
	//--------------------------------------------------------------------------
	public byte[] readOpaque() {
		int length = readInt() ;
		if( length < 0) {
			throw new IllegalArgumentException( "XDR opaque length is invalid.") ;
		}
		byte[] value = readFixedOpaque( length) ;
		return value ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 固定長バイト列を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 固定長バイト列読込</p>
	 *
	 * @param length	長さ
	 * @return バイト列
	 */
	//--------------------------------------------------------------------------
	public byte[] readFixedOpaque(int length) {
		if( length < 0) {
			throw new IllegalArgumentException( "XDR opaque length is invalid.") ;
		}
		require( length) ;
		byte[] value = Arrays.copyOfRange( data, position, position + length) ;
		position += length ;
		skipPadding( length) ;
		return value ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パディングなし固定長バイト列を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： パディングなし固定長バイト列読込</p>
	 *
	 * @param length	長さ
	 * @return バイト列
	 */
	//--------------------------------------------------------------------------
	public byte[] readFixedOpaqueWithoutPadding(int length) {
		if( length < 0) {
			throw new IllegalArgumentException( "XDR opaque length is invalid.") ;
		}
		require( length) ;
		byte[] value = Arrays.copyOfRange( data, position, position + length) ;
		position += length ;
		return value ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 文字列読込</p>
	 *
	 * @return 文字列
	 */
	//--------------------------------------------------------------------------
	public String readString() {
		byte[] value = readOpaque() ;
		return new String( value, StandardCharsets.UTF_8) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 文字列読込</p>
	 *
	 * @param charset	文字コード
	 * @return 文字列
	 */
	//--------------------------------------------------------------------------
	public String readString(Charset charset) {
		byte[] value = readOpaque() ;
		return new String( value, charset) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 残データを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 残データ読込</p>
	 *
	 * @return 残データ
	 */
	//--------------------------------------------------------------------------
	public byte[] readRemaining() {
		byte[] value = Arrays.copyOfRange( data, position, data.length) ;
		position = data.length ;
		return value ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 読込位置を取得します。<br><br>
	 *
	 * <p>メソッド名称： 読込位置取得</p>
	 *
	 * @return 読込位置
	 */
	//--------------------------------------------------------------------------
	public int getPosition() {
		return position ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 読込位置を設定します。<br><br>
	 *
	 * <p>メソッド名称： 読込位置設定</p>
	 *
	 * @param position	読込位置
	 */
	//--------------------------------------------------------------------------
	public void setPosition(int position) {
		if( position < 0 || position > data.length) {
			throw new IllegalArgumentException( "XDR position is invalid.") ;
		}
		this.position = position ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 残データ長を取得します。<br><br>
	 *
	 * <p>メソッド名称： 残データ長取得</p>
	 *
	 * @return 残データ長
	 */
	//--------------------------------------------------------------------------
	public int remainingLength() {
		return data.length - position ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 残データ有無を確認します。<br><br>
	 *
	 * <p>メソッド名称： 残データ有無確認</p>
	 *
	 * @return true:残データあり false:残データなし
	 */
	//--------------------------------------------------------------------------
	public boolean hasRemaining() {
		return position < data.length ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パディングを読み飛ばします。<br><br>
	 *
	 * <p>メソッド名称： パディング読飛</p>
	 *
	 * @param length	実データ長
	 */
	//--------------------------------------------------------------------------
	private void skipPadding(int length) {
		int padding = (4 - (length % 4)) % 4 ;

		// パディングが存在する場合
		if( padding > 0) {
			require( padding) ;
			position += padding ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 必要データ長を確認します。<br><br>
	 *
	 * <p>メソッド名称： 必要データ長確認</p>
	 *
	 * @param length	必要長
	 */
	//--------------------------------------------------------------------------
	private void require(int length) {
		// データが不足する場合
		if( position + length > data.length) {
			throw new IllegalArgumentException( "XDR data is too short.") ;
		}
	}
}
