/*******************************************************************************
 * Copyright (c) 2019-2022 Black Rook Software
 * This program and the accompanying materials are made available under 
 * the terms of the MIT License, which accompanies this distribution.
 ******************************************************************************/
package net.mtrop.doom.tools.struct.swing;

import java.awt.Component;
import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * A factory that creates file chooser dialogs.
 * @author Matthew Tropiano
 */
public final class FileChooserFactory
{
	private static final BiFunction<FileFilter, File, File> NO_CHANGE_TRANSFORM = (x0, file) -> file;
	
	/**
	 * Opens a directory chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param title the dialog title.
	 * @param initPath the initial path for the directory chooser.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected directory, or null if no directory was selected for whatever reason.
	 */
	public static File chooseDirectory(Component parent, String title, File initPath, String approveText, FileFilter ... choosableFilters)
	{
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (initPath != null)
			jfc.setSelectedFile(initPath);
		if (title != null)
			jfc.setDialogTitle(title);
		jfc.resetChoosableFileFilters();
		for (FileFilter filter : choosableFilters)
		{
			jfc.addChoosableFileFilter(filter);
			jfc.setFileFilter(filter);
		}
		switch (jfc.showDialog(parent, approveText))
		{
			default:
			case JFileChooser.CANCEL_OPTION: 
			case JFileChooser.ERROR_OPTION:
				return null;
			case JFileChooser.APPROVE_OPTION:
				return jfc.getSelectedFile();
		}
	}

	/**
	 * Opens a directory chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param title the dialog title.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected directory, or null if no directory was selected for whatever reason.
	 */
	public static File chooseDirectory(Component parent, String title, String approveText, FileFilter ... choosableFilters)
	{
		return chooseDirectory(parent, title, null, approveText, choosableFilters);
	}

	/**
	 * Opens a directory chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param initPath the initial path for the directory chooser.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected directory, or null if no directory was selected for whatever reason.
	 */
	public static File chooseDirectory(Component parent, File initPath, String approveText, FileFilter ... choosableFilters)
	{
		return chooseDirectory(parent, null, initPath, approveText, choosableFilters);
	}

	/**
	 * Opens a directory chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected directory, or null if no directory was selected for whatever reason.
	 */
	public static File chooseDirectory(Component parent, String approveText, FileFilter ... choosableFilters)
	{
		return chooseDirectory(parent, null, null, approveText, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param title the dialog title.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, String title, File initPath, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (initPath != null)
			jfc.setSelectedFile(initPath);
		if (title != null)
			jfc.setDialogTitle(title);
		jfc.resetChoosableFileFilters();
		for (FileFilter filter : choosableFilters)
		{
			jfc.addChoosableFileFilter(filter);
			jfc.setFileFilter(filter);
		}
		switch (jfc.showDialog(parent, approveText))
		{
			default:
			case JFileChooser.CANCEL_OPTION: 
			case JFileChooser.ERROR_OPTION:
				return null;
			case JFileChooser.APPROVE_OPTION:
				return transformFileFunction.apply(jfc.getFileFilter(), jfc.getSelectedFile());
		}
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param title the dialog title.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, String title, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, title, null, approveText, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, File initPath, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, null, initPath, approveText, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, null, null, approveText, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param title the dialog title.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(String title, File initPath, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(null, title, initPath, approveText, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param title the dialog title.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(String title, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(null, title, null, approveText, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param title the dialog title.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(String title, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(null, title, null, null, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(File initPath, String approveText, BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(null, null, initPath, approveText, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param transformFileFunction if a file is selected, use this function to set the final file name.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(BiFunction<FileFilter, File, File> transformFileFunction, FileFilter ... choosableFilters)
	{
		return chooseFile(null, null, null, null, transformFileFunction, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param title the dialog title.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, String title, File initPath, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, title, null, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param title the dialog title.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, String title, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, title, null, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, File initPath, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, null, initPath, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param parent the parent component for the chooser modal.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(Component parent, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(parent, null, null, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param title the dialog title.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(String title, File initPath, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(null, title, initPath, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param title the dialog title.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(String title, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(null, title, null, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param title the dialog title.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(String title, FileFilter ... choosableFilters)
	{
		return chooseFile(null, title, null, null, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param initPath the initial path for the file chooser.
	 * @param approveText the text to put on the approval button.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(File initPath, String approveText, FileFilter ... choosableFilters)
	{
		return chooseFile(null, null, initPath, approveText, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Opens a file chooser dialog.
	 * @param choosableFilters the choosable filters.
	 * @return the selected file, or null if no file was selected for whatever reason.
	 */
	public static File chooseFile(FileFilter ... choosableFilters)
	{
		return chooseFile(null, null, null, null, NO_CHANGE_TRANSFORM, choosableFilters);
	}

	/**
	 * Creates a file filter for file dialogs.
	 * @param description the description of the filter.
	 * @param filePredicate the qualifying predicate.
	 * @return the new filter.
	 */
	public static FileFilter fileFilter(final String description, final Predicate<File> filePredicate)
	{
		return new FileFilter() 
		{
			@Override
			public String getDescription() 
			{
				return description;
			}
			
			@Override
			public boolean accept(File f) 
			{
				return filePredicate.test(f);
			}
		};
	}

	/**
	 * Creates a file extension filter for file dialogs.
	 * @param description the description of the filter.
	 * @param extension the qualifying extension.
	 * @return the new filter.
	 */
	public static FileFilter fileExtensionFilter(final String description, final String extension)
	{
		return fileFilter(description, (file) -> {
			if (file.isDirectory())
				return true;
			String filename = file.getName();
			int lastIndexOf = filename.indexOf('.');
			return lastIndexOf >= 0 && filename.substring(lastIndexOf + 1).equalsIgnoreCase(extension);
		});
	}

	/**
	 * Creates a directory filter for file dialogs.
	 * @param description the description of the filter.
	 * @return the new filter.
	 */
	public static FileFilter fileDirectoryFilter(final String description)
	{
		return fileFilter(description, (file) -> file.isDirectory());
	}

}
