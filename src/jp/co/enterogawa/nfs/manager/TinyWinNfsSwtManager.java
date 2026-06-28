package jp.co.enterogawa.nfs.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import jp.co.enterogawa.nfs.config.ConfigBackup;
import jp.co.enterogawa.nfs.config.NfsServerConfig;
import jp.co.enterogawa.nfs.config.TinyWinNfsPaths;
import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * SWT版NFSサーバー管理ツールクラスです。<br><br>
 *
 * <p>クラス名称： SWT版NFSサーバー管理ツール</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class TinyWinNfsSwtManager {
	//	定数定義	------------------------------------------------------------
	/** サービス名 */
	private static final String			SERVICE_NAME = "TinyWinNfsServer" ;

	/** 旧サービス名 */
	private static final String[]		LEGACY_SERVICE_NAMES = { "OgawaNfsServer", "QnxNfsServer" } ;

	/** 製品名 */
	private static final String			PRODUCT_NAME = "TinyWinNFS Server" ;

	/** アイコンファイル名 */
	private static final String			ICON_FILE_NAME = "tinywin-nfs-server.png" ;

	/** RPCタイムアウト */
	private static final int				RPC_TIMEOUT = 3000 ;

	/** 時刻フォーマット */
	private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern( "yyyy/MM/dd HH:mm:ss") ;

	//	内部定義	------------------------------------------------------------
	/** SWT Display */
	private final Display				display ;

	/** プロジェクトルート */
	private final Path					rootPath ;

	/** データルート */
	private final Path					dataRootPath ;

	/** 設定ファイル */
	private final Path					configPath ;

	/** 表示メッセージ */
	private final ManagerMessages		messages ;

	/** Shell */
	private Shell						shell ;

	/** ログ */
	private Text						logText ;

	/** 状態表示 */
	private Label						statusValueLabel ;

	/** 管理者権限表示 */
	private Label						adminValueLabel ;

	/** サービス情報 */
	private Text						serviceInfoText ;

	/** export名 */
	private Text						exportNameText ;

	/** export一覧 */
	private Table						exportTable ;

	/** export定義 */
	private final List<ShareEntry>		shareEntries = new ArrayList<ShareEntry>() ;

	/** 選択中export */
	private int							selectedShareIndex = -1 ;

	/** exportパス */
	private Text						exportPathText ;

	/** export書込可否 */
	private Button						exportWritableButton ;

	/** 許可クライアント */
	private Text						exportAllowedClientsText ;

	/** サーバーホスト名 */
	private Text						serverHostText ;

	/** クライアント側マウントポイント */
	private Text						clientMountPointText ;

	/** mountコマンド */
	private Text						mountCommandText ;

	/** Portmapポート */
	private Text						portmapPortText ;

	/** NFSポート */
	private Text						nfsPortText ;

	/** MOUNTポート */
	private Text						mountPortText ;

	/** UID */
	private Text						uidText ;

	/** GID */
	private Text						gidText ;

	/** ファイルモード */
	private Text						fileModeText ;

	/** ディレクトリモード */
	private Text						directoryModeText ;

	/** ブロックサイズ */
	private Text						blockSizeText ;

	/** 読込サイズ */
	private Text						readSizeText ;

	/** 書込サイズ */
	private Text						writeSizeText ;

	/** ディレクトリ推奨サイズ */
	private Text						directoryPreferredSizeText ;

	/** 最大ファイルサイズ */
	private Text						maxFileSizeText ;

	/** 時刻精度ナノ秒 */
	private Text						timeDeltaNanosText ;

	/** PATHCONF link最大値 */
	private Text						pathconfLinkMaxText ;

	/** PATHCONF name最大値 */
	private Text						pathconfNameMaxText ;

	/** 同期書込 */
	private Button						writeSyncButton ;

	/** 書込キャッシュ */
	private Button						writeCacheEnabledButton ;

	/** 書込キャッシュ最大オープン数 */
	private Text						writeCacheMaxOpenText ;

	/** 書込キャッシュアイドル保持時間 */
	private Text						writeCacheIdleMillisText ;

	/** ファイル名文字コード */
	private Text						filenameCharsetText ;

	/** 表示言語 */
	private Combo						uiLanguageCombo ;

	/** RPC XID */
	private int							xid = 0x30000000 ;

	//--------------------------------------------------------------------------
	/**
	 * 管理ツールを開始します。<br><br>
	 *
	 * <p>メソッド名称： 管理ツール開始</p>
	 *
	 * @param args	起動引数
	 */
	//--------------------------------------------------------------------------
	public static void main(String[] args) {
		Display display = new Display() ;

		try {
			TinyWinNfsSwtManager manager = new TinyWinNfsSwtManager( display) ;
			manager.open() ;
		} finally {
			display.dispose() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 *
	 * @param display	SWT Display
	 */
	//--------------------------------------------------------------------------
	public TinyWinNfsSwtManager(Display display) {
		this.display = display ;
		rootPath = detectRootPath() ;
		dataRootPath = TinyWinNfsPaths.getDataRoot( rootPath) ;
		configPath = TinyWinNfsPaths.getConfigPath( rootPath) ;
		migrateLegacyConfiguration() ;
		messages = ManagerMessages.load( readConfiguredLanguage()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 画面を開きます。<br><br>
	 *
	 * <p>メソッド名称： 画面表示</p>
	 */
	//--------------------------------------------------------------------------
	private void open() {
		initShell() ;
		loadConfig() ;
		refreshStatus() ;
		appendLog( format( "log.root", rootPath)) ;
		appendLog( format( "log.dataRoot", dataRootPath)) ;
		appendLog( format( "log.config", configPath)) ;
		shell.open() ;

		while( !shell.isDisposed()) {
			// SWTイベントがない場合
			if( !display.readAndDispatch()) {
				display.sleep() ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * Shellを初期化します。<br><br>
	 *
	 * <p>メソッド名称： Shell初期化</p>
	 */
	//--------------------------------------------------------------------------
	private void initShell() {
		shell = new Shell( display, SWT.SHELL_TRIM) ;
		shell.setText( text( "app.title")) ;
		shell.setMinimumSize( 760, 560) ;
		shell.setSize( 900, 660) ;
		shell.setLayout( new GridLayout( 1, false)) ;
		applyWindowIcon() ;
		createHeader( shell) ;
		createTabs( shell) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ウィンドウアイコンを設定します。<br><br>
	 *
	 * <p>メソッド名称： ウィンドウアイコン設定</p>
	 */
	//--------------------------------------------------------------------------
	private void applyWindowIcon() {
		Path iconPath = rootPath.resolve( "assets").resolve( ICON_FILE_NAME) ;

		// アイコンファイルが存在しない場合
		if( !Files.exists( iconPath)) {
			return ;
		}

		try {
			Image image = new Image( display, iconPath.toString()) ;
			shell.setImage( image) ;
			shell.addDisposeListener( event -> image.dispose()) ;
		} catch( RuntimeException rex) {
			// アイコンが読み込めない場合は既定アイコンを使用する
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ヘッダーを作成します。<br><br>
	 *
	 * <p>メソッド名称： ヘッダー作成</p>
	 *
	 * @param parent	親Composite
	 */
	//--------------------------------------------------------------------------
	private void createHeader(Composite parent) {
		Composite header = new Composite( parent, SWT.NONE) ;
		header.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		header.setLayout( createGridLayout( 4, false, 10, 2)) ;

		createLabel( header, text( "header.service") ) ;
		statusValueLabel = createLabel( header, text( "header.checking") ) ;
		statusValueLabel.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		createLabel( header, text( "header.administrator") ) ;
		adminValueLabel = createLabel( header, boolText( isAdministrator()) ) ;
		Button refreshButton = createButton( header, text( "button.refresh"), event -> refreshStatus()) ;
		GridData buttonData = new GridData( SWT.RIGHT, SWT.CENTER, false, false) ;
		buttonData.horizontalSpan = 4 ;
		refreshButton.setLayoutData( buttonData) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * タブを作成します。<br><br>
	 *
	 * <p>メソッド名称： タブ作成</p>
	 *
	 * @param parent	親Composite
	 */
	//--------------------------------------------------------------------------
	private void createTabs(Composite parent) {
		TabFolder folder = new TabFolder( parent, SWT.NONE) ;
		folder.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		addTab( folder, text( "tab.share"), createShareComposite( folder)) ;
		addTab( folder, text( "tab.options"), createOptionsComposite( folder)) ;
		addTab( folder, text( "tab.service"), createServiceComposite( folder)) ;
		addTab( folder, text( "tab.log"), createLogComposite( folder)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * タブを追加します。<br><br>
	 *
	 * <p>メソッド名称： タブ追加</p>
	 *
	 * @param folder	タブフォルダ
	 * @param text		表示文字
	 * @param control	タブ内容
	 */
	//--------------------------------------------------------------------------
	private void addTab(TabFolder folder, String text, Composite control) {
		TabItem item = new TabItem( folder, SWT.NONE) ;
		item.setText( text) ;
		item.setControl( control) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有Compositeを作成します。<br><br>
	 *
	 * <p>メソッド名称： 共有Composite作成</p>
	 *
	 * @param parent	親Composite
	 * @return 共有Composite
	 */
	//--------------------------------------------------------------------------
	private Composite createShareComposite(Composite parent) {
		Composite panel = new Composite( parent, SWT.NONE) ;
		panel.setLayout( createGridLayout( 1, false, 10, 10)) ;

		Group listGroup = createGroup( panel, text( "group.sharedFolders"), 1) ;
		listGroup.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		exportTable = new Table( listGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE) ;
		exportTable.setHeaderVisible( true) ;
		exportTable.setLinesVisible( true) ;
		exportTable.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		createTableColumn( exportTable, text( "column.export"), 130) ;
		createTableColumn( exportTable, text( "column.folder"), 360) ;
		createTableColumn( exportTable, text( "column.writable"), 80) ;
		createTableColumn( exportTable, text( "column.allowedClients"), 160) ;
		exportTable.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				selectShare( exportTable.getSelectionIndex()) ;
			}
		}) ;

		Group shareGroup = createGroup( panel, text( "group.selectedShare"), 3) ;
		createLabel( shareGroup, text( "label.export") ) ;
		exportNameText = createText( shareGroup, SWT.BORDER) ;
		exportNameText.setLayoutData( createSpanData( 2)) ;
		createLabel( shareGroup, text( "label.folder") ) ;
		exportPathText = createText( shareGroup, SWT.BORDER) ;
		exportPathText.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		createButton( shareGroup, text( "button.browse"), event -> browseExportPath()) ;
		new Label( shareGroup, SWT.NONE) ;
		exportWritableButton = new Button( shareGroup, SWT.CHECK) ;
		exportWritableButton.setText( text( "label.writable") ) ;
		exportWritableButton.setLayoutData( createSpanData( 2)) ;
		createLabel( shareGroup, text( "label.allowedClients") ) ;
		exportAllowedClientsText = createText( shareGroup, SWT.BORDER) ;
		exportAllowedClientsText.setLayoutData( createSpanData( 2)) ;

		Composite shareButtons = createButtonRow( panel) ;
		createButton( shareButtons, text( "button.add"), event -> addShare()) ;
		createButton( shareButtons, text( "button.apply"), event -> applyShare()) ;
		createButton( shareButtons, text( "button.remove"), event -> removeShare()) ;

		Group mountGroup = createGroup( panel, text( "group.mount"), 2) ;
		mountCommandText = createText( mountGroup, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL) ;
		GridData commandData = new GridData( SWT.FILL, SWT.FILL, true, true) ;
		commandData.horizontalSpan = 2 ;
		commandData.heightHint = 88 ;
		mountCommandText.setLayoutData( commandData) ;
		createButton( mountGroup, text( "button.copyCommand"), event -> copyMountCommand()) ;
		createButton( mountGroup, text( "button.update"), event -> updateMountCommand()) ;

		Composite buttons = createButtonRow( panel) ;
		createButton( buttons, text( "button.reload"), event -> loadConfig()) ;
		createButton( buttons, text( "button.save"), event -> saveConfig()) ;
		createButton( buttons, text( "button.saveRestart"), event -> saveConfigAndRestart()) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * オプションCompositeを作成します。<br><br>
	 *
	 * <p>メソッド名称： オプションComposite作成</p>
	 *
	 * @param parent	親Composite
	 * @return オプションComposite
	 */
	//--------------------------------------------------------------------------
	private Composite createOptionsComposite(Composite parent) {
		Composite panel = new Composite( parent, SWT.NONE) ;
		panel.setLayout( createGridLayout( 1, false, 10, 10)) ;

		Group displayGroup = createGroup( panel, text( "group.display"), 3) ;
		createLabel( displayGroup, text( "label.language")) ;
		uiLanguageCombo = new Combo( displayGroup, SWT.DROP_DOWN | SWT.READ_ONLY) ;
		uiLanguageCombo.setItems( new String[] { text( "language.auto"), text( "language.en"), text( "language.ja") }) ;
		uiLanguageCombo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		new Label( displayGroup, SWT.NONE) ;

		Group mountGroup = createGroup( panel, text( "group.mountOptions"), 3) ;
		serverHostText = addField( mountGroup, text( "label.serverHost"), text( "button.detect"), event -> detectServerHost()) ;
		clientMountPointText = addField( mountGroup, text( "label.clientMountPoint")) ;

		Group networkGroup = createGroup( panel, text( "group.network"), 3) ;
		portmapPortText = addField( networkGroup, text( "label.portmapUdpPort")) ;
		nfsPortText = addField( networkGroup, text( "label.nfsUdpPort")) ;
		mountPortText = addField( networkGroup, text( "label.mountUdpPort")) ;

		Group permissionGroup = createGroup( panel, text( "group.fileAttributes"), 3) ;
		uidText = addField( permissionGroup, text( "label.uid")) ;
		gidText = addField( permissionGroup, text( "label.gid")) ;
		fileModeText = addField( permissionGroup, text( "label.fileMode")) ;
		directoryModeText = addField( permissionGroup, text( "label.directoryMode")) ;
		blockSizeText = addField( permissionGroup, text( "label.blockSize")) ;
		readSizeText = addField( permissionGroup, text( "label.readSize")) ;
		filenameCharsetText = addField( permissionGroup, text( "label.filenameCharset")) ;

		Group nfsV3Group = createGroup( panel, text( "group.nfsv3Compatibility"), 3) ;
		writeSizeText = addField( nfsV3Group, text( "label.writeSize")) ;
		directoryPreferredSizeText = addField( nfsV3Group, text( "label.directoryPreferredSize")) ;
		maxFileSizeText = addField( nfsV3Group, text( "label.maxFileSize")) ;
		timeDeltaNanosText = addField( nfsV3Group, text( "label.timeDeltaNanos")) ;
		pathconfLinkMaxText = addField( nfsV3Group, text( "label.pathconfLinkMax")) ;
		pathconfNameMaxText = addField( nfsV3Group, text( "label.pathconfNameMax")) ;

		Group performanceGroup = createGroup( panel, text( "group.performance"), 3) ;
		createLabel( performanceGroup, text( "label.writeSync")) ;
		writeSyncButton = new Button( performanceGroup, SWT.CHECK) ;
		writeSyncButton.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		new Label( performanceGroup, SWT.NONE) ;
		createLabel( performanceGroup, text( "label.writeCacheEnabled")) ;
		writeCacheEnabledButton = new Button( performanceGroup, SWT.CHECK) ;
		writeCacheEnabledButton.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		new Label( performanceGroup, SWT.NONE) ;
		writeCacheMaxOpenText = addField( performanceGroup, text( "label.writeCacheMaxOpen")) ;
		writeCacheIdleMillisText = addField( performanceGroup, text( "label.writeCacheIdleMillis")) ;

		Composite buttons = createButtonRow( panel) ;
		createButton( buttons, text( "button.reload"), event -> loadConfig()) ;
		createButton( buttons, text( "button.save"), event -> saveConfig()) ;
		createButton( buttons, text( "button.saveRestart"), event -> saveConfigAndRestart()) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービスCompositeを作成します。<br><br>
	 *
	 * <p>メソッド名称： サービスComposite作成</p>
	 *
	 * @param parent	親Composite
	 * @return サービスComposite
	 */
	//--------------------------------------------------------------------------
	private Composite createServiceComposite(Composite parent) {
		Composite panel = new Composite( parent, SWT.NONE) ;
		panel.setLayout( createGridLayout( 1, false, 8, 8)) ;

		Composite buttons = new Composite( panel, SWT.NONE) ;
		buttons.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false)) ;
		buttons.setLayout( createGridLayout( 11, false, 6, 6)) ;
		createButton( buttons, text( "button.install"), event -> runPrivilegedScriptAsync( "install-service.ps1")) ;
		createButton( buttons, text( "button.start"), event -> runPrivilegedScriptAsync( "start-service.ps1")) ;
		createButton( buttons, text( "button.stop"), event -> runPrivilegedScriptAsync( "stop-service.ps1")) ;
		createButton( buttons, text( "button.restart"), event -> runPrivilegedScriptAsync( "restart-service.ps1")) ;
		createButton( buttons, text( "button.uninstall"), event -> confirmAndRun( text( "dialog.uninstallService"), "uninstall-service.ps1")) ;
		createButton( buttons, text( "button.firewall"), event -> runPrivilegedScriptAsync( "add-firewall-rules.ps1")) ;
		createButton( buttons, text( "button.smokeTest"), event -> runSmokeTestAsync()) ;
		createButton( buttons, text( "button.status"), event -> refreshStatus()) ;
		createButton( buttons, text( "button.openLog"), event -> openPath( TinyWinNfsPaths.getLogPath( rootPath))) ;
		createButton( buttons, text( "button.openWinswLog"), event -> openPath( getWinswLogPath())) ;
		createButton( buttons, text( "button.createDiagnostics"), event -> createDiagnosticPackage()) ;

		serviceInfoText = createText( panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL) ;
		serviceInfoText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		updateServiceInfoText() ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログCompositeを作成します。<br><br>
	 *
	 * <p>メソッド名称： ログComposite作成</p>
	 *
	 * @param parent	親Composite
	 * @return ログComposite
	 */
	//--------------------------------------------------------------------------
	private Composite createLogComposite(Composite parent) {
		Composite panel = new Composite( parent, SWT.NONE) ;
		panel.setLayout( createGridLayout( 1, false, 8, 8)) ;
		logText = createText( panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL) ;
		logText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;

		Composite buttons = createButtonRow( panel) ;
		createButton( buttons, text( "button.clear"), event -> logText.setText( "")) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * グループを作成します。<br><br>
	 *
	 * <p>メソッド名称： グループ作成</p>
	 *
	 * @param parent	親Composite
	 * @param text		表示文字
	 * @param columns	列数
	 * @return グループ
	 */
	//--------------------------------------------------------------------------
	private Group createGroup(Composite parent, String text, int columns) {
		Group group = new Group( parent, SWT.NONE) ;
		group.setText( text) ;
		group.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false)) ;
		group.setLayout( createGridLayout( columns, false, 8, 6)) ;
		return group ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 入力項目を追加します。<br><br>
	 *
	 * <p>メソッド名称： 入力項目追加</p>
	 *
	 * @param parent	親Composite
	 * @param label		ラベル
	 * @param button	ボタン
	 * @return テキスト
	 */
	//--------------------------------------------------------------------------
	private Text addField(Composite parent, String label) {
		return addField( parent, label, null, null) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ボタン付き入力項目を追加します。<br><br>
	 *
	 * <p>メソッド名称： ボタン付き入力項目追加</p>
	 *
	 * @param parent		親Composite
	 * @param label			ラベル
	 * @param buttonText	ボタン表示文字
	 * @param listener		リスナ
	 * @return テキスト
	 */
	//--------------------------------------------------------------------------
	private Text addField(Composite parent, String label, String buttonText, SwtSelectionListener listener) {
		createLabel( parent, label) ;
		Text text = createText( parent, SWT.BORDER) ;
		text.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;

		// ボタンが存在する場合
		if( buttonText != null) {
			createButton( parent, buttonText, listener) ;
		}
		else {
			new Label( parent, SWT.NONE) ;
		}

		return text ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ボタン行を作成します。<br><br>
	 *
	 * <p>メソッド名称： ボタン行作成</p>
	 *
	 * @param parent	親Composite
	 * @return ボタン行
	 */
	//--------------------------------------------------------------------------
	private Composite createButtonRow(Composite parent) {
		Composite buttons = new Composite( parent, SWT.NONE) ;
		FillLayout layout = new FillLayout( SWT.HORIZONTAL) ;
		layout.spacing = 8 ;
		buttons.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, true, false)) ;
		buttons.setLayout( layout) ;
		return buttons ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ラベルを作成します。<br><br>
	 *
	 * <p>メソッド名称： ラベル作成</p>
	 *
	 * @param parent	親Composite
	 * @param text		表示文字
	 * @return ラベル
	 */
	//--------------------------------------------------------------------------
	private Label createLabel(Composite parent, String text) {
		Label label = new Label( parent, SWT.NONE) ;
		label.setText( text) ;
		return label ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テキストを作成します。<br><br>
	 *
	 * <p>メソッド名称： テキスト作成</p>
	 *
	 * @param parent	親Composite
	 * @param style		スタイル
	 * @return テキスト
	 */
	//--------------------------------------------------------------------------
	private Text createText(Composite parent, int style) {
		return new Text( parent, style) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テーブル列を作成します。<br><br>
	 *
	 * <p>メソッド名称： テーブル列作成</p>
	 *
	 * @param table		テーブル
	 * @param text		表示文字
	 * @param width		幅
	 */
	//--------------------------------------------------------------------------
	private void createTableColumn(Table table, String text, int width) {
		TableColumn column = new TableColumn( table, SWT.NONE) ;
		column.setText( text) ;
		column.setWidth( width) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ボタンを作成します。<br><br>
	 *
	 * <p>メソッド名称： ボタン作成</p>
	 *
	 * @param parent	親Composite
	 * @param text		表示文字
	 * @param listener	リスナ
	 * @return ボタン
	 */
	//--------------------------------------------------------------------------
	private Button createButton(Composite parent, String text, SwtSelectionListener listener) {
		Button button = new Button( parent, SWT.PUSH) ;
		button.setText( text) ;
		button.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				listener.widgetSelected( event) ;
			}
		}) ;
		return button ;
	}

	//--------------------------------------------------------------------------
	/**
	 * GridLayoutを作成します。<br><br>
	 *
	 * <p>メソッド名称： GridLayout作成</p>
	 *
	 * @param columns		列数
	 * @param equalWidth		同幅可否
	 * @param marginWidth	横余白
	 * @param marginHeight	縦余白
	 * @return GridLayout
	 */
	//--------------------------------------------------------------------------
	private GridLayout createGridLayout(int columns, boolean equalWidth, int marginWidth, int marginHeight) {
		GridLayout layout = new GridLayout( columns, equalWidth) ;
		layout.marginWidth = marginWidth ;
		layout.marginHeight = marginHeight ;
		return layout ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 横結合GridDataを作成します。<br><br>
	 *
	 * <p>メソッド名称： 横結合GridData作成</p>
	 *
	 * @param span	結合列数
	 * @return GridData
	 */
	//--------------------------------------------------------------------------
	private GridData createSpanData(int span) {
		GridData data = new GridData( SWT.FILL, SWT.CENTER, true, false) ;
		data.horizontalSpan = span ;
		return data ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 旧設定ファイルを移行します。<br><br>
	 *
	 * <p>メソッド名称： 旧設定ファイル移行</p>
	 */
	//--------------------------------------------------------------------------
	private void migrateLegacyConfiguration() {
		Path legacyConfigPath = TinyWinNfsPaths.getLegacyConfigPath( rootPath) ;

		// 新設定ファイルが既に存在する場合
		if( Files.exists( configPath)) {
			return ;
		}

		// 旧設定ファイルが存在しない場合
		if( !Files.exists( legacyConfigPath)) {
			return ;
		}

		// 移行元と移行先が同一の場合
		if( legacyConfigPath.equals( configPath)) {
			return ;
		}

		try {
			Files.createDirectories( configPath.getParent()) ;
			ConfigBackup.backupIfExists( legacyConfigPath, configPath.getParent(), "legacy-nfs-server") ;
			Files.copy( legacyConfigPath, configPath) ;
		} catch( IOException ioex) {
			// 保存時に権限エラーとして通知する
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定済み言語を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 設定済み言語読込</p>
	 *
	 * @return 言語コード
	 */
	//--------------------------------------------------------------------------
	private String readConfiguredLanguage() {
		Properties properties = new Properties() ;

		try {
			// 設定ファイルが存在する場合
			if( Files.exists( configPath)) {
				try( var reader = Files.newBufferedReader( configPath, StandardCharsets.UTF_8)) {
					properties.load( reader) ;
				}
			}
		} catch( IOException ioex) {
			return "auto" ;
		}

		return ManagerMessages.normalizeLanguage( properties.getProperty( "ui.language", "auto")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 言語選択を設定します。<br><br>
	 *
	 * <p>メソッド名称： 言語選択設定</p>
	 *
	 * @param language	言語コード
	 */
	//--------------------------------------------------------------------------
	private void setLanguageSelection(String language) {
		String normalized = ManagerMessages.normalizeLanguage( language) ;
		int index = switch( normalized) {
		case "en" -> 1 ;
		case "ja" -> 2 ;
		default -> 0 ;
		} ;
		uiLanguageCombo.select( index) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 選択中言語を取得します。<br><br>
	 *
	 * <p>メソッド名称： 選択中言語取得</p>
	 *
	 * @return 言語コード
	 */
	//--------------------------------------------------------------------------
	private String getSelectedLanguage() {
		int index = uiLanguageCombo.getSelectionIndex() ;

		return switch( index) {
		case 1 -> "en" ;
		case 2 -> "ja" ;
		default -> "auto" ;
		} ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 表示文字列を取得します。<br><br>
	 *
	 * <p>メソッド名称： 表示文字列取得</p>
	 *
	 * @param key	キー
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String text(String key) {
		return messages.text( key) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 表示文字列を書式化します。<br><br>
	 *
	 * <p>メソッド名称： 表示文字列書式化</p>
	 *
	 * @param key		キー
	 * @param arguments	引数
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String format(String key, Object... arguments) {
		return messages.format( key, arguments) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 真偽値表示を取得します。<br><br>
	 *
	 * <p>メソッド名称： 真偽値表示取得</p>
	 *
	 * @param value	値
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String boolText(boolean value) {
		return text( value ? "value.yes" : "value.no") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 許可クライアント表示を取得します。<br><br>
	 *
	 * <p>メソッド名称： 許可クライアント表示取得</p>
	 *
	 * @param value	許可クライアント
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String displayAllowedClients(String value) {
		// 未設定の場合
		if( value == null || value.isBlank()) {
			return text( "value.any") ;
		}

		return value ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有定義を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 共有定義読込</p>
	 *
	 * @param properties	設定値
	 */
	//--------------------------------------------------------------------------
	private void loadShareEntries(Properties properties) {
		shareEntries.clear() ;
		int count = parseInt( properties.getProperty( "exports.count"), 0) ;

		// 複数共有定義が存在する場合
		if( count > 0) {
			for( int i = 1; i <= count; i++) {
				String prefix = "exports." + i + "." ;
				String name = properties.getProperty( prefix + "name", "" ).trim() ;
				String path = properties.getProperty( prefix + "path", "" ).trim() ;
				boolean writable = Boolean.parseBoolean( properties.getProperty( prefix + "writable", "true")) ;
				String allowedClients = properties.getProperty( prefix + "allowed.clients", "" ).trim() ;

				// 共有定義が入力されている場合
				if( !name.isEmpty() && !path.isEmpty()) {
					shareEntries.add( new ShareEntry( name, resolveSharePath( path).toString(), writable, allowedClients)) ;
				}
			}
		}

		// 共有定義が存在しない場合
		if( shareEntries.isEmpty()) {
			String path = properties.getProperty( "export.path", defaultExportPath().toString()).trim() ;
			shareEntries.add( new ShareEntry(
					properties.getProperty( "export.name", "/export").trim(),
					resolveSharePath( path).toString(),
					Boolean.parseBoolean( properties.getProperty( "export.writable", "true")),
					properties.getProperty( "export.allowed.clients", "" ).trim())) ;
		}

		refreshShareTable() ;
		selectShare( 0) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有一覧を更新します。<br><br>
	 *
	 * <p>メソッド名称： 共有一覧更新</p>
	 */
	//--------------------------------------------------------------------------
	private void refreshShareTable() {
		exportTable.removeAll() ;

		// 共有定義を表示する
		for( ShareEntry entry : shareEntries) {
			TableItem item = new TableItem( exportTable, SWT.NONE) ;
			item.setText( new String[] {
					entry.getName(),
					entry.getPath(),
					boolText( entry.isWritable()),
					displayAllowedClients( entry.getAllowedClients()) }) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有定義を選択します。<br><br>
	 *
	 * <p>メソッド名称： 共有定義選択</p>
	 *
	 * @param index	選択位置
	 */
	//--------------------------------------------------------------------------
	private void selectShare(int index) {
		// 選択位置が不正な場合
		if( index < 0 || index >= shareEntries.size()) {
			selectedShareIndex = -1 ;
			exportNameText.setText( "" ) ;
			exportPathText.setText( "" ) ;
			exportWritableButton.setSelection( true) ;
			exportAllowedClientsText.setText( "" ) ;
			updateMountCommand() ;
			return ;
		}

		selectedShareIndex = index ;
		exportTable.setSelection( index) ;
		ShareEntry entry = shareEntries.get( index) ;
		exportNameText.setText( entry.getName()) ;
		exportPathText.setText( entry.getPath()) ;
		exportWritableButton.setSelection( entry.isWritable()) ;
		exportAllowedClientsText.setText( entry.getAllowedClients()) ;
		updateMountCommand() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有定義を追加します。<br><br>
	 *
	 * <p>メソッド名称： 共有定義追加</p>
	 */
	//--------------------------------------------------------------------------
	private void addShare() {
		try {
			ShareEntry entry = readShareFields() ;
			validateShareEntry( entry, -1) ;
			shareEntries.add( entry) ;
			refreshShareTable() ;
			selectShare( shareEntries.size() - 1) ;
			appendLog( format( "log.shareAdded", entry.getName())) ;
		} catch( Exception ex) {
			showError( text( "error.shareAddFailed"), ex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有定義を反映します。<br><br>
	 *
	 * <p>メソッド名称： 共有定義反映</p>
	 */
	//--------------------------------------------------------------------------
	private void applyShare() {
		try {
			applySelectedShare() ;
			appendLog( text( "log.shareApplied")) ;
		} catch( Exception ex) {
			showError( text( "error.shareApplyFailed"), ex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 選択中の共有定義へ入力値を反映します。<br><br>
	 *
	 * <p>メソッド名称： 選択共有定義反映</p>
	 */
	//--------------------------------------------------------------------------
	private void applySelectedShare() {
		// 選択中共有が存在しない場合
		if( selectedShareIndex < 0 || selectedShareIndex >= shareEntries.size()) {
			return ;
		}

		ShareEntry entry = readShareFields() ;
		validateShareEntry( entry, selectedShareIndex) ;
		shareEntries.set( selectedShareIndex, entry) ;
		refreshShareTable() ;
		selectShare( selectedShareIndex) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有定義を削除します。<br><br>
	 *
	 * <p>メソッド名称： 共有定義削除</p>
	 */
	//--------------------------------------------------------------------------
	private void removeShare() {
		// 選択中共有が存在しない場合
		if( selectedShareIndex < 0 || selectedShareIndex >= shareEntries.size()) {
			return ;
		}

		shareEntries.remove( selectedShareIndex) ;
		refreshShareTable() ;
		selectShare( Math.min( selectedShareIndex, shareEntries.size() - 1)) ;
		appendLog( text( "log.shareRemoved")) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 入力値から共有定義を取得します。<br><br>
	 *
	 * <p>メソッド名称： 共有定義取得</p>
	 *
	 * @return 共有定義
	 */
	//--------------------------------------------------------------------------
	private ShareEntry readShareFields() {
		return new ShareEntry(
				exportNameText.getText().trim(),
				exportPathText.getText().trim(),
				exportWritableButton.getSelection(),
				exportAllowedClientsText.getText().trim()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 表示対象の共有定義を取得します。<br><br>
	 *
	 * <p>メソッド名称： 表示共有定義取得</p>
	 *
	 * @return 共有定義
	 */
	//--------------------------------------------------------------------------
	private ShareEntry getSelectedOrFirstShare() {
		// 選択中共有が存在する場合
		if( selectedShareIndex >= 0 && selectedShareIndex < shareEntries.size()) {
			return readShareFields() ;
		}

		// 共有定義が存在する場合
		if( !shareEntries.isEmpty()) {
			return shareEntries.get( 0) ;
		}

		return new ShareEntry( "/export", defaultExportPath().toString(), true, "" ) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 既定公開フォルダを取得します。<br><br>
	 *
	 * <p>メソッド名称： 既定公開フォルダ取得</p>
	 *
	 * @return 既定公開フォルダ
	 */
	//--------------------------------------------------------------------------
	private Path defaultExportPath() {
		return TinyWinNfsPaths.getExportPath( rootPath) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有フォルダパスを解決します。<br><br>
	 *
	 * <p>メソッド名称： 共有フォルダパス解決</p>
	 *
	 * @param value	設定値
	 * @return 解決済みパス
	 */
	//--------------------------------------------------------------------------
	private Path resolveSharePath(String value) {
		return TinyWinNfsPaths.resolveConfiguredPath( dataRootPath, value) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 共有定義を検証します。<br><br>
	 *
	 * <p>メソッド名称： 共有定義検証</p>
	 *
	 * @param entry		共有定義
	 * @param selfIndex	自分の位置
	 */
	//--------------------------------------------------------------------------
	private void validateShareEntry(ShareEntry entry, int selfIndex) {
		// export名が不正な場合
		if( !isValidExportName( entry.getName())) {
			throw new IllegalArgumentException( text( "error.exportNameRequired") ) ;
		}

		// exportパスが未入力の場合
		if( entry.getPath().isEmpty()) {
			throw new IllegalArgumentException( text( "error.sharedFolderRequired") ) ;
		}

		Path exportPath = Path.of( entry.getPath()) ;

		// exportパスがフォルダではない場合
		if( !Files.isDirectory( exportPath)) {
			throw new IllegalArgumentException( format( "error.sharedFolderNotDirectory", exportPath)) ;
		}

		// exportパスが読込不可の場合
		if( !Files.isReadable( exportPath)) {
			throw new IllegalArgumentException( format( "error.sharedFolderNotReadable", exportPath)) ;
		}

		// 書込可能設定だがフォルダへ書き込めない場合
		if( entry.isWritable() && !Files.isWritable( exportPath)) {
			throw new IllegalArgumentException( format( "error.sharedFolderNotWritable", exportPath)) ;
		}

		validateAllowedClients( entry.getAllowedClients()) ;
		Path normalized = exportPath.toAbsolutePath().normalize() ;

		// 共有定義の重複を検証する
		for( int i = 0; i < shareEntries.size(); i++) {
			// 自分自身の場合
			if( i == selfIndex) {
				continue ;
			}

			ShareEntry existing = shareEntries.get( i) ;

			// export名が重複している場合
			if( existing.getName().equals( entry.getName())) {
				throw new IllegalArgumentException( format( "error.exportNameDuplicated", entry.getName())) ;
			}

			Path existingPath = Path.of( existing.getPath()).toAbsolutePath().normalize() ;

			// exportパスが重複している場合
			if( existingPath.equals( normalized)) {
				throw new IllegalArgumentException( format( "error.sharedFolderDuplicated", normalized)) ;
			}

			// exportパスが入れ子の場合
			if( existingPath.startsWith( normalized) || normalized.startsWith( existingPath)) {
				throw new IllegalArgumentException( format( "error.sharedFoldersNested", normalized, existingPath)) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開名を検証します。<br><br>
	 *
	 * <p>メソッド名称： 公開名検証</p>
	 *
	 * @param value	公開名
	 * @return true:正常 false:異常
	 */
	//--------------------------------------------------------------------------
	private boolean isValidExportName(String value) {
		// 公開名が絶対パス形式ではない場合
		if( value == null || value.isBlank() || !value.startsWith( "/") || "/".equals( value)) {
			return false ;
		}

		// 公開名が不正文字を含む場合
		if( value.contains( "\\" ) || value.contains( "//")) {
			return false ;
		}

		// 公開名の各文字を確認する
		for( int i = 0; i < value.length(); i++) {
			// 空白文字の場合
			if( Character.isWhitespace( value.charAt( i))) {
				return false ;
			}
		}

		String[] segments = value.split( "/" ) ;

		// パスセグメントを確認する
		for( String segment : segments) {
			// 親ディレクトリセグメントの場合
			if( "..".equals( segment)) {
				return false ;
			}
		}

		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 許可クライアントを検証します。<br><br>
	 *
	 * <p>メソッド名称： 許可クライアント検証</p>
	 *
	 * @param value	許可クライアント
	 */
	//--------------------------------------------------------------------------
	private void validateAllowedClients(String value) {
		// 未設定の場合
		if( value == null || value.isBlank()) {
			return ;
		}

		String[] entries = value.split( "," ) ;

		// 許可クライアントを検証する
		for( String entry : entries) {
			String address = entry.trim() ;

			// 空要素の場合
			if( address.isEmpty()) {
				continue ;
			}

			// IPv4アドレスではない場合
			if( !isExactIpv4Address( address)) {
				throw new IllegalArgumentException( format( "error.invalidAllowedClient", address)) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * IPv4アドレスを確認します。<br><br>
	 *
	 * <p>メソッド名称： IPv4アドレス確認</p>
	 *
	 * @param value	値
	 * @return true:IPv4 false:IPv4以外
	 */
	//--------------------------------------------------------------------------
	private boolean isExactIpv4Address(String value) {
		String[] parts = value.split( "\\.", -1) ;

		// 4オクテットではない場合
		if( parts.length != 4) {
			return false ;
		}

		// 各オクテットを検証する
		for( String part : parts) {
			// 空オクテットの場合
			if( part.isEmpty()) {
				return false ;
			}

			try {
				int octet = Integer.parseInt( part) ;

				// 範囲外の場合
				if( octet < 0 || octet > 255) {
					return false ;
				}
			} catch( NumberFormatException nfex) {
				return false ;
			}
		}

		return true ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： 設定読込</p>
	 */
	//--------------------------------------------------------------------------
	private void loadConfig() {
		Properties properties = new Properties() ;

		try {
			// 設定ファイルが存在する場合
			if( Files.exists( configPath)) {
				try( var reader = Files.newBufferedReader( configPath, StandardCharsets.UTF_8)) {
					properties.load( reader) ;
				}
			}

			loadShareEntries( properties) ;
			setLanguageSelection( ManagerMessages.normalizeLanguage( properties.getProperty( "ui.language", messages.getLanguageCode()))) ;
			serverHostText.setText( properties.getProperty( "client.server.host", detectLocalHostName())) ;
			clientMountPointText.setText( properties.getProperty( "client.mount.point", "/mnt")) ;
			portmapPortText.setText( properties.getProperty( "portmap.port", "111")) ;
			nfsPortText.setText( properties.getProperty( "nfs.port", "2049")) ;
			mountPortText.setText( properties.getProperty( "mount.port", "20048")) ;
			uidText.setText( properties.getProperty( "uid", "0")) ;
			gidText.setText( properties.getProperty( "gid", "0")) ;
			fileModeText.setText( properties.getProperty( "file.mode", "0644")) ;
			directoryModeText.setText( properties.getProperty( "directory.mode", "0755")) ;
			blockSizeText.setText( properties.getProperty( "block.size", "4096")) ;
			readSizeText.setText( properties.getProperty( "read.size", "8192")) ;
			writeSizeText.setText( properties.getProperty( "write.size", readSizeText.getText())) ;
			directoryPreferredSizeText.setText( properties.getProperty( "directory.preferred.size", blockSizeText.getText())) ;
			maxFileSizeText.setText( properties.getProperty( "max.file.size", "9223372036854775807")) ;
			timeDeltaNanosText.setText( properties.getProperty( "time.delta.nanos", "1000000")) ;
			pathconfLinkMaxText.setText( properties.getProperty( "pathconf.link.max", "1024")) ;
			pathconfNameMaxText.setText( properties.getProperty( "pathconf.name.max", "255")) ;
			filenameCharsetText.setText( properties.getProperty( "filename.charset", "UTF-8")) ;
			writeSyncButton.setSelection( Boolean.parseBoolean( properties.getProperty( "write.sync", "false"))) ;
			writeCacheEnabledButton.setSelection( Boolean.parseBoolean( properties.getProperty( "write.cache.enabled", "true"))) ;
			writeCacheMaxOpenText.setText( properties.getProperty( "write.cache.max.open", "64")) ;
			writeCacheIdleMillisText.setText( properties.getProperty( "write.cache.idle.millis", "3000")) ;
			updateMountCommand() ;
			appendLog( text( "log.configurationLoaded")) ;
		} catch( IOException ioex) {
			appendLog( format( "log.configurationLoadFailed", ioex.getMessage())) ;
			showError( text( "error.configurationLoadFailed"), ioex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定を保存します。<br><br>
	 *
	 * <p>メソッド名称： 設定保存</p>
	 *
	 * @return true:保存成功 false:保存失敗
	 */
	//--------------------------------------------------------------------------
	private boolean saveConfig() {
		try {
			applySelectedShare() ;
			validateFields() ;
			Path configDirectory = configPath.getParent() ;

			// 保護されたインストール先に一般ユーザーで保存する場合
			if( shouldRequireAdministratorForConfigSave( configDirectory)) {
				showConfigurationSaveAdminRequired() ;
				return false ;
			}

			Files.createDirectories( configDirectory) ;
			List<String> lines = new ArrayList<String>() ;
			ShareEntry firstShare = shareEntries.get( 0) ;
			lines.add( "# " + PRODUCT_NAME + " configuration.") ;
			lines.add( "" ) ;
			lines.add( "portmap.port=" + portmapPortText.getText().trim()) ;
			lines.add( "nfs.port=" + nfsPortText.getText().trim()) ;
			lines.add( "mount.port=" + mountPortText.getText().trim()) ;
			lines.add( "" ) ;
			lines.add( "export.name=" + firstShare.getName()) ;
			lines.add( "export.path=" + escapePath( firstShare.getPath()) ) ;
			lines.add( "export.writable=" + firstShare.isWritable()) ;
			lines.add( "export.allowed.clients=" + firstShare.getAllowedClients()) ;
			lines.add( "" ) ;
			lines.add( "exports.count=" + shareEntries.size()) ;

			for( int i = 0; i < shareEntries.size(); i++) {
				ShareEntry entry = shareEntries.get( i) ;
				String prefix = "exports." + (i + 1) + "." ;
				lines.add( prefix + "name=" + entry.getName()) ;
				lines.add( prefix + "path=" + escapePath( entry.getPath()) ) ;
				lines.add( prefix + "writable=" + entry.isWritable()) ;
				lines.add( prefix + "allowed.clients=" + entry.getAllowedClients()) ;
			}

			lines.add( "" ) ;
			lines.add( "client.server.host=" + serverHostText.getText().trim()) ;
			lines.add( "client.mount.point=" + clientMountPointText.getText().trim()) ;
			lines.add( "ui.language=" + getSelectedLanguage()) ;
			lines.add( "" ) ;
			lines.add( "uid=" + uidText.getText().trim()) ;
			lines.add( "gid=" + gidText.getText().trim()) ;
			lines.add( "file.mode=" + fileModeText.getText().trim()) ;
			lines.add( "directory.mode=" + directoryModeText.getText().trim()) ;
			lines.add( "block.size=" + blockSizeText.getText().trim()) ;
			lines.add( "read.size=" + readSizeText.getText().trim()) ;
			lines.add( "write.size=" + writeSizeText.getText().trim()) ;
			lines.add( "directory.preferred.size=" + directoryPreferredSizeText.getText().trim()) ;
			lines.add( "max.file.size=" + maxFileSizeText.getText().trim()) ;
			lines.add( "time.delta.nanos=" + timeDeltaNanosText.getText().trim()) ;
			lines.add( "pathconf.link.max=" + pathconfLinkMaxText.getText().trim()) ;
			lines.add( "pathconf.name.max=" + pathconfNameMaxText.getText().trim()) ;
			lines.add( "write.sync=" + writeSyncButton.getSelection()) ;
			lines.add( "write.cache.enabled=" + writeCacheEnabledButton.getSelection()) ;
			lines.add( "write.cache.max.open=" + writeCacheMaxOpenText.getText().trim()) ;
			lines.add( "write.cache.idle.millis=" + writeCacheIdleMillisText.getText().trim()) ;
			lines.add( "filename.charset=" + filenameCharsetText.getText().trim()) ;
			Path backupPath = writeValidatedConfig( configDirectory, lines) ;
			updateMountCommand() ;
			appendLog( text( "log.configurationSaved")) ;

			// バックアップを作成した場合
			if( backupPath != null) {
				appendLog( format( "log.configurationBackupCreated", backupPath)) ;
			}

			return true ;
		} catch( AccessDeniedException adex) {
			showConfigurationSaveAdminRequired() ;
			return false ;
		} catch( Exception ex) {
			appendLog( format( "log.configurationSaveFailed", ex.getMessage())) ;
			showError( text( "error.configurationSaveFailed"), ex) ;
			return false ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 検証済み設定を書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 検証済み設定書込</p>
	 *
	 * @param configDirectory	設定ディレクトリ
	 * @param lines				設定行
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private Path writeValidatedConfig(Path configDirectory, List<String> lines) throws IOException {
		Path temporaryPath = Files.createTempFile( configDirectory, "nfs-server-", ".properties") ;

		try {
			Files.write( temporaryPath, lines, StandardCharsets.UTF_8) ;
			NfsServerConfig.load( temporaryPath) ;
			Path backupPath = ConfigBackup.backupIfExists( configPath) ;
			moveConfigIntoPlace( temporaryPath) ;
			return backupPath ;
		} finally {
			Files.deleteIfExists( temporaryPath) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定ファイルを配置します。<br><br>
	 *
	 * <p>メソッド名称： 設定ファイル配置</p>
	 *
	 * @param temporaryPath	一時設定ファイル
	 * @throws IOException 移動異常
	 */
	//--------------------------------------------------------------------------
	private void moveConfigIntoPlace(Path temporaryPath) throws IOException {
		try {
			Files.move(
					temporaryPath,
					configPath,
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE) ;
		} catch( AtomicMoveNotSupportedException amnsex) {
			Files.move( temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定保存に管理者権限が必要か確認します。<br><br>
	 *
	 * <p>メソッド名称： 設定保存権限確認</p>
	 *
	 * @param configDirectory	設定ディレクトリ
	 * @return true:管理者権限が必要 false:不要
	 */
	//--------------------------------------------------------------------------
	private boolean shouldRequireAdministratorForConfigSave(Path configDirectory) {
		// 既に管理者権限を持つ場合
		if( isAdministrator()) {
			return false ;
		}

		return isProtectedInstallPath( configDirectory) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 保護されたインストール先か確認します。<br><br>
	 *
	 * <p>メソッド名称： 保護インストール先確認</p>
	 *
	 * @param path	確認パス
	 * @return true:保護パス false:非保護パス
	 */
	//--------------------------------------------------------------------------
	private boolean isProtectedInstallPath(Path path) {
		Path absolutePath = path.toAbsolutePath().normalize() ;
		return isUnderEnvironmentPath( absolutePath, "ProgramFiles")
				|| isUnderEnvironmentPath( absolutePath, "ProgramFiles(x86)")
				|| isUnderEnvironmentPath( absolutePath, "ProgramData") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 環境変数で指定されるフォルダ配下か確認します。<br><br>
	 *
	 * <p>メソッド名称： 環境変数配下確認</p>
	 *
	 * @param path			確認パス
	 * @param variableName	環境変数名
	 * @return true:配下 false:配下ではない
	 */
	//--------------------------------------------------------------------------
	private boolean isUnderEnvironmentPath(Path path, String variableName) {
		String value = System.getenv( variableName) ;

		// 環境変数が未設定の場合
		if( value == null || value.isBlank()) {
			return false ;
		}

		try {
			Path root = Path.of( value).toAbsolutePath().normalize() ;
			return path.startsWith( root) ;
		} catch( RuntimeException rex) {
			return false ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定保存に管理者権限が必要であることを通知します。<br><br>
	 *
	 * <p>メソッド名称： 設定保存管理者権限通知</p>
	 */
	//--------------------------------------------------------------------------
	private void showConfigurationSaveAdminRequired() {
		appendLog( format( "log.configurationSaveAdminRequired", configPath)) ;
		showError( format( "error.configurationSaveAdminRequired", configPath), null) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定を保存してサービスを再起動します。<br><br>
	 *
	 * <p>メソッド名称： 設定保存サービス再起動</p>
	 */
	//--------------------------------------------------------------------------
	private void saveConfigAndRestart() {
		boolean saved = saveConfig() ;

		// 保存に成功した場合
		if( saved) {
			appendLog( text( "log.restartAfterSaveStarted")) ;
			runPrivilegedScriptAsync( "restart-service.ps1", result -> {
				refreshStatus() ;

				// 再起動に成功した場合
				if( result.getExitCode() == 0) {
					appendLog( text( "log.restartAfterSaveSucceeded")) ;
				}
				// 再起動に失敗した場合
				else {
					appendLog( format( "log.restartAfterSaveFailed", result.getExitCode())) ;
				}
			}) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 入力値を検証します。<br><br>
	 *
	 * <p>メソッド名称： 入力値検証</p>
	 */
	//--------------------------------------------------------------------------
	private void validateFields() {
		validatePort( text( "label.portmapUdpPort"), portmapPortText.getText()) ;
		validatePort( text( "label.nfsUdpPort"), nfsPortText.getText()) ;
		validatePort( text( "label.mountUdpPort"), mountPortText.getText()) ;
		validatePositiveInt( text( "label.blockSize"), blockSizeText.getText()) ;
		validatePositiveInt( text( "label.readSize"), readSizeText.getText()) ;
		validatePositiveInt( text( "label.writeSize"), writeSizeText.getText()) ;
		validatePositiveInt( text( "label.directoryPreferredSize"), directoryPreferredSizeText.getText()) ;
		validatePositiveLong( text( "label.maxFileSize"), maxFileSizeText.getText()) ;
		validateTimeDeltaNanos() ;
		validatePositiveInt( text( "label.pathconfLinkMax"), pathconfLinkMaxText.getText()) ;
		validatePathconfNameMax() ;
		validatePositiveInt( text( "label.writeCacheMaxOpen"), writeCacheMaxOpenText.getText()) ;
		validatePositiveInt( text( "label.writeCacheIdleMillis"), writeCacheIdleMillisText.getText()) ;
		validateInt( text( "label.uid"), uidText.getText()) ;
		validateInt( text( "label.gid"), gidText.getText()) ;
		validateCharset( text( "label.filenameCharset"), filenameCharsetText.getText()) ;

		// 共有定義が存在しない場合
		if( shareEntries.isEmpty()) {
			throw new IllegalArgumentException( text( "error.atLeastOneSharedFolder") ) ;
		}

		// 共有定義を検証する
		for( int i = 0; i < shareEntries.size(); i++) {
			validateShareEntry( shareEntries.get( i), i) ;
		}

		// クライアント側マウントポイントが不正な場合
		if( !clientMountPointText.getText().trim().startsWith( "/")) {
			throw new IllegalArgumentException( text( "error.clientMountPointRequired") ) ;
		}

		// サーバーホストが未入力の場合
		if( serverHostText.getText().trim().isEmpty()) {
			throw new IllegalArgumentException( text( "error.serverHostRequired") ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ポートを検証します。<br><br>
	 *
	 * <p>メソッド名称： ポート検証</p>
	 *
	 * @param label	ラベル
	 * @param value	値
	 */
	//--------------------------------------------------------------------------
	private void validatePort(String label, String value) {
		int port = validatePositiveInt( label, value) ;

		// ポートが範囲外の場合
		if( port > 65535) {
			throw new IllegalArgumentException( format( "error.portOutOfRange", label) ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 正数を検証します。<br><br>
	 *
	 * <p>メソッド名称： 正数検証</p>
	 *
	 * @param label	ラベル
	 * @param value	値
	 * @return 数値
	 */
	//--------------------------------------------------------------------------
	private int validatePositiveInt(String label, String value) {
		int number = validateInt( label, value) ;

		// 正数ではない場合
		if( number <= 0) {
			throw new IllegalArgumentException( format( "error.mustBePositive", label) ) ;
		}

		return number ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 正のlong値を検証します。<br><br>
	 *
	 * <p>メソッド名称： 正long値検証</p>
	 *
	 * @param label	ラベル
	 * @param value	値
	 * @return 数値
	 */
	//--------------------------------------------------------------------------
	private long validatePositiveLong(String label, String value) {
		long number = validateLong( label, value) ;

		// 正数ではない場合
		if( number <= 0L) {
			throw new IllegalArgumentException( format( "error.mustBePositive", label) ) ;
		}

		return number ;
	}

	//--------------------------------------------------------------------------
	/**
	 * PATHCONF name最大値を検証します。<br><br>
	 *
	 * <p>メソッド名称： PATHCONF name最大値検証</p>
	 */
	//--------------------------------------------------------------------------
	private void validatePathconfNameMax() {
		int value = validatePositiveInt( text( "label.pathconfNameMax"), pathconfNameMaxText.getText()) ;

		// NFSファイル名上限を超える場合
		if( value > 255) {
			throw new IllegalArgumentException( format( "error.pathconfNameMaxOutOfRange", text( "label.pathconfNameMax")) ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 時刻精度ナノ秒を検証します。<br><br>
	 *
	 * <p>メソッド名称： 時刻精度ナノ秒検証</p>
	 */
	//--------------------------------------------------------------------------
	private void validateTimeDeltaNanos() {
		int value = validatePositiveInt( text( "label.timeDeltaNanos"), timeDeltaNanosText.getText()) ;

		// NFS nsecondsの範囲外の場合
		if( value > 999999999) {
			throw new IllegalArgumentException( format( "error.timeDeltaNanosOutOfRange", text( "label.timeDeltaNanos")) ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 整数を検証します。<br><br>
	 *
	 * <p>メソッド名称： 整数検証</p>
	 *
	 * @param label	ラベル
	 * @param value	値
	 * @return 数値
	 */
	//--------------------------------------------------------------------------
	private int validateInt(String label, String value) {
		try {
			return Integer.parseInt( value.trim()) ;
		} catch( NumberFormatException nfex) {
			throw new IllegalArgumentException( format( "error.mustBeInteger", label) ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * long値を検証します。<br><br>
	 *
	 * <p>メソッド名称： long値検証</p>
	 *
	 * @param label	ラベル
	 * @param value	値
	 * @return 数値
	 */
	//--------------------------------------------------------------------------
	private long validateLong(String label, String value) {
		try {
			return Long.parseLong( value.trim()) ;
		} catch( NumberFormatException nfex) {
			throw new IllegalArgumentException( format( "error.mustBeInteger", label) ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字コードを検証します。<br><br>
	 *
	 * <p>メソッド名称： 文字コード検証</p>
	 *
	 * @param label	ラベル
	 * @param value	値
	 */
	//--------------------------------------------------------------------------
	private void validateCharset(String label, String value) {
		try {
			Charset.forName( value.trim()) ;
		} catch( RuntimeException rex) {
			throw new IllegalArgumentException( format( "error.invalidCharset", label) ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 整数を変換します。<br><br>
	 *
	 * <p>メソッド名称： 整数変換</p>
	 *
	 * @param value			値
	 * @param defaultValue	デフォルト値
	 * @return 整数
	 */
	//--------------------------------------------------------------------------
	private int parseInt(String value, int defaultValue) {
		// 値がない場合
		if( value == null || value.isBlank()) {
			return defaultValue ;
		}

		try {
			return Integer.parseInt( value.trim()) ;
		} catch( NumberFormatException nfex) {
			return defaultValue ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * パスを設定ファイル用にエスケープします。<br><br>
	 *
	 * <p>メソッド名称： パスエスケープ</p>
	 *
	 * @param path	パス
	 * @return エスケープ済みパス
	 */
	//--------------------------------------------------------------------------
	private String escapePath(String path) {
		return path.trim().replace( "\\", "\\\\" ) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * exportパス選択を処理します。<br><br>
	 *
	 * <p>メソッド名称： exportパス選択処理</p>
	 */
	//--------------------------------------------------------------------------
	private void browseExportPath() {
		DirectoryDialog dialog = new DirectoryDialog( shell, SWT.OPEN) ;
		dialog.setText( text( "dialog.selectSharedFolder")) ;

		// 現在値が存在する場合
		if( !exportPathText.getText().trim().isEmpty()) {
			dialog.setFilterPath( exportPathText.getText().trim()) ;
		}

		String selected = dialog.open() ;

		// 選択された場合
		if( selected != null) {
			exportPathText.setText( Path.of( selected).toAbsolutePath().normalize().toString()) ;
			updateMountCommand() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーホストを検出します。<br><br>
	 *
	 * <p>メソッド名称： サーバーホスト検出</p>
	 */
	//--------------------------------------------------------------------------
	private void detectServerHost() {
		serverHostText.setText( detectLocalHostName()) ;
		updateMountCommand() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ローカルホスト名を検出します。<br><br>
	 *
	 * <p>メソッド名称： ローカルホスト名検出</p>
	 *
	 * @return ローカルホスト名
	 */
	//--------------------------------------------------------------------------
	private String detectLocalHostName() {
		try {
			String hostName = InetAddress.getLocalHost().getHostName() ;

			// ホスト名が取得できた場合
			if( hostName != null && !hostName.isBlank()) {
				return hostName ;
			}
		} catch( Exception ex) {
			// ホスト名を取得できない場合
		}

		return "windows-host" ;
	}

	//--------------------------------------------------------------------------
	/**
	 * mountコマンドを更新します。<br><br>
	 *
	 * <p>メソッド名称： mountコマンド更新</p>
	 */
	//--------------------------------------------------------------------------
	private void updateMountCommand() {
		String host = serverHostText.getText().trim() ;
		String mountPoint = clientMountPointText.getText().trim() ;
		ShareEntry share = getSelectedOrFirstShare() ;
		String exportName = share.getName() ;

		// ホストが未入力の場合
		if( host.isEmpty()) {
			host = "windows-host" ;
		}

		// export名が未入力の場合
		if( exportName.isEmpty()) {
			exportName = "/export" ;
		}

		// マウントポイントが未入力の場合
		if( mountPoint.isEmpty()) {
			mountPoint = "/mnt" ;
		}

		mountCommandText.setText(
				"mount_nfs " + host + ":" + exportName + " " + mountPoint + System.lineSeparator()
				+ System.lineSeparator()
				+ text( "mount.serverExportPath") + " " + share.getPath() + System.lineSeparator()
				+ text( "mount.writable") + " " + boolText( share.isWritable()) + System.lineSeparator()
				+ System.lineSeparator()
				+ text( "mount.availableExports") + System.lineSeparator()
				+ buildExportSummary()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 公開定義概要を作成します。<br><br>
	 *
	 * <p>メソッド名称： 公開定義概要作成</p>
	 *
	 * @return 公開定義概要
	 */
	//--------------------------------------------------------------------------
	private String buildExportSummary() {
		StringBuilder builder = new StringBuilder() ;

		// 公開定義を出力する
		for( ShareEntry entry : shareEntries) {
			builder.append( entry.getName())
					.append( " -> " )
					.append( entry.getPath())
					.append( " (" )
					.append( text( entry.isWritable() ? "value.rw" : "value.ro") )
					.append( ")" )
					.append( System.lineSeparator()) ;
		}

		return builder.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * mountコマンドをコピーします。<br><br>
	 *
	 * <p>メソッド名称： mountコマンドコピー</p>
	 */
	//--------------------------------------------------------------------------
	private void copyMountCommand() {
		updateMountCommand() ;
		String command = mountCommandText.getText().split( "\\R", 2)[0] ;
			Clipboard clipboard = new Clipboard( display) ;

		try {
			clipboard.setContents( new Object[] { command }, new Transfer[] { TextTransfer.getInstance() }) ;
			appendLog( format( "log.mountCommandCopied", command)) ;
		} finally {
			clipboard.dispose() ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 状態を更新します。<br><br>
	 *
	 * <p>メソッド名称： 状態更新</p>
	 */
	//--------------------------------------------------------------------------
	private void refreshStatus() {
		runCommandAsync( "service status", List.of( "powershell.exe", "-NoProfile", "-Command",
				"$names=@('" + SERVICE_NAME + "','" + String.join( "','", LEGACY_SERVICE_NAMES) + "'); "
				+ "foreach($n in $names){"
				+ "$s=Get-Service -Name $n -ErrorAction SilentlyContinue; "
				+ "if($s -ne $null){$s.Status.ToString() + ' (' + $n + ')'; exit}"
				+ "}; 'Not installed'" ),
				result -> {
					String status = result.getOutput().trim() ;

					// 状態が空の場合
					if( status.isEmpty()) {
						status = text( "status.unknown") ;
					}

					// 未インストールの場合
					if( "Not installed".equals( status)) {
						status = text( "status.notInstalled") ;
					}

					statusValueLabel.setText( status) ;
					adminValueLabel.setText( boolText( isAdministrator()) ) ;
					updateServiceInfoText() ;
					shell.layout( true, true) ;
				}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービス情報表示を更新します。<br><br>
	 *
	 * <p>メソッド名称： サービス情報表示更新</p>
	 */
	//--------------------------------------------------------------------------
	private void updateServiceInfoText() {
		// サービス情報表示が存在しない場合
		if( serviceInfoText == null || serviceInfoText.isDisposed()) {
			return ;
		}

		Path serviceExecutable = detectInstalledServiceExecutablePath() ;
		String executableText = serviceExecutable == null ? text( "status.notInstalled") : serviceExecutable.toString() ;
		Path logPath = TinyWinNfsPaths.getLogPath( rootPath) ;
		Path defaultExportPath = defaultExportPath() ;
		serviceInfoText.setText( format(
				"service.info",
				SERVICE_NAME,
				String.join( ", ", LEGACY_SERVICE_NAMES),
				rootPath,
				dataRootPath,
				configPath,
				executableText,
				logPath,
				describePathStatus( dataRootPath, true),
				describePathStatus( configPath, false),
				describePathStatus( defaultExportPath, true),
				describePathStatus( logPath.getParent(), true),
				describePortStatus( portmapPortText),
				describePortStatus( nfsPortText),
				describePortStatus( mountPortText),
				getWindowsNfsClientStatus(),
				configPath.getParent().resolve( "backups"))) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断パッケージを作成します。<br><br>
	 *
	 * <p>メソッド名称： 診断パッケージ作成</p>
	 */
	//--------------------------------------------------------------------------
	private void createDiagnosticPackage() {
		updateServiceInfoText() ;
		String summary = buildDiagnosticSummary() ;
		Path logPath = TinyWinNfsPaths.getLogPath( rootPath) ;
		Path backupPath = configPath.getParent().resolve( "backups") ;
		Path winswLogPath = getWinswLogPath() ;
		appendLog( text( "log.diagnosticPackageStarted")) ;

		new Thread( () -> {
			try {
				Path packagePath = writeDiagnosticPackage( summary, logPath, backupPath, winswLogPath) ;
				runOnUi( () -> {
					appendLog( format( "log.diagnosticPackageCreated", packagePath)) ;
					openPath( packagePath) ;
				}) ;
			} catch( Exception ex) {
				runOnUi( () -> {
					appendLog( format( "log.diagnosticPackageFailed", ex.getMessage())) ;
					showError( text( "error.diagnosticPackageFailed"), ex) ;
				}) ;
			}
		}, "diagnostic-package").start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断概要を作成します。<br><br>
	 *
	 * <p>メソッド名称： 診断概要作成</p>
	 *
	 * @return 診断概要
	 */
	//--------------------------------------------------------------------------
	private String buildDiagnosticSummary() {
		StringBuilder builder = new StringBuilder() ;
		builder.append( PRODUCT_NAME).append( " diagnostics" ).append( System.lineSeparator()) ;
		builder.append( "created=" ).append( LocalDateTime.now()).append( System.lineSeparator()) ;
		builder.append( "administrator=" ).append( isAdministrator()).append( System.lineSeparator()) ;
		builder.append( "serviceStatus=" ).append( statusValueLabel.getText()).append( System.lineSeparator()) ;
		builder.append( System.lineSeparator()) ;
		builder.append( "Service information" ).append( System.lineSeparator()) ;
		builder.append( serviceInfoText == null || serviceInfoText.isDisposed() ? "" : serviceInfoText.getText()).append( System.lineSeparator()) ;
		builder.append( System.lineSeparator()) ;
		builder.append( "Exports" ).append( System.lineSeparator()) ;
		builder.append( buildExportSummary()) ;
		return builder.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断パッケージを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 診断パッケージ書込</p>
	 *
	 * @param summary		概要
	 * @param logPath		ログパス
	 * @param backupPath	バックアップパス
	 * @param winswLogPath	WinSWログパス
	 * @return 診断パッケージ
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private Path writeDiagnosticPackage(String summary, Path logPath, Path backupPath, Path winswLogPath) throws IOException {
		Path diagnosticDirectory = dataRootPath.resolve( "diagnostics") ;
		Files.createDirectories( diagnosticDirectory) ;
		String timestamp = DateTimeFormatter.ofPattern( "yyyyMMdd-HHmmss").format( LocalDateTime.now()) ;
		Path packagePath = diagnosticDirectory.resolve( "tinywin-nfs-diagnostics-" + timestamp + ".zip") ;

		try( ZipOutputStream zip = new ZipOutputStream( Files.newOutputStream( packagePath))) {
			addTextEntry( zip, "summary.txt", summary) ;
			addFileEntryIfExists( zip, "conf/nfs-server.properties", configPath) ;
			addDirectoryEntries( zip, "conf/backups", backupPath, ".properties") ;
			addFileEntryIfExists( zip, "logs/nfs-server.log", logPath) ;
			addDirectoryEntries( zip, "winsw-logs", winswLogPath, ".log", ".out", ".err") ;
		}

		return packagePath ;
	}

	//--------------------------------------------------------------------------
	/**
	 * テキストエントリを追加します。<br><br>
	 *
	 * <p>メソッド名称： テキストエントリ追加</p>
	 *
	 * @param zip		Zip出力
	 * @param entryName	エントリ名
	 * @param text		テキスト
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private void addTextEntry(ZipOutputStream zip, String entryName, String text) throws IOException {
		zip.putNextEntry( new ZipEntry( entryName)) ;
		zip.write( text.getBytes( StandardCharsets.UTF_8)) ;
		zip.closeEntry() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ファイルエントリを追加します。<br><br>
	 *
	 * <p>メソッド名称： ファイルエントリ追加</p>
	 *
	 * @param zip		Zip出力
	 * @param entryName	エントリ名
	 * @param filePath	ファイルパス
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private void addFileEntryIfExists(ZipOutputStream zip, String entryName, Path filePath) throws IOException {
		// 通常ファイルではない場合
		if( !Files.isRegularFile( filePath)) {
			return ;
		}

		zip.putNextEntry( new ZipEntry( entryName.replace( "\\", "/" ))) ;
		Files.copy( filePath, zip) ;
		zip.closeEntry() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ディレクトリ内エントリを追加します。<br><br>
	 *
	 * <p>メソッド名称： ディレクトリ内エントリ追加</p>
	 *
	 * @param zip			Zip出力
	 * @param entryPrefix	エントリ接頭辞
	 * @param directory		ディレクトリ
	 * @param extensions	拡張子
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private void addDirectoryEntries(ZipOutputStream zip, String entryPrefix, Path directory, String... extensions) throws IOException {
		// ディレクトリではない場合
		if( !Files.isDirectory( directory)) {
			return ;
		}

		try( var stream = Files.list( directory)) {
			List<Path> files = stream
					.filter( Files::isRegularFile )
					.sorted()
					.toList() ;

			// 対象ファイルを追加する
			for( Path file : files) {
				// 拡張子が対象外の場合
				if( !hasAnyExtension( file, extensions)) {
					continue ;
				}

				addFileEntryIfExists( zip, entryPrefix + "/" + file.getFileName().toString(), file) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 拡張子一致を確認します。<br><br>
	 *
	 * <p>メソッド名称： 拡張子一致確認</p>
	 *
	 * @param file			ファイル
	 * @param extensions	拡張子
	 * @return true:一致 false:不一致
	 */
	//--------------------------------------------------------------------------
	private boolean hasAnyExtension(Path file, String... extensions) {
		String fileName = file.getFileName().toString().toLowerCase() ;

		// 拡張子を確認する
		for( String extension : extensions) {
			// 拡張子が一致する場合
			if( fileName.endsWith( extension.toLowerCase())) {
				return true ;
			}
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パス状態を表示します。<br><br>
	 *
	 * <p>メソッド名称： パス状態表示</p>
	 *
	 * @param path				パス
	 * @param directoryRequired	true:ディレクトリ必須 false:ファイル
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String describePathStatus(Path path, boolean directoryRequired) {
		// パスが存在する場合
		if( Files.exists( path)) {
			// ディレクトリ必須でディレクトリではない場合
			if( directoryRequired && !Files.isDirectory( path)) {
				return text( "diagnostic.notDirectory") ;
			}

			return Files.isWritable( path) ? text( "diagnostic.existsWritable") : text( "diagnostic.existsReadOnly") ;
		}

		Path parent = directoryRequired ? path.getParent() : path.toAbsolutePath().normalize().getParent() ;

		// 親パスが書込可能な場合
		if( parent != null && Files.exists( parent) && Files.isWritable( parent)) {
			return text( "diagnostic.missingParentWritable") ;
		}

		return text( "diagnostic.missingParentNotWritable") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ポート状態を表示します。<br><br>
	 *
	 * <p>メソッド名称： ポート状態表示</p>
	 *
	 * @param portText	ポートText
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String describePortStatus(Text portText) {
		// ポートTextが存在しない場合
		if( portText == null || portText.isDisposed()) {
			return text( "status.unknown") ;
		}

		try {
			int port = Integer.parseInt( portText.getText().trim()) ;
			return format( "diagnostic.portStatus", describeUdpPortStatus( port), describeTcpPortStatus( port)) ;
		} catch( NumberFormatException nfex) {
			return text( "status.unknown") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * UDPポート状態を表示します。<br><br>
	 *
	 * <p>メソッド名称： UDPポート状態表示</p>
	 *
	 * @param port	ポート
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String describeUdpPortStatus(int port) {
		try( DatagramSocket socket = new DatagramSocket( null)) {
			socket.setReuseAddress( false) ;
			socket.bind( new InetSocketAddress( InetAddress.getByName( "0.0.0.0"), port)) ;
			return text( "diagnostic.available") ;
		} catch( IOException ioex) {
			return text( "diagnostic.inUseOrProtected") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * TCPポート状態を表示します。<br><br>
	 *
	 * <p>メソッド名称： TCPポート状態表示</p>
	 *
	 * @param port	ポート
	 * @return 表示文字列
	 */
	//--------------------------------------------------------------------------
	private String describeTcpPortStatus(int port) {
		try( ServerSocket socket = new ServerSocket()) {
			socket.setReuseAddress( false) ;
			socket.bind( new InetSocketAddress( InetAddress.getByName( "0.0.0.0"), port)) ;
			return text( "diagnostic.available") ;
		} catch( IOException ioex) {
			return text( "diagnostic.inUseOrProtected") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * Windows NFS Client状態を取得します。<br><br>
	 *
	 * <p>メソッド名称： Windows NFS Client状態取得</p>
	 *
	 * @return 状態
	 */
	//--------------------------------------------------------------------------
	private String getWindowsNfsClientStatus() {
		try {
			ProcessBuilder builder = new ProcessBuilder(
					"powershell.exe",
					"-NoProfile",
					"-Command",
					"$s=Get-Service -Name NfsClnt -ErrorAction SilentlyContinue; "
					+ "if($s -eq $null){'Not installed'}else{$s.Status.ToString()}") ;
			builder.redirectErrorStream( true) ;
			Process process = builder.start() ;
			String output = readProcessOutput( process).trim() ;
			process.waitFor() ;

			// 状態が空の場合
			if( output.isBlank()) {
				return text( "status.unknown") ;
			}

			// 未インストールの場合
			if( "Not installed".equals( output)) {
				return text( "status.notInstalled") ;
			}

			return output ;
		} catch( IOException ioex) {
			return text( "status.unknown") ;
		} catch( InterruptedException iex) {
			Thread.currentThread().interrupt() ;
			return text( "status.unknown") ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * WinSWログパスを取得します。<br><br>
	 *
	 * <p>メソッド名称： WinSWログパス取得</p>
	 *
	 * @return WinSWログパス
	 */
	//--------------------------------------------------------------------------
	private Path getWinswLogPath() {
		Path serviceExecutable = detectInstalledServiceExecutablePath() ;

		// サービス実行ファイルが取得できる場合
		if( serviceExecutable != null && serviceExecutable.getParent() != null) {
			return serviceExecutable.getParent() ;
		}

		return rootPath.resolve( "service").resolve( "winsw") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * パスを開きます。<br><br>
	 *
	 * <p>メソッド名称： パスオープン</p>
	 *
	 * @param path	パス
	 */
	//--------------------------------------------------------------------------
	private void openPath(Path path) {
		try {
			Path normalized = path.toAbsolutePath().normalize() ;

			// ファイルが存在する場合
			if( Files.isRegularFile( normalized)) {
				new ProcessBuilder( "explorer.exe", "/select," + normalized).start() ;
				return ;
			}

			// ディレクトリが存在する場合
			if( Files.isDirectory( normalized)) {
				new ProcessBuilder( "explorer.exe", normalized.toString()).start() ;
				return ;
			}

			Path parent = normalized.getParent() ;

			// 親ディレクトリが存在する場合
			if( parent != null && Files.isDirectory( parent)) {
				new ProcessBuilder( "explorer.exe", parent.toString()).start() ;
				return ;
			}

			appendLog( format( "log.pathOpenFailed", normalized)) ;
		} catch( IOException ioex) {
			appendLog( format( "log.pathOpenFailed", path)) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 確認後にスクリプトを実行します。<br><br>
	 *
	 * <p>メソッド名称： 確認スクリプト実行</p>
	 *
	 * @param message	確認メッセージ
	 * @param script	スクリプト
	 */
	//--------------------------------------------------------------------------
	private void confirmAndRun(String message, String script) {
		MessageBox dialog = new MessageBox( shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO) ;
		dialog.setText( text( "dialog.confirm") ) ;
		dialog.setMessage( message) ;
		int result = dialog.open() ;

		// はいが選択された場合
		if( result == SWT.YES) {
			runPrivilegedScriptAsync( script) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * スモークテストを非同期実行します。<br><br>
	 *
	 * <p>メソッド名称： スモークテスト非同期実行</p>
	 */
	//--------------------------------------------------------------------------
	private void runSmokeTestAsync() {
		appendLog( text( "log.startSmokeTest")) ;
		new Thread( () -> {
			try {
				List<String> messages = runSmokeTest() ;
				runOnUi( () -> {
					// テストメッセージを追加する
					for( String message : messages) {
						appendLog( message) ;
					}

					appendLog( text( "log.endSmokeTest")) ;
				}) ;
			} catch( Exception ex) {
				runOnUi( () -> {
					appendLog( format( "log.smokeTestFailed", ex.getMessage())) ;
					showError( text( "error.smokeTestFailed"), ex) ;
				}) ;
			}
		}, "manager-smoke-test").start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * スモークテストを実行します。<br><br>
	 *
	 * <p>メソッド名称： スモークテスト実行</p>
	 *
	 * @return テストメッセージ
	 * @throws Exception テスト異常
	 */
	//--------------------------------------------------------------------------
	private List<String> runSmokeTest() throws Exception {
		List<String> messages = new ArrayList<String>() ;
		int portmapPort = validatePositiveInt( text( "label.portmapUdpPort"), portmapPortText.getText()) ;
		int nfsPort = validatePositiveInt( text( "label.nfsUdpPort"), nfsPortText.getText()) ;
		int mountPort = validatePositiveInt( text( "label.mountUdpPort"), mountPortText.getText()) ;
		int mappedNfsPort = getMappedPort( portmapPort, RpcConstants.PROGRAM_NFS, 2) ;

		// Portmapが返したNFSポートが一致しない場合
		if( mappedNfsPort != nfsPort) {
			throw new IllegalStateException( format( "error.unexpectedNfsPort", mappedNfsPort)) ;
		}

		messages.add( "PASS: service portmap GETPORT") ;
		byte[] rootHandle = mountExport( mountPort) ;
		messages.add( "PASS: service mount MNT") ;
		checkRootGetAttr( nfsPort, rootHandle) ;
		messages.add( "PASS: service nfs GETATTR") ;
		messages.add( "SERVICE SMOKE TEST PASSED") ;
		return messages ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Portmapからポートを取得します。<br><br>
	 *
	 * <p>メソッド名称： Portmapポート取得</p>
	 *
	 * @param portmapPort	Portmapポート
	 * @param program		Program
	 * @param version		Version
	 * @return ポート
	 * @throws Exception 取得異常
	 */
	//--------------------------------------------------------------------------
	private int getMappedPort(int portmapPort, int program, int version) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeInt( program) ;
		arguments.writeInt( version) ;
		arguments.writeInt( RpcConstants.IPPROTO_UDP) ;
		arguments.writeInt( 0) ;
		XdrReader reader = rpcCall( portmapPort, RpcConstants.PROGRAM_PORTMAP, 2, 3, arguments) ;
		return reader.readInt() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * exportをmountします。<br><br>
	 *
	 * <p>メソッド名称： export mount</p>
	 *
	 * @param mountPort	MOUNTポート
	 * @return ルートファイルハンドル
	 * @throws Exception mount異常
	 */
	//--------------------------------------------------------------------------
	private byte[] mountExport(int mountPort) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeString( getSelectedOrFirstShare().getName()) ;
		XdrReader reader = rpcCall( mountPort, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments) ;
		int status = reader.readInt() ;

		// mountが失敗した場合
		if( status != 0) {
			throw new IllegalStateException( format( "error.mountFailed", status)) ;
		}

		return reader.readFixedOpaque( FileHandle.LENGTH) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ルートGETATTRを確認します。<br><br>
	 *
	 * <p>メソッド名称： ルートGETATTR確認</p>
	 *
	 * @param nfsPort		NFSポート
	 * @param rootHandle	ルートファイルハンドル
	 * @throws Exception 確認異常
	 */
	//--------------------------------------------------------------------------
	private void checkRootGetAttr(int nfsPort, byte[] rootHandle) throws Exception {
		XdrWriter arguments = new XdrWriter() ;
		arguments.writeFixedOpaque( rootHandle) ;
		XdrReader reader = rpcCall( nfsPort, RpcConstants.PROGRAM_NFS, 2, 1, arguments) ;
		int status = reader.readInt() ;

		// GETATTRが失敗した場合
		if( status != 0) {
			throw new IllegalStateException( format( "error.getAttrFailed", status)) ;
		}

		int type = reader.readInt() ;

		// ルートがディレクトリではない場合
		if( type != 2) {
			throw new IllegalStateException( format( "error.rootTypeNotDirectory", type)) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * RPCを呼び出します。<br><br>
	 *
	 * <p>メソッド名称： RPC呼出</p>
	 *
	 * @param port		ポート
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return 応答本文
	 * @throws Exception 呼出異常
	 */
	//--------------------------------------------------------------------------
	private XdrReader rpcCall(int port, int program, int version, int procedure, XdrWriter arguments) throws Exception {
		int requestXid = ++xid ;
		byte[] request = createRpcCall( requestXid, program, version, procedure, arguments) ;
		byte[] buffer = new byte[65535] ;
		DatagramPacket response = new DatagramPacket( buffer, buffer.length) ;

		try( DatagramSocket socket = new DatagramSocket()) {
			socket.setSoTimeout( RPC_TIMEOUT) ;
			DatagramPacket packet = new DatagramPacket( request, request.length, InetAddress.getByName( "127.0.0.1"), port) ;
			socket.send( packet) ;
			socket.receive( response) ;
		}

		XdrReader reader = new XdrReader( response.getData(), response.getLength()) ;
		checkEquals( "xid", requestXid, reader.readInt()) ;
		checkEquals( "message type", RpcConstants.MSG_REPLY, reader.readInt()) ;
		checkEquals( "reply status", RpcConstants.REPLY_STAT_ACCEPTED, reader.readInt()) ;
		reader.readInt() ;
		reader.readOpaque() ;
		checkEquals( "accept status", RpcConstants.ACCEPT_SUCCESS, reader.readInt()) ;
		return new XdrReader( reader.readRemaining()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * RPC CALLを作成します。<br><br>
	 *
	 * <p>メソッド名称： RPC CALL作成</p>
	 *
	 * @param xid		XID
	 * @param program	Program
	 * @param version	Version
	 * @param procedure	Procedure
	 * @param arguments	引数
	 * @return RPC CALL
	 */
	//--------------------------------------------------------------------------
	private byte[] createRpcCall(int xid, int program, int version, int procedure, XdrWriter arguments) {
		XdrWriter writer = new XdrWriter() ;
		writer.writeInt( xid) ;
		writer.writeInt( RpcConstants.MSG_CALL) ;
		writer.writeInt( RpcConstants.RPC_VERSION) ;
		writer.writeInt( program) ;
		writer.writeInt( version) ;
		writer.writeInt( procedure) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeInt( RpcConstants.AUTH_NONE) ;
		writer.writeOpaque( new byte[0]) ;
		writer.writeFixedOpaque( arguments.toByteArray()) ;
		return writer.toByteArray() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 数値一致を確認します。<br><br>
	 *
	 * <p>メソッド名称： 数値一致確認</p>
	 *
	 * @param name		名称
	 * @param expected	期待値
	 * @param actual	実値
	 */
	//--------------------------------------------------------------------------
	private void checkEquals(String name, int expected, int actual) {
		// 値が一致しない場合
		if( expected != actual) {
			throw new IllegalStateException( format( "error.expectedActual", name, expected, actual)) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * スクリプトを非同期実行します。<br><br>
	 *
	 * <p>メソッド名称： スクリプト非同期実行</p>
	 *
	 * @param scriptName	スクリプト名
	 */
	//--------------------------------------------------------------------------
	private void runScriptAsync(String scriptName) {
		runScriptAsync( scriptName, result -> refreshStatus()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 権限が必要なスクリプトを非同期実行します。<br><br>
	 *
	 * <p>メソッド名称： 権限必要スクリプト非同期実行</p>
	 *
	 * @param scriptName	スクリプト名
	 */
	//--------------------------------------------------------------------------
	private void runPrivilegedScriptAsync(String scriptName) {
		runPrivilegedScriptAsync( scriptName, result -> refreshStatus()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 権限が必要なスクリプトを非同期実行します。<br><br>
	 *
	 * <p>メソッド名称： 権限必要スクリプト非同期実行</p>
	 *
	 * @param scriptName	スクリプト名
	 * @param callback	コールバック
	 */
	//--------------------------------------------------------------------------
	private void runPrivilegedScriptAsync(String scriptName, ResultCallback callback) {
		// 管理者権限がない場合
		if( !isAdministrator()) {
			appendLog( text( "log.adminRequired")) ;
			showError( text( "error.adminRequired"), null) ;
			refreshStatus() ;
			return ;
		}

		runScriptAsync( scriptName, callback) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * スクリプトを非同期実行します。<br><br>
	 *
	 * <p>メソッド名称： スクリプト非同期実行</p>
	 *
	 * @param scriptName	スクリプト名
	 * @param callback	コールバック
	 */
	//--------------------------------------------------------------------------
	private void runScriptAsync(String scriptName, ResultCallback callback) {
		Path scriptPath = rootPath.resolve( "scripts").resolve( scriptName) ;

		// スクリプトが存在しない場合
		if( !Files.exists( scriptPath)) {
			showError( format( "error.scriptNotFound", scriptPath), null) ;
			return ;
		}

		runCommandAsync( scriptName, List.of(
				"powershell.exe",
				"-NoProfile",
				"-ExecutionPolicy",
				"Bypass",
				"-File",
				scriptPath.toString()), callback) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * コマンドを非同期実行します。<br><br>
	 *
	 * <p>メソッド名称： コマンド非同期実行</p>
	 *
	 * @param title		タイトル
	 * @param command	コマンド
	 * @param callback	コールバック
	 */
	//--------------------------------------------------------------------------
	private void runCommandAsync(String title, List<String> command, ResultCallback callback) {
		appendLog( format( "log.start", title)) ;
		new Thread( () -> {
			ProcessResult result = runCommand( command) ;
			runOnUi( () -> {
				appendLog( result.getOutput()) ;
				appendLog( format( "log.end", title, result.getExitCode())) ;

				// コマンドが失敗した場合
				if( result.getExitCode() != 0) {
					showError( format( "error.commandFailed", title), null) ;
				}

				// コールバックが存在する場合
				if( callback != null) {
					callback.completed( result) ;
				}
			}) ;
		}, "manager-command").start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * コマンドを実行します。<br><br>
	 *
	 * <p>メソッド名称： コマンド実行</p>
	 *
	 * @param command	コマンド
	 * @return 実行結果
	 */
	//--------------------------------------------------------------------------
	private ProcessResult runCommand(List<String> command) {
		StringBuilder output = new StringBuilder() ;

		try {
			ProcessBuilder builder = new ProcessBuilder( command) ;
			builder.directory( rootPath.toFile()) ;
			builder.redirectErrorStream( true) ;
			Process process = builder.start() ;
			output.append( readProcessOutput( process)) ;
			int exitCode = process.waitFor() ;
			return new ProcessResult( exitCode, output.toString()) ;
		} catch( IOException ioex) {
			output.append( ioex.getMessage()) ;
			return new ProcessResult( 1, output.toString()) ;
		} catch( InterruptedException iex) {
			Thread.currentThread().interrupt() ;
			output.append( iex.getMessage()) ;
			return new ProcessResult( 1, output.toString()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * プロセス出力を読み取ります。<br><br>
	 *
	 * <p>メソッド名称： プロセス出力読取</p>
	 *
	 * @param process	プロセス
	 * @return 出力
	 * @throws IOException 読取異常
	 */
	//--------------------------------------------------------------------------
	private String readProcessOutput(Process process) throws IOException {
		StringBuilder output = new StringBuilder() ;

		try( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream(), Charset.defaultCharset()))) {
			String line ;

			while( (line = reader.readLine()) != null) {
				output.append( line).append( System.lineSeparator()) ;
			}
		}

		return output.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 管理者権限を確認します。<br><br>
	 *
	 * <p>メソッド名称： 管理者権限確認</p>
	 *
	 * @return true:管理者 false:一般ユーザー
	 */
	//--------------------------------------------------------------------------
	private boolean isAdministrator() {
		ProcessResult result = runCommand( List.of(
				"powershell.exe",
				"-NoProfile",
				"-Command",
				"$p=New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent()); $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)") ) ;
		return result.getOutput().trim().equalsIgnoreCase( "True") ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ルートパスを検出します。<br><br>
	 *
	 * <p>メソッド名称： ルートパス検出</p>
	 *
	 * @return ルートパス
	 */
	//--------------------------------------------------------------------------
	private Path detectRootPath() {
		String home = System.getenv( "TINYWIN_NFS_HOME") ;

		// 環境変数指定がある場合
		if( home != null && !home.isBlank()) {
			return Path.of( home).toAbsolutePath().normalize() ;
		}

		home = System.getenv( "OGAWA_NFS_HOME") ;

		// 旧環境変数指定がある場合
		if( home != null && !home.isBlank()) {
			return Path.of( home).toAbsolutePath().normalize() ;
		}

		home = System.getenv( "QNX_NFS_HOME") ;

		// 旧環境変数指定がある場合
		if( home != null && !home.isBlank()) {
			return Path.of( home).toAbsolutePath().normalize() ;
		}

		Path serviceRoot = detectInstalledServiceRootPath() ;

		// サービス登録先が取得できた場合
		if( serviceRoot != null) {
			return serviceRoot ;
		}

		try {
			CodeSource codeSource = TinyWinNfsSwtManager.class.getProtectionDomain().getCodeSource() ;
			Path location = Path.of( codeSource.getLocation().toURI()).toAbsolutePath().normalize() ;

			// binフォルダから起動している場合
			if( Files.isDirectory( location) && "bin".equalsIgnoreCase( location.getFileName().toString())) {
				return location.getParent() ;
			}

			Path parent = location.getParent() ;

			// jpackageのappフォルダから起動している場合
			if( parent != null && "app".equalsIgnoreCase( parent.getFileName().toString())) {
				return parent.getParent() ;
			}
		} catch( URISyntaxException usex) {
			// ルート検出に失敗した場合
		}

		return Path.of( System.getProperty( "user.dir")).toAbsolutePath().normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 登録済みサービスのルートパスを検出します。<br><br>
	 *
	 * <p>メソッド名称： 登録済みサービスルートパス検出</p>
	 *
	 * @return ルートパス
	 */
	//--------------------------------------------------------------------------
	private Path detectInstalledServiceRootPath() {
		Path executablePath = detectInstalledServiceExecutablePath() ;

		// 実行ファイルが取得できない場合
		if( executablePath == null) {
			return null ;
		}

		Path winswDirectory = executablePath.getParent() ;

		// WinSWフォルダが取得できない場合
		if( winswDirectory == null) {
			return null ;
		}

		Path serviceDirectory = winswDirectory.getParent() ;

		// serviceフォルダが取得できない場合
		if( serviceDirectory == null) {
			return null ;
		}

		Path serviceRoot = serviceDirectory.getParent() ;

		// ルートフォルダが取得できない場合
		if( serviceRoot == null) {
			return null ;
		}

		return serviceRoot.toAbsolutePath().normalize() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 登録済みサービスの実行ファイルパスを検出します。<br><br>
	 *
	 * <p>メソッド名称： 登録済みサービス実行ファイルパス検出</p>
	 *
	 * @return 実行ファイルパス
	 */
	//--------------------------------------------------------------------------
	private Path detectInstalledServiceExecutablePath() {
		try {
			ProcessBuilder builder = new ProcessBuilder(
					"powershell.exe",
					"-NoProfile",
					"-Command",
					"$names=@('" + SERVICE_NAME + "','" + String.join( "','", LEGACY_SERVICE_NAMES) + "'); "
					+ "foreach($n in $names){"
					+ "$s=Get-CimInstance Win32_Service -Filter \"Name='$n'\" -ErrorAction SilentlyContinue; "
					+ "if($s -ne $null){$s.PathName; exit}"
					+ "}") ;
			builder.redirectErrorStream( true) ;
			Process process = builder.start() ;
			StringBuilder output = new StringBuilder() ;

			try( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream(), Charset.defaultCharset()))) {
				String line ;

				while( (line = reader.readLine()) != null) {
					output.append( line).append( System.lineSeparator()) ;
				}
			}

			process.waitFor() ;
			String pathName = output.toString().trim() ;

			// サービスが登録されていない場合
			if( pathName.isBlank()) {
				return null ;
			}

			return extractExecutablePath( pathName) ;
		} catch( IOException ioex) {
			// サービス登録先を取得できない場合
		} catch( InterruptedException iex) {
			Thread.currentThread().interrupt() ;
		}

		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービスPathNameから実行ファイルパスを抽出します。<br><br>
	 *
	 * <p>メソッド名称： 実行ファイルパス抽出</p>
	 *
	 * @param pathName	PathName
	 * @return 実行ファイルパス
	 */
	//--------------------------------------------------------------------------
	private Path extractExecutablePath(String pathName) {
		String executable = pathName.trim() ;

		// ダブルクォートで囲まれている場合
		if( executable.startsWith( "\"" )) {
			int endIndex = executable.indexOf( "\"", 1) ;

			// 終端クォートが存在する場合
			if( endIndex > 1) {
				return Path.of( executable.substring( 1, endIndex)).toAbsolutePath().normalize() ;
			}
		}

		int exeIndex = executable.toLowerCase().indexOf( ".exe") ;

		// exe拡張子が存在する場合
		if( exeIndex >= 0) {
			return Path.of( executable.substring( 0, exeIndex + 4)).toAbsolutePath().normalize() ;
		}

		return null ;
	}

	//--------------------------------------------------------------------------
	/**
	 * UIスレッドで処理します。<br><br>
	 *
	 * <p>メソッド名称： UIスレッド処理</p>
	 *
	 * @param runnable	処理
	 */
	//--------------------------------------------------------------------------
	private void runOnUi(Runnable runnable) {
		// Displayが破棄済みの場合
		if( display.isDisposed()) {
			return ;
		}

		// UIスレッドの場合
		if( Thread.currentThread() == display.getThread()) {
			runnable.run() ;
			return ;
		}

		display.asyncExec( () -> {
			// Shellが破棄済みではない場合
			if( shell != null && !shell.isDisposed()) {
				runnable.run() ;
			}
		}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログを追加します。<br><br>
	 *
	 * <p>メソッド名称： ログ追加</p>
	 *
	 * @param message	メッセージ
	 */
	//--------------------------------------------------------------------------
	private void appendLog(String message) {
		// UIスレッドではない場合
		if( Thread.currentThread() != display.getThread()) {
			runOnUi( () -> appendLog( message)) ;
			return ;
		}

		String text = message ;

		// メッセージが空の場合
		if( text == null || text.isBlank() || logText == null || logText.isDisposed()) {
			return ;
		}

		logText.append( "[" + LocalDateTime.now().format( LOG_TIME_FORMAT) + "] " + text + System.lineSeparator()) ;
		logText.setSelection( logText.getCharCount()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * エラーを表示します。<br><br>
	 *
	 * <p>メソッド名称： エラー表示</p>
	 *
	 * @param message	メッセージ
	 * @param exception	例外
	 */
	//--------------------------------------------------------------------------
	private void showError(String message, Exception exception) {
		// UIスレッドではない場合
		if( Thread.currentThread() != display.getThread()) {
			runOnUi( () -> showError( message, exception)) ;
			return ;
		}

		String detail = message ;

		// 例外が存在する場合
		if( exception != null) {
			detail = message + System.lineSeparator() + exception.getMessage() ;
		}

		MessageBox dialog = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK) ;
		dialog.setText( text( "dialog.error") ) ;
		dialog.setMessage( detail) ;
		dialog.open() ;
	}

	//------------------------------------------------------------------------------
	/**
	 * 実行結果クラスです。<br><br>
	 *
	 * <p>クラス名称： 実行結果</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class ProcessResult {
		//	内部定義	--------------------------------------------------------
		/** 終了コード */
		private final int				exitCode ;

		/** 出力 */
		private final String				output ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param exitCode	終了コード
		 * @param output	出力
		 */
		//----------------------------------------------------------------------
		ProcessResult(int exitCode, String output) {
			this.exitCode = exitCode ;
			this.output = output ;
		}

		//----------------------------------------------------------------------
		/**
		 * 終了コードを取得します。<br><br>
		 *
		 * <p>メソッド名称： 終了コード取得</p>
		 *
		 * @return 終了コード
		 */
		//----------------------------------------------------------------------
		int getExitCode() {
			return exitCode ;
		}

		//----------------------------------------------------------------------
		/**
		 * 出力を取得します。<br><br>
		 *
		 * <p>メソッド名称： 出力取得</p>
		 *
		 * @return 出力
		 */
		//----------------------------------------------------------------------
		String getOutput() {
			return output ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 共有定義クラスです。<br><br>
	 *
	 * <p>クラス名称： 共有定義</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class ShareEntry {
		//	内部定義	--------------------------------------------------------
		/** 公開名 */
		private final String				name ;

		/** 公開パス */
		private final String				path ;

		/** 書込可否 */
		private final boolean			writable ;

		/** 許可クライアント */
		private final String				allowedClients ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param name		公開名
		 * @param path		公開パス
		 * @param writable			書込可否
		 * @param allowedClients	許可クライアント
		 */
		//----------------------------------------------------------------------
		ShareEntry(String name, String path, boolean writable, String allowedClients) {
			this.name = name == null ? "" : name.trim() ;
			this.path = path == null ? "" : path.trim() ;
			this.writable = writable ;
			this.allowedClients = allowedClients == null ? "" : allowedClients.trim() ;
		}

		//----------------------------------------------------------------------
		/**
		 * 公開名を取得します。<br><br>
		 *
		 * <p>メソッド名称： 公開名取得</p>
		 *
		 * @return 公開名
		 */
		//----------------------------------------------------------------------
		String getName() {
			return name ;
		}

		//----------------------------------------------------------------------
		/**
		 * 公開パスを取得します。<br><br>
		 *
		 * <p>メソッド名称： 公開パス取得</p>
		 *
		 * @return 公開パス
		 */
		//----------------------------------------------------------------------
		String getPath() {
			return path ;
		}

		//----------------------------------------------------------------------
		/**
		 * 書込可否を取得します。<br><br>
		 *
		 * <p>メソッド名称： 書込可否取得</p>
		 *
		 * @return true:書込可 false:読込専用
		 */
		//----------------------------------------------------------------------
		boolean isWritable() {
			return writable ;
		}

		//----------------------------------------------------------------------
		/**
		 * 許可クライアントを取得します。<br><br>
		 *
		 * <p>メソッド名称： 許可クライアント取得</p>
		 *
		 * @return 許可クライアント
		 */
		//----------------------------------------------------------------------
		String getAllowedClients() {
			return allowedClients ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 実行結果コールバックインターフェースです。<br><br>
	 *
	 * <p>クラス名称： 実行結果コールバック</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	@FunctionalInterface
	private interface ResultCallback {
		//----------------------------------------------------------------------
		/**
		 * 完了処理を行います。<br><br>
		 *
		 * <p>メソッド名称： 完了処理</p>
		 *
		 * @param result	実行結果
		 */
		//----------------------------------------------------------------------
		void completed(ProcessResult result) ;
	}

	//------------------------------------------------------------------------------
	/**
	 * SWT選択リスナーインターフェースです。<br><br>
	 *
	 * <p>クラス名称： SWT選択リスナー</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	@FunctionalInterface
	private interface SwtSelectionListener {
		//----------------------------------------------------------------------
		/**
		 * 選択処理を行います。<br><br>
		 *
		 * <p>メソッド名称： 選択処理</p>
		 *
		 * @param event	選択イベント
		 */
		//----------------------------------------------------------------------
		void widgetSelected(SelectionEvent event) ;
	}
}
