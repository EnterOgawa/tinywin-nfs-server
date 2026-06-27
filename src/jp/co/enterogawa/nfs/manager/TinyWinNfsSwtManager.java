package jp.co.enterogawa.nfs.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
		configPath = rootPath.resolve( "conf").resolve( "nfs-server.properties") ;
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
		createTableColumn( exportTable, text( "column.folder"), 460) ;
		createTableColumn( exportTable, text( "column.writable"), 80) ;
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
		buttons.setLayout( createGridLayout( 8, false, 6, 6)) ;
		createButton( buttons, text( "button.install"), event -> runScriptAsync( "install-service.ps1")) ;
		createButton( buttons, text( "button.start"), event -> runScriptAsync( "start-service.ps1")) ;
		createButton( buttons, text( "button.stop"), event -> runScriptAsync( "stop-service.ps1")) ;
		createButton( buttons, text( "button.restart"), event -> runScriptAsync( "restart-service.ps1")) ;
		createButton( buttons, text( "button.uninstall"), event -> confirmAndRun( text( "dialog.uninstallService"), "uninstall-service.ps1")) ;
		createButton( buttons, text( "button.firewall"), event -> runScriptAsync( "add-firewall-rules.ps1")) ;
		createButton( buttons, text( "button.smokeTest"), event -> runSmokeTestAsync()) ;
		createButton( buttons, text( "button.status"), event -> refreshStatus()) ;

		Text statusText = createText( panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL) ;
		statusText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		statusText.setText( format( "service.info", SERVICE_NAME, String.join( ", ", LEGACY_SERVICE_NAMES))) ;
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

				// 共有定義が入力されている場合
				if( !name.isEmpty() && !path.isEmpty()) {
					shareEntries.add( new ShareEntry( name, path, writable)) ;
				}
			}
		}

		// 共有定義が存在しない場合
		if( shareEntries.isEmpty()) {
			shareEntries.add( new ShareEntry(
					properties.getProperty( "export.name", "/export").trim(),
					properties.getProperty( "export.path", rootPath.resolve( "export").toString()).trim(),
					Boolean.parseBoolean( properties.getProperty( "export.writable", "true")))) ;
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
					boolText( entry.isWritable()) }) ;
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
			updateMountCommand() ;
			return ;
		}

		selectedShareIndex = index ;
		exportTable.setSelection( index) ;
		ShareEntry entry = shareEntries.get( index) ;
		exportNameText.setText( entry.getName()) ;
		exportPathText.setText( entry.getPath()) ;
		exportWritableButton.setSelection( entry.isWritable()) ;
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
				exportWritableButton.getSelection()) ;
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

		return new ShareEntry( "/export", rootPath.resolve( "export").toString(), true) ;
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
		if( !entry.getName().startsWith( "/")) {
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
			filenameCharsetText.setText( properties.getProperty( "filename.charset", "UTF-8")) ;
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
			Files.createDirectories( configPath.getParent()) ;
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
			lines.add( "" ) ;
			lines.add( "exports.count=" + shareEntries.size()) ;

			for( int i = 0; i < shareEntries.size(); i++) {
				ShareEntry entry = shareEntries.get( i) ;
				String prefix = "exports." + (i + 1) + "." ;
				lines.add( prefix + "name=" + entry.getName()) ;
				lines.add( prefix + "path=" + escapePath( entry.getPath()) ) ;
				lines.add( prefix + "writable=" + entry.isWritable()) ;
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
			lines.add( "filename.charset=" + filenameCharsetText.getText().trim()) ;
			Files.write( configPath, lines, StandardCharsets.UTF_8) ;
			updateMountCommand() ;
			appendLog( text( "log.configurationSaved")) ;
			return true ;
		} catch( Exception ex) {
			appendLog( format( "log.configurationSaveFailed", ex.getMessage())) ;
			showError( text( "error.configurationSaveFailed"), ex) ;
			return false ;
		}
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
			runScriptAsync( "restart-service.ps1") ;
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
					shell.layout( true, true) ;
				}) ;
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
			runScriptAsync( script) ;
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
				scriptPath.toString()), result -> refreshStatus()) ;
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

			try( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream(), Charset.defaultCharset()))) {
				String line ;

				while( (line = reader.readLine()) != null) {
					output.append( line).append( System.lineSeparator()) ;
				}
			}

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

			Path executablePath = extractExecutablePath( pathName) ;

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

			Path configFile = serviceRoot.resolve( "conf").resolve( "nfs-server.properties") ;

			// 設定ファイルが存在する場合
			if( Files.exists( configFile)) {
				return serviceRoot.toAbsolutePath().normalize() ;
			}
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

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param name		公開名
		 * @param path		公開パス
		 * @param writable	書込可否
		 */
		//----------------------------------------------------------------------
		ShareEntry(String name, String path, boolean writable) {
			this.name = name == null ? "" : name.trim() ;
			this.path = path == null ? "" : path.trim() ;
			this.writable = writable ;
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
