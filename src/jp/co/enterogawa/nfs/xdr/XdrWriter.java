package jp.co.enterogawa.nfs.xdr;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

//------------------------------------------------------------------------------
/**
 * XDR書込クラスです。<br><br>
 *
 * <p>クラス名称： XDR書込</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class XdrWriter {
	//	内部定義	------------------------------------------------------------
	/** 出力バッファ */
	private final ByteArrayOutputStream	output = new ByteArrayOutputStream() ;

	//--------------------------------------------------------------------------
	/**
	 * 32bit整数を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 32bit整数書込</p>
	 *
	 * @param value	32bit整数
	 */
	//--------------------------------------------------------------------------
	public void writeInt(int value) {
		output.write( (value >>> 24) & 0xff) ;
		output.write( (value >>> 16) & 0xff) ;
		output.write( (value >>> 8) & 0xff) ;
		output.write( value & 0xff) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 符号なし32bit整数を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 符号なし32bit整数書込</p>
	 *
	 * @param value	符号なし32bit整数
	 */
	//--------------------------------------------------------------------------
	public void writeUnsignedInt(long value) {
		writeInt( (int)(value & 0xffffffffL)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 64bit整数を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 64bit整数書込</p>
	 *
	 * @param value	64bit整数
	 */
	//--------------------------------------------------------------------------
	public void writeLong(long value) {
		writeUnsignedInt( value >>> 32) ;
		writeUnsignedInt( value) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * boolean値を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： boolean値書込</p>
	 *
	 * @param value	boolean値
	 */
	//--------------------------------------------------------------------------
	public void writeBoolean(boolean value) {
		writeInt( value ? 1 : 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 可変長バイト列を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 可変長バイト列書込</p>
	 *
	 * @param value	バイト列
	 */
	//--------------------------------------------------------------------------
	public void writeOpaque(byte[] value) {
		writeInt( value.length) ;
		writeFixedOpaque( value) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 固定長バイト列を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 固定長バイト列書込</p>
	 *
	 * @param value	バイト列
	 */
	//--------------------------------------------------------------------------
	public void writeFixedOpaque(byte[] value) {
		output.writeBytes( value) ;
		writePadding( value.length) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 文字列書込</p>
	 *
	 * @param value	文字列
	 */
	//--------------------------------------------------------------------------
	public void writeString(String value) {
		writeOpaque( value.getBytes( StandardCharsets.UTF_8)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 文字列書込</p>
	 *
	 * @param value		文字列
	 * @param charset	文字コード
	 */
	//--------------------------------------------------------------------------
	public void writeString(String value, Charset charset) {
		writeOpaque( value.getBytes( charset)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * バイト配列に変換します。<br><br>
	 *
	 * <p>メソッド名称： バイト配列変換</p>
	 *
	 * @return バイト配列
	 */
	//--------------------------------------------------------------------------
	public byte[] toByteArray() {
		return output.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パディングを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： パディング書込</p>
	 *
	 * @param length	実データ長
	 */
	//--------------------------------------------------------------------------
	private void writePadding(int length) {
		int padding = (4 - (length % 4)) % 4 ;

		// パディングバイト分繰り返す
		for( int i = 0; i < padding; i++) {
			output.write( 0) ;
		}
	}
}
