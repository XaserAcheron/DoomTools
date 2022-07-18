package net.mtrop.doom.tools.gui.managers.settings;

import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;

import net.mtrop.doom.tools.gui.DoomToolsSettings;
import net.mtrop.doom.tools.gui.managers.DoomToolsLogger;
import net.mtrop.doom.tools.struct.SingletonProvider;


/**
 * WadScript GUI settings singleton.
 * @author Matthew Tropiano
 */
public final class WadScriptSettingsManager extends DoomToolsSettings
{
	/** Settings filename. */
    private static final String SETTINGS_FILENAME = "wadscript.properties";

    /** The instance encapsulator. */
    private static final SingletonProvider<WadScriptSettingsManager> INSTANCE = new SingletonProvider<>(() -> new WadScriptSettingsManager());
    
	/**
	 * @return the singleton instance of this settings object.
	 */
	public static WadScriptSettingsManager get()
	{
		return INSTANCE.get();
	}
	
	/* ==================================================================== */
	
    private static final String PATH_LAST_FILE = "path.lastFile";
    private static final String TREE_WIDTH = "tree.width";

	/* ==================================================================== */

	private WadScriptSettingsManager()
	{
		super(getConfigFile(SETTINGS_FILENAME), DoomToolsLogger.getLogger(WadScriptSettingsManager.class));
	}
	
	/**
	 * Sets window bounds.
	 * @param window the window to get size from.
	 */
	public void setBounds(Frame window)
	{
		setFrameBounds("default", window.getX(), window.getY(), window.getWidth(), window.getHeight(), (window.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);
		commit();
	}
	
	/**
	 * Gets window bounds.
	 * @return the bounds.
	 */
	public Rectangle getBounds()
	{
		return getFrameBounds("default");
	}
	
	/**
	 * @return if the main DoomTools window should be maximized.
	 */
	public boolean getBoundsMaximized()
	{
		return getFrameMaximized("default");
	}

	/**
	 * Sets the last file opened or saved.
	 * @param path the file.
	 */
	public void setLastTouchedFile(File path) 
	{
		setFile(PATH_LAST_FILE, path);
		commit();
	}

	/**
	 * @return the last file opened or saved.
	 */
	public File getLastTouchedFile() 
	{
		return getFile(PATH_LAST_FILE);
	}

	/**
	 * Sets tree panel width.
	 * @param width the width in pixels.
	 */
	public void setTreeWidth(int width)
	{
		setInteger(TREE_WIDTH, width);
		commit();
	}
	
	/**
	 * Gets tree panel width.
	 * @return the width.
	 */
	public int getTreeWidth()
	{
		return getInteger(TREE_WIDTH, 250);
	}

}
