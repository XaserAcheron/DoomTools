package net.mtrop.doom.tools.gui.swing;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.mtrop.doom.tools.Version;
import net.mtrop.doom.tools.common.Common;
import net.mtrop.doom.tools.gui.DoomToolsApplicationInstance;
import net.mtrop.doom.tools.gui.DoomToolsGUIUtils;
import net.mtrop.doom.tools.gui.DoomToolsLanguageManager;
import net.mtrop.doom.tools.gui.doommake.DoomMakeNewProjectApp;
import net.mtrop.doom.tools.gui.doommake.DoomMakeProjectApp;
import net.mtrop.doom.tools.gui.swing.panels.DoomToolsAboutPanel;
import net.mtrop.doom.tools.gui.swing.panels.DoomToolsDesktopPane;

import static net.mtrop.doom.tools.struct.swing.ComponentFactory.*;
import static net.mtrop.doom.tools.struct.swing.ContainerFactory.*;

import java.awt.event.KeyEvent;

/**
 * The main DoomTools application window.
 * @author Matthew Tropiano
 */
public class DoomToolsMainWindow extends JFrame 
{
	private static final long serialVersionUID = -8837485206120777188L;

	/** Utils. */
	private DoomToolsGUIUtils utils;
    /** Language manager. */
    private DoomToolsLanguageManager language;
	/** Desktop pane. */
	private DoomToolsDesktopPane desktop;
	/** Shutdown hook. */
	private Runnable shutDownHook;
	
	/**
	 * Creates the DoomTools main window.
	 * @param shutDownHook the application shutdown hook.
	 */
	public DoomToolsMainWindow(Runnable shutDownHook)
	{
		super();
		this.utils = DoomToolsGUIUtils.get();
		this.language = DoomToolsLanguageManager.get();
		this.shutDownHook = shutDownHook;
		
		setIconImages(utils.getWindowIcons());
		setTitle("DoomTools v" + Version.DOOMTOOLS);
		setJMenuBar(createMenuBar());
		setContentPane(this.desktop = new DoomToolsDesktopPane());
		setLocationByPlatform(true);
		pack();
	}

	private JMenuBar createMenuBar()
	{
		return menuBar(
			utils.createMenuFromLanguageKey("doomtools.menu.file",
				utils.createItemFromLanguageKey("doomtools.menu.file.item.exit",
					(c, e) -> shutDownHook.run()
				)
			),
			utils.createMenuFromLanguageKey("doomtools.menu.tools",
				utils.createItemFromLanguageKey("doomtools.menu.tools.item.doommake",
					utils.createItemFromLanguageKey("doomtools.menu.tools.item.doommake.new",
						(c, e) -> addApplication(DoomMakeNewProjectApp.class)
					),
					utils.createItemFromLanguageKey("doomtools.menu.tools.item.doommake.open",
						(c, e) -> addApplication(DoomMakeProjectApp.class)
					)
				)
			),
			utils.createMenuFromLanguageKey("doomtools.menu.help",
				utils.createItemFromLanguageKey("doomtools.menu.help.item.about",
					(c, e) -> openAboutModal()
				)
			)
		);
	}
	
	private void openAboutModal()
	{
		modal(this, utils.getWindowIcons(), 
			language.getText("doomtools.about.title"), 
			new DoomToolsAboutPanel(), 
			choice("OK", KeyEvent.VK_O)
		).openThenDispose();
	}
	
	/**
	 * Adds a new application instance to the desktop.
	 * @param <I> the instance type.
	 * @param applicationClass the application class.
	 * @throws RuntimeException if the class could not be instantiated.
	 */
	public <I extends DoomToolsApplicationInstance> void addApplication(Class<I> applicationClass)
	{
		desktop.addApplicationFrame(Common.create(applicationClass)).setVisible(true);
	}
	
}