package jp.co.enterogawa.nfs.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.swt.widgets.FileDialog;
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
import jp.co.enterogawa.nfs.diagnostic.NfsDiagnostics;
import jp.co.enterogawa.nfs.diagnostic.NfsDiagnostics.DiagnosticMessage;
import jp.co.enterogawa.nfs.diagnostic.NfsDiagnostics.DiagnosticReport;
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

	/** 製品バージョン */
	private static final String			PRODUCT_VERSION = "1.11.0" ;

	/** ログ読込最大バイト数 */
	private static final long			MAX_LOG_READ_BYTES = 1024L * 1024L ;

	/** mountクライアント QNX */
	private static final int				MOUNT_CLIENT_QNX = 0 ;

	/** mountクライアント Windows */
	private static final int				MOUNT_CLIENT_WINDOWS = 1 ;

	/** mountクライアント Linux */
	private static final int				MOUNT_CLIENT_LINUX = 2 ;

	/** mountプロトコル NFSv2 UDP */
	private static final int				MOUNT_PROTOCOL_V2_UDP = 0 ;

	/** mountプロトコル NFSv3 UDP */
	private static final int				MOUNT_PROTOCOL_V3_UDP = 1 ;

	/** mountプロトコル NFSv3 TCP */
	private static final int				MOUNT_PROTOCOL_V3_TCP = 2 ;

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

	/** サーバーログ */
	private Text						serverLogText ;

	/** サーバーログ検索 */
	private Text						logSearchText ;

	/** サーバーログフィルタ */
	private Combo						logFilterCombo ;

	/** サーバーログ自動更新 */
	private Button						logAutoRefreshButton ;

	/** サーバーログ内容 */
	private String						serverLogContent = "" ;

	/** 診断一覧 */
	private Table						diagnosticTable ;

	/** 診断詳細 */
	private Text						diagnosticDetailText ;

	/** 診断重大度フィルタ */
	private Combo						diagnosticSeverityCombo ;

	/** 状態表示 */
	private Label						statusValueLabel ;

	/** 管理者権限表示 */
	private Label						adminValueLabel ;

	/** サービス情報 */
	private Text						serviceInfoText ;

	/** サービス操作結果 */
	private Text						serviceResultText ;

	/** サービス操作ボタン */
	private final List<Button>			serviceActionButtons = new ArrayList<Button>() ;

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

	/** mountクライアント種別 */
	private Combo						mountClientCombo ;

	/** mountプロトコル */
	private Combo						mountProtocolCombo ;

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
		TabItem diagnosticItem = addTab( folder, text( "tab.diagnostics"), createDiagnosticComposite( folder)) ;
		addTab( folder, text( "tab.service"), createServiceComposite( folder)) ;
		addTab( folder, text( "tab.log"), createLogComposite( folder)) ;
		folder.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// 診断タブが選択され、未表示の場合
				if( folder.getSelection().length > 0 && folder.getSelection()[0] == diagnosticItem && diagnosticTable.getItemCount() == 0) {
					refreshDiagnosticView() ;
				}
			}
		}) ;
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
	private TabItem addTab(TabFolder folder, String text, Composite control) {
		TabItem item = new TabItem( folder, SWT.NONE) ;
		item.setText( text) ;
		item.setControl( control) ;
		return item ;
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

		Group mountGroup = createGroup( panel, text( "group.mount"), 4) ;
		createLabel( mountGroup, text( "label.mountClient")) ;
		mountClientCombo = new Combo( mountGroup, SWT.DROP_DOWN | SWT.READ_ONLY) ;
		mountClientCombo.setItems( new String[] { text( "mount.client.qnx"), text( "mount.client.windows"), text( "mount.client.linux") }) ;
		mountClientCombo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		mountClientCombo.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateMountCommand() ;
			}
		}) ;
		createLabel( mountGroup, text( "label.mountProtocol")) ;
		mountProtocolCombo = new Combo( mountGroup, SWT.DROP_DOWN | SWT.READ_ONLY) ;
		mountProtocolCombo.setItems( new String[] { text( "mount.protocol.v2udp"), text( "mount.protocol.v3udp"), text( "mount.protocol.v3tcp") }) ;
		mountProtocolCombo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		mountProtocolCombo.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateMountCommand() ;
			}
		}) ;
		mountCommandText = createText( mountGroup, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL) ;
		GridData commandData = new GridData( SWT.FILL, SWT.FILL, true, true) ;
		commandData.horizontalSpan = 4 ;
		commandData.heightHint = 88 ;
		mountCommandText.setLayoutData( commandData) ;
		createButton( mountGroup, text( "button.copyCommand"), event -> copyMountCommand()) ;
		createButton( mountGroup, text( "button.update"), event -> updateMountCommand()) ;
		new Label( mountGroup, SWT.NONE) ;
		new Label( mountGroup, SWT.NONE) ;

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
		createButton( buttons, text( "button.exportConfig"), event -> exportConfig()) ;
		createButton( buttons, text( "button.importConfig"), event -> importConfig()) ;
		createButton( buttons, text( "button.resetDefaults"), event -> resetDefaultConfig()) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断Compositeを作成します。<br><br>
	 *
	 * <p>メソッド名称： 診断Composite作成</p>
	 *
	 * @param parent	親Composite
	 * @return 診断Composite
	 */
	//--------------------------------------------------------------------------
	private Composite createDiagnosticComposite(Composite parent) {
		Composite panel = new Composite( parent, SWT.NONE) ;
		panel.setLayout( createGridLayout( 1, false, 8, 8)) ;

		Composite filter = new Composite( panel, SWT.NONE) ;
		filter.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false)) ;
		filter.setLayout( createGridLayout( 8, false, 6, 6)) ;
		createLabel( filter, text( "label.severity")) ;
		diagnosticSeverityCombo = new Combo( filter, SWT.DROP_DOWN | SWT.READ_ONLY) ;
		diagnosticSeverityCombo.setItems( new String[] { text( "diagnostic.severity.all"), text( "diagnostic.severity.error"), text( "diagnostic.severity.warning"), text( "diagnostic.severity.info") }) ;
		diagnosticSeverityCombo.select( 0) ;
		diagnosticSeverityCombo.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				refreshDiagnosticView() ;
			}
		}) ;
		createButton( filter, text( "button.update"), event -> refreshDiagnosticView()) ;
		createButton( filter, text( "button.openConfig"), event -> openPath( configPath)) ;
		createButton( filter, text( "button.openLog"), event -> openPath( TinyWinNfsPaths.getLogPath( rootPath))) ;
		createButton( filter, text( "button.createDiagnostics"), event -> createDiagnosticPackage()) ;
		new Label( filter, SWT.NONE).setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;

		diagnosticTable = new Table( panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE) ;
		diagnosticTable.setHeaderVisible( true) ;
		diagnosticTable.setLinesVisible( true) ;
		diagnosticTable.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		createTableColumn( diagnosticTable, text( "column.severity"), 90) ;
		createTableColumn( diagnosticTable, text( "column.source"), 160) ;
		createTableColumn( diagnosticTable, text( "column.code"), 220) ;
		createTableColumn( diagnosticTable, text( "column.message"), 520) ;

		diagnosticDetailText = createText( panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL) ;
		GridData detailData = new GridData( SWT.FILL, SWT.FILL, true, true) ;
		detailData.heightHint = 160 ;
		diagnosticDetailText.setLayoutData( detailData) ;
		diagnosticDetailText.setText( text( "diagnostic.notRun")) ;
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
		createServiceButton( buttons, text( "button.install"), event -> runPrivilegedScriptAsync( "install-service.ps1")) ;
		createServiceButton( buttons, text( "button.start"), event -> runPrivilegedScriptAsync( "start-service.ps1")) ;
		createServiceButton( buttons, text( "button.stop"), event -> runPrivilegedScriptAsync( "stop-service.ps1")) ;
		createServiceButton( buttons, text( "button.restart"), event -> runPrivilegedScriptAsync( "restart-service.ps1")) ;
		createServiceButton( buttons, text( "button.uninstall"), event -> confirmAndRun( text( "dialog.uninstallService"), "uninstall-service.ps1")) ;
		createServiceButton( buttons, text( "button.firewall"), event -> runPrivilegedScriptAsync( "add-firewall-rules.ps1")) ;
		createServiceButton( buttons, text( "button.smokeTest"), event -> runSmokeTestAsync()) ;
		createButton( buttons, text( "button.status"), event -> refreshStatus()) ;
		createButton( buttons, text( "button.openLog"), event -> openPath( TinyWinNfsPaths.getLogPath( rootPath))) ;
		createButton( buttons, text( "button.openWinswLog"), event -> openPath( getWinswLogPath())) ;
		createButton( buttons, text( "button.createDiagnostics"), event -> createDiagnosticPackage()) ;

		serviceInfoText = createText( panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL) ;
		serviceInfoText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		serviceResultText = createText( panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL) ;
		GridData resultData = new GridData( SWT.FILL, SWT.FILL, true, true) ;
		resultData.heightHint = 130 ;
		serviceResultText.setLayoutData( resultData) ;
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

		Group serverLogGroup = createGroup( panel, text( "group.serverLog"), 1) ;
		serverLogGroup.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		Composite filters = new Composite( serverLogGroup, SWT.NONE) ;
		filters.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false)) ;
		filters.setLayout( createGridLayout( 8, false, 6, 6)) ;
		createLabel( filters, text( "label.search")) ;
		logSearchText = createText( filters, SWT.BORDER) ;
		logSearchText.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false)) ;
		logSearchText.addModifyListener( event -> applyServerLogFilter()) ;
		createLabel( filters, text( "label.logFilter")) ;
		logFilterCombo = new Combo( filters, SWT.DROP_DOWN | SWT.READ_ONLY) ;
		logFilterCombo.setItems( new String[] { text( "log.filter.all"), text( "log.filter.error"), text( "log.filter.mutation"), text( "log.filter.rpc") }) ;
		logFilterCombo.select( 0) ;
		logFilterCombo.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				applyServerLogFilter() ;
			}
		}) ;
		logAutoRefreshButton = new Button( filters, SWT.CHECK) ;
		logAutoRefreshButton.setText( text( "label.autoRefresh")) ;
		logAutoRefreshButton.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// 自動更新が有効になった場合
				if( logAutoRefreshButton.getSelection()) {
					refreshServerLogView() ;
					scheduleLogAutoRefresh() ;
				}
			}
		}) ;
		createButton( filters, text( "button.update"), event -> refreshServerLogView()) ;
		createButton( filters, text( "button.openLog"), event -> openPath( TinyWinNfsPaths.getLogPath( rootPath))) ;
		serverLogText = createText( serverLogGroup, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL) ;
		serverLogText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;

		Group managerLogGroup = createGroup( panel, text( "group.managerLog"), 1) ;
		managerLogGroup.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;
		logText = createText( managerLogGroup, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL) ;
		logText.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true)) ;

		Composite buttons = createButtonRow( panel) ;
		createButton( buttons, text( "button.clear"), event -> logText.setText( "")) ;
		refreshServerLogView() ;
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
	 * サービス操作ボタンを作成します。<br><br>
	 *
	 * <p>メソッド名称： サービス操作ボタン作成</p>
	 *
	 * @param parent	親Composite
	 * @param text		表示文字
	 * @param listener	選択リスナー
	 * @return ボタン
	 */
	//--------------------------------------------------------------------------
	private Button createServiceButton(Composite parent, String text, SwtSelectionListener listener) {
		Button button = createButton( parent, text, listener) ;
		serviceActionButtons.add( button) ;
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

			applyProperties( properties) ;
			appendLog( text( "log.configurationLoaded")) ;
		} catch( IOException ioex) {
			appendLog( format( "log.configurationLoadFailed", ioex.getMessage())) ;
			showError( text( "error.configurationLoadFailed"), ioex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定値をUIへ反映します。<br><br>
	 *
	 * <p>メソッド名称： 設定値UI反映</p>
	 *
	 * @param properties	設定値
	 */
	//--------------------------------------------------------------------------
	private void applyProperties(Properties properties) {
		loadShareEntries( properties) ;
		setLanguageSelection( ManagerMessages.normalizeLanguage( properties.getProperty( "ui.language", messages.getLanguageCode()))) ;
		serverHostText.setText( properties.getProperty( "client.server.host", detectLocalHostName())) ;
		clientMountPointText.setText( properties.getProperty( "client.mount.point", "/mnt")) ;
		setComboSelection( mountClientCombo, parseInt( properties.getProperty( "client.mount.profile"), MOUNT_CLIENT_QNX), MOUNT_CLIENT_QNX) ;
		setComboSelection( mountProtocolCombo, parseInt( properties.getProperty( "client.mount.protocol"), MOUNT_PROTOCOL_V2_UDP), MOUNT_PROTOCOL_V2_UDP) ;
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
	}

	//--------------------------------------------------------------------------
	/**
	 * Combo選択値を設定します。<br><br>
	 *
	 * <p>メソッド名称： Combo選択値設定</p>
	 *
	 * @param combo			Combo
	 * @param value			選択値
	 * @param defaultValue	既定値
	 */
	//--------------------------------------------------------------------------
	private void setComboSelection(Combo combo, int value, int defaultValue) {
		// Comboが存在しない場合
		if( combo == null || combo.isDisposed()) {
			return ;
		}

		int selection = value ;

		// 選択値が範囲外の場合
		if( value < 0 || value >= combo.getItemCount()) {
			selection = defaultValue ;
		}

		combo.select( selection) ;
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
			Path backupPath = writeValidatedConfig( configDirectory, buildConfigLines()) ;
			updateMountCommand() ;
			appendLog( text( "log.configurationSaved")) ;
			appendConfigurationWarnings() ;

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
	 * 設定行を作成します。<br><br>
	 *
	 * <p>メソッド名称： 設定行作成</p>
	 *
	 * @return 設定行
	 */
	//--------------------------------------------------------------------------
	private List<String> buildConfigLines() {
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

		// 共有定義を出力する
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
		lines.add( "client.mount.profile=" + selectedComboIndex( mountClientCombo, MOUNT_CLIENT_QNX)) ;
		lines.add( "client.mount.protocol=" + selectedComboIndex( mountProtocolCombo, MOUNT_PROTOCOL_V2_UDP)) ;
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
		return lines ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Combo選択位置を取得します。<br><br>
	 *
	 * <p>メソッド名称： Combo選択位置取得</p>
	 *
	 * @param combo			Combo
	 * @param defaultValue	既定値
	 * @return 選択位置
	 */
	//--------------------------------------------------------------------------
	private int selectedComboIndex(Combo combo, int defaultValue) {
		// Comboが存在しない場合
		if( combo == null || combo.isDisposed() || combo.getSelectionIndex() < 0) {
			return defaultValue ;
		}

		return combo.getSelectionIndex() ;
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
	 * 設定をエクスポートします。<br><br>
	 *
	 * <p>メソッド名称： 設定エクスポート</p>
	 */
	//--------------------------------------------------------------------------
	private void exportConfig() {
		try {
			applySelectedShare() ;
			validateFields() ;
			FileDialog dialog = new FileDialog( shell, SWT.SAVE) ;
			dialog.setText( text( "dialog.exportConfig")) ;
			dialog.setFileName( "nfs-server.properties") ;
			String selected = dialog.open() ;

			// 保存先が選択されなかった場合
			if( selected == null) {
				return ;
			}

			Path targetPath = Path.of( selected).toAbsolutePath().normalize() ;
			Files.write( targetPath, buildConfigLines(), StandardCharsets.UTF_8) ;
			NfsServerConfig.load( targetPath) ;
			appendLog( format( "log.configurationExported", targetPath)) ;
		} catch( Exception ex) {
			showError( text( "error.configurationExportFailed"), ex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定をインポートします。<br><br>
	 *
	 * <p>メソッド名称： 設定インポート</p>
	 */
	//--------------------------------------------------------------------------
	private void importConfig() {
		try {
			FileDialog dialog = new FileDialog( shell, SWT.OPEN) ;
			dialog.setText( text( "dialog.importConfig")) ;
			dialog.setFilterExtensions( new String[] { "*.properties", "*.*" }) ;
			String selected = dialog.open() ;

			// インポート元が選択されなかった場合
			if( selected == null) {
				return ;
			}

			Path sourcePath = Path.of( selected).toAbsolutePath().normalize() ;
			importConfigFromPath( sourcePath) ;
		} catch( Exception ex) {
			showError( text( "error.configurationImportFailed"), ex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 指定パスから設定をインポートします。<br><br>
	 *
	 * <p>メソッド名称： 指定パス設定インポート</p>
	 *
	 * @param sourcePath	インポート元
	 * @throws IOException インポート異常
	 */
	//--------------------------------------------------------------------------
	private void importConfigFromPath(Path sourcePath) throws IOException {
		Path configDirectory = configPath.getParent() ;
		Files.createDirectories( configDirectory) ;
		Path temporaryPath = Files.createTempFile( configDirectory, "nfs-server-import-", ".properties") ;

		try {
			Files.copy( sourcePath, temporaryPath, StandardCopyOption.REPLACE_EXISTING) ;
			NfsServerConfig.load( temporaryPath) ;
			Path backupPath = ConfigBackup.backupIfExists( configPath) ;
			moveConfigIntoPlace( temporaryPath) ;
			loadConfig() ;
			appendLog( format( "log.configurationImported", sourcePath)) ;

			// バックアップを作成した場合
			if( backupPath != null) {
				appendLog( format( "log.configurationBackupCreated", backupPath)) ;
			}
		} finally {
			Files.deleteIfExists( temporaryPath) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 既定設定へ戻します。<br><br>
	 *
	 * <p>メソッド名称： 既定設定復元</p>
	 */
	//--------------------------------------------------------------------------
	private void resetDefaultConfig() {
		MessageBox dialog = new MessageBox( shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO) ;
		dialog.setText( text( "dialog.confirm") ) ;
		dialog.setMessage( text( "dialog.resetDefaults")) ;
		int result = dialog.open() ;

		// はいが選択されなかった場合
		if( result != SWT.YES) {
			return ;
		}

		applyProperties( new Properties()) ;

		// 既定設定の保存に成功した場合
		if( saveConfig()) {
			appendLog( text( "log.configurationReset")) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定警告をログへ出力します。<br><br>
	 *
	 * <p>メソッド名称： 設定警告ログ出力</p>
	 */
	//--------------------------------------------------------------------------
	private void appendConfigurationWarnings() {
		try {
			NfsServerConfig config = NfsServerConfig.load( configPath) ;
			List<DiagnosticMessage> messages = NfsDiagnostics.inspectConfiguration( config) ;

			// 診断メッセージをログへ出力する
			for( DiagnosticMessage message : messages) {
				// 情報メッセージの場合
				if( "INFO".equals( message.getSeverity())) {
					continue ;
				}

				appendLog( format( "log.configurationDiagnostic", message.formatText())) ;
			}
		} catch( Exception ex) {
			appendLog( format( "log.configurationDiagnostic", ex.getMessage())) ;
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
		if( !isValidClientMountPoint( clientMountPointText.getText().trim())) {
			throw new IllegalArgumentException( text( "error.clientMountPointRequired") ) ;
		}

		// サーバーホストが未入力の場合
		if( serverHostText.getText().trim().isEmpty()) {
			throw new IllegalArgumentException( text( "error.serverHostRequired") ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * クライアント側mount pointを検証します。<br><br>
	 *
	 * <p>メソッド名称： クライアント側mount point検証</p>
	 *
	 * @param value	値
	 * @return true:正常 false:異常
	 */
	//--------------------------------------------------------------------------
	private boolean isValidClientMountPoint(String value) {
		// Windowsクライアントの場合
		if( selectedComboIndex( mountClientCombo, MOUNT_CLIENT_QNX) == MOUNT_CLIENT_WINDOWS) {
			return value.matches( "[A-Za-z]:" ) || value.startsWith( "\\\\" ) ;
		}

		return value.startsWith( "/" ) ;
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

		mountCommandText.setText( buildMountCommandText( host, exportName, mountPoint, share)) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * mountコマンド表示を作成します。<br><br>
	 *
	 * <p>メソッド名称： mountコマンド表示作成</p>
	 *
	 * @param host			サーバーホスト
	 * @param exportName	export名
	 * @param mountPoint	mount point
	 * @param share			共有定義
	 * @return mountコマンド表示
	 */
	//--------------------------------------------------------------------------
	private String buildMountCommandText(String host, String exportName, String mountPoint, ShareEntry share) {
		String command = buildMountCommand( host, exportName, mountPoint) ;
		StringBuilder builder = new StringBuilder() ;
		builder.append( command).append( System.lineSeparator()) ;
		builder.append( System.lineSeparator()) ;
		builder.append( text( "mount.client") ).append( " " ).append( selectedMountClientLabel()).append( System.lineSeparator()) ;
		builder.append( text( "mount.protocol") ).append( " " ).append( selectedMountProtocolLabel()).append( System.lineSeparator()) ;
		builder.append( text( "mount.serverExportPath") ).append( " " ).append( share.getPath()).append( System.lineSeparator()) ;
		builder.append( text( "mount.writable") ).append( " " ).append( boolText( share.isWritable())).append( System.lineSeparator()) ;
		builder.append( System.lineSeparator()) ;
		builder.append( text( "mount.availableExports") ).append( System.lineSeparator()) ;
		builder.append( buildExportSummary()) ;
		return builder.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * mountコマンドを作成します。<br><br>
	 *
	 * <p>メソッド名称： mountコマンド作成</p>
	 *
	 * @param host			サーバーホスト
	 * @param exportName	export名
	 * @param mountPoint	mount point
	 * @return mountコマンド
	 */
	//--------------------------------------------------------------------------
	private String buildMountCommand(String host, String exportName, String mountPoint) {
		int client = selectedComboIndex( mountClientCombo, MOUNT_CLIENT_QNX) ;
		int protocol = selectedComboIndex( mountProtocolCombo, MOUNT_PROTOCOL_V2_UDP) ;

		// Windows Client for NFSの場合
		if( client == MOUNT_CLIENT_WINDOWS) {
			String windowsMountPoint = isValidWindowsMountPoint( mountPoint) ? mountPoint : "Z:" ;
			return "mount -o anon \\\\" + host + "\\" + exportName.replaceFirst( "^/+", "" ) + " " + windowsMountPoint ;
		}

		// Linux/WSLの場合
		if( client == MOUNT_CLIENT_LINUX) {
			return "sudo mount -t nfs -o " + linuxMountOptions( protocol) + " " + host + ":" + exportName + " " + mountPoint ;
		}

		return "mount_nfs " + host + ":" + exportName + " " + mountPoint ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Windows mount pointか確認します。<br><br>
	 *
	 * <p>メソッド名称： Windows mount point確認</p>
	 *
	 * @param value	値
	 * @return true:Windows mount point false:その他
	 */
	//--------------------------------------------------------------------------
	private boolean isValidWindowsMountPoint(String value) {
		return value.matches( "[A-Za-z]:" ) || value.startsWith( "\\\\" ) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * Linux mountオプションを作成します。<br><br>
	 *
	 * <p>メソッド名称： Linux mountオプション作成</p>
	 *
	 * @param protocol	プロトコル選択
	 * @return mountオプション
	 */
	//--------------------------------------------------------------------------
	private String linuxMountOptions(int protocol) {
		// NFSv3/TCPの場合
		if( protocol == MOUNT_PROTOCOL_V3_TCP) {
			return "vers=3,proto=tcp" ;
		}

		// NFSv3/UDPの場合
		if( protocol == MOUNT_PROTOCOL_V3_UDP) {
			return "vers=3,proto=udp" ;
		}

		return "vers=2,proto=udp" ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 選択中mountクライアント名を取得します。<br><br>
	 *
	 * <p>メソッド名称： 選択mountクライアント名取得</p>
	 *
	 * @return mountクライアント名
	 */
	//--------------------------------------------------------------------------
	private String selectedMountClientLabel() {
		int index = selectedComboIndex( mountClientCombo, MOUNT_CLIENT_QNX) ;
		return mountClientCombo == null || mountClientCombo.isDisposed() ? "" : mountClientCombo.getItem( index) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 選択中mountプロトコル名を取得します。<br><br>
	 *
	 * <p>メソッド名称： 選択mountプロトコル名取得</p>
	 *
	 * @return mountプロトコル名
	 */
	//--------------------------------------------------------------------------
	private String selectedMountProtocolLabel() {
		int index = selectedComboIndex( mountProtocolCombo, MOUNT_PROTOCOL_V2_UDP) ;
		return mountProtocolCombo == null || mountProtocolCombo.isDisposed() ? "" : mountProtocolCombo.getItem( index) ;
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
	 * 診断ビューを更新します。<br><br>
	 *
	 * <p>メソッド名称： 診断ビュー更新</p>
	 */
	//--------------------------------------------------------------------------
	private void refreshDiagnosticView() {
		// 診断ビューが存在しない場合
		if( diagnosticTable == null || diagnosticTable.isDisposed()) {
			return ;
		}

		String serviceStatus = statusValueLabel == null || statusValueLabel.isDisposed() ? text( "status.unknown") : statusValueLabel.getText() ;
		String portmapStatus = describePortStatus( portmapPortText) ;
		String nfsStatus = describePortStatus( nfsPortText) ;
		String mountStatus = describePortStatus( mountPortText) ;
		diagnosticTable.removeAll() ;
		diagnosticDetailText.setText( text( "diagnostic.loading")) ;

		new Thread( () -> {
			try {
				NfsServerConfig config = NfsServerConfig.load( configPath) ;
				DiagnosticReport report = NfsDiagnostics.collect( config) ;
				List<DiagnosticRow> rows = buildDiagnosticRows( report, serviceStatus, portmapStatus, nfsStatus, mountStatus) ;
				runOnUi( () -> showDiagnosticRows( rows, report.formatText())) ;
			} catch( Exception ex) {
				runOnUi( () -> {
					diagnosticTable.removeAll() ;
					addDiagnosticRow( diagnosticTable, new DiagnosticRow( "ERROR", text( "diagnostic.source.config"), "DIAGNOSTIC_FAILED", ex.getMessage())) ;
					diagnosticDetailText.setText( ex.getClass().getSimpleName() + ": " + ex.getMessage()) ;
				}) ;
			}
		}, "diagnostic-view").start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断行を作成します。<br><br>
	 *
	 * <p>メソッド名称： 診断行作成</p>
	 *
	 * @param report		診断レポート
	 * @param serviceStatus	サービス状態
	 * @param portmapStatus	Portmap状態
	 * @param nfsStatus		NFS状態
	 * @param mountStatus	MOUNT状態
	 * @return 診断行
	 */
	//--------------------------------------------------------------------------
	private List<DiagnosticRow> buildDiagnosticRows(DiagnosticReport report, String serviceStatus, String portmapStatus, String nfsStatus, String mountStatus) {
		List<DiagnosticRow> rows = new ArrayList<DiagnosticRow>() ;
		rows.add( new DiagnosticRow( "INFO", text( "diagnostic.source.service"), "SERVICE_STATUS", serviceStatus)) ;
		rows.add( new DiagnosticRow( "INFO", text( "diagnostic.source.port"), "PORTMAP_PORT", portmapStatus)) ;
		rows.add( new DiagnosticRow( "INFO", text( "diagnostic.source.port"), "NFS_PORT", nfsStatus)) ;
		rows.add( new DiagnosticRow( "INFO", text( "diagnostic.source.port"), "MOUNT_PORT", mountStatus)) ;

		// 設定診断を追加する
		for( DiagnosticMessage message : report.getConfigurationMessages()) {
			rows.add( new DiagnosticRow( message.getSeverity(), text( "diagnostic.source.config"), message.getCode(), message.getMessage())) ;
		}

		// export診断を追加する
		for( NfsDiagnostics.ExportReport exportReport : report.getExportReports()) {
			rows.add( createExportSummaryRow( exportReport)) ;

			for( DiagnosticMessage message : exportReport.getMessages()) {
				rows.add( new DiagnosticRow( message.getSeverity(), exportReport.getExport().getName(), message.getCode(), message.getMessage())) ;
			}
		}

		return rows ;
	}

	//--------------------------------------------------------------------------
	/**
	 * export概要診断行を作成します。<br><br>
	 *
	 * <p>メソッド名称： export概要診断行作成</p>
	 *
	 * @param exportReport	export診断
	 * @return 診断行
	 */
	//--------------------------------------------------------------------------
	private DiagnosticRow createExportSummaryRow(NfsDiagnostics.ExportReport exportReport) {
		String severity = exportReport.getMessages().stream().anyMatch( message -> "ERROR".equals( message.getSeverity())) ? "ERROR" : "INFO" ;

		// エラーがなく警告がある場合
		if( "INFO".equals( severity) && exportReport.getMessages().stream().anyMatch( message -> "WARNING".equals( message.getSeverity()))) {
			severity = "WARNING" ;
		}

		String message = "path=" + exportReport.getExport().getPath()
				+ ", exists=" + exportReport.exists()
				+ ", directory=" + exportReport.isDirectory()
				+ ", readable=" + exportReport.isReadable()
				+ ", writable=" + exportReport.isWritable()
				+ ", files=" + exportReport.getFileCount()
				+ ", directories=" + exportReport.getDirectoryCount()
				+ ", bytes=" + exportReport.getTotalBytes()
				+ ", caseCollisions=" + exportReport.getCaseCollisions().size() ;
		return new DiagnosticRow( severity, exportReport.getExport().getName(), "EXPORT_SUMMARY", message) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断行を表示します。<br><br>
	 *
	 * <p>メソッド名称： 診断行表示</p>
	 *
	 * @param rows		診断行
	 * @param detail	詳細
	 */
	//--------------------------------------------------------------------------
	private void showDiagnosticRows(List<DiagnosticRow> rows, String detail) {
		diagnosticTable.removeAll() ;

		// 診断行を表示する
		for( DiagnosticRow row : rows) {
			// 重大度フィルタに一致しない場合
			if( !matchesDiagnosticSeverity( row)) {
				continue ;
			}

			addDiagnosticRow( diagnosticTable, row) ;
		}

		diagnosticDetailText.setText( detail) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断重大度フィルタ一致を確認します。<br><br>
	 *
	 * <p>メソッド名称： 診断重大度フィルタ一致確認</p>
	 *
	 * @param row	診断行
	 * @return true:一致 false:不一致
	 */
	//--------------------------------------------------------------------------
	private boolean matchesDiagnosticSeverity(DiagnosticRow row) {
		int selection = selectedComboIndex( diagnosticSeverityCombo, 0) ;

		// すべて表示の場合
		if( selection == 0) {
			return true ;
		}

		// エラーのみの場合
		if( selection == 1) {
			return "ERROR".equals( row.getSeverity()) ;
		}

		// 警告のみの場合
		if( selection == 2) {
			return "WARNING".equals( row.getSeverity()) ;
		}

		return "INFO".equals( row.getSeverity()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断行を追加します。<br><br>
	 *
	 * <p>メソッド名称： 診断行追加</p>
	 *
	 * @param table	診断Table
	 * @param row	診断行
	 */
	//--------------------------------------------------------------------------
	private void addDiagnosticRow(Table table, DiagnosticRow row) {
		TableItem item = new TableItem( table, SWT.NONE) ;
		item.setText( new String[] { severityText( row.getSeverity()), row.getSource(), row.getCode(), row.getMessage() }) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 重大度表示文字を取得します。<br><br>
	 *
	 * <p>メソッド名称： 重大度表示文字取得</p>
	 *
	 * @param severity	重大度
	 * @return 表示文字
	 */
	//--------------------------------------------------------------------------
	private String severityText(String severity) {
		// エラーの場合
		if( "ERROR".equals( severity)) {
			return text( "diagnostic.severity.error") ;
		}

		// 警告の場合
		if( "WARNING".equals( severity)) {
			return text( "diagnostic.severity.warning") ;
		}

		return text( "diagnostic.severity.info") ;
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
		String serviceText = serviceInfoText == null || serviceInfoText.isDisposed() ? "" : serviceInfoText.getText() ;
		String serviceStatus = statusValueLabel.getText() ;
		String exportSummary = buildExportSummary() ;
		boolean administrator = isAdministrator() ;
		Path logPath = TinyWinNfsPaths.getLogPath( rootPath) ;
		Path backupPath = configPath.getParent().resolve( "backups") ;
		Path winswLogPath = getWinswLogPath() ;
		appendLog( text( "log.diagnosticPackageStarted")) ;

		new Thread( () -> {
			try {
				DiagnosticText diagnosticText = buildDiagnosticText( serviceStatus, serviceText, exportSummary, administrator) ;
				Path packagePath = writeDiagnosticPackage( diagnosticText, logPath, backupPath, winswLogPath) ;
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
	 * @param serviceStatus	サービス状態
	 * @param serviceText	サービス情報
	 * @param exportSummary	export概要
	 * @param administrator	管理者権限有無
	 * @return 診断概要
	 */
	//--------------------------------------------------------------------------
	private String buildDiagnosticSummary(String serviceStatus, String serviceText, String exportSummary, boolean administrator) {
		StringBuilder builder = new StringBuilder() ;
		builder.append( PRODUCT_NAME).append( " diagnostics" ).append( System.lineSeparator()) ;
		builder.append( "created=" ).append( LocalDateTime.now()).append( System.lineSeparator()) ;
		builder.append( "productVersion=" ).append( resolveProductVersion()).append( System.lineSeparator()) ;
		builder.append( "javaVersion=" ).append( System.getProperty( "java.version", "unknown")).append( System.lineSeparator()) ;
		builder.append( "javaVendor=" ).append( System.getProperty( "java.vendor", "unknown")).append( System.lineSeparator()) ;
		builder.append( "osName=" ).append( System.getProperty( "os.name", "unknown")).append( System.lineSeparator()) ;
		builder.append( "osVersion=" ).append( System.getProperty( "os.version", "unknown")).append( System.lineSeparator()) ;
		builder.append( "osArch=" ).append( System.getProperty( "os.arch", "unknown")).append( System.lineSeparator()) ;
		builder.append( "userName=" ).append( System.getProperty( "user.name", "unknown")).append( System.lineSeparator()) ;
		builder.append( "administrator=" ).append( administrator).append( System.lineSeparator()) ;
		builder.append( "serviceStatus=" ).append( serviceStatus).append( System.lineSeparator()) ;
		builder.append( System.lineSeparator()) ;
		builder.append( "Service information" ).append( System.lineSeparator()) ;
		builder.append( serviceText).append( System.lineSeparator()) ;
		builder.append( System.lineSeparator()) ;
		builder.append( "Exports" ).append( System.lineSeparator()) ;
		builder.append( exportSummary) ;
		return builder.toString() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 製品バージョンを解決します。<br><br>
	 *
	 * <p>メソッド名称： 製品バージョン解決</p>
	 *
	 * @return 製品バージョン
	 */
	//--------------------------------------------------------------------------
	private String resolveProductVersion() {
		Package packageInfo = TinyWinNfsSwtManager.class.getPackage() ;

		// Package情報が存在する場合
		if( packageInfo != null && packageInfo.getImplementationVersion() != null) {
			return packageInfo.getImplementationVersion() ;
		}

		return PRODUCT_VERSION ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断テキストを作成します。<br><br>
	 *
	 * <p>メソッド名称： 診断テキスト作成</p>
	 *
	 * @param serviceStatus	サービス状態
	 * @param serviceText	サービス情報
	 * @param exportSummary	export概要
	 * @param administrator	管理者権限有無
	 * @return 診断テキスト
	 */
	//--------------------------------------------------------------------------
	private DiagnosticText buildDiagnosticText(String serviceStatus, String serviceText, String exportSummary, boolean administrator) {
		String summary = buildDiagnosticSummary( serviceStatus, serviceText, exportSummary, administrator) ;

		try {
			NfsServerConfig config = NfsServerConfig.load( configPath) ;
			DiagnosticReport report = NfsDiagnostics.collect( config) ;
			return new DiagnosticText( summary + System.lineSeparator() + report.formatText(), report.formatText()) ;
		} catch( Exception ex) {
			String failure = "Diagnostic report failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + System.lineSeparator() ;
			return new DiagnosticText( summary + System.lineSeparator() + failure, failure) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 診断パッケージを書き込みます。<br><br>
	 *
	 * <p>メソッド名称： 診断パッケージ書込</p>
	 *
	 * @param diagnosticText	診断テキスト
	 * @param logPath			ログパス
	 * @param backupPath		バックアップパス
	 * @param winswLogPath		WinSWログパス
	 * @return 診断パッケージ
	 * @throws IOException 書込異常
	 */
	//--------------------------------------------------------------------------
	private Path writeDiagnosticPackage(DiagnosticText diagnosticText, Path logPath, Path backupPath, Path winswLogPath) throws IOException {
		Path diagnosticDirectory = dataRootPath.resolve( "diagnostics") ;
		Files.createDirectories( diagnosticDirectory) ;
		String timestamp = DateTimeFormatter.ofPattern( "yyyyMMdd-HHmmss").format( LocalDateTime.now()) ;
		Path packagePath = diagnosticDirectory.resolve( "tinywin-nfs-diagnostics-" + timestamp + ".zip") ;

		try( ZipOutputStream zip = new ZipOutputStream( Files.newOutputStream( packagePath))) {
			addTextEntry( zip, "summary.txt", diagnosticText.getSummary()) ;
			addTextEntry( zip, "diagnostics/report.txt", diagnosticText.getReport()) ;
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
			String output = readStreamOutput( process.getInputStream()).trim() ;
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
	 * サーバーログビューを更新します。<br><br>
	 *
	 * <p>メソッド名称： サーバーログビュー更新</p>
	 */
	//--------------------------------------------------------------------------
	private void refreshServerLogView() {
		// サーバーログビューが存在しない場合
		if( serverLogText == null || serverLogText.isDisposed()) {
			return ;
		}

		Path logPath = TinyWinNfsPaths.getLogPath( rootPath) ;
		serverLogText.setText( text( "log.loading") ) ;

		new Thread( () -> {
			String content = readLogTail( logPath) ;
			runOnUi( () -> {
				serverLogContent = content ;
				applyServerLogFilter() ;
			}) ;
		}, "server-log-view").start() ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログ末尾を読み込みます。<br><br>
	 *
	 * <p>メソッド名称： ログ末尾読込</p>
	 *
	 * @param logPath	ログパス
	 * @return ログ内容
	 */
	//--------------------------------------------------------------------------
	private String readLogTail(Path logPath) {
		try {
			// ログファイルが存在しない場合
			if( !Files.isRegularFile( logPath)) {
				return format( "log.serverLogMissing", logPath) ;
			}

			long size = Files.size( logPath) ;
			long skip = Math.max( 0L, size - MAX_LOG_READ_BYTES) ;

			try( InputStream input = Files.newInputStream( logPath)) {
				long skipped = input.skip( skip) ;

				// skipが不足した場合
				while( skipped < skip) {
					long additional = input.skip( skip - skipped) ;

					// これ以上skipできない場合
					if( additional <= 0L) {
						break ;
					}

					skipped += additional ;
				}

				String content = new String( input.readAllBytes(), StandardCharsets.UTF_8) ;

				// 途中から読んだ場合
				if( skip > 0L) {
					int lineBreak = content.indexOf( '\n' ) ;

					// 先頭の途中行を除去できる場合
					if( lineBreak >= 0 && lineBreak + 1 < content.length()) {
						content = content.substring( lineBreak + 1) ;
					}
				}

				return content ;
			}
		} catch( IOException ioex) {
			return format( "log.serverLogReadFailed", ioex.getMessage()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * サーバーログフィルタを適用します。<br><br>
	 *
	 * <p>メソッド名称： サーバーログフィルタ適用</p>
	 */
	//--------------------------------------------------------------------------
	private void applyServerLogFilter() {
		// サーバーログビューが存在しない場合
		if( serverLogText == null || serverLogText.isDisposed()) {
			return ;
		}

		String search = logSearchText == null || logSearchText.isDisposed() ? "" : logSearchText.getText().trim().toLowerCase() ;
		int filter = selectedComboIndex( logFilterCombo, 0) ;
		StringBuilder builder = new StringBuilder() ;
		String[] lines = serverLogContent.split( "\\R" ) ;

		// ログ行をフィルタする
		for( String line : lines) {
			// 空行の場合
			if( line.isBlank()) {
				continue ;
			}

			// 検索文字列に一致しない場合
			if( !search.isEmpty() && !line.toLowerCase().contains( search)) {
				continue ;
			}

			// 種別フィルタに一致しない場合
			if( !matchesLogFilter( line, filter)) {
				continue ;
			}

			builder.append( line).append( System.lineSeparator()) ;
		}

		serverLogText.setText( builder.toString()) ;

		// 末尾追従が有効な場合
		if( logAutoRefreshButton != null && !logAutoRefreshButton.isDisposed() && logAutoRefreshButton.getSelection()) {
			serverLogText.setSelection( serverLogText.getCharCount()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ログ種別フィルタ一致を確認します。<br><br>
	 *
	 * <p>メソッド名称： ログ種別フィルタ一致確認</p>
	 *
	 * @param line		ログ行
	 * @param filter	フィルタ
	 * @return true:一致 false:不一致
	 */
	//--------------------------------------------------------------------------
	private boolean matchesLogFilter(String line, int filter) {
		// すべて表示の場合
		if( filter == 0) {
			return true ;
		}

		// エラー系の場合
		if( filter == 1) {
			return line.contains( "parse-error" )
					|| line.contains( "denied" )
					|| line.contains( "error" )
					|| line.matches( ".*status=(?!0\\b)\\d+.*" ) ;
		}

		// 変更操作の場合
		if( filter == 2) {
			return containsAny( line, "WRITE", "CREATE", "REMOVE", "RENAME", "MKDIR", "RMDIR", "SYMLINK", "LINK", "SETATTR", "COMMIT") ;
		}

		return line.contains( "program=" ) || line.contains( "procedure=" ) || line.contains( "MOUNT" ) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 文字列がいずれかの語を含むか確認します。<br><br>
	 *
	 * <p>メソッド名称： 文字列語句包含確認</p>
	 *
	 * @param value		値
	 * @param keywords	語句
	 * @return true:含む false:含まない
	 */
	//--------------------------------------------------------------------------
	private boolean containsAny(String value, String... keywords) {
		// 語句を確認する
		for( String keyword : keywords) {
			// 語句を含む場合
			if( value.contains( keyword)) {
				return true ;
			}
		}

		return false ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログ自動更新を予約します。<br><br>
	 *
	 * <p>メソッド名称： ログ自動更新予約</p>
	 */
	//--------------------------------------------------------------------------
	private void scheduleLogAutoRefresh() {
		display.timerExec( 5000, () -> {
			// 自動更新が無効な場合
			if( logAutoRefreshButton == null || logAutoRefreshButton.isDisposed() || !logAutoRefreshButton.getSelection()) {
				return ;
			}

			refreshServerLogView() ;
			scheduleLogAutoRefresh() ;
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
		setServiceOperationRunning( true) ;
		new Thread( () -> {
			try {
				List<String> messages = runSmokeTest() ;
				runOnUi( () -> {
					// テストメッセージを追加する
					for( String message : messages) {
						appendLog( message) ;
					}

					appendLog( text( "log.endSmokeTest")) ;
					showServiceResult( "smoke test", new ProcessResult( 0, String.join( System.lineSeparator(), messages), "")) ;
					setServiceOperationRunning( false) ;
				}) ;
			} catch( Exception ex) {
				runOnUi( () -> {
					appendLog( format( "log.smokeTestFailed", ex.getMessage())) ;
					showError( text( "error.smokeTestFailed"), ex) ;
					showServiceResult( "smoke test", new ProcessResult( 1, "", ex.getMessage())) ;
					setServiceOperationRunning( false) ;
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

		setServiceOperationRunning( true) ;
		runCommandAsync( scriptName, List.of(
				"powershell.exe",
				"-NoProfile",
				"-ExecutionPolicy",
				"Bypass",
				"-File",
				scriptPath.toString()), result -> {
					setServiceOperationRunning( false) ;
					showServiceResult( scriptName, result) ;

					// コールバックが存在する場合
					if( callback != null) {
						callback.completed( result) ;
					}
				}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービス操作中状態を設定します。<br><br>
	 *
	 * <p>メソッド名称： サービス操作中状態設定</p>
	 *
	 * @param running	true:実行中 false:停止中
	 */
	//--------------------------------------------------------------------------
	private void setServiceOperationRunning(boolean running) {
		// サービス操作ボタンを切り替える
		for( Button button : serviceActionButtons) {
			// ボタンが破棄済みではない場合
			if( !button.isDisposed()) {
				button.setEnabled( !running) ;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * サービス操作結果を表示します。<br><br>
	 *
	 * <p>メソッド名称： サービス操作結果表示</p>
	 *
	 * @param title		タイトル
	 * @param result	結果
	 */
	//--------------------------------------------------------------------------
	private void showServiceResult(String title, ProcessResult result) {
		// サービス結果表示が存在しない場合
		if( serviceResultText == null || serviceResultText.isDisposed()) {
			return ;
		}

		StringBuilder builder = new StringBuilder() ;
		builder.append( format( "service.resultHeader", title, result.getExitCode())) ;
		builder.append( System.lineSeparator()) ;
		builder.append( classifyServiceResult( result)).append( System.lineSeparator()) ;

		// 標準出力が存在する場合
		if( !result.getOutput().isBlank()) {
			builder.append( System.lineSeparator()).append( "stdout:" ).append( System.lineSeparator()).append( result.getOutput()) ;
		}

		// 標準エラーが存在する場合
		if( !result.getErrorOutput().isBlank()) {
			builder.append( System.lineSeparator()).append( "stderr:" ).append( System.lineSeparator()).append( result.getErrorOutput()) ;
		}

		serviceResultText.setText( builder.toString()) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービス操作結果を分類します。<br><br>
	 *
	 * <p>メソッド名称： サービス操作結果分類</p>
	 *
	 * @param result	結果
	 * @return 分類結果
	 */
	//--------------------------------------------------------------------------
	private String classifyServiceResult(ProcessResult result) {
		// 成功の場合
		if( result.getExitCode() == 0) {
			return text( "service.result.success") ;
		}

		String output = result.getCombinedOutput().toLowerCase() ;

		// 管理者権限不足の場合
		if( output.contains( "access is denied" ) || output.contains( "administrator" ) || output.contains( "管理者" )) {
			return text( "service.result.adminRequired") ;
		}

		// ポート使用中の場合
		if( output.contains( "address already in use" ) || output.contains( "bind" ) || output.contains( "port" ) || output.contains( "ポート" )) {
			return text( "service.result.portInUse") ;
		}

		// サービス未登録の場合
		if( output.contains( "does not exist" ) || output.contains( "not installed" ) || output.contains( "not found" ) || output.contains( "見つかりません" )) {
			return text( "service.result.notInstalled") ;
		}

		return text( "service.result.failed") ;
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
				appendLog( result.getCombinedOutput()) ;
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
		StringBuilder errorOutput = new StringBuilder() ;

		try {
			ProcessBuilder builder = new ProcessBuilder( command) ;
			builder.directory( rootPath.toFile()) ;
			Process process = builder.start() ;
			Thread outputThread = readProcessOutputAsync( process.getInputStream(), output) ;
			Thread errorThread = readProcessOutputAsync( process.getErrorStream(), errorOutput) ;
			int exitCode = process.waitFor() ;
			outputThread.join() ;
			errorThread.join() ;
			return new ProcessResult( exitCode, output.toString(), errorOutput.toString()) ;
		} catch( IOException ioex) {
			errorOutput.append( ioex.getMessage()) ;
			return new ProcessResult( 1, output.toString(), errorOutput.toString()) ;
		} catch( InterruptedException iex) {
			Thread.currentThread().interrupt() ;
			errorOutput.append( iex.getMessage()) ;
			return new ProcessResult( 1, output.toString(), errorOutput.toString()) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * プロセス出力を非同期に読み取ります。<br><br>
	 *
	 * <p>メソッド名称： プロセス出力非同期読取</p>
	 *
	 * @param input		入力ストリーム
	 * @param output	出力先
	 * @return 読取Thread
	 */
	//--------------------------------------------------------------------------
	private Thread readProcessOutputAsync(InputStream input, StringBuilder output) {
		Thread thread = new Thread( () -> {
			try {
				output.append( readStreamOutput( input)) ;
			} catch( IOException ioex) {
				output.append( ioex.getMessage()).append( System.lineSeparator()) ;
			}
		}, "process-output-reader") ;
		thread.start() ;
		return thread ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ストリーム出力を読み取ります。<br><br>
	 *
	 * <p>メソッド名称： ストリーム出力読取</p>
	 *
	 * @param input	入力ストリーム
	 * @return 出力
	 * @throws IOException 読取異常
	 */
	//--------------------------------------------------------------------------
	private String readStreamOutput(InputStream input) throws IOException {
		StringBuilder output = new StringBuilder() ;

		try( BufferedReader reader = new BufferedReader( new InputStreamReader( input, Charset.defaultCharset()))) {
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
	 * 診断テキストクラスです。<br><br>
	 *
	 * <p>クラス名称： 診断テキスト</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class DiagnosticText {
		//	内部定義	--------------------------------------------------------
		/** 概要 */
		private final String				summary ;

		/** レポート */
		private final String				report ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param summary	概要
		 * @param report	レポート
		 */
		//----------------------------------------------------------------------
		DiagnosticText(String summary, String report) {
			this.summary = summary ;
			this.report = report ;
		}

		//----------------------------------------------------------------------
		/**
		 * 概要を取得します。<br><br>
		 *
		 * <p>メソッド名称： 概要取得</p>
		 *
		 * @return 概要
		 */
		//----------------------------------------------------------------------
		String getSummary() {
			return summary ;
		}

		//----------------------------------------------------------------------
		/**
		 * レポートを取得します。<br><br>
		 *
		 * <p>メソッド名称： レポート取得</p>
		 *
		 * @return レポート
		 */
		//----------------------------------------------------------------------
		String getReport() {
			return report ;
		}
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

		/** エラー出力 */
		private final String				errorOutput ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param exitCode	終了コード
		 * @param output		出力
		 * @param errorOutput	エラー出力
		 */
		//----------------------------------------------------------------------
		ProcessResult(int exitCode, String output, String errorOutput) {
			this.exitCode = exitCode ;
			this.output = output == null ? "" : output ;
			this.errorOutput = errorOutput == null ? "" : errorOutput ;
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

		//----------------------------------------------------------------------
		/**
		 * エラー出力を取得します。<br><br>
		 *
		 * <p>メソッド名称： エラー出力取得</p>
		 *
		 * @return エラー出力
		 */
		//----------------------------------------------------------------------
		String getErrorOutput() {
			return errorOutput ;
		}

		//----------------------------------------------------------------------
		/**
		 * 結合出力を取得します。<br><br>
		 *
		 * <p>メソッド名称： 結合出力取得</p>
		 *
		 * @return 結合出力
		 */
		//----------------------------------------------------------------------
		String getCombinedOutput() {
			// エラー出力がない場合
			if( errorOutput.isBlank()) {
				return output ;
			}

			// 標準出力がない場合
			if( output.isBlank()) {
				return errorOutput ;
			}

			return output + System.lineSeparator() + errorOutput ;
		}
	}

	//------------------------------------------------------------------------------
	/**
	 * 診断行クラスです。<br><br>
	 *
	 * <p>クラス名称： 診断行</p>
	 *
	 * @author Shunji Ogawa
	 * @version 01.00.00
	 */
	//------------------------------------------------------------------------------
	private static class DiagnosticRow {
		//	内部定義	--------------------------------------------------------
		/** 重大度 */
		private final String				severity ;

		/** 発生元 */
		private final String				source ;

		/** コード */
		private final String				code ;

		/** メッセージ */
		private final String				message ;

		//----------------------------------------------------------------------
		/**
		 * インスタンスを生成します。<br><br>
		 *
		 * <p>メソッド名称： コンストラクタ</p>
		 *
		 * @param severity	重大度
		 * @param source	発生元
		 * @param code		コード
		 * @param message	メッセージ
		 */
		//----------------------------------------------------------------------
		DiagnosticRow(String severity, String source, String code, String message) {
			this.severity = severity == null ? "" : severity ;
			this.source = source == null ? "" : source ;
			this.code = code == null ? "" : code ;
			this.message = message == null ? "" : message ;
		}

		//----------------------------------------------------------------------
		/**
		 * 重大度を取得します。<br><br>
		 *
		 * <p>メソッド名称： 重大度取得</p>
		 *
		 * @return 重大度
		 */
		//----------------------------------------------------------------------
		String getSeverity() {
			return severity ;
		}

		//----------------------------------------------------------------------
		/**
		 * 発生元を取得します。<br><br>
		 *
		 * <p>メソッド名称： 発生元取得</p>
		 *
		 * @return 発生元
		 */
		//----------------------------------------------------------------------
		String getSource() {
			return source ;
		}

		//----------------------------------------------------------------------
		/**
		 * コードを取得します。<br><br>
		 *
		 * <p>メソッド名称： コード取得</p>
		 *
		 * @return コード
		 */
		//----------------------------------------------------------------------
		String getCode() {
			return code ;
		}

		//----------------------------------------------------------------------
		/**
		 * メッセージを取得します。<br><br>
		 *
		 * <p>メソッド名称： メッセージ取得</p>
		 *
		 * @return メッセージ
		 */
		//----------------------------------------------------------------------
		String getMessage() {
			return message ;
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
