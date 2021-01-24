package net.mtrop.doom.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.mtrop.doom.tools.WadScriptMain.Mode;
import net.mtrop.doom.tools.common.Common;
import net.mtrop.doom.tools.doommake.DoomMakeFunctions;
import net.mtrop.doom.tools.doommake.ProjectTemplate;
import net.mtrop.doom.tools.doommake.ToolInvocationFunctions;
import net.mtrop.doom.tools.exception.OptionParseException;

import static net.mtrop.doom.tools.doommake.ProjectTemplate.create;
import static net.mtrop.doom.tools.doommake.ProjectTemplate.file;
import static net.mtrop.doom.tools.doommake.ProjectTemplate.dir;
import static net.mtrop.doom.tools.doommake.ProjectTemplate.fileAppend;

/**
 * Main class for DoomMake.
 * @author Matthew Tropiano
 */
public final class DoomMakeMain 
{
	private static final String DOOM_VERSION = Common.getVersionString("doom");
	private static final String ROOKSCRIPT_VERSION = Common.getVersionString("rookscript");
	private static final String WADSCRIPT_VERSION = Common.getVersionString("wadscript");
	private static final String VERSION = Common.getVersionString("doommake");

	private static final int ERROR_NONE = 0;
	private static final int ERROR_BAD_OPTIONS = 1;
	private static final int ERROR_BAD_PROPERTIES = 2;
	private static final int ERROR_BAD_SCRIPT = 3;
	private static final int ERROR_BAD_TEMPLATE = 4;
	private static final int ERROR_BAD_PROJECT = 5;
	private static final int ERROR_UNKNOWN = 100;

	private static final String SWITCH_HELP = "--help";
	private static final String SWITCH_HELP2 = "-h";
	private static final String SWITCH_VERSION = "--version";
	private static final String SWITCH_FUNCHELP1 = "--function-help";
	private static final String SWITCH_FUNCHELP2 = "--function-help-markdown";
	private static final String SWITCH_LISTTEMPLATES = "--list-templates";
	private static final String SWITCH_NEWPROJECT = "--new-project";
	private static final String SWITCH_NEWPROJECT2 = "-n";
	
	private static final String SWITCH_SCRIPTFILE = "--script";
	private static final String SWITCH_SCRIPTFILE2 = "-s";
	private static final String SWITCH_PROPERTYFILE = "--properties";
	private static final String SWITCH_PROPERTYFILE2 = "-p";
	private static final String SWITCH_RUNAWAYLIMIT1 = "--runaway-limit";
	private static final String SWITCH_ACTIVATIONDEPTH1 = "--activation-depth";
	private static final String SWITCH_STACKDEPTH1 = "--stack-depth";

	/** The templates. */
	private static final Map<String, ProjectTemplate> TEMPLATES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/**
	 * Program options.
	 */
	public static class Options
	{
		private PrintStream stdout;
		private PrintStream stderr;
		private InputStream stdin;
		private boolean help;
		private boolean version;
		private boolean listTemplates;

		private List<String> createTemplates;
		
		private Mode mode;
		private Integer runawayLimit;
		private Integer activationDepth;
		private Integer stackDepth;

		private String targetName;

		private File propertiesFile;
		private File scriptFile;
		private List<String> args;
		
		private Options()
		{
			this.stdout = null;
			this.stderr = null;
			this.stdin = null;
			this.help = false;
			this.version = false;
			this.listTemplates = false;
			this.createTemplates = null;

			this.mode = Mode.EXECUTE;
			this.runawayLimit = 0;
			this.activationDepth = 256;
			this.stackDepth = 2048;
			this.propertiesFile = new File("doommake.properties");
			this.scriptFile = new File("doommake.script");
			this.targetName = "make";
			this.args = new LinkedList<>();
		}
		
		public Options setStdout(OutputStream out) 
		{
			this.stdout = new PrintStream(out, true);;
			return this;
		}
		
		public Options setStderr(OutputStream err) 
		{
			this.stderr = new PrintStream(err, true);
			return this;
		}
		
		public Options setStdin(InputStream stdin) 
		{
			this.stdin = stdin;
			return this;
		}

		public Options setRunawayLimit(Integer runawayLimit)
		{
			this.runawayLimit = runawayLimit;
			return this;
		}
		
		public Options setActivationDepth(Integer activationDepth)
		{
			this.activationDepth = activationDepth;
			return this;
		}
		
		public Options setStackDepth(Integer stackDepth)
		{
			this.stackDepth = stackDepth;
			return this;
		}

		public Options setTargetName(String targetName) 
		{
			this.targetName = targetName;
			return this;
		}
		
		public Options setPropertiesFile(File propertiesFile) 
		{
			this.propertiesFile = propertiesFile;
			return this;
		}
		
		public Options setScriptFile(File scriptFile) 
		{
			this.scriptFile = scriptFile;
			return this;
		}
		
		public Options setArgs(String[] args)
		{
			this.args.clear();
			for (String s : args)
				this.args.add(s);
			return this;
		}

		public Options addArg(String arg)
		{
			this.args.add(arg);
			return this;
		}
		
	}
	
	/**
	 * Program context.
	 */
	private static class Context
	{
		private Options options;
	
		private Context(Options options)
		{
			this.options = options;
		}
		
		public int call()
		{
			if (options.help)
			{
				splash(options.stdout);
				usage(options.stdout);
				options.stdout.println();
				help(options.stdout);
				return ERROR_NONE;
			}
			
			if (options.version)
			{
				splash(options.stdout);
				return ERROR_NONE;
			}

			if (options.listTemplates)
			{
				setUpTemplates();
				for (Map.Entry<String, ProjectTemplate> entry : TEMPLATES.entrySet())
				{
					String description = entry.getValue().getDescription();
					options.stdout.println(entry.getKey());
					Common.printWrapped(options.stdout, 0, 4, 76, Common.isEmpty(description) ? "" : description);
					options.stdout.println();
				}
				return ERROR_NONE;
			}

			if (options.createTemplates != null)
			{
				if (Common.isEmpty(options.targetName))
				{
					options.stderr.println("ERROR: No directory path.");
					return ERROR_BAD_PROJECT;
				}
				
				File targetDirectory = new File(options.targetName);
				
				if (targetDirectory.exists() && targetDirectory.listFiles().length > 0)
				{
					options.stderr.println("ERROR: Target directory contains files. Project creation aborted.");
					return ERROR_BAD_PROJECT;
				}
				
				setUpTemplates();
				ProjectTemplate template = create();
				for (String name : options.createTemplates)
				{
					ProjectTemplate found;
					if ((found = TEMPLATES.get(name)) == null)
					{
						options.stderr.println("ERROR: No such project template: " + name);
						options.stderr.println("Try running DoomMake with `--list-templates` for the list of available templates.");
						return ERROR_BAD_TEMPLATE;
					}
					template.add(found);
				}
				
				try {
					template.createIn(new File(options.targetName));
					options.stdout.println("Created project: " + options.targetName);
				} catch (IOException e) {
					options.stderr.println("ERROR: " + e.getLocalizedMessage());
				}
				
				return ERROR_NONE;
			}

			if (options.mode == Mode.EXECUTE && !options.scriptFile.exists())
			{
				options.stderr.printf("ERROR: Script file \"%s\" could not be found!\n", options.scriptFile.getPath());
				return ERROR_BAD_SCRIPT;
			}
			
			if (options.propertiesFile.exists())
			{
				try (Reader reader = new InputStreamReader(new FileInputStream(options.propertiesFile)))
				{
					System.getProperties().load(reader);
				} 
				catch (IOException e) 
				{
					options.stderr.printf("ERROR: Properties file \"%s\" could not be loaded: %s\n", options.propertiesFile.getPath(), e.getLocalizedMessage());
					return ERROR_BAD_PROPERTIES;
				}
			}
			
			try {
				WadScriptMain.Options wsOptions = WadScriptMain.options(options.stdout, options.stderr, options.stdin)
					.setMode(options.mode)
					.setEntryPointName(options.targetName)
					.setStackDepth(options.stackDepth)
					.setActivationDepth(options.activationDepth)
					.setRunawayLimit(options.runawayLimit)
					.setScriptFile(options.scriptFile)
					.addResolver("DoomMake Functions", DoomMakeFunctions.createResolver())
					.addResolver("Tool Invocation", "TOOL", ToolInvocationFunctions.createResolver())
				;
				for (Object obj : options.args)
					wsOptions.addArg(obj);
				return WadScriptMain.call(wsOptions);
			} catch (OptionParseException e) {
				/** Will not be thrown. */
				return ERROR_UNKNOWN;
			}
		}
	}
	
	/**
	 * Reads command line arguments and sets options.
	 * @param out the standard output print stream.
	 * @param err the standard error print stream.
	 * @param in the standard in print stream. 
	 * @param args the argument args.
	 * @return the parsed options.
	 * @throws OptionParseException if a parse exception occurs.
	 */
	public static Options options(PrintStream out, PrintStream err, InputStream in, String ... args) throws OptionParseException
	{
		Options options = new Options();
		options.stdout = out;
		options.stderr = err;
		options.stdin = in;
	
		final int STATE_START = 0;
		final int STATE_SCRIPTFILE = 1;
		final int STATE_PROPERTYFILE = 2;
		final int STATE_SWITCHES_ACTIVATION = 3;
		final int STATE_SWITCHES_STACK = 4;
		final int STATE_SWITCHES_RUNAWAY = 5;
		final int STATE_TEMPLATENAME = 6;
		int state = STATE_START;
		
		boolean target = false;
		
		int i = 0;
		while (i < args.length)
		{
			String arg = args[i];
			switch (state)
			{
				case STATE_START:
				{
					if (arg.equalsIgnoreCase(SWITCH_HELP) || arg.equalsIgnoreCase(SWITCH_HELP2))
						options.help = true;
					else if (arg.equalsIgnoreCase(SWITCH_VERSION))
						options.version = true;
					else if (arg.equalsIgnoreCase(SWITCH_LISTTEMPLATES))
						options.listTemplates = true;
					else if (arg.equalsIgnoreCase(SWITCH_NEWPROJECT) || arg.equalsIgnoreCase(SWITCH_NEWPROJECT2))
					{
						options.createTemplates = new LinkedList<>();
						state = STATE_TEMPLATENAME;
					}
					else if (SWITCH_FUNCHELP1.equalsIgnoreCase(arg))
						options.mode = Mode.FUNCTIONHELP;
					else if (SWITCH_FUNCHELP2.equalsIgnoreCase(arg))
						options.mode = Mode.FUNCTIONHELP_MARKDOWN;
					else if (SWITCH_SCRIPTFILE.equalsIgnoreCase(arg) || SWITCH_SCRIPTFILE2.equalsIgnoreCase(arg))
						state = STATE_SCRIPTFILE;
					else if (SWITCH_PROPERTYFILE.equalsIgnoreCase(arg) || SWITCH_PROPERTYFILE2.equalsIgnoreCase(arg))
						state = STATE_PROPERTYFILE;
					else if (SWITCH_RUNAWAYLIMIT1.equalsIgnoreCase(arg))
						state = STATE_SWITCHES_RUNAWAY;
					else if (SWITCH_ACTIVATIONDEPTH1.equalsIgnoreCase(arg))
						state = STATE_SWITCHES_ACTIVATION;
					else if (SWITCH_STACKDEPTH1.equalsIgnoreCase(arg))
						state = STATE_SWITCHES_STACK;
					else if (target)
						options.args.add(arg);
					else
					{
						options.targetName = arg;
						target = true;
					}
				}
				break;
				
				case STATE_SCRIPTFILE:
				{
					options.scriptFile = new File(arg);
					state = STATE_START;
				}
				break;
				
				case STATE_PROPERTYFILE:
				{
					options.propertiesFile = new File(arg);
					state = STATE_START;
				}
				break;
				
				case STATE_TEMPLATENAME:
				{
					options.createTemplates.add(arg);
				}
				break;
				
				case STATE_SWITCHES_RUNAWAY:
				{
					int n;
					try {
						n = Integer.parseInt(arg);
						options.runawayLimit = n > 0 ? n : null;
					} catch (NumberFormatException e) {
						options.runawayLimit = null;
						throw new OptionParseException("Runaway limit needs to be a number greater than 0.");
					}
					state = STATE_START;
				}
				break;
				
				case STATE_SWITCHES_ACTIVATION:
				{
					int n;
					try {
						n = Integer.parseInt(arg);
						options.activationDepth = n > 0 ? n : null;
					} catch (NumberFormatException e) {
						options.activationDepth = null;
						throw new OptionParseException("Activation depth needs to be a number greater than 0.");
					}
					state = STATE_START;
				}
				break;
				
				case STATE_SWITCHES_STACK:
				{
					int n;
					try {
						n = Integer.parseInt(arg);
						options.stackDepth = n > 0 ? n : null;
					} catch (NumberFormatException e) {
						options.stackDepth = null;
						throw new OptionParseException("Stack depth needs to be a number greater than 0.");
					}
					state = STATE_START;
				}
				break;
				
			}
			i++;
		}
		
		if (state == STATE_SCRIPTFILE)
			throw new OptionParseException("ERROR: Expected script file after switch.");
		if (state == STATE_PROPERTYFILE)
			throw new OptionParseException("ERROR: Expected properties file after switch.");
		if (state == STATE_SWITCHES_ACTIVATION)
			throw new OptionParseException("ERROR: Expected number after activation depth switch.");
		if (state == STATE_SWITCHES_STACK)
			throw new OptionParseException("ERROR: Expected number after stack depth switch.");
		if (state == STATE_SWITCHES_RUNAWAY)
			throw new OptionParseException("ERROR: Expected number after runaway limit switch.");
		
		return options;
	}
	
	/**
	 * Calls the utility using a set of options.
	 * @param options the options to call with.
	 * @return the error code.
	 */
	public static int call(Options options)
	{
		return (new Context(options)).call();
	}
	
	public static void main(String[] args) throws IOException
	{
		try {
			Options options = options(System.out, System.err, System.in, args);
			int status = call(options);
			if (status != 0 && args.length == 0 && !options.scriptFile.exists())
			{
				splash(System.out);
				usage(System.out);
				System.exit(-1);
				return;
			}
			System.exit(status);
		} catch (OptionParseException e) {
			System.err.println(e.getMessage());
			System.exit(ERROR_BAD_OPTIONS);
		}
	}
	
	/**
	 * Prints the splash.
	 * @param out the print stream to print to.
	 */
	private static void splash(PrintStream out)
	{
		out.println("DoomMake v" + VERSION + " by Matt Tropiano");
		out.println("(using DoomStruct v" + DOOM_VERSION + ", RookScript v" + ROOKSCRIPT_VERSION + ", WadScript v" + WADSCRIPT_VERSION + ")");
	}

	/**
	 * Prints the usage.
	 * @param out the print stream to print to.
	 */
	private static void usage(PrintStream out)
	{
		out.println("Usage: doommake [target] [args] [switches]");
		out.println("                [--help | -h | --version]");
		out.println("                [--function-help | --function-help-markdown]");
		out.println("                --list-templates");
		out.println("                [directory] --new-project [templates]");
	}
	
	/**
	 * Prints the help.
	 * @param out the print stream to print to.
	 */
	private static void help(PrintStream out)
	{
		out.println("[target]:");
		out.println("    The target entry point to execute. Default is \"make\".");
		out.println();
		out.println("[args]:");
		out.println("    The additional arguments to pass along to the script.");
		out.println();
		out.println("[templates]:");
		out.println("    The names of the template(s) to use or combine for the new ");
		out.println("    project (applied altogether).");
		out.println("    E.g.: --new-project simple-maps git");
		out.println("        Combines the \"simple-maps\", and \"git\" templates together,");
		out.println("        in that order.");
		out.println();
		out.println("[directory]:");
		out.println("    The directory for the new project.");
		out.println();
		out.println("[switches]:");
		out.println("    --help, -h                    Prints this help.");
		out.println("    --version                     Prints the version of this utility.");
		out.println("    --function-help               Prints all available function usages.");
		out.println("    --function-help-markdown      Prints all available function usages in");
		out.println("                                      Markdown format.");
		out.println();
		out.println("    --new-project, -n [templates] Creates a new project made up of a set of");
		out.println("                                      templates (requires [directory]).");
		out.println();
		out.println("    --script, -s [filename]       Use [filename] for the root build script.");
		out.println("                                      Default is \"doommake.script\".");
		out.println("    --properties, -p [filename]   Use [filename] for the project properties.");
		out.println("                                      Default is \"doommake.properties\".");
		out.println();
		out.println("    --runaway-limit [num]         Sets the runaway limit (in operations)");
		out.println("                                      before the soft protection on infinite");
		out.println("                                      loops triggers. 0 is no limit.");
		out.println("                                      Default: 0");
		out.println("    --activation-depth [num]      Sets the activation depth to [num].");
		out.println("                                      Default: 256");
		out.println("    --stack-depth [num]           Sets the stack value depth to [num].");
		out.println("                                      Default: 2048");
	}

	/**
	 * Sets up the used templates (for lazy init).
	 */
	private static void setUpTemplates()
	{
		final ProjectTemplate GIT = create(
			"Git repository support.",
			file(".gitignore", 
				"doommake/git/gitignore.txt"),
			file(".gitattributes", 
				"doommake/git/gitattributes.txt")
		);
		final ProjectTemplate COMMON = create(
			"Common WAD info.",
			file("src/wadinfo.txt", 
				"doommake/common/wadinfo.txt"),
			file("src/credits.txt", 
				"doommake/common/credits.txt")
		);
		final ProjectTemplate COMMON_INIT = create(
			"Common blank init.",
			fileAppend("doommake.script", 
				"doommake/common/init/doommake.script")
		);
		final ProjectTemplate COMMON_MAKE = create(
			"Common make scripts.",
			file("doommake.script", 
				"doommake/doommake.script"),
			file("doommake.properties", 
				"doommake/doommake.properties"),
			file("README.md",
				"doommake/README.md")
		);
		final ProjectTemplate COMMON_RELEASE_STUB = create(
			"Common release stubbing.",
			dir("dist"),
			fileAppend("doommake.properties", 
				"doommake/common/release/doommake.properties"),
			fileAppend("doommake.script", 
				"doommake/common/release/doommake.script"),
			fileAppend("README.md",
				"doommake/common/release/README.md")
		);
		final ProjectTemplate COMMON_MAPS = create(
			"Common map project structure.",
			dir("src/maps"),
			fileAppend("doommake.properties", 
				"doommake/common/maps/doommake.properties"),
			fileAppend("doommake.script", 
				"doommake/common/maps/doommake.script"),
			fileAppend("README.md",
				"doommake/common/maps/README.md")
		);
		final ProjectTemplate COMMON_ASSETS = create(
			"Common asset project structure.",
			dir("src/assets/_global"),
			dir("src/assets/graphics"),
			dir("src/assets/music"),
			dir("src/assets/sounds"),
			dir("src/assets/sprites"),
			file("scripts/merge-assets.txt",
				"doommake/common/assets/wadmerge.txt"),
			fileAppend("doommake.properties", 
				"doommake/common/assets/doommake.properties"),
			fileAppend("doommake.script", 
				"doommake/common/assets/doommake.script"),
			fileAppend("README.md",
				"doommake/common/assets/README.md")
		);
		final ProjectTemplate COMMON_IWAD = create(
			"Common IWAD dependency.",
			fileAppend("doommake.properties", 
				"doommake/common/iwad/doommake.properties"),
			fileAppend("doommake.script", 
				"doommake/common/iwad/doommake.script")
		);
		final ProjectTemplate COMMON_TEXTURES = create(
			"Common texture WAD dependency.",
			dir("src/textures/flats"),
			dir("src/textures/patches"),
			file("scripts/merge-textures.txt",
				"doommake/common/textures/wadmerge.txt"),
			file("src/textures/texture1.txt", 
				"doommake/common/textures/texture1.txt"),
			file("src/textures/texture2.txt", 
				"doommake/common/textures/texture2.txt"),
			fileAppend("doommake.properties", 
				"doommake/common/textures/doommake.properties"),
			fileAppend("doommake.script", 
				"doommake/common/textures/doommake.script"),
			fileAppend("README.md",
				"doommake/common/textures/README.md")
		);
		final ProjectTemplate COMMON_TEXTURE_WAD = create(
			"Common texture WAD dependency.",
			dir("src/wads/textures"),
			fileAppend("doommake.properties", 
				"doommake/common/texwad/doommake.properties"),
			fileAppend("doommake.script", 
				"doommake/common/texwad/doommake.script"),
			fileAppend("README.md",
				"doommake/common/texwad/README.md")
		);

		TEMPLATES.put("git",
			create("Adds files for Git repository support (ignores, attributes).")
				.add(GIT)
			);
		TEMPLATES.put("maps",
			create("A project that builds just a set of maps.")
				.add(COMMON)
				.add(COMMON_MAKE)
				.add(COMMON_INIT)
				.add(COMMON_RELEASE_STUB)
				.add(COMMON_MAPS)
				.add(create(
					file("scripts/merge-release.txt", 
						"doommake/projects/maps/wadmerge.txt"),
					fileAppend("doommake.script", 
						"doommake/projects/maps/doommake.script")
				))
			);
		TEMPLATES.put("maps-assets",
			create("A project that builds maps and non-texture assets together.")
				.add(COMMON)
				.add(COMMON_MAKE)
				.add(COMMON_INIT)
				.add(COMMON_RELEASE_STUB)
				.add(COMMON_MAPS)
				.add(COMMON_ASSETS)
				.add(create(
					file("scripts/merge-release.txt", 
						"doommake/projects/assets/wadmerge.txt"),
					fileAppend("doommake.script", 
						"doommake/projects/assets/doommake.script")
				))
			);
		TEMPLATES.put("maps-texwad",
			create("A project that builds maps and uses textures from a set of texture WADs.")
				.add(COMMON)
				.add(COMMON_MAKE)
				.add(COMMON_IWAD)
				.add(COMMON_INIT)
				.add(COMMON_RELEASE_STUB)
				.add(COMMON_MAPS)
				.add(COMMON_TEXTURE_WAD)
				.add(create(
					file("scripts/merge-release.txt", 
						"doommake/projects/texmaps/wadmerge.txt"),
					fileAppend("doommake.script", 
						"doommake/projects/texmaps/doommake.script")
				))
			);
		TEMPLATES.put("assets-maps-texwad",
			create("A project that builds maps, non-texture assets, and uses textures from a set of texture WADs.")
				.add(COMMON)
				.add(COMMON_MAKE)
				.add(COMMON_IWAD)
				.add(COMMON_INIT)
				.add(COMMON_RELEASE_STUB)
				.add(COMMON_MAPS)
				.add(COMMON_ASSETS)
				.add(COMMON_TEXTURE_WAD)
				.add(create(
					file("scripts/merge-release.txt", 
						"doommake/projects/assetmaptex/wadmerge.txt"),
					fileAppend("doommake.script", 
						"doommake/projects/assetmaptex/doommake.script")
				))
			);
	}

}
