package net.mtrop.doom.tools.gui.apps;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Rectangle;
import java.io.File;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.mtrop.doom.tools.gui.DoomToolsApplicationInstance;
import net.mtrop.doom.tools.gui.managers.DoomToolsGUIUtils;
import net.mtrop.doom.tools.gui.managers.settings.WTExportSettingsManager;
import net.mtrop.doom.tools.gui.swing.panels.DoomToolsStatusPanel;
import net.mtrop.doom.tools.gui.swing.panels.WTExportParametersPanel;
import net.mtrop.doom.tools.struct.swing.FormFactory.JFormField;
import net.mtrop.doom.tools.struct.swing.SwingUtils;
import net.mtrop.doom.tools.struct.util.ObjectUtils;
import net.mtrop.doom.tools.struct.util.ValueUtils;

import static net.mtrop.doom.tools.struct.swing.ComponentFactory.*;
import static net.mtrop.doom.tools.struct.swing.ContainerFactory.*;
import static net.mtrop.doom.tools.struct.swing.FormFactory.*;
import static net.mtrop.doom.tools.struct.swing.LayoutFactory.*;


/**
 * The WTexScanApp GUI application.
 * @author Matthew Tropiano
 */
public class WTExportApp extends DoomToolsApplicationInstance
{
	private WTExportSettingsManager settings;
	
    /** Input file. */
    private JFormField<File> inputFileField;
    /** Parameter panel. */
    private WTExportParametersPanel parametersPanel;
    /** Status message. */
    private DoomToolsStatusPanel statusPanel;
    
	/**
	 * Creates a new WTExport app instance.
	 */
	public WTExportApp() 
	{
		this.settings = WTExportSettingsManager.get();
		
		this.inputFileField = fileField(
			(current) -> getUtils().chooseFile(
				getApplicationContainer(), 
				getLanguage().getText("wtexport.inputfile.browse.title"), 
				getLanguage().getText("wtexport.inputfile.browse.choose"), 
				settings::getLastTouchedFile, 
				settings::setLastTouchedFile,
				getUtils().getTextFileFilter()
			)
		);

		this.parametersPanel = new WTExportParametersPanel();
		this.statusPanel = new DoomToolsStatusPanel();
	}
	
	@Override
	public String getTitle()
	{
		return getLanguage().getText("wtexport.title");
	}
	
	@Override
	public Map<String, String> getApplicationState()
	{
		Map<String, String> state = super.getApplicationState();

		File inputFile = inputFileField.getValue();
		File[] textureWads = parametersPanel.getTextureWads();
		File baseWad = parametersPanel.getBaseWad();
		File outputWad = parametersPanel.getOutputWad();
		boolean create = parametersPanel.getCreate();
		String nullTexture = parametersPanel.getNullTexture();
		boolean noAnim = parametersPanel.getNoAnimated();
		boolean noSwitch = parametersPanel.getNoSwitches();

		state.put("input", inputFile.getAbsolutePath());
		
		state.put("files.count", String.valueOf(textureWads.length));
		for (int i = 0; i < textureWads.length; i++) 
			state.put("files." + i, textureWads[i].getAbsolutePath());
			
		state.put("basewad", baseWad.getAbsolutePath());
		state.put("outputwad", outputWad.getAbsolutePath());
		state.put("create", String.valueOf(create));
		state.put("nulltexture", nullTexture != null ? nullTexture : "");
		state.put("noanim", String.valueOf(noAnim));
		state.put("noswitch", String.valueOf(noSwitch));
		
		return state;
	}

	@Override
	public void setApplicationState(Map<String, String> state)
	{
		int count = ValueUtils.parseInt(state.get("files.count"), 0);

		File[] files = new File[count];
		
		final Function<String, File> fileParse = (input) -> new File(input);
		
		inputFileField.setValue(ValueUtils.parse(state.get("input"), fileParse));
		
		for (int i = 0; i < files.length; i++) 
			files[i] = ValueUtils.parse(state.get("files." + i), fileParse);

		parametersPanel.setBaseWad(ValueUtils.parse(state.get("basewad"), fileParse));
		parametersPanel.setOutputWad(ValueUtils.parse(state.get("outputwad"), fileParse));
		parametersPanel.setCreate(ValueUtils.parseBoolean(state.get("create"), false));
		parametersPanel.setNullTexture(ValueUtils.parse(state.get("nulltexture"), (input) -> (ObjectUtils.isEmpty(input) ? null : input)));
		parametersPanel.setNoAnimated(ValueUtils.parseBoolean(state.get("noanim"), false));
		parametersPanel.setNoSwitches(ValueUtils.parseBoolean(state.get("noswitch"), false));
	}

	@Override
	public JMenuBar createDesktopMenuBar() 
	{
		DoomToolsGUIUtils utils = getUtils();
		
		return menuBar(
			utils.createMenuFromLanguageKey("wtexport.menu.file",
				utils.createItemFromLanguageKey("wtexport.menu.file.item.exit", (c, e) -> attemptClose())
			)
		);
	}
	
	@Override
	public JMenuBar createInternalMenuBar() 
	{
		return null;
	}

	@Override
	public Container createContentPane()
	{
		return containerOf(dimension(320, 450), borderLayout(0, 4),
			node(BorderLayout.NORTH, getUtils().createForm(form(getLanguage().getInteger("wtexport.label.width")),
				getUtils().formField("wtexport.input", inputFileField)
			)),
			node(BorderLayout.CENTER, parametersPanel),
			node(BorderLayout.SOUTH, containerOf(
				node(BorderLayout.CENTER, statusPanel),
				node(BorderLayout.LINE_END, containerOf(flowLayout(Flow.TRAILING),
					node(getUtils().createButtonFromLanguageKey("wtexport.button.start", (c, e) -> onScanMaps()))
				))
			))
		);
	}

	@Override
	public void onCreate(Object frame) 
	{
		if (frame instanceof JFrame)
		{
			JFrame f = (JFrame)frame;
			Rectangle bounds = settings.getBounds();
			boolean maximized = settings.getBoundsMaximized();
			f.setBounds(bounds);
			if (maximized)
				f.setExtendedState(f.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		}
	}

	@Override
	public void onOpen(Object frame) 
	{
		statusPanel.setSuccessMessage(getLanguage().getText("wtexscan.status.message.ready"));
	}

	@Override
	public void onClose(Object frame) 
	{
		if (frame instanceof JFrame)
		{
			JFrame f = (JFrame)frame;
			settings.setBounds(f);
		}
	}
	
	private void onScanMaps() 
	{
		File inputFile = inputFileField.getValue();
		File[] textureWads = parametersPanel.getTextureWads();
		File baseWad = parametersPanel.getBaseWad();
		File outputWad = parametersPanel.getOutputWad();
		boolean create = parametersPanel.getCreate();
		String nullTexture = parametersPanel.getNullTexture();
		boolean noAnim = parametersPanel.getNoAnimated();
		boolean noSwitch = parametersPanel.getNoSwitches();
		
		if (outputWad.exists() && create)
		{
			if (SwingUtils.noTo(getApplicationContainer(), getLanguage().getText("wtexport.overwrite.message", outputWad.getName())))
				return;
		}
		
		getCommon().onExecuteWTExport(getApplicationContainer(), statusPanel, 
			inputFile,
			textureWads, 
			baseWad, 
			outputWad, 
			create,
			noAnim,
			noSwitch,
			nullTexture
		);	
	}
	
}
