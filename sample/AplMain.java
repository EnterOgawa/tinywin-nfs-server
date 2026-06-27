package jp.co.daifuku.jcsim2;

import java.io.BufferedWriter;
import java.io.File ;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map ;
import java.util.Properties;

import org.apache.commons.io.FileUtils ;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.osgi.framework.BundleContext;

import jp.co.daifuku.jcsim2.communication.bez.ComBez;
import jp.co.daifuku.jcsim2.communication.bez.ComBezVirtual;
import jp.co.daifuku.jcsim2.communication.cet.ComCet;
import jp.co.daifuku.jcsim2.communication.cet.ComCetVirtual;
import jp.co.daifuku.jcsim2.communication.clw.ComClw;
import jp.co.daifuku.jcsim2.communication.clw.ComClwVirtual;
import jp.co.daifuku.jcsim2.communication.iot.ComIot;
import jp.co.daifuku.jcsim2.communication.iot.ComIotVirtual;
import jp.co.daifuku.jcsim2.communication.loadtester.ComLoadtester;
import jp.co.daifuku.jcsim2.communication.loadtester.ComLoadtesterVirtual;
import jp.co.daifuku.jcsim2.communication.mcp.ComMcp;
import jp.co.daifuku.jcsim2.communication.mcp.ComMcpVirtual;
import jp.co.daifuku.jcsim2.communication.mos.ComMos;
import jp.co.daifuku.jcsim2.communication.mos.ComMosVirtual;
import jp.co.daifuku.jcsim2.communication.opc.ComOpc;
import jp.co.daifuku.jcsim2.communication.opc.ComOpcVirtual;
import jp.co.daifuku.jcsim2.communication.stk.ComStk;
import jp.co.daifuku.jcsim2.communication.stk.ComStkVirtual;
import jp.co.daifuku.jcsim2.communication.vlfsafety.ComVlfSafety;
import jp.co.daifuku.jcsim2.communication.vlfsafety.ComVlfSafetyVirtual;
import jp.co.daifuku.jcsim2.communication.zcs.ComZcs;
import jp.co.daifuku.jcsim2.communication.zcs.ComZcsVirtual;
import jp.co.daifuku.jcsim2.communication.zcu4.ComZcu4;
import jp.co.daifuku.jcsim2.communication.zcu4.ComZcu4Virtual;
import jp.co.daifuku.jcsim2.data.GlobalDefine;
import jp.co.daifuku.jcsim2.data.file.SimFiles;
import jp.co.daifuku.jcsim2.data.file.project.Workspace;
import jp.co.daifuku.jcsim2.hmi.ApplicationActionBarAdvisor;
import jp.co.daifuku.jcsim2.hmi.GlobalColor;
import jp.co.daifuku.jcsim2.hmi.GlobalFont;
import jp.co.daifuku.jcsim2.hmi.GlobalImages;
import jp.co.daifuku.jcsim2.hmi.perspective.monitor.MonitorPerspective;
import jp.co.daifuku.jcsim2.hmi.perspective.monitor.views.layout.LayoutView;
import jp.co.daifuku.jcsim2.hmi.perspective.monitor.views.message.MessageView;
import jp.co.daifuku.jcsim2.hmi.util.IRefreshControl;
import jp.co.daifuku.jcsim2.hmi.util.RefreshControl;
import jp.co.daifuku.jcsim2.log.Sim2SocketLogger;
import jp.co.entersystem.commons.graphics.util.GraphicsResources;
import jp.co.entersystem.commons.log.communication.CommunicationLogData ;
import jp.co.entersystem.commons.log.communication.ICommunicationLogListener ;
import jp.co.entersystem.commons.log.logback.message.MessageLogger;
import jp.co.entersystem.commons.log.message.IMessageLogListener;
import jp.co.entersystem.commons.log.message.MessageLogData ;
import jp.co.entersystem.commons.net.gateway.Gateway ;
import jp.co.entersystem.commons.util.DuplicateStartCheck;
import jp.co.entersystem.commons.util.FileUtil;
import jp.co.entersystem.commons.util.LightHashMap ;
import jp.co.entersystem.commons.util.Messages ;
import jp.co.entersystem.jni.WinSystem ;
import jp.co.entersystem.rcp_common.data.usermanagement.GroupData;
import jp.co.entersystem.rcp_common.data.usermanagement.UserData;
import jp.co.entersystem.rcp_common.event.ViewEvent ;
import jp.co.entersystem.rcp_common.util.RcpUtil ;
import jp.co.entersystem.rcp_common.views.socketlog.SocketLogView;

//------------------------------------------------------------------------------
/**
 * プラグインメインクラスです。<br><br>
 *
 * <p>クラス名称： プラグインメインクラス</p>
 *
 * <p>著作権： Copyright (c) 2021-2025 EnterSystem CO.,LTD. All Rights Reserved.</p>
 *
 * <p>更新履歴：
 * <pre>
 *     VerNo.         author        update     comment
 *     Ver.01.00.00   Shunji Ogawa  2021/06/21 新規作成
 *     Ver.01.01.00   Shunji Ogawa  2021/12/07 車輪交換Robot対応
 *     Ver.01.02.00   Shunji Ogawa  2022/03/31 MCP通信ポート設定変更
 *     Ver.01.03.00   Shunji Ogawa  2022/06/16 レイアウト背景色を指定可能とした
 *     Ver.01.04.00   Shunji Ogawa  2022/09/01 FastRecovery対応
 *     Ver.01.05.00   Shunji Ogawa  2023/02/07 指定番地のステータス報告を送信しない機能追加
 *     Ver.01.06.00   Shunji Ogawa  2023/04/13 台車復帰アイコン追加
 *     Ver.01.07.00   Shunji Ogawa  2023/08/17 レイアウト切り替え対応
 *     Ver.01.08.00   Shunji Ogawa  2023/09/04 追加レイアウト対応
 *     Ver.01.09.00   Shunji Ogawa  2023/09/20 旧レイアウト接続対応
 *     Ver.01.09.01   Shunji Ogawa  2023/10/26 アクティブなレイアウトビューのみ表示するようにした
 *     Ver.01.10.00   Shunji Ogawa  2024/03/11 ダークモード対応
 *     Ver.01.11.00   Shunji Ogawa  2024/08/29 CET対応
 *     Ver.01.12.00   Shunji Ogawa  2025/02/03 実機台車ZCU4対応
 *     Ver.01.13.00   Shunji Ogawa  2025/10/23 ZCS対応
 * </pre>
 * </p>
 * @author Shunji Ogawa
 * @version 01.13.00
 */
//------------------------------------------------------------------------------
public class AplMain extends jp.co.entersystem.rcp_common.AplMain implements ICommunicationLogListener, IMessageLogListener {
	//	定数定義	------------------------------------------------------------
	/**	プラグインID */
	public static final String				ID = "jp.co.daifuku.jcsim2" ;

	/**	プラグイン共有インスタンス */
	protected static AplMain				plugin  ;

	/** デバッグ区分 */
	public static final boolean				DEBUG = Boolean.parseBoolean( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.DEBUG, GlobalSystemProperties.DEBUG_DEFAULT)) ;

	/**	連続メッセージ表示間隔(sec) */
	private static int						MESSAGE_INTERVAL = 5 ;
	
	/** ドライバ有効区分 */
	private static boolean					DRIVER_ENABLED = false ;

	/** CETデバッグ区分<ul>
	 * 		false	: デバッグ無し<br>
	 * 		true	: デバッグあり</ul>
	 */
	public static final boolean				DEBUG_CET = false ;

	//	内部定義（標準）	----------------------------------------------------
	/** 完全制御プロジェクト情報 <br>
	 * MOS間通信やOPCSim間通信などを含めた完全な制御を行うプロジェクト情報。<br><br>
	 */
	private ProjectInfo						allControlProject ;

	/** プロジェクト情報マップ */
	private Map< String, ProjectInfo>		projectInfoMap = new LightHashMap< String, ProjectInfo>() ;
	
	/** 選択プロジェクト情報 */
	private ProjectInfo						projectInfoSelect ;
	
	/** 最終フォーカス時間<br>
	 * フォーカス切り替えが頻繁に行われる問題に対処する為、最後に切り替わった時間を取得しておき閾値未満の時間で
	 * 切り替わったフォーカスは無視する動作を可能とする。<br><br>
	 **/
	private long							lastLayoutViewFocusTime ;

	/** システム情報更新処理管理マップ<ul>
	 * 		String			: 処理ID(ViewID or ClassName)<br>
	 * 		RefreshControl	: 表示更新処理</ul>
	 */
	private Map<String,RefreshControl>		systemRefreshMap = new HashMap<String,RefreshControl>() ;

	/** メッセージログ */
	private MessageLogger					loggerMessage ;  

	/** シミュレータ間通信ログ */
	private Sim2SocketLogger				sim2Logger ;
	
	/** 異常メッセージ表示時間<br>
	 * 連続でメッセージが表示されると操作が出来ない為、メッセージボックスを閉じた後しばらくは開かないようにする。為に利用。<br><br>
	 */
	private long							lastErrorMessageDisplayTime ;
	
	/** メッセージビュー */
	private MessageView						messageView ;

	/** バージョンファイル */
	private File							versionFile ;
	
	//--------------------------------------------------------------------------
	/**
	 * プラグインクラスの初期化を行います。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	public AplMain() {
		// DLL初期化処理
		initDll() ;

		// 利用CPUコアの設定
		String property = RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.CPU_AFFINITY, GlobalSystemProperties.CPU_AFFINITY_DEFAULT) ;
		// 指定ありの時
		if( !property.equals( GlobalSystemProperties.CPU_AFFINITY_DEFAULT)) {
			WinSystem.SetProcessAffinity( Long.parseUnsignedLong( property.substring(2), 16)) ;
		}
		
//		// ダークモードの時
//		if( Display.isSystemDarkTheme()) {
//			System.out.println( "dark") ;
//		}
//		else {
//			System.out.println( "light") ;
//		}

		// 重複起動確認
		try {
			// ロックファイル作成と取得
			final FileOutputStream lock = DuplicateStartCheck.lock( SimFiles.FILE_LOCK) ;
			if( lock == null) {
				MessageDialog.openError( Display.getDefault().getActiveShell(),
						Messages.getString( this, "messageBoxTitle"),
						Messages.getString( this, "messageBoxMessage1")) ;
				System.exit(0) ;
			}

			// ロック開放処理
			Runtime.getRuntime().addShutdownHook( new Thread( () -> {
				DuplicateStartCheck.unlock( lock) ;
			})) ;
		} catch( Exception ex) {} ;

		// Widget確認用
//		Display.getDefault().addFilter(SWT.MouseHover, new Listener() {
//			public void handleEvent(Event event) {
//				System.out.println(event);
//			}
//		});
	}

    //--------------------------------------------------------------------------
	/**
	 * プラグイン開始処理を行います。<br><br>
	 *
	 * <p>メソッド名称： プラグイン開始処理</p>
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	//--------------------------------------------------------------------------
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this ;
	}

	//--------------------------------------------------------------------------
	/**
	 * システム情報更新処理の追加を行います。<br>
	 * 追加後自動的に開始される。<br><br>
	 *
	 * <p>メソッド名称： システム情報更新処理の追加</p>
	 *
	 * @param id			処理ID(ViewID or ClassName)
	 * @param refreshCycle	表示更新周期
	 * @param listener		リスナ
	 */
	//--------------------------------------------------------------------------
	public void addSystemRefresh( String id, int refreshCycle, IRefreshControl listener) {
		// 既に登録済みの場合は停止する
		RefreshControl refreshControl = systemRefreshMap.get(id) ;
		if( refreshControl != null) {
			refreshControl.stop() ;
			systemRefreshMap.remove( id) ;
		}
		
		// 新規に作成
		refreshControl = new RefreshControl( id, refreshCycle, Thread.MIN_PRIORITY, listener) ;
		
		// 登録
		systemRefreshMap.put( id, refreshControl) ;
		
		// 更新処理の開始
		refreshControl.start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * アプリケーション終了処理を行います。<br>
	 * この処理は、全てのウインドウが閉じられた状態での最後の処理を行います。<br>
	 * 動作しているスレッドの停止や、状態の保存などに利用してください。<br><br>
	 *
	 * <p>メソッド名称： アプリケーション終了処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplExit()
	 */
	//--------------------------------------------------------------------------
	protected void aplExit() {
		// シミュレータ間通信ログ停止
		sim2Logger.stop() ;

		// 高速周期の終了
		WinSystem.EndHighPeriod() ;

		// ドライバ停止
		if( DRIVER_ENABLED)
			WinSystem.DisposeDriver() ;

		super.aplExit() ;
		
		// ゲートウェイ停止
		Gateway.getInstance().stop() ;
		
		// メッセージログ停止
		loggerMessage.stop() ;

		// WinSystem終了処理
		WinSystem.Dispose() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * アプリケーション初期化処理を行います。<br>
	 * この処理は、内部で動作させる各スレッドなどの起動・ログの初期化・モジュールの生成
	 * などを行う場合に利用してください。<br>
	 * この時点では、画面表示に関係する処理は実行しないでください。<br><br>
	 *
	 * <p>メソッド名称： アプリケーション初期化処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplInitial()
	 */
	//--------------------------------------------------------------------------
	public void aplInitial() throws Exception {
		// プライオリティーの変更
		WinSystem.SetProcessPriority( WinSystem.GetCurrentProcess(), WinSystem.PROCESS_PRIO_REALTIME) ;

		// ドライバのロード
		if( DRIVER_ENABLED)
			WinSystem.initDriver64() ;
		
		// OSの分解能を上げる
		WinSystem.StartHighPeriod() ;

		// ログ初期化処理
		logInitial() ;

		// ディレクトリの初期化
		File[]	listFiles = getStateLocation().toFile().listFiles() ;
		if( listFiles != null) {
			for( File file : listFiles) {
				if( file.isDirectory()) {
					// dataフォルダは消さない
					if( file.getName().indexOf( SimFiles.DIR_DATA) != -1) {
						continue ;
					}

					// logフォルダは消さない
					if( file.getName().indexOf( SimFiles.DIR_LOG) != -1) {
						continue ;
					}
				}

				FileUtils.forceDelete( file) ;
			}
		}

		// 作業用ディレクトリの作成
		new File( RcpUtil.getDefaultSavePath() + SimFiles.DIR_TEMP).mkdir() ;
		
		// バージョンログ
		MessageLogger.getInstance().setLog( MessageLogData.CATEGORY_INFORMATION, getWindowTitle() + " " + getVersion()) ;

		// ワークスペースの初期化
		initWorkspace() ;
		
		// ゲートウェイの初期化
		initGateway() ;
		
		// バージョン情報の初期化
		initVersionInfo() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログインダイアログ表示を行います。<br>
	 * ログイン処理が必要な場合、この処理でログインダイアログの生成を行い、設定が
	 * 行われた後に、この処理を抜けるようにしてください。<br><br>
	 *
	 * <p>メソッド名称： ログインダイアログ表示</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplLogin()
	 */
	//--------------------------------------------------------------------------
	public boolean aplLogin() {
		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * アプリケーションウィンドウクローズ時処理を行います。<br>
	 * 終了時にウインドウがクローズされる直前に行う処理が必要な場合に利用してください。<br><br>
	 *
	 * <p>メソッド名称： アプリケーションウィンドウクローズ時処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplWindowClose(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
	 * @param configurer
	 */
	//--------------------------------------------------------------------------
	public void aplWindowClose( IWorkbenchWindowConfigurer configurer) {

		// 終了メッセージ
		MessageLogger.getInstance().setLog(
				MessageLogData.CATEGORY_INFORMATION,
				NLS.bind( Messages.getString( this, "systemstop"), AplMain.getLogin().getName())) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * アプリケーションウィンドウ作成時処理を行います。<br>
	 * 開始時のウィンドウの初期化(aplWindowInitial)が完了した後、表示される前の処理が必要な場合に利用してください。<br><br>
	 *
	 * <p>メソッド名称： アプリケーションウィンドウ作成時処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplWindowCreate(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
	 * @param configurer
	 */
	//--------------------------------------------------------------------------
	public void aplWindowCreate( IWorkbenchWindowConfigurer configurer) {
		try {
			// 全てのレイアウトビューを閉じる
			closeLayoutView() ;

			// 初期表示パースペクティブを設定
			RcpUtil.showPerspective( AplMain.getDefault().getInitialDisplayPerspectiveId()) ;
		} catch (WorkbenchException wex) {
			AplMain.errorLog( wex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * アプリケーションウィンドウ初期化時処理を行います。<br>
	 * 進捗表示などが必要な場合の、初期化処理に利用してください。<br><br>
	 *
	 * <p>メソッド名称： アプリケーションウィンドウ初期化処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplWindowInitial(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
	 * @param configurer
	 */
	//--------------------------------------------------------------------------
	public void aplWindowInitial( IWorkbenchWindowConfigurer configurer) {
		// パースペクティブバー無効
		configurer.setShowPerspectiveBar( false) ;
		
		// 現在がダークモードの時
		if( RcpUtil.getActiveTheme().indexOf( "dark") != -1) {
			AplMain.setDarkMode( true) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * アプリケーションウィンドウ表示時処理を行います。<br>
	 * メインウィンドウが表示が完了した時点での処理が必要な場合に利用してください。<br><br>
	 *
	 * <p>メソッド名称： アプリケーションウィンドウ表示時処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplWindowOpen(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
	 * @param configurer
	 */
	//--------------------------------------------------------------------------
	public void aplWindowOpen( IWorkbenchWindowConfigurer configurer) {
		// ログ通知設定
		loggerMessage.addListener( this) ;

		// ログインメッセージ
		MessageLogger.getInstance().setLog(
				MessageLogData.CATEGORY_INFORMATION,
				Messages.getString( this, "systemstart")) ;
	}
	
	//--------------------------------------------------------------------------
	/**
	 * ワークベンチウィンドウの終了前処理を行います。<br>
	 * 閉じるボタンでのみ呼び出される為、閉じるボタンでの確認に使用する。<br><br>
	 *
	 * <p>メソッド名称： ワークベンチウィンドウ終了前処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#aplWindowPreClose(org.eclipse.ui.application.IWorkbenchWindowConfigurer)
	 * @param configurer
	 */
	//--------------------------------------------------------------------------
	public boolean aplWindowPreClose( IWorkbenchWindowConfigurer configurer) {
		// 更新処理を停止
		removeAllSystemRefresh() ;
		
		// 実行中プロジェクトを全て停止
		Map<String, ProjectInfo>	projectInfoMap = AplMain.getDefault().getProjectInfoMap() ;
		projectInfoMap.forEach(( k, v) -> v.stop()) ;

		IWorkbench				workbench = PlatformUI.getWorkbench()  ;
		IWorkbenchWindow		activeWorkbenchWindow = workbench.getActiveWorkbenchWindow()  ;
		IWorkbenchPage			workbenchPage = activeWorkbenchWindow.getActivePage() ;
		
		IPerspectiveRegistry	registry =workbench.getPerspectiveRegistry() ;

		// 次の起動時にMonitorPerspectiveを必ず表示させる為の処理
		workbenchPage.setPerspective( registry.findPerspectiveWithId( getInitialDisplayPerspectiveId()));

		// メッセージログ通知の終了
		loggerMessage.removeListener( this) ;

		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 全てのレイアウトビューを閉じる処理を行います。<br><br>
	 *
	 * <p>メソッド名称： 全てのレイアウトビューを閉じる処理</p>
	 *
	 */
	//--------------------------------------------------------------------------
	public static void closeLayoutView() {
		IWorkbenchPage 		page = RcpUtil.getActiveWorkbenchPage() ;
		if( page == null) {
			return ;
		}

		// ビューの情報取得
		IViewReference[]	references = page.getViewReferences() ;
		for (int i = 0; i < references.length; i++) {
			// Eclipseビューは初期化しない事とする
			if( references[i].getId().indexOf( "eclipse") != -1) {
				continue ;
			}

			String id = references[i].getId()  ;

			for( int j = 1; j <= GlobalDefine.LAYOUT_MAX; j++) {
				if( id.indexOf( ".layout" + j) != -1) {
					LayoutView view = ( LayoutView)RcpUtil.getView(id) ;
					if( view == null) {
						// ビューを非表示にする
						page.hideView( references[i]) ;
						break ;
					}
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 例外ログの設定を行います。<br>
	 * 連続的に発生した場合、始めのログから5秒間はログを取得しません。<br>
	 * このようにすることで連続的発生を止める手段をユーザーに与えます。<br><br>
	 *
	 * <p>メソッド名称： 例外ログの設定</p>
	 *
	 * @param throwable		例外
	 */
	//--------------------------------------------------------------------------
	public static void errorLog( Throwable throwable) {
		AplMain.errorLog( throwable.getMessage(), throwable) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 例外ログの設定を行います。<br>
	 * 連続的に発生した場合、始めのログから5秒間はログを取得しません。<br>
	 * このようにすることで連続的発生を止める手段をユーザーに与えます。<br><br>
	 *
	 * <p>メソッド名称： 例外ログの設定</p>
	 *
	 * @param message		メッセージ
	 * @param throwable		例外
	 */
	//--------------------------------------------------------------------------
	public static void errorLog( String message, Throwable throwable) {
		final AplMain aplMain = getDefault() ;
		// エラーメッセージ表示中の時
		if( System.currentTimeMillis() - aplMain.lastErrorMessageDisplayTime < ( MESSAGE_INTERVAL * 1000)) {
			return ;
		}

		Display.getDefault().syncExec( () -> {
			// ログ設定
			getDefault().getLog().log( new Status( Status.ERROR, AplMain.ID, message, throwable)) ;
	
			// 閉じた時の時間取得
			aplMain.lastErrorMessageDisplayTime = System.currentTimeMillis() ;
		}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * メニュー・ツールバー初期化オブジェクト取得処理を行います。<br>
	 * メニュー・ツールバーに関しては、アプリケーションによる変更が多い為、ユーザー管理で
	 * 生成する事とします。よって、生成されたオブジェクトをこの関数により取得します。<br><br>
	 *
	 * <p>メソッド名称： メニュー・ツールバー初期化オブジェクト取得処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#getActionBarAdvisor(org.eclipse.ui.application.IActionBarConfigurer)
	 */
	//--------------------------------------------------------------------------
	public ActionBarAdvisor getActionBarAdvisor( IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor( configurer) ;
	}

    //--------------------------------------------------------------------------
	/**
	 * 完全制御プロジェクトの取得を行います。<br><br>
	 *
	 * <p>メソッド名称： 完全制御プロジェクトの取得</p>
	 *
	 * @return	完全制御プロジェクト
	 */
	//--------------------------------------------------------------------------
	public ProjectInfo getAllControlProject() {
		return allControlProject ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ワークベンチウィンドウの初期化オブジェクト取得処理を行います。<br>
	 * ワークベンチウィンドウの初期化に関しては、アプリケーションによる変更が多い為、ユーザー管理で
	 * 生成する事とします。よって、生成されたオブジェクトをこの関数により取得します。<br><br>
	 *
	 * <p>メソッド名称： ワークベンチウィンドウの初期化オブジェクト取得処理</p>
	 * 
	 * @param configurer	ワークベンチウィンドウ設定
	 * @return	ワークベンチウィンドウの初期化オブジェクト
	 */
	//--------------------------------------------------------------------------
	protected ApplicationWorkbenchWindowAdvisor getApplicationWorkbenchWindowAdvisor( IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor( configurer) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * プラグインインスタンス取得を行います。<br><br>
	 *
	 * <p>メソッド名称： プラグインインスタンス取得</p>
	 * 
	 * @return	プラグインインスタンス
	 */
	//--------------------------------------------------------------------------
	public static synchronized AplMain getDefault() {
		return plugin ;
	}

	//--------------------------------------------------------------------------
	/**
	 * プラグインID取得処理を行います。<br><br>
	 *
	 * <p>メソッド名称： プラグインID取得処理</p>
	 * 
	 * @return	プラグインID
	 */
	//--------------------------------------------------------------------------
	public String getId() {
		return ID ;
	}

	//--------------------------------------------------------------------------
	/**
	 * イメージハンドル取得を行います。<br><br>
	 *
	 * <p>メソッド名称： イメージハンドル取得</p>
	 * 
	 * @param path	イメージパス（icon/***.gifのように指定）
	 * @return	イメージハンドル
	 */
	//--------------------------------------------------------------------------
	public ImageDescriptor getImageDescriptorFromPlugin(String path) {
		return ResourceLocator.imageDescriptorFromBundle( ID, ICONS_PATH + path).get() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 初期表示パースペクティブ取得処理を行います。<br><br>
	 *
	 * <p>メソッド名称： 初期表示パースペクティブ取得処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#getInitialDisplayPerspectiveId()
	 */
	//--------------------------------------------------------------------------
	public String getInitialDisplayPerspectiveId() {
		return MonitorPerspective.ID ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 初期表示ビュー取得処理を行います。<br><br>
	 *
	 * <p>メソッド名称： 初期表示ビュー取得処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#getInitialDisplayViewId()
	 */
	//--------------------------------------------------------------------------
	public String getInitialDisplayViewId() {
		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 最終フォーカス時間の設定を行います。t<br><br>
	 *
	 * <p>メソッド名称： 最終フォーカス時間の設定</p>
	 *
	 * @return	最終フォーカス時間
	 */
	//--------------------------------------------------------------------------
	public long getLastLayoutViewFocusTime() {
		return lastLayoutViewFocusTime ;
	}

	//--------------------------------------------------------------------------
	/**
	 * プロジェクト情報の取得を行います。<br><br>
	 *
	 * <p>メソッド名称： プロジェクト情報の取得</p>
	 *
	 * @param layoutId	レイアウトID
	 * @return	プロジェクト情報
	 */
	//--------------------------------------------------------------------------
	public ProjectInfo getProjectInfo( String layoutId) {
		return projectInfoMap.get( layoutId) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * プロジェクト情報管理マップの取得を行います。<br><br>
	 *
	 * <p>メソッド名称： プロジェクト情報管理マップの取得</p>
	 *
	 * @return	プロジェクト情報管理マップ
	 */
	//--------------------------------------------------------------------------
	public Map<String, ProjectInfo> getProjectInfoMap() {
		return projectInfoMap ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 作業中プロジェクト情報の取得を行います。<br><br>
	 *
	 * <p>メソッド名称： 作業中プロジェクト情報の取得</p>
	 *
	 * @return	選択中プロジェクト情報
	 */
	//--------------------------------------------------------------------------
	public ProjectInfo getProjectInfoSelect() {
		return projectInfoSelect ;
	}

	//--------------------------------------------------------------------------
	/**
	 * バージョン番号の取得を行います。<br><br>
	 *
	 * <p>メソッド名称： バージョン番号の取得</p>
	 *
	 * @return	バージョン番号
	 */
	//--------------------------------------------------------------------------
	public String getVersion() {
		String versionText = "" ;
		try {
			String[]	version = RcpUtil.getProductVersion().split("\\.") ;
			versionText = String.format( "%02d.%02d.%02d",
											Integer.parseInt( version[0]),
											Integer.parseInt( version[1]),
											Integer.parseInt( version[2])) ;
		} catch (IOException e) {
		}

		return versionText ;
	}
	
	//--------------------------------------------------------------------------
	/**
	 * バージョンファイルの取得を行います。<br><br>
	 *
	 * <p>メソッド名称： バージョンファイルの取得</p>
	 *
	 * @return	バージョンファイル
	 */
	//--------------------------------------------------------------------------
	public File getVersionFile() {
		return versionFile ;
	}
	
	//--------------------------------------------------------------------------
	/**
	 * ユーザー区分の確認を行います。<br><br>
	 *
	 * <p>メソッド名称： ユーザー区分の確認</p>
	 *
	 * @return	ユーザー区分<ul>
	 * 				CATEGORY_ADMINISTRATOR	: 管理者<br>
	 * 				CATEGORY_OPERATOR		: オペレーター<br>
	 * 				CATEGORY_MONITOR		: モニター</ul>
	 * @see GroupData
	 */
	//--------------------------------------------------------------------------
	public int getUserGroupCategory() {
		UserData	user = (UserData) AplMain.getLogin().getData() ;
		return user.getGroupData().getCategory() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ウィンドウタイトル取得処理を行います。<br><br>
	 *
	 * <p>メソッド名称： ウィンドウタイトル取得処理</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#getWindowTitle()
	 */
	//--------------------------------------------------------------------------
	public String getWindowTitle() {
		return Messages.getString( this, "title") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 背景色の初期化を行います。<br><br>
	 *
	 * <p>メソッド名称： 背景色の初期化</p>
	 * 
	 * @param colorRegistry	色レジストリ
	 */
	//--------------------------------------------------------------------------
	protected void initializeBackgroundColor( ColorRegistry colorRegistry) {
		// ダークモード背景色設定
		if( isDarkMode()) {
			String[]	color = RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.LAYOUT_BACKGROUND_DARK_COLOR, GlobalSystemProperties.LAYOUT_BACKGROUND_DARK_COLOR_DEFAULT).split(",") ;
			colorRegistry.put( GlobalColor.LAYOUT_BACKGROUND_DARK_COLOR,	new RGB( Integer.decode( color[0]), Integer.decode( color[1]), Integer.decode( color[2]))) ;
			GraphicsResources.setBackgroundColor( getColor( GlobalColor.LAYOUT_BACKGROUND_DARK_COLOR));
		}
		// 通常モード背景色設定
		else {
			String[]	color = RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.LAYOUT_BACKGROUND_COLOR, GlobalSystemProperties.LAYOUT_BACKGROUND_COLOR_DEFAULT).split(",") ;
			colorRegistry.put( GlobalColor.LAYOUT_BACKGROUND_COLOR,	new RGB( Integer.decode( color[0]), Integer.decode( color[1]), Integer.decode( color[2]))) ;
			GraphicsResources.setBackgroundColor( getColor( GlobalColor.LAYOUT_BACKGROUND_COLOR));
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 色登録を行います。<br>
	 * 標準以外の色が必要な場合は、この処理にて追加します。<br><br>
	 *
	 * <p>メソッド名称： 色登録</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#initializeUserColorRegistry(org.eclipse.jface.resource.ColorRegistry)
	 */
	//--------------------------------------------------------------------------
	public void initializeUserColorRegistry( ColorRegistry registry) {
		//	レイアウト表示関連	------------------------------------------------
		// 番地色
		registry.put( GlobalColor.LAYOUT_ITEM_ADDRESS, 					GraphicsResources.getForegroundColor().getRGB()) ;
		// レール色
		registry.put( GlobalColor.LAYOUT_ITEM_RAIL, 					GraphicsResources.getForegroundColor().getRGB()) ;
		// レール色（インターフェースエリア）
		registry.put( GlobalColor.LAYOUT_ITEM_RAIL_INTERFACE, 			new RGB( 0xC0, 0x00, 0xC0)) ;
		// レール色（存在しない番地）
		registry.put( GlobalColor.LAYOUT_ITEM_RAIL_CHANGEDPOSITION,		new RGB( 0xFF, 0x00, 0x00)) ;
		// ルート色
		registry.put( GlobalColor.LAYOUT_ITEM_ROUTE, 					new RGB( 0xFF, 0x00, 0x00)) ;
		// HIDレール色
		registry.put( GlobalColor.LAYOUT_ITEM_HID_RAIL, 				new RGB( 0xFA, 0x80, 0x72)) ;
		// 二重化HIDレール色
		registry.put( GlobalColor.LAYOUT_ITEM_DUPLEXED_HID_RAIL, 		new RGB( 0xDA, 0x70, 0xD6)) ;
		// 選択色
		registry.put( GlobalColor.LAYOUT_ITEM_SELECTED, 				new RGB( 0x00, 0xC0, 0x00)) ;
		// ステーション色
		registry.put( GlobalColor.LAYOUT_ITEM_STATION, 					GraphicsResources.getForegroundColor().getRGB()) ;
		// ステーションラベル色
		registry.put( GlobalColor.LAYOUT_ITEM_STATION_LABEL, 			new RGB( 0xC0, 0x00, 0x00)) ;
		// ステーション状態（通常）
		registry.put( GlobalColor.LAYOUT_ITEM_STSTATE_NORMAL,			new RGB( 0xFF, 0xFF, 0xC0)) ;
		// ライトガイド（通常）
		registry.put( GlobalColor.LAYOUT_ITEM_LG, 						new RGB( 0xFF, 0x8D, 0x8D)) ;
		// ライトガイド（高速通過）
		registry.put( GlobalColor.LAYOUT_ITEM_LG_HIGHSPEED, 			new RGB( 0x8D, 0xFF, 0x8D)) ;
		// センサー
		registry.put( GlobalColor.LAYOUT_ITEM_SENSOR, 					new RGB( 0xC0, 0xC0, 0xFF)) ;
		// AD
		registry.put( GlobalColor.LAYOUT_ITEM_AD, 						new RGB( 0xFF, 0xCC, 0xCC)) ;
		// FD
		registry.put( GlobalColor.LAYOUT_ITEM_FD, 						new RGB( 0xFF, 0xCC, 0xCC)) ;
		// HID
		registry.put( GlobalColor.LAYOUT_ITEM_HID, 						new RGB( 0xFF, 0xFF, 0x00)) ;
		// TB棚
		registry.put( GlobalColor.LAYOUT_ITEM_TBSHELF, 					new RGB( 0xC0, 0xFF, 0xC0)) ;
		// TB棚枠
		registry.put( GlobalColor.LAYOUT_ITEM_TBSHELF_FRAME, 			GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;
		// ラベル色
		registry.put( GlobalColor.LAYOUT_LABEL, 						GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;
		// CADラベル文字色
		registry.put( GlobalColor.LAYOUT_LABEL_CAD, 					new RGB( 0x80, 0x80, 0xFF)) ;
		// CADラベル（旧のみ）文字色
		registry.put( GlobalColor.LAYOUT_LABEL_CAD_OLD, 				new RGB( 0xCA, 0x4D, 0x41)) ;
		// CADラベル（新旧）文字色
		registry.put( GlobalColor.LAYOUT_LABEL_CAD_NEWOLD, 				new RGB( 0x3A, 0xC1, 0x45)) ;
		// 台車ラベル文字色
		registry.put( GlobalColor.LAYOUT_LABEL_CLW, 					GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;
		// ライトガイドラベル文字色
		registry.put( GlobalColor.LAYOUT_LABEL_LG, 						GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;
		// センサーラベル文字色
		registry.put( GlobalColor.LAYOUT_LABEL_SENSOR, 					GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;
		// TBラベル文字色
		registry.put( GlobalColor.LAYOUT_LABEL_TB, 						GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;
		// MLラベル文字色
		registry.put( GlobalColor.LAYOUT_LABEL_ML, 						GraphicsResources.getModeColor( new RGB( 0x60, 0x60, 0x60))) ;

		//	台車表示関連	----------------------------------------------------
		// 台車色
		registry.put( GlobalColor.LAYOUT_ITEM_CLW, 						GraphicsResources.getForegroundColor().getRGB()) ;
		// 台車色
		registry.put( GlobalColor.LAYOUT_ITEM_CLW_LOAD, 				new RGB( 0x01, 0x01, 0x01)) ;
		// 台車状態色（通信異常）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_COMERROR,		new RGB( 0xFF, 0x33, 0xFF)) ;
		// 台車状態色（手動）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_MAINTENANCE,	new RGB( 0x33, 0x66, 0xFF)) ;
		// 台車状態色（異常）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_ERROR,			new RGB( 0xFF, 0x00, 0x00)) ;
		// 台車状態色（位置確認）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_POSITIONCHECK,	new RGB( 0xCC, 0x66, 0x33)) ;
		// 台車状態色（追突防止待機中）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_HITSTOP,		new RGB( 0xCC, 0xFF, 0x33)) ;
		// 台車状態色（渋滞）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_CONGESTION,		new RGB( 0xFF, 0xFF, 0x00)) ;
		// 台車状態色（停滞）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_STAGNATION,		new RGB( 0xFF, 0x99, 0x33)) ;
		// 台車状態色（BZ-STOP）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_BZSTOP,			new RGB( 0xCC, 0xCC, 0x33)) ;
		// 台車状態色（移動中）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_MOVE,			new RGB( 0x00, 0xFF, 0x00)) ;
		// 台車状態色（掬い/卸しサイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_TRANSFERING,	new RGB( 0x00, 0xFF, 0xFF)) ;
		// 台車状態色（抜き取りサイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_OUT,			new RGB( 0x99, 0x99, 0x99)) ;
		// 台車状態色（走行制御番地移動サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_MOVE_ADDRESS,	new RGB( 0xC0, 0xC0, 0xff)) ;
		// 台車状態色（階間移動サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_MOVE_FLOORS,	new RGB( 0x75, 0xB6, 0xA5)) ;
		// 台車状態色（吸引口移動サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_MOVE_SUCTIONHOLE,	new RGB( 0x80, 0x00, 0x80)) ;
		// 台車状態色（棟間移動サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_MOVE_BUILDINGS,	new RGB( 0xFF, 0x80, 0x80)) ;
		// 台車状態色（洗車サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_WASH,			new RGB( 0xB9, 0xD1, 0xEA)) ;
		// 台車状態色（車輪交換サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_REPLACE_WHEELS,	new RGB( 0x5B, 0x97, 0xEF)) ;
		// 台車状態色（作業なし）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_IDLE,			new RGB( 0x00, 0xCC, 0x00)) ;
		// 台車状態色（移載タイムアウト）
		registry.put( GlobalColor.LAYOUT_ITEM_CLWSTATUS_TRANSFERTIMEOUT,	new RGB( 0xFF, 0x14, 0x93)) ;

		//	TB表示関連	----------------------------------------------------
		// TB台車色
		registry.put( GlobalColor.LAYOUT_ITEM_TB, 						GraphicsResources.getForegroundColor().getRGB()) ;
		// TB台車状態色（通信異常）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_COMERROR,		new RGB( 0xFF, 0x33, 0xFF)) ;
		// TB台車状態色（手動）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_MAINTENANCE,		new RGB( 0x33, 0x66, 0xFF)) ;
		// TB台車状態色（異常）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_ERROR,			new RGB( 0xFF, 0x00, 0x00)) ;
		// TB台車状態色（PMモード）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_PMMODE,			new RGB( 0xCC, 0x66, 0x33)) ;
		// TB台車状態色（走行待機中）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_WAIT,			new RGB( 0xFF, 0xFF, 0x00)) ;
		// TB台車状態色（退避走行中）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_EVADE,			new RGB( 0xCC, 0xCC, 0x33)) ;
		// TB台車状態色（追突防止待機中）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_HITSTOP,			new RGB( 0xCC, 0xFF, 0x33)) ;
		// TB台車状態色（移動中）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_MOVE,			new RGB( 0x00, 0xFF, 0x00)) ;
		// TB台車状態色（掬い/卸しサイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_TRANSFERING,		new RGB( 0x00, 0xFF, 0xFF)) ;
		// TB台車状態色（作業無し）
		registry.put( GlobalColor.LAYOUT_ITEM_TBSTATUS_IDLE,			new RGB( 0x00, 0xCC, 0x00)) ;

		//	ML表示関連	----------------------------------------------------
		// ML色
		registry.put( GlobalColor.LAYOUT_ITEM_ML, 						GraphicsResources.getForegroundColor().getRGB()) ;
		// ML状態色（通信異常）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_COMERROR,		new RGB( 0xFF, 0x33, 0xFF)) ;
		// ML状態色（手動）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_MAINTENANCE,		new RGB( 0x33, 0x66, 0xFF)) ;
		// ML状態色（異常）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_ERROR,			new RGB( 0xFF, 0x00, 0x00)) ;
		// ML状態色（位置確認）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_POSITIONCHECK,	new RGB( 0xCC, 0x66, 0x33)) ;
		// ML状態色（追突防止待機中）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_HITSTOP,			new RGB( 0xCC, 0xFF, 0x33)) ;
		// ML状態色（渋滞）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_CONGESTION,		new RGB( 0xFF, 0xFF, 0x00)) ;
		// ML状態色（停滞）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_STAGNATION,		new RGB( 0xFF, 0x99, 0x33)) ;
		// ML状態色（BZ-STOP）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_BZSTOP,			new RGB( 0xCC, 0xCC, 0x33)) ;
		// ML状態色（移動中）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_MOVE,			new RGB( 0x00, 0xFF, 0x00)) ;
		// ML状態色（掬い/卸しサイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_TRANSFERING,		new RGB( 0x00, 0xFF, 0xFF)) ;
		// ML状態色（走行制御番地移動サイクル）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_MOVE_ADDRESS,	new RGB( 0xC0, 0xC0, 0xff)) ;
		// ML状態色（作業なし）
		registry.put( GlobalColor.LAYOUT_ITEM_MLSTATUS_IDLE,			new RGB( 0x00, 0xCC, 0x00)) ;
		
		
		//	サムネイル関連	----------------------------------------------------
		// 領域色
		registry.put( GlobalColor.THUMBNAIL_AREA, 						new RGB( 0xFF, 0x00, 0x00)) ;
		//	デバッグ関連	----------------------------------------------------
		// 信号色
		registry.put( GlobalColor.DEBUG_SIGNAL, 						GraphicsResources.getModeColor( new RGB( 0x75, 0xF7, 0x78))) ;
		
		//　メッセージログ関連	------------------------------------------------
		// 状態色（情報）
		registry.put( GlobalColor.MESSAGEVIEW_INFO, 					new RGB( 0x00, 0x00, 0x00)) ;
		// 状態色（警告）
		registry.put( GlobalColor.MESSAGEVIEW_WARNING, 					new RGB( 0xFF, 0x00, 0xFF)) ;
		// 状態色（異常）
		registry.put( GlobalColor.MESSAGEVIEW_ERROR, 					new RGB( 0xFF, 0x00, 0x00)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * フォント登録を行います。<br>
	 * 標準以外のフォントが必要な場合は、この処理にて追加します。<br><br>
	 *
	 * <p>メソッド名称： フォント登録</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#initializeUserFontRegistry(org.eclipse.jface.resource.FontRegistry)
	 */
	//--------------------------------------------------------------------------
	public void initializeUserFontRegistry( FontRegistry registry) {
		// RCPフォントレジストリ登録
		registry.put( GlobalFont.COMMON_BORDER,
				new FontData[] { new FontData( "MS UI Gothic", 9, SWT.BOLD)}) ;
		registry.put( GlobalFont.LAYOUT_DEFAULT,
				new FontData[] { new FontData( "MS UI Gothic", 9, SWT.NORMAL)}) ;
		registry.put( GlobalFont.DEBUG_SIGNALNAME,
				new FontData[] { new FontData( "MS UI Gothic", 9, SWT.NORMAL)}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * イメージ登録を行います。<br>
	 * 標準以外のイメージが必要な場合は、この処理にて追加します。<br><br>
	 *
	 * <p>メソッド名称： イメージ登録</p>
	 * 
	 * @see jp.co.entersystem.rcp_common.AplMain#initializeUserImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	//--------------------------------------------------------------------------
	public void initializeUserImageRegistry( ImageRegistry registry) {
		// アプリケーションイメージ作成
		registry.put( GlobalImages.SYSTEM_APPLICATION, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_APPLICATION + ".png")) ;
		// バージョンダイアログイメージ作成
		registry.put( GlobalImages.SYSTEM_VERSION_DIALOG, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_VERSION_DIALOG + ".png")) ;
		// ダークモードイメージ作成
		registry.put( GlobalImages.SYSTEM_DARK_MODE, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_DARK_MODE + ".png")) ;

		//	ワークスペースビュー関連	----------------------------------------
		// クライアントトイメージ作成
		registry.put( GlobalImages.WORKSPACE_CLIENT, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_CLIENT + ".gif")) ;
		// 新規クライアントトイメージ作成
		registry.put( GlobalImages.WORKSPACE_NEW_CLIENT, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_NEW_CLIENT + ".gif")) ;
		// プロジェクトトイメージ作成
		registry.put( GlobalImages.WORKSPACE_PROJECT, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_PROJECT + ".gif")) ;
		// 新規プロジェクトトイメージ作成
		registry.put( GlobalImages.WORKSPACE_NEW_PROJECT, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_NEW_PROJECT + ".gif")) ;
		// ラインイメージ作成
		registry.put( GlobalImages.WORKSPACE_LINE, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_LINE + ".gif")) ;
		// 新規ラインメージ作成
		registry.put( GlobalImages.WORKSPACE_NEW_LINE, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_NEW_LINE + ".gif")) ;
		// バージョンイメージ作成
		registry.put( GlobalImages.WORKSPACE_VERSION, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_VERSION + ".gif")) ;
		// バージョンイメージ作成
		registry.put( GlobalImages.WORKSPACE_VERSION_SNAPSHOT, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_VERSION_SNAPSHOT + ".gif")) ;
		// 新規バージョンイメージ作成
		registry.put( GlobalImages.WORKSPACE_NEW_VERSION, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_NEW_VERSION + ".gif")) ;
		// 設定イメージ作成
		registry.put( GlobalImages.WORKSPACE_CONFIG, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_CONFIG + ".gif")) ;
		// バージョン情報エクスポートイメージ作成
		registry.put( GlobalImages.WORKSPACE_EXPORT_VERSION, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_EXPORT_VERSION + ".gif")) ;
		// バージョン情報インポートイメージ作成
		registry.put( GlobalImages.WORKSPACE_IMPORT_VERSION, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_IMPORT_VERSION + ".gif")) ;
		// レイアウト再読み込みイメージ作成
		registry.put( GlobalImages.WORKSPACE_LAYOUT_RELOAD, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_LAYOUT_RELOAD + ".png")) ;
		// コピーイメージ作成
		registry.put( GlobalImages.COPY, getImageDescriptorFromPlugin( GlobalImages.COPY + ".png")) ;
		// 貼り付けイメージ作成
		registry.put( GlobalImages.PASTE, getImageDescriptorFromPlugin( GlobalImages.PASTE + ".gif")) ;
		// 名称を変更イメージ作成
		registry.put( GlobalImages.RENAME, getImageDescriptorFromPlugin( GlobalImages.RENAME + ".gif")) ;
		// 3DViewイメージ作成
		registry.put( GlobalImages.WORKSPACE_3DVIEW, getImageDescriptorFromPlugin( GlobalImages.WORKSPACE_3DVIEW + ".png")) ;

		//	パースペクティブ関連	--------------------------------------------
		// パースペクティブグループイメージ作成
		registry.put( GlobalImages.PERSPECTIVE_GROUP, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_GROUP + ".gif")) ;
		// モニタリングイメージ作成
		registry.put( GlobalImages.PERSPECTIVE_MONITORING, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_MONITORING + ".gif")) ;
		// デバッグイメージ作成
		registry.put( GlobalImages.PERSPECTIVE_DEBUG, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_DEBUG + ".gif")) ;
		// テストイメージ作成
		registry.put( GlobalImages.PERSPECTIVE_TEST, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_TEST + ".png")) ;
		// 異常設定イメージ作成
		registry.put( GlobalImages.PERSPECTIVE_ERROR, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_ERROR + ".gif")) ;
		// 履歴イメージ作成
		registry.put( GlobalImages.PERSPECTIVE_HISTORY, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_HISTORY + ".gif")) ;

		//	システム状態ビュー関連	--------------------------------------------
		// 開始イメージ作成
		registry.put( GlobalImages.SYSTEM_START, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_START + ".gif")) ;
		// 停止イメージ作成
		registry.put( GlobalImages.SYSTEM_STOP, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_STOP + ".gif")) ;
		// 中断イメージ作成
		registry.put( GlobalImages.SYSTEM_PAUSE, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_PAUSE + ".gif")) ;
		// 再開イメージ作成
		registry.put( GlobalImages.SYSTEM_RESUME, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_RESUME + ".gif")) ;
		// 再開（スナップショット）イメージ作成
		registry.put( GlobalImages.SYSTEM_RESUME_SNAPSHOT, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_RESUME_SNAPSHOT + ".gif")) ;
		// 棟間接続開始イメージ作成
		registry.put( GlobalImages.SYSTEM_START_JOINBUILDINGS, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_START_JOINBUILDINGS + ".gif")) ;
		// 旧レイアウト接続開始イメージ作成
		registry.put( GlobalImages.SYSTEM_START_JOINOLDLAYOUT, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_START_JOINOLDLAYOUT + ".gif")) ;
		// CPU Timeイメージ作成
		registry.put( GlobalImages.SYSTEM_CPUTIME, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_CPUTIME + ".gif")) ;
		// Memoryイメージ作成
		registry.put( GlobalImages.SYSTEM_MEMORY, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_MEMORY + ".gif")) ;
		// サーバー選択イメージ作成
		registry.put( GlobalImages.SYSTEM_SELECT_SERVER, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_SELECT_SERVER + ".png")) ;
		// サーバー設定イメージ作成
		registry.put( GlobalImages.SYSTEM_SETTING_SERVER, getImageDescriptorFromPlugin( GlobalImages.SYSTEM_SETTING_SERVER + ".gif")) ;

		//	レイアウトビュー関連	--------------------------------------------
		// マーカーイメージ作成
		registry.put( GlobalImages.LAYOUT_MARKER, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_MARKER + ".png")) ;
		// MTL作成
		registry.put( GlobalImages.LAYOUT_MTL, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_MTL + ".gif")) ;
		// MTL分岐番地作成
		registry.put( GlobalImages.LAYOUT_MTL_OUT, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_VHL_ENTER_EXIT + ".png")) ;
		// 番地表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_ADDRESS, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_ADDRESS + ".gif")) ;
		// ステーション表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_STATION, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_STATION + ".gif")) ;
		// CAD情報表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_CAD, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_CAD + ".gif")) ;
		// ZCU情報表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_ZCU, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_ZCU + ".gif")) ;
		// HID情報表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_HID, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_HID + ".gif")) ;
		// TB情報表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_TB, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_TB + ".gif")) ;
		// 全画面表示作成
		registry.put( GlobalImages.LAYOUT_ALL, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_ALL + ".gif")) ;
		// 拡大作成
		registry.put( GlobalImages.LAYOUT_ZOOMIN, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_ZOOMIN + ".gif")) ;
		// 縮小作成
		registry.put( GlobalImages.LAYOUT_ZOOMOUT, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_ZOOMOUT + ".gif")) ;
		// 基準サイズ作成
		registry.put( GlobalImages.LAYOUT_DEFAULT_SIZE, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DEFAULT_SIZE + ".gif")) ;
		// TB卸し作成
		registry.put( GlobalImages.LAYOUT_TB_DEPOSIT, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_TB_DEPOSIT + ".gif")) ;
		// TB掬い作成
		registry.put( GlobalImages.LAYOUT_TB_ACQUIRE, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_TB_ACQUIRE + ".png")) ;
		// レイヤー表示設定（重ねて表示）作成
		registry.put( GlobalImages.LAYOUT_LAYER, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_LAYER + ".png")) ;
		// レイヤー表示設定（重ねて表示）作成
		registry.put( GlobalImages.LAYOUT_LAYER_OVERLAY, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_LAYER_OVERLAY + ".png")) ;
		// レイヤー表示設定（左右に並べて表示）作成
		registry.put( GlobalImages.LAYOUT_LAYER_HORIZONTAL, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_LAYER_HORIZONTAL + ".png")) ;
		// レイヤー表示設定（上下に並べて表示）作成
		registry.put( GlobalImages.LAYOUT_LAYER_VERTICAL, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_LAYER_VERTICAL + ".png")) ;
		// VLF作成
		registry.put( GlobalImages.LAYOUT_VLF, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_VLF + ".gif")) ;
		// ISU作成
		registry.put( GlobalImages.LAYOUT_ISU, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_ISU + ".gif")) ;
		// OLF作成
		registry.put( GlobalImages.LAYOUT_OLF, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_OLF + ".gif")) ;
		// 中継ポイント作成
		registry.put( GlobalImages.LAYOUT_RLY, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_RLY + ".gif")) ;
		// 中継ポイント作成（退出）
		registry.put( GlobalImages.LAYOUT_RLY_OUT, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_RLY_OUT + ".gif")) ;
		// 中継ポイント作成（進入）
		registry.put( GlobalImages.LAYOUT_RLY_IN, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_RLY_IN + ".gif")) ;
		// 異常メッセージ表示作成
		registry.put( GlobalImages.LAYOUT_DISPLAY_ERROR, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_DISPLAY_ERROR + ".gif")) ;
		// CLL作成
		registry.put( GlobalImages.LAYOUT_CLL, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_CLL + ".png")) ;
		// 台車入り口作成
		registry.put( GlobalImages.LAYOUT_VHL_ENTER, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_VHL_ENTER + ".png")) ;
		// 台車出口作成
		registry.put( GlobalImages.LAYOUT_VHL_EXIT, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_VHL_EXIT + ".png")) ;
		// 台車出入口作成
		registry.put( GlobalImages.LAYOUT_VHL_ENTER_EXIT, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_VHL_ENTER_EXIT + ".png")) ;
		// ZCU4作成
		registry.put( GlobalImages.LAYOUT_ZCU4, getImageDescriptorFromPlugin( GlobalImages.LAYOUT_ZCU4 + ".png")) ;

		//	旧シミュレータ情報関連	--------------------------------------------
		// ノード作成
		registry.put( GlobalImages.OLDSIM_NODE, getImageDescriptorFromPlugin( GlobalImages.OLDSIM_NODE + ".gif")) ;
		// グループ作成
		registry.put( GlobalImages.OLDSIM_GROUP, getImageDescriptorFromPlugin( GlobalImages.OLDSIM_GROUP + ".gif")) ;
		// パラメータ作成
		registry.put( GlobalImages.OLDSIM_PARAM, getImageDescriptorFromPlugin( GlobalImages.OLDSIM_PARAM + ".gif")) ;
		//	番地ビュー関連	----------------------------------------------------
		// 番地作成
		registry.put( GlobalImages.ADDRESS_SHAPE, getImageDescriptorFromPlugin( GlobalImages.ADDRESS_SHAPE + ".gif")) ;

		//	ステーションビュー関連	----------------------------------------------------
		// ステーション作成
		registry.put( GlobalImages.STATION_SHAPE, getImageDescriptorFromPlugin( GlobalImages.STATION_SHAPE + ".gif")) ;

		//	台車ビュー関連	----------------------------------------------------
		// 台車作成
		registry.put( GlobalImages.CLW_SHAPE, getImageDescriptorFromPlugin( GlobalImages.CLW_SHAPE + ".gif")) ;
		// 未投入台車作成
		registry.put( GlobalImages.CLW_NOTIN, getImageDescriptorFromPlugin( GlobalImages.CLW_NOTIN + ".gif")) ;
		// 投入済み台車作成
		registry.put( GlobalImages.CLW_NORMAL, getImageDescriptorFromPlugin( GlobalImages.CLW_NORMAL + ".gif")) ;
		// 異常台車作成
		registry.put( GlobalImages.CLW_ERROR, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR + ".gif")) ;
		// 警告台車作成
		registry.put( GlobalImages.CLW_WARNING, getImageDescriptorFromPlugin( GlobalImages.CLW_WARNING + ".gif")) ;
		// 台車投入作成
		registry.put( GlobalImages.CLW_IN, getImageDescriptorFromPlugin( GlobalImages.CLW_IN + ".gif")) ;
		// 全台車投入作成
		registry.put( GlobalImages.CLW_IN_ALL, getImageDescriptorFromPlugin( GlobalImages.CLW_IN_ALL + ".gif")) ;
		// 全台車抜き取り作成
		registry.put( GlobalImages.CLW_OUT_ALL, getImageDescriptorFromPlugin( GlobalImages.CLW_OUT_ALL + ".gif")) ;
		// 起動OFF台車抜き取り作成
		registry.put( GlobalImages.CLW_OUT_OFFLINE, getImageDescriptorFromPlugin( GlobalImages.CLW_OUT_OFFLINE + ".gif")) ;
		// 台車抜き取り作成
		registry.put( GlobalImages.CLW_OUT, getImageDescriptorFromPlugin( GlobalImages.CLW_OUT + ".gif")) ;
		// 台車設定作成
		registry.put( GlobalImages.CLW_OPERATION, getImageDescriptorFromPlugin( GlobalImages.CLW_OPERATION + ".gif")) ;
		// 走行経路表示作成
		registry.put( GlobalImages.CLW_ROUTE, getImageDescriptorFromPlugin( GlobalImages.CLW_ROUTE + ".gif")) ;
		// 走行経路開始作成
		registry.put( GlobalImages.CLW_ROUTE_START, getImageDescriptorFromPlugin( GlobalImages.CLW_ROUTE_START + ".png")) ;
		// 走行経路終了作成
		registry.put( GlobalImages.CLW_ROUTE_END, getImageDescriptorFromPlugin( GlobalImages.CLW_ROUTE_END + ".png")) ;
		// 台車ランダム投入作成
		registry.put( GlobalImages.CLW_IN_RANDOM, getImageDescriptorFromPlugin( GlobalImages.CLW_IN_RANDOM + ".gif")) ;
		// 台車MCP情報投入作成
		registry.put( GlobalImages.CLW_IN_MCPINFO, getImageDescriptorFromPlugin( GlobalImages.CLW_IN_MCPINFO + ".gif")) ;
		// デバッグ追加作成
		registry.put( GlobalImages.CLW_DEBUG_ADD, getImageDescriptorFromPlugin( GlobalImages.CLW_DEBUG_ADD + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.CLW_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.CLW_DISCONNECT + ".gif")) ;
		// タイムアウト作成
		registry.put( GlobalImages.CLW_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.CLW_TIMEOUT + ".gif")) ;
		// デフォルトマップバージョン設定
		registry.put( GlobalImages.CLW_DEFAULT_MAPVERSION_SETTING, getImageDescriptorFromPlugin( GlobalImages.CLW_DEFAULT_MAPVERSION_SETTING + ".gif")) ;
		// 作業指示NG応答設定
		registry.put( GlobalImages.CLW_DIRECTION_NGREPLY_SETTING, getImageDescriptorFromPlugin( GlobalImages.CLW_DIRECTION_NGREPLY_SETTING + ".gif")) ;
		// 台車待ち行列
		registry.put( GlobalImages.CLW_QUEUE, getImageDescriptorFromPlugin( GlobalImages.CLW_QUEUE + ".gif")) ;
		// ZCU4テスト用台車投入
		registry.put( GlobalImages.CLW_ZCU4_TEST, getImageDescriptorFromPlugin( GlobalImages.CLW_ZCU4_TEST + ".png")) ;
		// リポートDisable
		registry.put( GlobalImages.CLW_REPRT_DISABLE, getImageDescriptorFromPlugin( GlobalImages.CLW_REPRT_DISABLE + ".png")) ;
		// 台車復帰
		registry.put( GlobalImages.CLW_RETURN, getImageDescriptorFromPlugin( GlobalImages.CLW_RETURN + ".gif")) ;
		// 新旧レイアウト切り替え
		registry.put( GlobalImages.CLW_NEWOLD_MAP, getImageDescriptorFromPlugin( GlobalImages.CLW_NEWOLD_MAP + ".png")) ;
		
		//	MTLビュー関連	----------------------------------------------------
		// MTL作成
		registry.put( GlobalImages.MTL_SHAPE, getImageDescriptorFromPlugin( GlobalImages.MTL_SHAPE + ".gif")) ;
		// 正常MTL作成
		registry.put( GlobalImages.MTL_NORMAL, getImageDescriptorFromPlugin( GlobalImages.MTL_NORMAL + ".gif")) ;
		// 異常MTL作成
		registry.put( GlobalImages.MTL_ERROR, getImageDescriptorFromPlugin( GlobalImages.MTL_ERROR + ".gif")) ;
		// MTL設定作成
		registry.put( GlobalImages.MTL_OPERATION, getImageDescriptorFromPlugin( GlobalImages.MTL_OPERATION + ".gif")) ;
		// MTL抜き取り要求作成
		registry.put( GlobalImages.MTL_OUT_REQUEST, getImageDescriptorFromPlugin( GlobalImages.MTL_OUT_REQUEST + ".gif")) ;
		// MTL洗車要求作成
		registry.put( GlobalImages.MTL_WASH_REQUEST, getImageDescriptorFromPlugin( GlobalImages.MTL_WASH_REQUEST + ".png")) ;
		// MTL投入完了作成
		registry.put( GlobalImages.MTL_IN_COMPLETE, getImageDescriptorFromPlugin( GlobalImages.MTL_IN_COMPLETE + ".gif")) ;
		// MTL抜き取り完了作成
		registry.put( GlobalImages.MTL_OUT_COMPLETE, getImageDescriptorFromPlugin( GlobalImages.MTL_OUT_COMPLETE + ".gif")) ;
		// MTL抜き取り確認作成
		registry.put( GlobalImages.MTL_OUT_CONFIRM, getImageDescriptorFromPlugin( GlobalImages.MTL_OUT_CONFIRM + ".gif")) ;
		// MTL洗車確認作成
		registry.put( GlobalImages.MTL_WASH_CONFIRM, getImageDescriptorFromPlugin( GlobalImages.MTL_WASH_CONFIRM + ".gif")) ;
		// 信号状態表示作成
		registry.put( GlobalImages.MTL_SIGNAL_STATUS, getImageDescriptorFromPlugin( GlobalImages.MTL_SIGNAL_STATUS + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.MTL_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.MTL_DISCONNECT + ".gif")) ;
		// タイムアウト作成
		registry.put( GlobalImages.MTL_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.MTL_TIMEOUT + ".gif")) ;
		// 車輪交換要求作成
		registry.put( GlobalImages.MTL_REPLACE_TIRE, getImageDescriptorFromPlugin( GlobalImages.MTL_REPLACE_TIRE + ".gif")) ;

		//	TBビュー関連	----------------------------------------------------
		// TB作成
		registry.put( GlobalImages.TB_SHAPE, getImageDescriptorFromPlugin( GlobalImages.TB_SHAPE + ".gif")) ;
		// 正常TB作成
		registry.put( GlobalImages.TB_NORMAL, getImageDescriptorFromPlugin( GlobalImages.TB_NORMAL + ".gif")) ;
		// 異常TB作成
		registry.put( GlobalImages.TB_ERROR, getImageDescriptorFromPlugin( GlobalImages.TB_ERROR + ".gif")) ;
		// TB設定作成
		registry.put( GlobalImages.TB_OPERATION, getImageDescriptorFromPlugin( GlobalImages.TB_OPERATION + ".gif")) ;
		// 走行経路表示作成
		registry.put( GlobalImages.TB_ROUTE, getImageDescriptorFromPlugin( GlobalImages.TB_ROUTE + ".gif")) ;
		// 走行経路開始作成
		registry.put( GlobalImages.TB_ROUTE_START, getImageDescriptorFromPlugin( GlobalImages.TB_ROUTE_START + ".png")) ;
		// 走行経路終了作成
		registry.put( GlobalImages.TB_ROUTE_END, getImageDescriptorFromPlugin( GlobalImages.TB_ROUTE_END + ".png")) ;
		// 信号状態表示作成
		registry.put( GlobalImages.TB_SIGNAL_STATUS, getImageDescriptorFromPlugin( GlobalImages.TB_SIGNAL_STATUS + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.TB_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.TB_DISCONNECT + ".gif")) ;
		// タイムアウト作成
		registry.put( GlobalImages.TB_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.TB_TIMEOUT + ".gif")) ;

		//	ZCUビュー関連	----------------------------------------------------
		// ZCU作成
		registry.put( GlobalImages.ZCU_SHAPE, getImageDescriptorFromPlugin( GlobalImages.ZCU_SHAPE + ".gif")) ;
		// 正常ZCU作成
		registry.put( GlobalImages.ZCU_NORMAL, getImageDescriptorFromPlugin( GlobalImages.ZCU_NORMAL + ".gif")) ;
		// 異常ZCU作成
		registry.put( GlobalImages.ZCU_ERROR, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR + ".gif")) ;
		// ZCU設定作成
		registry.put( GlobalImages.ZCU_OPERATION, getImageDescriptorFromPlugin( GlobalImages.ZCU_OPERATION + ".gif")) ;
		// 信号状態表示作成
		registry.put( GlobalImages.ZCU_SIGNAL_STATUS, getImageDescriptorFromPlugin( GlobalImages.ZCU_SIGNAL_STATUS + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.ZCU_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.ZCU_DISCONNECT + ".gif")) ;
		// タイムアウト作成
		registry.put( GlobalImages.ZCU_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.ZCU_TIMEOUT + ".gif")) ;
		// 異常ZCU作成
		registry.put( GlobalImages.ZCU_ERRORALL, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERRORALL + ".gif")) ;
		// 起動ZCU作成
		registry.put( GlobalImages.ZCU_STARTALL, getImageDescriptorFromPlugin( GlobalImages.ZCU_STARTALL + ".gif")) ;
		// 停止ZCU作成
		registry.put( GlobalImages.ZCU_STOPALL, getImageDescriptorFromPlugin( GlobalImages.ZCU_STOPALL + ".gif")) ;
		// ZCU GridHid作成
		registry.put( GlobalImages.ZCU_GRIDHID, getImageDescriptorFromPlugin( GlobalImages.ZCU_GRIDHID + ".gif")) ;
		// ZCU Zone在籍数作成
		registry.put( GlobalImages.ZCU_ZONECOUNT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ZONECOUNT + ".gif")) ;
		// ZCU SW Unit状態設定作成
		registry.put( GlobalImages.ZCU_SWUNIT_STATUS, getImageDescriptorFromPlugin( GlobalImages.ZCU_SWUNIT_STATUS + ".gif")) ;
		// ZCU 安全信号状態設定作成
		registry.put( GlobalImages.ZCU_SAFETY_STATUS, getImageDescriptorFromPlugin( GlobalImages.ZCU_SAFETY_STATUS + ".png")) ;
		// ZCU 信号検知状態設定作成
		registry.put( GlobalImages.ZCU_DETECT_STATUS, getImageDescriptorFromPlugin( GlobalImages.ZCU_DETECT_STATUS + ".gif")) ;

		//	OLFビュー関連	----------------------------------------------------
		// ZCU作成
		registry.put( GlobalImages.OLF_SHAPE, getImageDescriptorFromPlugin( GlobalImages.OLF_SHAPE + ".png")) ;
		// 正常ZCU作成
		registry.put( GlobalImages.OLF_NORMAL, getImageDescriptorFromPlugin( GlobalImages.OLF_NORMAL + ".gif")) ;
		// 異常ZCU作成
		registry.put( GlobalImages.OLF_ERROR, getImageDescriptorFromPlugin( GlobalImages.OLF_NORMAL + ".gif")) ;
		// ZCU設定作成
		registry.put( GlobalImages.OLF_OPERATION, getImageDescriptorFromPlugin( GlobalImages.OLF_OPERATION + ".png")) ;
		// 信号状態表示作成
		registry.put( GlobalImages.OLF_SIGNAL_STATUS, getImageDescriptorFromPlugin( GlobalImages.OLF_SIGNAL_STATUS + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.OLF_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.OLF_DISCONNECT + ".gif")) ;
		// タイムアウト作成
		registry.put( GlobalImages.OLF_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.OLF_TIMEOUT + ".gif")) ;

		//	VLFビュー関連	----------------------------------------------------
		registry.put( GlobalImages.VLF_OPERATION, getImageDescriptorFromPlugin( GlobalImages.VLF_OPERATION + ".gif")) ;
													// VLF設定
		
		//	ポートビュー関連	----------------------------------------------------
		// 信号状態表示作成
		registry.put( GlobalImages.PORT_SIGNAL_STATUS, getImageDescriptorFromPlugin( GlobalImages.PORT_SIGNAL_STATUS + ".gif")) ;
		// ポート状態初期化作成
		registry.put( GlobalImages.PORT_INIT_PARAM, getImageDescriptorFromPlugin( GlobalImages.PORT_INIT_PARAM + ".gif")) ;
		// ACS/OBS設定作成
		registry.put( GlobalImages.PORT_SETTING_ACSOBS, getImageDescriptorFromPlugin( GlobalImages.PORT_SETTING_ACSOBS + ".png")) ;

		//	HIDビュー関連	----------------------------------------------------
		// HID設定作成
		registry.put( GlobalImages.HID_OPERATION, getImageDescriptorFromPlugin( GlobalImages.HID_OPERATION + ".gif")) ;
		// 異常設定作成
		registry.put( GlobalImages.HID_SET_ERROR, getImageDescriptorFromPlugin( GlobalImages.HID_SET_ERROR + ".gif")) ;
		// 異常解除作成
		registry.put( GlobalImages.HID_RESET_ERROR, getImageDescriptorFromPlugin( GlobalImages.HID_RESET_ERROR + ".png")) ;
		// 正常HID作成
		registry.put( GlobalImages.HID_NORMAL, getImageDescriptorFromPlugin( GlobalImages.HID_NORMAL + ".gif")) ;
		// 異常HID作成
		registry.put( GlobalImages.HID_ERROR, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.HID_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.HID_DISCONNECT + ".gif")) ;
		// タイムアウト作成
		registry.put( GlobalImages.HID_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.HID_TIMEOUT + ".gif")) ;

		//	デバッグビュー関連	------------------------------------------------
		// ブレークOFF作成
		registry.put( GlobalImages.DEBUG_BREAK_OFF, getImageDescriptorFromPlugin( GlobalImages.DEBUG_BREAK_OFF + ".gif")) ;
		// ブレークON作成
		registry.put( GlobalImages.DEBUG_BREAK_ON, getImageDescriptorFromPlugin( GlobalImages.DEBUG_BREAK_ON + ".gif")) ;
		// 進捗OFF作成
		registry.put( GlobalImages.DEBUG_PROGRESS_OFF, getImageDescriptorFromPlugin( GlobalImages.DEBUG_PROGRESS_OFF + ".gif")) ;
		// 進捗ON作成
		registry.put( GlobalImages.DEBUG_PROGRESS_ON, getImageDescriptorFromPlugin( GlobalImages.DEBUG_PROGRESS_ON + ".gif")) ;
		// 状態実行中作成
		registry.put( GlobalImages.DEBUG_STATUS_EXEC, getImageDescriptorFromPlugin( GlobalImages.DEBUG_STATUS_EXEC + ".gif")) ;
		// 状態未完了作成
		registry.put( GlobalImages.DEBUG_STATUS_INCOMPLETE, getImageDescriptorFromPlugin( GlobalImages.DEBUG_STATUS_INCOMPLETE + ".gif")) ;
		// 状態完了作成
		registry.put( GlobalImages.DEBUG_STATUS_COMPLETE, getImageDescriptorFromPlugin( GlobalImages.DEBUG_STATUS_COMPLETE+ ".gif")) ;
		// 状態停止作成
		registry.put( GlobalImages.DEBUG_STATUS_STOP, getImageDescriptorFromPlugin( GlobalImages.DEBUG_STATUS_STOP+ ".gif")) ;
		// ブレーク設定作成
		registry.put( GlobalImages.DEBUG_SET_BREAK, getImageDescriptorFromPlugin( GlobalImages.DEBUG_SET_BREAK + ".gif")) ;
		// 進捗設定作成
		registry.put( GlobalImages.DEBUG_SET_PROGRESS, getImageDescriptorFromPlugin( GlobalImages.DEBUG_SET_PROGRESS + ".gif")) ;
		// シナリオ設定作成
		registry.put( GlobalImages.DEBUG_SET_SCENARIO, getImageDescriptorFromPlugin( GlobalImages.DEBUG_SET_SCENARIO + ".gif")) ;
		// シナリオ設定解除作成
		registry.put( GlobalImages.DEBUG_RESET_SCENARIO, getImageDescriptorFromPlugin( GlobalImages.DEBUG_RESET_SCENARIO + ".gif")) ;
		// シナリオ設定（ML）作成
		registry.put( GlobalImages.DEBUG_SET_SCENARIO_ML, getImageDescriptorFromPlugin( GlobalImages.DEBUG_SET_SCENARIO_ML + ".gif")) ;
		// シナリオ設定解除（ML）作成
		registry.put( GlobalImages.DEBUG_RESET_SCENARIO_ML, getImageDescriptorFromPlugin( GlobalImages.DEBUG_RESET_SCENARIO_ML + ".gif")) ;
		// 実行作成
		registry.put( GlobalImages.DEBUG_RUN, getImageDescriptorFromPlugin( GlobalImages.DEBUG_RUN + ".gif")) ;
		// ステップ実行作成
		registry.put( GlobalImages.DEBUG_RUNSTEP, getImageDescriptorFromPlugin( GlobalImages.DEBUG_RUNSTEP + ".gif")) ;
		// 中断作成
		registry.put( GlobalImages.DEBUG_PAUSE, getImageDescriptorFromPlugin( GlobalImages.DEBUG_PAUSE + ".gif")) ;
		// デバッグ終了作成
		registry.put( GlobalImages.DEBUG_STOP, getImageDescriptorFromPlugin( GlobalImages.DEBUG_STOP + ".gif")) ;
		// 作業指示応答作成
		registry.put( GlobalImages.DEBUG_SET_REPLY_WORKDIRECTION, getImageDescriptorFromPlugin( GlobalImages.DEBUG_SET_REPLY_WORKDIRECTION + ".gif")) ;
		// ブレーク番地作成
		registry.put( GlobalImages.DEBUG_SET_BREAKADDRESS, getImageDescriptorFromPlugin( GlobalImages.DEBUG_SET_BREAKADDRESS + ".gif")) ;

		//	サイクル履歴ビュー関連	--------------------------------------------
		// サイクル履歴表示作成
		registry.put( GlobalImages.CYCLEHISTORY_GET_HISTORY, getImageDescriptorFromPlugin( GlobalImages.CYCLEHISTORY_GET_HISTORY + ".gif")) ;
		
		//	接続状態ビュー関連	--------------------------------------------
		// 接続作成
		registry.put( GlobalImages.CONNECTSTATUS_CONNECT, getImageDescriptorFromPlugin( GlobalImages.CONNECTSTATUS_CONNECT + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.CONNECTSTATUS_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.CONNECTSTATUS_DISCONNECT + ".gif")) ;

		//	MLビュー関連	----------------------------------------------------
		// ML作成
		registry.put( GlobalImages.ML_SHAPE, getImageDescriptorFromPlugin( GlobalImages.ML_SHAPE + ".gif")) ;
		// 未投入ML作成
		registry.put( GlobalImages.ML_NOTIN, getImageDescriptorFromPlugin( GlobalImages.ML_NOTIN + ".gif")) ;
		// 投入済みML作成
		registry.put( GlobalImages.ML_NORMAL, getImageDescriptorFromPlugin( GlobalImages.ML_NORMAL + ".gif")) ;
		// 異常ML作成
		registry.put( GlobalImages.ML_ERROR, getImageDescriptorFromPlugin( GlobalImages.ML_ERROR + ".gif")) ;
		// 警告ML作成
		registry.put( GlobalImages.ML_WARNING, getImageDescriptorFromPlugin( GlobalImages.ML_WARNING + ".gif")) ;
		// ML投入作成
		registry.put( GlobalImages.ML_IN, getImageDescriptorFromPlugin( GlobalImages.ML_IN + ".gif")) ;
		// 全ML投入作成
		registry.put( GlobalImages.ML_IN_ALL, getImageDescriptorFromPlugin( GlobalImages.ML_IN_ALL + ".gif")) ;
		// 全ML抜き取り作成
		registry.put( GlobalImages.ML_OUT_ALL, getImageDescriptorFromPlugin( GlobalImages.ML_OUT_ALL + ".gif")) ;
		// ML抜き取り作成
		registry.put( GlobalImages.ML_OUT, getImageDescriptorFromPlugin( GlobalImages.ML_OUT + ".gif")) ;
		// ML設定作成
		registry.put( GlobalImages.ML_OPERATION, getImageDescriptorFromPlugin( GlobalImages.ML_OPERATION + ".gif")) ;
		// 走行経路表示作成
		registry.put( GlobalImages.ML_ROUTE, getImageDescriptorFromPlugin( GlobalImages.ML_ROUTE + ".gif")) ;
		// 走行経路開始作成
		registry.put( GlobalImages.ML_ROUTE_START, getImageDescriptorFromPlugin( GlobalImages.ML_ROUTE_START + ".png")) ;
		// 走行経路終了作成
		registry.put( GlobalImages.ML_ROUTE_END, getImageDescriptorFromPlugin( GlobalImages.ML_ROUTE_END + ".png")) ;
		// MLランダム投入作成
		registry.put( GlobalImages.ML_IN_RANDOM, getImageDescriptorFromPlugin( GlobalImages.ML_IN_RANDOM + ".gif")) ;
		// デバッグ追加作成
		registry.put( GlobalImages.ML_DEBUG_ADD, getImageDescriptorFromPlugin( GlobalImages.ML_DEBUG_ADD + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.ML_DISCONNECT, getImageDescriptorFromPlugin( GlobalImages.ML_DISCONNECT + ".gif")) ;
		// 未接続作成
		registry.put( GlobalImages.ML_TIMEOUT, getImageDescriptorFromPlugin( GlobalImages.ML_TIMEOUT + ".gif")) ;

		//	台車異常設定ビュー関連	--------------------------------------------
		// 追加作成
		registry.put( GlobalImages.CLW_ERROR_SETTING_ADD, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR_SETTING_ADD + ".gif")) ;
		// 削除作成
		registry.put( GlobalImages.CLW_ERROR_SETTING_REMOVE, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR_SETTING_REMOVE + ".gif")) ;
		// 編集作成
		registry.put( GlobalImages.CLW_ERROR_SETTING_EDIT, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR_SETTING_EDIT + ".gif")) ;
		// クリア作成
		registry.put( GlobalImages.CLW_ERROR_SETTING_CLEAR, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR_SETTING_CLEAR + ".gif")) ;
		// インポート作成
		registry.put( GlobalImages.CLW_ERROR_SETTING_IMPORT, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR_SETTING_IMPORT + ".gif")) ;
		// エクスポート作成
		registry.put( GlobalImages.CLW_ERROR_SETTING_EXPORT, getImageDescriptorFromPlugin( GlobalImages.CLW_ERROR_SETTING_EXPORT + ".gif")) ;

		//	HID異常設定ビュー関連	--------------------------------------------
		// 追加作成
		registry.put( GlobalImages.HID_ERROR_SETTING_ADD, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR_SETTING_ADD + ".gif")) ;
		// 削除作成
		registry.put( GlobalImages.HID_ERROR_SETTING_REMOVE, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR_SETTING_REMOVE + ".gif")) ;
		// 編集作成
		registry.put( GlobalImages.HID_ERROR_SETTING_EDIT, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR_SETTING_EDIT + ".gif")) ;
		// クリア作成
		registry.put( GlobalImages.HID_ERROR_SETTING_CLEAR, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR_SETTING_CLEAR + ".gif")) ;
		// インポート作成
		registry.put( GlobalImages.HID_ERROR_SETTING_IMPORT, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR_SETTING_IMPORT + ".gif")) ;
		// エクスポート作成
		registry.put( GlobalImages.HID_ERROR_SETTING_EXPORT, getImageDescriptorFromPlugin( GlobalImages.HID_ERROR_SETTING_EXPORT + ".gif")) ;

		//	ZCUカウント異常設定ビュー関連	--------------------------------------------
		// 追加作成
		registry.put( GlobalImages.ZCU_ERROR_COUNT_SETTING_ADD, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_COUNT_SETTING_ADD + ".gif")) ;
		// 削除作成
		registry.put( GlobalImages.ZCU_ERROR_COUNT_SETTING_REMOVE, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_COUNT_SETTING_REMOVE + ".gif")) ;
		// 編集作成
		registry.put( GlobalImages.ZCU_ERROR_COUNT_SETTING_EDIT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_COUNT_SETTING_EDIT + ".gif")) ;
		// クリア作成
		registry.put( GlobalImages.ZCU_ERROR_COUNT_SETTING_CLEAR, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_COUNT_SETTING_CLEAR + ".gif")) ;
		// インポート作成
		registry.put( GlobalImages.ZCU_ERROR_COUNT_SETTING_IMPORT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_COUNT_SETTING_IMPORT + ".gif")) ;
		// エクスポート作成
		registry.put( GlobalImages.ZCU_ERROR_COUNT_SETTING_EXPORT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_COUNT_SETTING_EXPORT + ".gif")) ;

		//	ZCU Down異常設定ビュー関連	--------------------------------------------
		// 追加作成
		registry.put( GlobalImages.ZCU_ERROR_DOWN_SETTING_ADD, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_DOWN_SETTING_ADD + ".gif")) ;
		// 削除作成
		registry.put( GlobalImages.ZCU_ERROR_DOWN_SETTING_REMOVE, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_DOWN_SETTING_REMOVE + ".gif")) ;
		// 編集作成
		registry.put( GlobalImages.ZCU_ERROR_DOWN_SETTING_EDIT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_DOWN_SETTING_EDIT + ".gif")) ;
		// クリア作成
		registry.put( GlobalImages.ZCU_ERROR_DOWN_SETTING_CLEAR, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_DOWN_SETTING_CLEAR + ".gif")) ;
		// インポート作成
		registry.put( GlobalImages.ZCU_ERROR_DOWN_SETTING_IMPORT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_DOWN_SETTING_IMPORT + ".gif")) ;
		// エクスポート作成
		registry.put( GlobalImages.ZCU_ERROR_DOWN_SETTING_EXPORT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_DOWN_SETTING_EXPORT + ".gif")) ;

		//	ZCU待ち行列異常設定ビュー関連	--------------------------------------------
		// 追加作成
		registry.put( GlobalImages.ZCU_ERROR_QUEUE_SETTING_ADD, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_QUEUE_SETTING_ADD + ".gif")) ;
		// 削除作成
		registry.put( GlobalImages.ZCU_ERROR_QUEUE_SETTING_REMOVE, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_QUEUE_SETTING_REMOVE + ".gif")) ;
		// 編集作成
		registry.put( GlobalImages.ZCU_ERROR_QUEUE_SETTING_EDIT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_QUEUE_SETTING_EDIT + ".gif")) ;
		// クリア作成
		registry.put( GlobalImages.ZCU_ERROR_QUEUE_SETTING_CLEAR, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_QUEUE_SETTING_CLEAR + ".gif")) ;
		// インポート作成
		registry.put( GlobalImages.ZCU_ERROR_QUEUE_SETTING_IMPORT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_QUEUE_SETTING_IMPORT + ".gif")) ;
		// エクスポート作成
		registry.put( GlobalImages.ZCU_ERROR_QUEUE_SETTING_EXPORT, getImageDescriptorFromPlugin( GlobalImages.ZCU_ERROR_QUEUE_SETTING_EXPORT + ".gif")) ;
	
		//ログ参照関連----------------------------------------------------------------------------------------------------
		registry.put( GlobalImages.PERSPECTIVE_MCPLOG, getImageDescriptorFromPlugin( GlobalImages.PERSPECTIVE_MCPLOG + ".gif")) ;
		//最上部
		registry.put( GlobalImages.MCPLOG_VIEW_PAGE_TOP, getImageDescriptorFromPlugin( GlobalImages.MCPLOG_VIEW_PAGE_TOP + ".png")) ;
		//前ページ
		registry.put( GlobalImages.MCPLOG_VIEW_PAGE_PREVIOUS, getImageDescriptorFromPlugin( GlobalImages.MCPLOG_VIEW_PAGE_PREVIOUS + ".gif")) ;
		//次ページ
		registry.put( GlobalImages.MCPLOG_VIEW_PAGE_NEXT, getImageDescriptorFromPlugin( GlobalImages.MCPLOG_VIEW_PAGE_NEXT + ".gif")) ;
		//最下部
		registry.put( GlobalImages.MCPLOG_VIEW_PAGE_LAST, getImageDescriptorFromPlugin( GlobalImages.MCPLOG_VIEW_PAGE_LAST + ".png")) ;
		//クリア処理
		registry.put( GlobalImages.MCPLOG_VIEW_CLEAR, getImageDescriptorFromPlugin( GlobalImages.MCPLOG_VIEW_CLEAR + ".png")) ;
		
		//スループット関連----------------------------------------------------------------------------------------------------
		//クリア処理
		registry.put( GlobalImages.THROUGHPUT_VIEW_CLEAR, getImageDescriptorFromPlugin( GlobalImages.THROUGHPUT_VIEW_CLEAR + ".png")) ;
		//保存処理
		registry.put( GlobalImages.THROUGHPUT_PDF_SAVE, getImageDescriptorFromPlugin( GlobalImages.THROUGHPUT_PDF_SAVE + ".gif")) ;
		//チャート値表示モード切り替え
		registry.put( GlobalImages.THROUGHPUT_VALUE_CHANGE, getImageDescriptorFromPlugin( GlobalImages.THROUGHPUT_VALUE_CHANGE + ".gif")) ;
		
		//NG応答関連----------------------------------------------------------------------------------------------------
		//クリア処理
		registry.put( GlobalImages.NGR_VIEW_CLEAR, getImageDescriptorFromPlugin( GlobalImages.NGR_VIEW_CLEAR + ".png")) ;
		//保存処理
		registry.put( GlobalImages.NGR_PDF_SAVE, getImageDescriptorFromPlugin( GlobalImages.NGR_PDF_SAVE + ".gif")) ;
		//チャート値表示モード切り替え
		registry.put( GlobalImages.NGR_VALUE_CHANGE, getImageDescriptorFromPlugin( GlobalImages.NGR_VALUE_CHANGE + ".gif")) ;
		//検索ダイアログ
		registry.put( GlobalImages.NGR_DIALOG_SEARCH, getImageDescriptorFromPlugin( GlobalImages.NGR_DIALOG_SEARCH + ".gif")) ;
		
	}

	//--------------------------------------------------------------------------
	/**
	 * 通信ログ通知処理を行います。<br><br>
	 *
	 * <p>メソッド名称： 通信ログ通知処理</p>
	 *
	 * @see jp.co.entersystem.commons.log.communication.ICommunicationLogListener#notifyCommunicationLog(jp.co.entersystem.commons.log.communication.CommunicationLogData)
	 */
	//--------------------------------------------------------------------------
	public void notifyCommunicationLog(CommunicationLogData logData) {
		// 初期化完了時
		if( isDisplayInitialCompleted()) {
			// ビューの更新要求イベント発行
			AplMain.getViewEventManager().notifyEvent(
					ViewEvent.EVENTTYPE_NOSWT, SocketLogView.ID, SocketLogView.EVENT_REFRESH, logData) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * メッセージログ通知処理を行います。<br><br>
	 *
	 * <p>メソッド名称： メッセージログ通知処理</p>
	 *
	 * @see jp.co.entersystem.commons.log.message.IMessageLogListener#notifyMessageLog(jp.co.entersystem.commons.log.message.MessageLogData)
	 */
	//--------------------------------------------------------------------------
	public void notifyMessageLog(MessageLogData logData) {
		// メッセージビューが未取得の場合
		if( messageView == null) {
			Display display = Display.getDefault() ;
			// 表示が利用できない場合
			if( display == null || display.isDisposed()) {
				return ;
			}

			display.asyncExec( () -> {
				// 表示が破棄されている場合
				if( display.isDisposed()) {
					return ;
				}

				// ワークベンチが終了している場合
				if( !PlatformUI.isWorkbenchRunning()) {
					return ;
				}

				IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow() ;
				// ワークベンチウィンドウが存在しない場合
				if( workbenchWindow == null) {
					return ;
				}

				IWorkbenchPage workbenchPage = workbenchWindow.getActivePage() ;
				// ワークベンチページが存在しない場合
				if( workbenchPage == null) {
					return ;
				}

				IViewReference[] references = workbenchPage.getViewReferences() ;
				// ビュー参照からメッセージビューを取得
				for (int i = 0; i < references.length; i++) {
					// メッセージビューIDではない場合
					if( !MessageView.ID.equals( references[i].getId())) {
						continue ;
					}

					messageView = (MessageView)references[i].getView( false) ;
					break ;
				}

				// メッセージビューが取得できた場合
				if( messageView != null) {
					// メッセージ通知
					messageView.notifyMessage(logData) ;
				}
			});
		}
		else {
			// メッセージ通知
			messageView.notifyMessage(logData) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * プロジェクト情報の設定を行います。<br><br>
	 *
	 * <p>メソッド名称： プロジェクト情報の設定</p>
	 *
	 * @param layoutId		レイアウトID
	 * @param projectInfo	プロジェクト情報
	 */
	//--------------------------------------------------------------------------
	public void putProjectInfo(String layoutId, ProjectInfo projectInfo) {
		projectInfoMap.put( layoutId, projectInfo) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 全更新処理の削除を行います。<br><br>
	 *
	 * <p>メソッド名称： 全更新処理の削除</p>
	 */
	//--------------------------------------------------------------------------
	public void removeAllSystemRefresh() {
		systemRefreshMap.forEach(( k, v) -> {
			// 更新の停止
			v.stop() ;
		});
		
		// マップから削除
		systemRefreshMap.clear() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 更新処理の削除を行います。<br><br>
	 *
	 * <p>メソッド名称： 更新処理の削除</p>
	 *
	 * @param id	処理ID(ViewID or ClassName)
	 */
	//--------------------------------------------------------------------------
	public void removeSystemRefresh( String id) {
		RefreshControl refreshControl = systemRefreshMap.get( id) ;
		if( refreshControl == null) {
			return ;
		}

		// 更新の停止
		refreshControl.stop() ;
		
		// 削除
		systemRefreshMap.remove( id) ;
	}


	//--------------------------------------------------------------------------
	/**
	 * 完全制御プロジェクトの設定を行います。<br><br>
	 *
	 * <p>メソッド名称： 完全制御プロジェクトの設定</p>
	 *
	 * @param allControlProject	完全制御プロジェクト
	 */
	//--------------------------------------------------------------------------
	public void setAllControlProject(ProjectInfo allControlProject) {
		this.allControlProject = allControlProject ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 最終フォーカス時間の設定を行います。<br><br>
	 *
	 * <p>メソッド名称： 最終フォーカス時間の設定</p>
	 *
	 * @param lastLayoutViewFocusTime	最終フォーカス時間
	 */
	//--------------------------------------------------------------------------
	public void setLastLayoutViewFocusTime(long lastLayoutViewFocusTime) {
		this.lastLayoutViewFocusTime = lastLayoutViewFocusTime ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 選択プロジェクト情報の設定を行います。<br><br>
	 *
	 * <p>メソッド名称： 選択プロジェクト情報の設定</p>
	 *
	 * @param projectInfo	プロジェクト情報
	 */
	//--------------------------------------------------------------------------
	public void setProjectInfoSelect( ProjectInfo projectInfo) {
		this.projectInfoSelect = projectInfo ; 
	}
	
	//--------------------------------------------------------------------------
	/**
	 * ワークスペースファイルの確認を行います。<br>
	 * 旧シミュレータで作成されたワークスペースの場合は、新しいバージョン向けにコンバートを行う。<br><br>
	 *
	 * <p>メソッド名称： ワークスペースファイルの確認</p>
	 *
	 * @param workspaceFile		ワークスペースファイル
	 * @throws IOException 処理異常
	 */
	//--------------------------------------------------------------------------
	private void checkWorkspaceFile( File workspaceFile) throws IOException {
		// sim.xmlを読み込む
		List<String>	lines = Files.readAllLines( workspaceFile.toPath()) ;
		
		// 新しい定義に置換
		boolean			replace = false ;
		List<String>	newLines = new ArrayList<String>() ;
		for( String line : lines) {
			if( line.indexOf( "jp.co.daifuku.jcsim.commons.") != -1) {
				newLines.add( line.replace( "jp.co.daifuku.jcsim.commons.", "jp.co.daifuku.jcsim2.data.")) ;
				replace = true ;
			}
			else {
				newLines.add( line) ;
			}
		}
		
		// 置換が行われない場合
		if( !replace) {
			return ;
		}
		
		// 既存のファイルを削除
		workspaceFile.delete() ;
		
		// ファイル書き込み
		try(
			FileWriter writer = new FileWriter( workspaceFile) ;
			BufferedWriter bwriter= new BufferedWriter(writer) ;
			PrintWriter pwriter = new PrintWriter( new BufferedWriter( bwriter));
		) {
			for( String line : newLines) {
				pwriter.println( line) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * DLLの初期化を行います。<br><br>
	 *
	 * <p>メソッド名称： DLLの初期化</p>
	 */
	//--------------------------------------------------------------------------
	private void initDll() {
		Properties properties = System.getProperties() ;
		
		// Winsystem DLLファイルパス設定
		properties.setProperty( WinSystem.LIB_PATH_PROP, FileUtil.getResourcePath( "/resources/dll/winsystem.dll")) ;

		// Winsystem Driverファイルパス設定
		if( DRIVER_ENABLED)
			properties.setProperty( WinSystem.DRIVER_PATH_PROP, FileUtil.getResourcePath( "/resources/driver/JWinSystem.sys")) ;

		// ImGui DLLファイルパス設定
		properties.setProperty(
				"imgui.library.path",
				new File( FileUtil.getResourcePath( "/resources/dll/imgui-java64.dll")).getParent()) ;
	}
	
	//--------------------------------------------------------------------------
	/**
	 * ゲートウェイ初期化処理を行います。<br><br>
	 *
	 * <p>メソッド名称： ゲートウェイ初期化処理</p>
	 * @throws Exception 初期化異常
	 */
	//--------------------------------------------------------------------------
	private void initGateway() throws Exception {
		MessageLogger.getInstance().setLog( MessageLogData.CATEGORY_INFORMATION, Messages.getString( this, "gatewayStart")) ;
		Gateway	gateway = Gateway.getInstance() ;

		// BEZ間通信初期化
		gateway.addControl( ComBez.getInstance()) ;
		ComBez.getInstance().init() ;

		// 仮想BEZ間通信初期化
		gateway.addControl( ComBezVirtual.getInstance()) ;
		ComBezVirtual.getInstance().init(
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.BESIM_COM_TCP_PORT, GlobalSystemProperties.BESIM_COM_TCP_PORT_DEFAULT)),
				true ) ;

		// OPC間通信初期化
		gateway.addControl( ComOpc.getInstance()) ;
		ComOpc.getInstance().init() ;

		// 仮想BEZ間通信初期化
		gateway.addControl( ComOpcVirtual.getInstance()) ;
		
		// MOS間通信初期化
		gateway.addControl( ComMos.getInstance()) ;
		ComMos.getInstance().init() ;

		// 仮想MOS間通信初期化
		gateway.addControl( ComMosVirtual.getInstance()) ;
		ComMosVirtual.getInstance().init(
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.MOS_COM_TCP_PORT, GlobalSystemProperties.MOS_COM_TCP_PORT_DEFAULT)),
				true ) ;

		// STK間通信初期化
		gateway.addControl( ComStk.getInstance()) ;
		ComStk.getInstance().init() ;

		// 仮想STK間通信初期化
		gateway.addControl( ComStkVirtual.getInstance()) ;
		ComStkVirtual.getInstance().init(
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.STK_COM_TCP_PORT, GlobalSystemProperties.STK_COM_TCP_PORT_DEFAULT)),
				true ) ;

		// シミュレータMCP通信制御初期化
		gateway.addControl( ComMcp.getInstance()) ;
		ComMcp.getInstance().init() ;
		
		// シミュレータ仮想MCP通信制御初期化
		gateway.addControl( ComMcpVirtual.getInstance()) ;

		// シミュレータLoadtester通信制御初期化
		gateway.addControl( ComLoadtester.getInstance()) ;
		ComLoadtester.getInstance().init() ;
		
		// シミュレータ仮想Loadtester通信制御初期化
		gateway.addControl( ComLoadtesterVirtual.getInstance()) ;
		ComLoadtesterVirtual.getInstance().init(
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.LOADTESTER_COM_TCP_PORT, GlobalSystemProperties.LOADTESTER_COM_TCP_PORT_DEFAULT)),
				true) ;

		// ZCU4通信制御初期化
		gateway.addControl( ComZcu4.getInstance()) ;
		ComZcu4.getInstance().init() ;

		// 仮想ZCU4間通信初期化
		gateway.addControl( ComZcu4Virtual.getInstance()) ;

		// IoT通信制御初期化
		gateway.addControl( ComIot.getInstance()) ;
		ComIot.getInstance().init() ;

		// 仮想IoT間通信初期化
		gateway.addControl( ComIotVirtual.getInstance()) ;

		// VlfSafety通信制御初期化
		gateway.addControl( ComVlfSafety.getInstance()) ;
		ComVlfSafety.getInstance().init() ;

		// 仮想VlfSafety通信初期化
		gateway.addControl( ComVlfSafetyVirtual.getInstance()) ;

		// CET通信制御初期化
		gateway.addControl( ComCet.getInstance()) ;
		ComCet.getInstance().init() ;

		// 仮想CET通信初期化
		gateway.addControl( ComCetVirtual.getInstance()) ;

		// 実機台車通信制御初期化
		gateway.addControl( ComClw.getInstance()) ;
		ComClw.getInstance().init() ;

		// 仮想実機台車通信初期化
		gateway.addControl( ComClwVirtual.getInstance()) ;

		// ZCS通信制御初期化
		gateway.addControl( ComZcs.getInstance()) ;
		ComZcu4.getInstance().init() ;

		// 仮想ZCS間通信初期化
		gateway.addControl( ComZcsVirtual.getInstance()) ;

//		// シミュレータ間通信のIPAddressを確認
//		String	ipAddress = RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.SIM2_NIC_ADDRESS, GlobalSystemProperties.SIM2_NIC_ADDRESS_DEFAULT) ;
//		
//		boolean	checkNic = true ;
//
//		// 指定された場合
//		if( !ipAddress.equals("AUTO")) {
//			if( !ipAddress.equals("NONE")) {
//				// アドレスの確認
//				if( !IpUtil.checkLocalNic(ipAddress)) {
//					MessageDialog.openError( Display.getDefault().getActiveShell(),
//							Messages.getString( this, "messageBoxTitle"),
//							String.format( Messages.getString( this, "messageBoxMessage2"),ipAddress)) ;
//					checkNic = false ;
//				}
//			}
//			else {
//				checkNic = false ;
//			}
//		}
//		// 設定に問題ない時
//		if( checkNic) { 
//			// シミュレータ間通信制御初期化
//			gateway.addControl( ComSim2.getInstance()) ;
//			
//			// シミュレータ間通信仮想通信制御初期化
//			gateway.addControl( ComSim2Virtual.getInstance()) ;
//			ComSim2Virtual.getInstance().init(
//					new ComSim2MessageCreater(),
//					RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.SIM2_COM_MULTICAST_IP, GlobalSystemProperties.SIM2_COM_MULTICAST_IP_DEFAULT),
//					Integer.parseInt( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.SIM2_COM_TCP_PORT, GlobalSystemProperties.SIM2_COM_TCP_PORT_DEFAULT)),
//					Integer.parseInt( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.SIM2_COM_MUTICAST_PORT, GlobalSystemProperties.SIM2_COM_MULTICAST_PORT_DEFAULT)),
//					ipAddress,
//					true) ;
//		}

		// パイプ接続
		gateway.linkPipe() ;
		
		// 登録された各処理の開始
		Gateway.getInstance().start() ;
		
	}

	//--------------------------------------------------------------------------
	/**
	 * ワークスペース情報の初期化を行います。<br><br>
	 *
	 * <p>メソッド名称： ワークスペース情報の初期化</p>
	 */
	//--------------------------------------------------------------------------
	private void initWorkspace() {
		// デフォルト保存場所取得
		File	file = new File( RcpUtil.getDefaultSavePath() + SimFiles.DIR_DATA) ;
		File	workspaceFile = new File( file.getAbsolutePath() + "/" + SimFiles.FILE_WORKSPACE) ;
	
		// ワークスペース情報の取得
		Workspace	workspace = new Workspace() ;
	
		try {
			
			// ワークスペース情報の存在確認
			if( workspaceFile.exists()) {
				// ワークスペースファイルの確認（旧ワークスペースへの対応）
				checkWorkspaceFile( workspaceFile) ;
				
				workspace.load( workspaceFile) ;
			}
			// ワークスペース情報が存在しない時
			else {
				workspace.init( file.getAbsolutePath()) ;
				workspace.save( file.getAbsolutePath() + "/" + SimFiles.FILE_WORKSPACE) ;
			}				
		} catch (Exception ioex) {
			AplMain.errorLog( ioex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * バージョン情報の初期化を行います。<br><br>
	 *
	 * <p>メソッド名称： バージョン情報の初期化</p>
	 */
	//--------------------------------------------------------------------------
	private void initVersionInfo() {
		versionFile = new File( FileUtil.getResourcePath( SimFiles.FILE_VERSION)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログの初期化を行います。<br><br>
	 *
	 * <p>メソッド名称： ログ初期化処理</p>
	 *
	 * @throws Exception
	 */
	//--------------------------------------------------------------------------
	public void logInitial() throws Exception {
		// メッセージログの起動
		loggerMessage = MessageLogger.getInstance() ;
		loggerMessage.init(
				RcpUtil.getDefaultSavePath() + SimFiles.DIR_LOG,
				MessageLogger.DEFAULT_NAME,
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.MESSAGE_LOG_TIMEUNIT, GlobalSystemProperties.MESSAGE_LOG_TIMEUNIT_DEFAULT)),
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.MESSAGE_LOG_SIZE, GlobalSystemProperties.MESSAGE_LOG_SIZE_DEFAULT)) * 1024,
				Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.MESSAGE_LOG_COUNT, GlobalSystemProperties.MESSAGE_LOG_COUNT_DEFAULT)),
				false) ;
		
		// シミュレータ間通信ログの起動
		sim2Logger = Sim2SocketLogger.getInstance() ;
		sim2Logger.init(
					RcpUtil.getDefaultSavePath() + SimFiles.DIR_LOG,
					Sim2SocketLogger.DEFAULT_NAME,
					Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.SIM2_COM_LOG_MAXSIZE, GlobalSystemProperties.SIM2_COM_LOG_MAXSIZE_DEFAULT)) * 1024,
					Integer.valueOf( RcpUtil.getSystemProperties().getProperty( GlobalSystemProperties.SIM2_COM_LOG_MAXCOUNT, GlobalSystemProperties.SIM2_COM_LOG_MAXCOUNT_DEFAULT)),
					false
					) ;
	}
}
