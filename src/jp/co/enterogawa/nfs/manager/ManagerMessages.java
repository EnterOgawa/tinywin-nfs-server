package jp.co.enterogawa.nfs.manager;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

//------------------------------------------------------------------------------
/**
 * 管理ツール表示メッセージクラスです。<br><br>
 *
 * <p>クラス名称： 管理ツール表示メッセージ</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
class ManagerMessages {
	//	定数定義	------------------------------------------------------------
	/** リソース名 */
	private static final String			BUNDLE_NAME = "jp.co.enterogawa.nfs.manager.messages" ;

	/** 英語リソース */
	private static final ResourceBundle	FALLBACK_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME, Locale.ENGLISH) ;

	//	内部定義	------------------------------------------------------------
	/** 言語コード */
	private final String					languageCode ;

	/** リソース */
	private final ResourceBundle			bundle ;

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param languageCode	言語コード
	 * @param bundle		リソース
	 */
	//--------------------------------------------------------------------------
	private ManagerMessages(String languageCode, ResourceBundle bundle) {
		this.languageCode = languageCode ;
		this.bundle = bundle ;
	}

	//--------------------------------------------------------------------------
	/**
	 * メッセージを読み込みます。<br><br>
	 *
	 * <p>メソッド名称： メッセージ読込</p>
	 *
	 * @param configuredLanguage	設定言語
	 * @return メッセージ
	 */
	//--------------------------------------------------------------------------
	static ManagerMessages load(String configuredLanguage) {
		String language = normalizeLanguage( configuredLanguage) ;
		Locale locale = "auto".equals( language) ? Locale.getDefault() : Locale.forLanguageTag( language) ;
		ResourceBundle resourceBundle = ResourceBundle.getBundle( BUNDLE_NAME, locale) ;
		return new ManagerMessages( language, resourceBundle) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 言語コードを取得します。<br><br>
	 *
	 * <p>メソッド名称： 言語コード取得</p>
	 *
	 * @return 言語コード
	 */
	//--------------------------------------------------------------------------
	String getLanguageCode() {
		return languageCode ;
	}

	//--------------------------------------------------------------------------
	/**
	 * メッセージを取得します。<br><br>
	 *
	 * <p>メソッド名称： メッセージ取得</p>
	 *
	 * @param key	キー
	 * @return メッセージ
	 */
	//--------------------------------------------------------------------------
	String text(String key) {
		try {
			return bundle.getString( key) ;
		} catch( MissingResourceException mrex) {
			try {
				return FALLBACK_BUNDLE.getString( key) ;
			} catch( MissingResourceException fallbackEx) {
				return key ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * メッセージを書式化します。<br><br>
	 *
	 * <p>メソッド名称： メッセージ書式化</p>
	 *
	 * @param key		キー
	 * @param arguments	引数
	 * @return メッセージ
	 */
	//--------------------------------------------------------------------------
	String format(String key, Object... arguments) {
		return MessageFormat.format( text( key), arguments) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 言語コードを正規化します。<br><br>
	 *
	 * <p>メソッド名称： 言語コード正規化</p>
	 *
	 * @param language	言語コード
	 * @return 言語コード
	 */
	//--------------------------------------------------------------------------
	static String normalizeLanguage(String language) {
		// 設定がない場合
		if( language == null || language.isBlank()) {
			return "auto" ;
		}

		String normalized = language.trim().toLowerCase( Locale.ROOT) ;

		// 日本語の場合
		if( normalized.equals( "ja") || normalized.startsWith( "ja-" ) || normalized.startsWith( "ja_" )) {
			return "ja" ;
		}

		// 英語の場合
		if( normalized.equals( "en") || normalized.startsWith( "en-" ) || normalized.startsWith( "en_" )) {
			return "en" ;
		}

		return "auto" ;
	}
}
