package net.mtrop.doom.tools.gui;

import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;

import javax.swing.JFrame;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import net.mtrop.doom.tools.common.Common;
import net.mtrop.doom.tools.gui.apps.DecoHackEditorApp;
import net.mtrop.doom.tools.gui.apps.DoomMakeNewProjectApp;
import net.mtrop.doom.tools.gui.apps.DoomMakeOpenProjectApp;
import net.mtrop.doom.tools.gui.apps.WadScriptEditorApp;
import net.mtrop.doom.tools.gui.managers.DoomToolsEditorProvider;
import net.mtrop.doom.tools.gui.managers.DoomToolsIconManager;
import net.mtrop.doom.tools.gui.managers.DoomToolsImageManager;
import net.mtrop.doom.tools.gui.managers.DoomToolsLanguageManager;
import net.mtrop.doom.tools.gui.managers.DoomToolsLogger;
import net.mtrop.doom.tools.gui.managers.DoomToolsSettingsManager;
import net.mtrop.doom.tools.gui.managers.DoomToolsTaskManager;
import net.mtrop.doom.tools.gui.swing.DoomToolsApplicationFrame;
import net.mtrop.doom.tools.gui.swing.DoomToolsMainWindow;
import net.mtrop.doom.tools.gui.swing.panels.MultiFileEditorPanel;
import net.mtrop.doom.tools.struct.SingletonProvider;
import net.mtrop.doom.tools.struct.swing.SwingUtils;
import net.mtrop.doom.tools.struct.util.ArrayUtils;
import net.mtrop.doom.tools.struct.util.EnumUtils;
import net.mtrop.doom.tools.struct.util.ObjectUtils;
import net.mtrop.doom.tools.struct.LoggingFactory.Logger;

/**
 * Manages the DoomTools GUI window. 
 * @author Matthew Tropiano
 */
public final class DoomToolsGUIMain 
{
	/**
	 * Valid application names. 
	 */
	public interface ApplicationNames
	{
		/** DoomMake - New Project. */
		String DOOMMAKE_NEW = "doommake-new";
		/** DoomMake - Open Project. */
		String DOOMMAKE_OPEN = "doommake-open";
		/** WadScript. */
		String WADSCRIPT = "wadscript";
		/** DECOHack. */
		String DECOHACK = "decohack";
	}
	
	/**
	 * Supported GUI Themes
	 */
	public enum GUIThemeType
	{
		LIGHT("com.formdev.flatlaf.FlatLightLaf"),
		DARK("com.formdev.flatlaf.FlatDarkLaf"),
		INTELLIJ("com.formdev.flatlaf.FlatIntelliJLaf"),
		DARCULA("com.formdev.flatlaf.FlatDarculaLaf");
		
		public static final Map<String, GUIThemeType> MAP = EnumUtils.createCaseInsensitiveNameMap(GUIThemeType.class);
		
		private final String className;
		
		private GUIThemeType(String className)
		{
			this.className = className;
		}
	}
	
    /** Logger. */
    private static final Logger LOG = DoomToolsLogger.getLogger(DoomToolsGUIMain.class); 

    /** Instance socket. */
	private static final int INSTANCE_SOCKET_PORT = 54666;
    /** The instance encapsulator. */
    private static final SingletonProvider<DoomToolsGUIMain> INSTANCE = new SingletonProvider<>(() -> new DoomToolsGUIMain());
    /** Application starter linker. */
    private static final DoomToolsApplicationStarter STARTER = new DoomToolsApplicationStarter()
	{
		@Override
		public void startApplication(DoomToolsApplicationInstance applicationInstance) 
		{
			DoomToolsGUIMain.startApplication(applicationInstance);
		}
	};
    
    /** Instance socket. */
    @SuppressWarnings("unused")
	private static ServerSocket instanceSocket;
    
	/**
	 * @return the singleton instance of this settings object.
	 */
	public static DoomToolsGUIMain get()
	{
		return INSTANCE.get();
	}

	/**
	 * @return true if already running, false if not.
	 */
	public static boolean isAlreadyRunning()
	{
		try {
			instanceSocket = new ServerSocket(INSTANCE_SOCKET_PORT, 50, InetAddress.getByName(null));
			return false;
		} catch (IOException e) {
			return true;
		}
	}
	
	/**
	 * Starts an orphaned main GUI Application.
	 * Inherits the working directory and environment.
	 * @return the process created.
	 * @throws IOException if the application could not be created.
	 * @see Common#spawnJava(Class) 
	 */
	public static Process startGUIAppProcess() throws IOException
	{
		return Common.spawnJava(DoomToolsGUIMain.class).exec();
	}
	
	/**
	 * Starts an orphaned GUI Application by name.
	 * Inherits the working directory and environment.
	 * @param appName the application name (see {@link ApplicationNames}).
	 * @param args optional addition arguments (some apps require them).
	 * @return the process created.
	 * @throws IOException if the application could not be created.
	 * @see Common#spawnJava(Class) 
	 */
	public static Process startGUIAppProcess(String appName, String ... args) throws IOException
	{
		return Common.spawnJava(DoomToolsGUIMain.class).arg(appName).args(args).exec();
	}
	
	
	/* ==================================================================== */

	/**
	 * Adds a new application instance to the main desktop.
	 * @param applicationInstance the application instance.
	 */
	private static void startApplication(final DoomToolsApplicationInstance applicationInstance)
	{
		final DoomToolsApplicationFrame frame = new DoomToolsApplicationFrame(applicationInstance, STARTER);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e) 
			{
				if (applicationInstance.shouldClose())
				{
					frame.setVisible(false);
					applicationInstance.onClose(e.getSource());
					frame.dispose();
				}
			}
		});
		frame.setVisible(true);
	}
	
	/**
	 * Sets the preferred Look And Feel.
	 */
	public static void setLAF() 
	{
		GUIThemeType theme = GUIThemeType.MAP.get(DoomToolsSettingsManager.get().getThemeName());
		SwingUtils.setLAF(theme != null ? theme.className : GUIThemeType.LIGHT.className);
	}

	/**
	 * Pre-loads all of the completion providers into memory in a separate thread.
	 * <p>
	 * The completion providers are not instantiated until they are needed for a
	 * particular style, but depending on how complex they are, this will cause a very noticeable
	 * hitch on first use. Calling this function will start pre-loading them in a separate thread
	 * so that they are ready to be used instantly.
	 */
	private static void preWarmCompletionProviders()
	{
		DoomToolsTaskManager tasks = DoomToolsTaskManager.get();
		LOG.info("Pre-warming completion providers...");
		tasks.spawn(() -> {
			DoomToolsEditorProvider editorProvider = DoomToolsEditorProvider.get();
			editorProvider.getProviderByStyle(DoomToolsEditorProvider.SYNTAX_STYLE_DECOHACK);
			editorProvider.getProviderByStyle(DoomToolsEditorProvider.SYNTAX_STYLE_DOOMMAKE);
			editorProvider.getProviderByStyle(DoomToolsEditorProvider.SYNTAX_STYLE_ROOKSCRIPT);
			editorProvider.getProviderByStyle(DoomToolsEditorProvider.SYNTAX_STYLE_WADMERGE);
			editorProvider.getProviderByStyle(DoomToolsEditorProvider.SYNTAX_STYLE_WADSCRIPT);
			LOG.info("Completion providers pre-warm finished.");
		});
	}
	
	/**
	 * Pre-loads common icons.
	 */
	private static void preWarmCommonIcons() 
	{
		DoomToolsTaskManager tasks = DoomToolsTaskManager.get();
		LOG.info("Pre-warming common icons...");
		tasks.spawn(() -> {
			DoomToolsIconManager iconManager = DoomToolsIconManager.get();
			iconManager.getImage("activity.gif");
			LOG.info("Icon pre-warm finished.");
		});
	}

	/**
	 * Pre-loads common non-animated images.
	 */
	private static void preWarmCommonImages() 
	{
		DoomToolsTaskManager tasks = DoomToolsTaskManager.get();
		LOG.info("Pre-warming common images...");
		tasks.spawn(() -> {
			DoomToolsImageManager imageManager = DoomToolsImageManager.get();
			imageManager.getImage("doomtools-logo-16.png"); 
			imageManager.getImage("doomtools-logo-32.png"); 
			imageManager.getImage("doomtools-logo-48.png"); 
			imageManager.getImage("doomtools-logo-64.png"); 
			imageManager.getImage("doomtools-logo-96.png"); 
			imageManager.getImage("doomtools-logo-128.png"); 
			imageManager.getImage("script.png");
			imageManager.getImage("script-unsaved.png");
			imageManager.getImage("close-icon.png");
			imageManager.getImage("success.png");
			imageManager.getImage("error.png");
			LOG.info("Image pre-warm finished.");
		});
	}

	/**
	 * Pre-loads common components.
	 */
	private static void preWarmCommonComponents()
	{
		DoomToolsTaskManager tasks = DoomToolsTaskManager.get();
		LOG.info("Pre-warming common components...");
		tasks.spawn(() -> {
			DoomToolsEditorProvider editorProvider = DoomToolsEditorProvider.get();
			editorProvider.initCustomLanguages();
			new MultiFileEditorPanel();
			new RSyntaxTextArea();
			LOG.info("Component pre-warm finished.");
		});
	}

	
	/**
	 * Pre-warms a bunch of elements for DoomTools to avoid weird UX-related hitches.
	 * Only useful for loading the full application.
	 */
    private static void preWarmCommonElements() 
    {
    	preWarmCompletionProviders();
    	preWarmCommonImages();
    	preWarmCommonIcons();
    	preWarmCommonComponents();
	}

    /* ==================================================================== */

	/**
	 * Main method - check for running local instance. If running, do nothing.
	 * @param args command line arguments.
	 */
	public static void main(String[] args) 
	{
		setLAF();
		
		// no args - run main application.
		if (args.length == 0)
		{
	    	if (isAlreadyRunning())
	    	{
	    		System.err.println("DoomTools is already running.");
	    		System.exit(1);
	    		return;
	    	}
	    	preWarmCommonElements();
			get().createAndDisplayMainWindow();
		}
		// run standalone application.
		else 
		{
			try 
			{
				switch (args[0])
				{
					default:
					{
		        		SwingUtils.error("Expected valid application name.");
			    		System.err.println("ERROR: Expected valid application name.");
		        		System.exit(-1);
			        	return;
					}
		        	
					case ApplicationNames.DOOMMAKE_NEW:
					{
						startApplication(new DoomMakeNewProjectApp(ArrayUtils.arrayElement(args, 1)));
						break;
					}
					
					case ApplicationNames.DOOMMAKE_OPEN:
					{
						String path = ArrayUtils.arrayElement(args, 1);
						
						// No path. Open file.
						if (ObjectUtils.isEmpty(path))
						{
							DoomMakeOpenProjectApp app;
							if ((app = DoomMakeOpenProjectApp.openAndCreate(null)) != null)
								startApplication(app);
						}
						else
						{
							File projectDirectory = new File(args[1]);
							if (DoomMakeOpenProjectApp.isProjectDirectory(projectDirectory))
								startApplication(new DoomMakeOpenProjectApp(projectDirectory));
							else
								SwingUtils.error(DoomToolsLanguageManager.get().getText("doommake.project.open.browse.baddir", projectDirectory.getAbsolutePath()));
						}
						break;
					}

					case ApplicationNames.WADSCRIPT:
					{
						startApplication(new WadScriptEditorApp());
						break;
					}

					case ApplicationNames.DECOHACK:
					{
						startApplication(new DecoHackEditorApp());
						break;
					}
					
				}
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
	    		SwingUtils.error("Missing argument for application: " + e.getLocalizedMessage());
	    		System.err.println("ERROR: Missing argument for application.");
        		System.exit(-1);
			}
		}
		
	}

	/** Settings singleton. */
	private DoomToolsSettingsManager settings;
	/** Language manager. */
    private DoomToolsLanguageManager language;
    /** The main window. */
    private DoomToolsMainWindow window;
    
    private DoomToolsGUIMain()
    {
    	this.settings = DoomToolsSettingsManager.get();
    	this.language = DoomToolsLanguageManager.get();
    	this.window = null;
    }

    /**
     * Creates and displays the main window.
     */
    public void createAndDisplayMainWindow()
    {
    	LOG.info("Creating main window...");
    	window = new DoomToolsMainWindow(this::attemptShutDown);
    	window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    	window.addWindowListener(new WindowAdapter()
    	{
    		@Override
    		public void windowClosing(WindowEvent e) 
    		{
    			attemptShutDown();
    		}
		});
    	
    	Rectangle windowBounds;
    	if ((windowBounds = settings.getBounds()) != null)
    		window.setBounds(windowBounds);
    	
    	window.setVisible(true);
		if (settings.getBoundsMaximized())
			window.setExtendedState(window.getExtendedState() | DoomToolsMainWindow.MAXIMIZED_BOTH);
		
    	LOG.info("Window created.");
    }

    // Attempts a shutdown, prompting the user first.
    private boolean attemptShutDown()
    {
    	LOG.debug("Shutdown attempted.");
		if (SwingUtils.yesTo(window, language.getText("doomtools.quit")))
		{
			shutDown();
			return true;
		}
		return false;
    }

    // Saves and quits.
    private void shutDown()
    {
    	LOG.info("Shutting down DoomTools GUI...");
    	
    	LOG.info("Sending close to all open apps...");
    	if (!window.shutDownApps())
    	{
        	LOG.info("Shutdown aborted. All apps could not be closed!");
    		return;
    	}
    	
    	LOG.debug("Disposing main window...");
    	settings.setBounds(window);
    	window.setVisible(false);
    	window.dispose();
    	LOG.debug("Main window disposed.");
    	
    	LOG.info("Exiting JVM...");
    	System.exit(0);
    }
    
}
