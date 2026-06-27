package jp.co.enterogawa.nfs.manager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
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

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import jp.co.enterogawa.nfs.export.FileHandle;
import jp.co.enterogawa.nfs.rpc.RpcConstants;
import jp.co.enterogawa.nfs.xdr.XdrReader;
import jp.co.enterogawa.nfs.xdr.XdrWriter;

//------------------------------------------------------------------------------
/**
 * NFSサーバー管理ツールクラスです。<br><br>
 *
 * <p>クラス名称： NFSサーバー管理ツール</p>
 *
 * @author Shunji Ogawa
 * @version 01.00.00
 */
//------------------------------------------------------------------------------
public class TinyWinNfsManager extends JFrame {
	//	定数定義	------------------------------------------------------------
	/** シリアルバージョン */
	private static final long			serialVersionUID = 1L ;

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
	/** プロジェクトルート */
	private final Path					rootPath ;

	/** 設定ファイル */
	private final Path					configPath ;

	/** ログ */
	private final JTextArea				logText = new JTextArea() ;

	/** 状態表示 */
	private final JLabel					statusLabel = new JLabel() ;

	/** 管理者権限表示 */
	private final JLabel					adminLabel = new JLabel() ;

	/** export名 */
	private final JTextField				exportNameField = new JTextField() ;

	/** exportパス */
	private final JTextField				exportPathField = new JTextField() ;

	/** export書込可否 */
	private final JCheckBox				exportWritableCheckBox = new JCheckBox() ;

	/** サーバーホスト名 */
	private final JTextField				serverHostField = new JTextField() ;

	/** クライアント側マウントポイント */
	private final JTextField				clientMountPointField = new JTextField() ;

	/** mountコマンド */
	private final JTextArea				mountCommandText = new JTextArea() ;

	/** Portmapポート */
	private final JTextField				portmapPortField = new JTextField() ;

	/** NFSポート */
	private final JTextField				nfsPortField = new JTextField() ;

	/** MOUNTポート */
	private final JTextField				mountPortField = new JTextField() ;

	/** UID */
	private final JTextField				uidField = new JTextField() ;

	/** GID */
	private final JTextField				gidField = new JTextField() ;

	/** ファイルモード */
	private final JTextField				fileModeField = new JTextField() ;

	/** ディレクトリモード */
	private final JTextField				directoryModeField = new JTextField() ;

	/** ブロックサイズ */
	private final JTextField				blockSizeField = new JTextField() ;

	/** 読込サイズ */
	private final JTextField				readSizeField = new JTextField() ;

	/** ファイル名文字コード */
	private final JTextField				filenameCharsetField = new JTextField() ;

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
		SwingUtilities.invokeLater( () -> {
			try {
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName()) ;
			} catch( Exception ex) {
				// システムLookAndFeelを設定できない場合
			}

			TinyWinNfsManager manager = new TinyWinNfsManager() ;
			manager.setVisible( true) ;
		}) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * インスタンスを生成します。<br><br>
	 *
	 * <p>メソッド名称： コンストラクタ</p>
	 */
	//--------------------------------------------------------------------------
	public TinyWinNfsManager() {
		rootPath = detectRootPath() ;
		configPath = rootPath.resolve( "conf").resolve( "nfs-server.properties") ;
		initWindow() ;
		loadConfig() ;
		refreshStatus() ;
		appendLog( "Root: " + rootPath) ;
		appendLog( "Config: " + configPath) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ウィンドウを初期化します。<br><br>
	 *
	 * <p>メソッド名称： ウィンドウ初期化</p>
	 */
	//--------------------------------------------------------------------------
	private void initWindow() {
		setTitle( PRODUCT_NAME + " Manager") ;
		applyWindowIcon() ;
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE) ;
		setSize( 860, 640) ;
		setLocationRelativeTo( null) ;
		setLayout( new BorderLayout( 8, 8)) ;

		add( createHeaderPanel(), BorderLayout.NORTH) ;
		add( createTabbedPane(), BorderLayout.CENTER) ;
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

		try( var inputStream = Files.newInputStream( iconPath)) {
			Image image = ImageIO.read( inputStream) ;

			// 画像を読み込めた場合
			if( image != null) {
				setIconImages( List.of( image)) ;
			}
		} catch( IOException ioex) {
			// アイコンが読み込めない場合は既定アイコンを使用する
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * ヘッダーパネルを作成します。<br><br>
	 *
	 * <p>メソッド名称： ヘッダーパネル作成</p>
	 *
	 * @return ヘッダーパネル
	 */
	//--------------------------------------------------------------------------
	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel( new BorderLayout()) ;
		JPanel leftPanel = new JPanel( new FlowLayout( FlowLayout.LEFT)) ;
		JPanel rightPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT)) ;

		statusLabel.setText( "Service: checking..." ) ;
		adminLabel.setText( "Administrator: " + (isAdministrator() ? "yes" : "no")) ;
		leftPanel.add( statusLabel) ;
		leftPanel.add( adminLabel) ;
		rightPanel.add( createButton( "Refresh", this::onRefreshStatus)) ;
		panel.add( leftPanel, BorderLayout.WEST) ;
		panel.add( rightPanel, BorderLayout.EAST) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * タブを作成します。<br><br>
	 *
	 * <p>メソッド名称： タブ作成</p>
	 *
	 * @return タブ
	 */
	//--------------------------------------------------------------------------
	private JTabbedPane createTabbedPane() {
		JTabbedPane tabbedPane = new JTabbedPane() ;
		tabbedPane.addTab( "Settings", createSettingsPanel()) ;
		tabbedPane.addTab( "Mount", createMountPanel()) ;
		tabbedPane.addTab( "Service", createServicePanel()) ;
		tabbedPane.addTab( "Log", createLogPanel()) ;
		return tabbedPane ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定パネルを作成します。<br><br>
	 *
	 * <p>メソッド名称： 設定パネル作成</p>
	 *
	 * @return 設定パネル
	 */
	//--------------------------------------------------------------------------
	private JPanel createSettingsPanel() {
		JPanel panel = new JPanel( new BorderLayout()) ;
		JPanel formPanel = new JPanel( new GridBagLayout()) ;
		int row = 0 ;

		row = addField( formPanel, row, "Export path", exportPathField, createButton( "Browse", this::onBrowseExportPath)) ;
		row = addCheckBox( formPanel, row, "Writable", exportWritableCheckBox) ;
		row = addField( formPanel, row, "Portmap port", portmapPortField, null) ;
		row = addField( formPanel, row, "NFS port", nfsPortField, null) ;
		row = addField( formPanel, row, "Mount port", mountPortField, null) ;
		row = addField( formPanel, row, "UID", uidField, null) ;
		row = addField( formPanel, row, "GID", gidField, null) ;
		row = addField( formPanel, row, "File mode", fileModeField, null) ;
		row = addField( formPanel, row, "Directory mode", directoryModeField, null) ;
		row = addField( formPanel, row, "Block size", blockSizeField, null) ;
		row = addField( formPanel, row, "Read size", readSizeField, null) ;
		addField( formPanel, row, "Filename charset", filenameCharsetField, null) ;

		JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT)) ;
		buttonPanel.add( createButton( "Reload", event -> loadConfig())) ;
		buttonPanel.add( createButton( "Save", event -> saveConfig())) ;
		buttonPanel.add( createButton( "Save + Restart", event -> saveConfigAndRestart())) ;
		panel.add( formPanel, BorderLayout.CENTER) ;
		panel.add( buttonPanel, BorderLayout.SOUTH) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * マウントパネルを作成します。<br><br>
	 *
	 * <p>メソッド名称： マウントパネル作成</p>
	 *
	 * @return マウントパネル
	 */
	//--------------------------------------------------------------------------
	private JPanel createMountPanel() {
		JPanel panel = new JPanel( new BorderLayout()) ;
		JPanel formPanel = new JPanel( new GridBagLayout()) ;
		int row = 0 ;

		row = addField( formPanel, row, "Server host", serverHostField, createButton( "Detect", event -> detectServerHost())) ;
		row = addField( formPanel, row, "Server mount name", exportNameField, null) ;
		row = addField( formPanel, row, "Client mount point", clientMountPointField, null) ;
		addCommandArea( formPanel, row, "Mount command", mountCommandText) ;

		JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT)) ;
		buttonPanel.add( createButton( "Update Command", event -> updateMountCommand())) ;
		buttonPanel.add( createButton( "Copy Command", event -> copyMountCommand())) ;
		buttonPanel.add( createButton( "Save", event -> saveConfig())) ;
		buttonPanel.add( createButton( "Save + Restart", event -> saveConfigAndRestart())) ;
		panel.add( formPanel, BorderLayout.CENTER) ;
		panel.add( buttonPanel, BorderLayout.SOUTH) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービスパネルを作成します。<br><br>
	 *
	 * <p>メソッド名称： サービスパネル作成</p>
	 *
	 * @return サービスパネル
	 */
	//--------------------------------------------------------------------------
	private JPanel createServicePanel() {
		JPanel panel = new JPanel( new BorderLayout()) ;
		JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.LEFT)) ;

		buttonPanel.add( createButton( "Install", event -> runScriptAsync( "install-service.ps1")) ) ;
		buttonPanel.add( createButton( "Start", event -> runScriptAsync( "start-service.ps1")) ) ;
		buttonPanel.add( createButton( "Stop", event -> runScriptAsync( "stop-service.ps1")) ) ;
		buttonPanel.add( createButton( "Restart", event -> runScriptAsync( "restart-service.ps1")) ) ;
		buttonPanel.add( createButton( "Uninstall", event -> confirmAndRun( "Uninstall service?", "uninstall-service.ps1")) ) ;
		buttonPanel.add( createButton( "Firewall", event -> runScriptAsync( "add-firewall-rules.ps1")) ) ;
		buttonPanel.add( createButton( "Smoke Test", event -> runSmokeTestAsync()) ) ;
		buttonPanel.add( createButton( "Status", this::onRefreshStatus)) ;
		panel.add( buttonPanel, BorderLayout.NORTH) ;
		panel.add( createServiceHelpPanel(), BorderLayout.CENTER) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * サービスヘルプパネルを作成します。<br><br>
	 *
	 * <p>メソッド名称： サービスヘルプパネル作成</p>
	 *
	 * @return サービスヘルプパネル
	 */
	//--------------------------------------------------------------------------
	private JPanel createServiceHelpPanel() {
		JPanel panel = new JPanel( new BorderLayout()) ;
		JTextArea textArea = new JTextArea() ;
		textArea.setEditable( false) ;
		textArea.setText(
				"Service ID: " + SERVICE_NAME + "\n"
				+ "Legacy service IDs: " + String.join( ", ", LEGACY_SERVICE_NAMES) + "\n"
				+ "Install/Uninstall and firewall changes require Administrator privileges.\n"
				+ "After changing settings, restart the service.\n"
				+ "Default ports: UDP/TCP 111, UDP/TCP 2049, UDP/TCP 20048.\n") ;
		panel.add( new JScrollPane( textArea), BorderLayout.CENTER) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ログパネルを作成します。<br><br>
	 *
	 * <p>メソッド名称： ログパネル作成</p>
	 *
	 * @return ログパネル
	 */
	//--------------------------------------------------------------------------
	private JPanel createLogPanel() {
		JPanel panel = new JPanel( new BorderLayout()) ;
		logText.setEditable( false) ;
		panel.add( new JScrollPane( logText), BorderLayout.CENTER) ;
		panel.add( createButton( "Clear", event -> logText.setText( "")), BorderLayout.SOUTH) ;
		return panel ;
	}

	//--------------------------------------------------------------------------
	/**
	 * フォーム項目を追加します。<br><br>
	 *
	 * <p>メソッド名称： フォーム項目追加</p>
	 *
	 * @param panel		パネル
	 * @param row		行
	 * @param label		ラベル
	 * @param field		入力欄
	 * @param button	ボタン
	 * @return 次の行
	 */
	//--------------------------------------------------------------------------
	private int addField(JPanel panel, int row, String label, JTextField field, JButton button) {
		GridBagConstraints labelConstraints = new GridBagConstraints() ;
		labelConstraints.gridx = 0 ;
		labelConstraints.gridy = row ;
		labelConstraints.anchor = GridBagConstraints.WEST ;
		labelConstraints.insets = new Insets( 6, 8, 6, 8) ;
		panel.add( new JLabel( label), labelConstraints) ;

		GridBagConstraints fieldConstraints = new GridBagConstraints() ;
		fieldConstraints.gridx = 1 ;
		fieldConstraints.gridy = row ;
		fieldConstraints.weightx = 1 ;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL ;
		fieldConstraints.insets = new Insets( 6, 8, 6, 8) ;
		panel.add( field, fieldConstraints) ;

		// ボタンが存在する場合
		if( button != null) {
			GridBagConstraints buttonConstraints = new GridBagConstraints() ;
			buttonConstraints.gridx = 2 ;
			buttonConstraints.gridy = row ;
			buttonConstraints.insets = new Insets( 6, 8, 6, 8) ;
			panel.add( button, buttonConstraints) ;
		}

		return row + 1 ;
	}

	//--------------------------------------------------------------------------
	/**
	 * チェック項目を追加します。<br><br>
	 *
	 * <p>メソッド名称： チェック項目追加</p>
	 *
	 * @param panel		パネル
	 * @param row		行
	 * @param label		ラベル
	 * @param checkBox	チェックボックス
	 * @return 次の行
	 */
	//--------------------------------------------------------------------------
	private int addCheckBox(JPanel panel, int row, String label, JCheckBox checkBox) {
		GridBagConstraints labelConstraints = new GridBagConstraints() ;
		labelConstraints.gridx = 0 ;
		labelConstraints.gridy = row ;
		labelConstraints.anchor = GridBagConstraints.WEST ;
		labelConstraints.insets = new Insets( 6, 8, 6, 8) ;
		panel.add( new JLabel( label), labelConstraints) ;

		GridBagConstraints checkConstraints = new GridBagConstraints() ;
		checkConstraints.gridx = 1 ;
		checkConstraints.gridy = row ;
		checkConstraints.gridwidth = 2 ;
		checkConstraints.anchor = GridBagConstraints.WEST ;
		checkConstraints.insets = new Insets( 6, 8, 6, 8) ;
		panel.add( checkBox, checkConstraints) ;
		return row + 1 ;
	}

	//--------------------------------------------------------------------------
	/**
	 * コマンド欄を追加します。<br><br>
	 *
	 * <p>メソッド名称： コマンド欄追加</p>
	 *
	 * @param panel		パネル
	 * @param row		行
	 * @param label		ラベル
	 * @param textArea	テキストエリア
	 * @return 次の行
	 */
	//--------------------------------------------------------------------------
	private int addCommandArea(JPanel panel, int row, String label, JTextArea textArea) {
		GridBagConstraints labelConstraints = new GridBagConstraints() ;
		labelConstraints.gridx = 0 ;
		labelConstraints.gridy = row ;
		labelConstraints.anchor = GridBagConstraints.NORTHWEST ;
		labelConstraints.insets = new Insets( 6, 8, 6, 8) ;
		panel.add( new JLabel( label), labelConstraints) ;

		textArea.setRows( 5) ;
		textArea.setLineWrap( true) ;
		textArea.setWrapStyleWord( true) ;
		GridBagConstraints textConstraints = new GridBagConstraints() ;
		textConstraints.gridx = 1 ;
		textConstraints.gridy = row ;
		textConstraints.gridwidth = 2 ;
		textConstraints.weightx = 1 ;
		textConstraints.weighty = 1 ;
		textConstraints.fill = GridBagConstraints.BOTH ;
		textConstraints.insets = new Insets( 6, 8, 6, 8) ;
		panel.add( new JScrollPane( textArea), textConstraints) ;
		return row + 1 ;
	}

	//--------------------------------------------------------------------------
	/**
	 * ボタンを作成します。<br><br>
	 *
	 * <p>メソッド名称： ボタン作成</p>
	 *
	 * @param text		表示文字
	 * @param listener	リスナ
	 * @return ボタン
	 */
	//--------------------------------------------------------------------------
	private JButton createButton(String text, java.awt.event.ActionListener listener) {
		JButton button = new JButton( text) ;
		button.addActionListener( listener) ;
		return button ;
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

			exportNameField.setText( properties.getProperty( "export.name", "/export")) ;
			exportPathField.setText( properties.getProperty( "export.path", rootPath.resolve( "export").toString())) ;
			exportWritableCheckBox.setSelected( Boolean.parseBoolean( properties.getProperty( "export.writable", "true")) ) ;
			serverHostField.setText( properties.getProperty( "client.server.host", detectLocalHostName())) ;
			clientMountPointField.setText( properties.getProperty( "client.mount.point", "/mnt")) ;
			portmapPortField.setText( properties.getProperty( "portmap.port", "111")) ;
			nfsPortField.setText( properties.getProperty( "nfs.port", "2049")) ;
			mountPortField.setText( properties.getProperty( "mount.port", "20048")) ;
			uidField.setText( properties.getProperty( "uid", "0")) ;
			gidField.setText( properties.getProperty( "gid", "0")) ;
			fileModeField.setText( properties.getProperty( "file.mode", "0644")) ;
			directoryModeField.setText( properties.getProperty( "directory.mode", "0755")) ;
			blockSizeField.setText( properties.getProperty( "block.size", "4096")) ;
			readSizeField.setText( properties.getProperty( "read.size", "8192")) ;
			filenameCharsetField.setText( properties.getProperty( "filename.charset", "UTF-8")) ;
			updateMountCommand() ;
			appendLog( "Configuration loaded.") ;
		} catch( IOException ioex) {
			appendLog( "Configuration load failed: " + ioex.getMessage()) ;
			showError( "Configuration load failed.", ioex) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * 設定を保存します。<br><br>
	 *
	 * <p>メソッド名称： 設定保存</p>
	 */
	//--------------------------------------------------------------------------
	private boolean saveConfig() {
		try {
			validateFields() ;
			Files.createDirectories( configPath.getParent()) ;
			List<String> lines = new ArrayList<String>() ;
			lines.add( "# " + PRODUCT_NAME + " configuration.") ;
			lines.add( "" ) ;
			lines.add( "portmap.port=" + portmapPortField.getText().trim()) ;
			lines.add( "nfs.port=" + nfsPortField.getText().trim()) ;
			lines.add( "mount.port=" + mountPortField.getText().trim()) ;
			lines.add( "" ) ;
			lines.add( "export.name=" + exportNameField.getText().trim()) ;
			lines.add( "export.path=" + exportPathField.getText().trim().replace( "\\", "\\\\") ) ;
			lines.add( "export.writable=" + exportWritableCheckBox.isSelected()) ;
			lines.add( "" ) ;
			lines.add( "client.server.host=" + serverHostField.getText().trim()) ;
			lines.add( "client.mount.point=" + clientMountPointField.getText().trim()) ;
			lines.add( "" ) ;
			lines.add( "uid=" + uidField.getText().trim()) ;
			lines.add( "gid=" + gidField.getText().trim()) ;
			lines.add( "file.mode=" + fileModeField.getText().trim()) ;
			lines.add( "directory.mode=" + directoryModeField.getText().trim()) ;
			lines.add( "block.size=" + blockSizeField.getText().trim()) ;
			lines.add( "read.size=" + readSizeField.getText().trim()) ;
			lines.add( "filename.charset=" + filenameCharsetField.getText().trim()) ;
			Files.write( configPath, lines, StandardCharsets.UTF_8) ;
			updateMountCommand() ;
			appendLog( "Configuration saved.") ;
			return true ;
		} catch( Exception ex) {
			appendLog( "Configuration save failed: " + ex.getMessage()) ;
			showError( "Configuration save failed.", ex) ;
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
		validatePort( "Portmap port", portmapPortField.getText()) ;
		validatePort( "NFS port", nfsPortField.getText()) ;
		validatePort( "Mount port", mountPortField.getText()) ;
		validatePositiveInt( "Block size", blockSizeField.getText()) ;
		validatePositiveInt( "Read size", readSizeField.getText()) ;
		validateInt( "UID", uidField.getText()) ;
		validateInt( "GID", gidField.getText()) ;
		validateCharset( "Filename charset", filenameCharsetField.getText()) ;

		// export名が不正な場合
		if( !exportNameField.getText().trim().startsWith( "/")) {
			throw new IllegalArgumentException( "Export name must start with '/'." ) ;
		}

		// exportパスが未入力の場合
		if( exportPathField.getText().trim().isEmpty()) {
			throw new IllegalArgumentException( "Export path is required." ) ;
		}

		Path exportPath = Path.of( exportPathField.getText().trim()) ;

		// exportパスがフォルダではない場合
		if( !Files.isDirectory( exportPath)) {
			throw new IllegalArgumentException( "Export path is not a directory: " + exportPath) ;
		}

		// クライアント側マウントポイントが不正な場合
		if( !clientMountPointField.getText().trim().startsWith( "/")) {
			throw new IllegalArgumentException( "Client mount point must start with '/'." ) ;
		}

		// サーバーホストが未入力の場合
		if( serverHostField.getText().trim().isEmpty()) {
			throw new IllegalArgumentException( "Server host is required." ) ;
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
			throw new IllegalArgumentException( label + " is out of range." ) ;
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
			throw new IllegalArgumentException( label + " must be greater than zero." ) ;
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
			throw new IllegalArgumentException( label + " must be integer." ) ;
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
			throw new IllegalArgumentException( label + " is not a supported charset." ) ;
		}
	}

	//--------------------------------------------------------------------------
	/**
	 * exportパス選択を処理します。<br><br>
	 *
	 * <p>メソッド名称： exportパス選択処理</p>
	 *
	 * @param event	イベント
	 */
	//--------------------------------------------------------------------------
	private void onBrowseExportPath(ActionEvent event) {
		JFileChooser chooser = new JFileChooser( FileSystemView.getFileSystemView()) ;
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY) ;
		chooser.setDialogTitle( "Select export folder") ;

		// 現在値が存在する場合
		if( !exportPathField.getText().trim().isEmpty()) {
			chooser.setSelectedFile( Path.of( exportPathField.getText().trim()).toFile()) ;
		}

		int result = chooser.showOpenDialog( this) ;

		// 選択された場合
		if( result == JFileChooser.APPROVE_OPTION) {
			exportPathField.setText( chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString()) ;
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
		serverHostField.setText( detectLocalHostName()) ;
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
		String host = serverHostField.getText().trim() ;
		String exportName = exportNameField.getText().trim() ;
		String mountPoint = clientMountPointField.getText().trim() ;

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
				+ "Server export path: " + exportPathField.getText().trim()) ;
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
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents( new StringSelection( command), null) ;
		appendLog( "Mount command copied: " + command) ;
	}

	//--------------------------------------------------------------------------
	/**
	 * 状態更新イベントを処理します。<br><br>
	 *
	 * <p>メソッド名称： 状態更新イベント処理</p>
	 *
	 * @param event	イベント
	 */
	//--------------------------------------------------------------------------
	private void onRefreshStatus(ActionEvent event) {
		refreshStatus() ;
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
						status = "Unknown" ;
					}

					statusLabel.setText( "Service: " + status) ;
					adminLabel.setText( "Administrator: " + (isAdministrator() ? "yes" : "no")) ;
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
		int result = JOptionPane.showConfirmDialog( this, message, "Confirm", JOptionPane.YES_NO_OPTION) ;

		// はいが選択された場合
		if( result == JOptionPane.YES_OPTION) {
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
		appendLog( "START: smoke test") ;
		new Thread( () -> {
			try {
				List<String> messages = runSmokeTest() ;
				SwingUtilities.invokeLater( () -> {
					// テストメッセージを追加する
					for( String message : messages) {
						appendLog( message) ;
					}

					appendLog( "END: smoke test exitCode=0") ;
				}) ;
			} catch( Exception ex) {
				SwingUtilities.invokeLater( () -> {
					appendLog( "Smoke test failed: " + ex.getMessage()) ;
					showError( "Smoke test failed.", ex) ;
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
		int portmapPort = validatePositiveInt( "Portmap port", portmapPortField.getText()) ;
		int nfsPort = validatePositiveInt( "NFS port", nfsPortField.getText()) ;
		int mountPort = validatePositiveInt( "Mount port", mountPortField.getText()) ;
		int mappedNfsPort = getMappedPort( portmapPort, RpcConstants.PROGRAM_NFS, 2) ;

		// Portmapが返したNFSポートが一致しない場合
		if( mappedNfsPort != nfsPort) {
			throw new IllegalStateException( "Unexpected NFS port: " + mappedNfsPort) ;
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
		arguments.writeString( exportNameField.getText().trim()) ;
		XdrReader reader = rpcCall( mountPort, RpcConstants.PROGRAM_MOUNT, 1, 1, arguments) ;
		int status = reader.readInt() ;

		// mountが失敗した場合
		if( status != 0) {
			throw new IllegalStateException( "Mount failed: " + status) ;
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
			throw new IllegalStateException( "GETATTR failed: " + status) ;
		}

		int type = reader.readInt() ;

		// ルートがディレクトリではない場合
		if( type != 2) {
			throw new IllegalStateException( "Root type is not directory: " + type) ;
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
			throw new IllegalStateException( name + " expected=" + expected + " actual=" + actual) ;
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
			showError( "Script not found: " + scriptPath, null) ;
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
		appendLog( "START: " + title) ;
		new Thread( () -> {
			ProcessResult result = runCommand( command) ;
			SwingUtilities.invokeLater( () -> {
				appendLog( result.getOutput()) ;
				appendLog( "END: " + title + " exitCode=" + result.getExitCode()) ;

				// コマンドが失敗した場合
				if( result.getExitCode() != 0) {
					showError( title + " failed.", null) ;
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
			CodeSource codeSource = TinyWinNfsManager.class.getProtectionDomain().getCodeSource() ;
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
	 * ログを追加します。<br><br>
	 *
	 * <p>メソッド名称： ログ追加</p>
	 *
	 * @param message	メッセージ
	 */
	//--------------------------------------------------------------------------
	private void appendLog(String message) {
		String text = message ;

		// メッセージが空の場合
		if( text == null || text.isBlank()) {
			return ;
		}

		logText.append( "[" + LocalDateTime.now().format( LOG_TIME_FORMAT) + "] " + text + System.lineSeparator()) ;
		logText.setCaretPosition( logText.getDocument().getLength()) ;
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
		String detail = message ;

		// 例外が存在する場合
		if( exception != null) {
			detail = message + System.lineSeparator() + exception.getMessage() ;
		}

		JOptionPane.showMessageDialog( this, detail, "Error", JOptionPane.ERROR_MESSAGE) ;
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
}
