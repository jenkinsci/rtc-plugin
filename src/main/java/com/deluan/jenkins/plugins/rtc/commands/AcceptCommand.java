package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptNewOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptOldOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.BaseAcceptOutputParser;
import hudson.util.ArgumentListBuilder;
import hudson.model.*;
import hudson.FilePath;
import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * @author deluan
 */
public class AcceptCommand extends AbstractCommand implements ParseableCommand<Map<String, JazzChangeSet>> {

    public static final String NEW_FORMAT_VERSION = "2.1.0";
    private Collection<String> changeSets;
    private BaseAcceptOutputParser parser;
    protected boolean oldFormat = false;
	public TaskListener listener;
	private String jazzExecutable = null;

	public AcceptCommand(JazzConfiguration configurationProvider, 
    						Collection<String> changeSets, 
    						String version) {
		this(configurationProvider, changeSets, version, null, null);
    }
	
    public AcceptCommand(JazzConfiguration configurationProvider, 
    						Collection<String> changeSets, 
    						String version, TaskListener listener, String jazzExecutable) {
        super(configurationProvider);
            	
        this.changeSets = new LinkedHashSet<String>(changeSets);
        this.oldFormat = (version.compareTo(NEW_FORMAT_VERSION) < 0);
		this.listener = listener;
		this.jazzExecutable = jazzExecutable;
        parser = (oldFormat) ? new AcceptOldOutputParser() : new AcceptNewOutputParser();
    }
	
	
    public ArgumentListBuilder getArguments() {
		PrintStream output = null;
		if (listener != null) {
			output = listener.getLogger();
		}
		List<ArgumentListBuilder> list = getScmCommands();
		return list.get(0);
	}
	
	public List<ArgumentListBuilder> getScmCommands()
	{
		List<ArgumentListBuilder> result = new ArrayList();
        ArgumentListBuilder args = new ArgumentListBuilder(); 

		// Get load rules.
        String sLoadRules = getLoadRules();
		PrintStream output = null;
		if (listener != null) {
			output = listener.getLogger();
		}  		
		
		if (sLoadRules == null || sLoadRules.isEmpty()) {
			args.add(jazzExecutable);
			args.add("accept");
			addLoginArgument(args);
			
			if (getConfig().isUsingSharedWorkspace() == false) {
				addLocalWorkspaceArgument(args);
			}
			else {
				addSharedWorkspaceArgument(args);
			}

			args.add("--flow-components", "-o", "-v");
			addRepositoryArgument(args);
			result.add(args);
		} else { // Use load rules.
			if (output != null) {
				output.println("     -- Using Load Rules...[");
				output.println(sLoadRules);
				output.println("     ]");
			}
	    	processLoadRules(sLoadRules, result);
		}
        
        return result;
    }

	// Process the load rules.
	private void processLoadRules(String sLoadRules, List<ArgumentListBuilder> list) {
		getConfig().consoleOut("-------------------------------");	
		getConfig().consoleOut("-- process Load Rules - START --");	
		getConfig().consoleOut("-------------------------------");	
		String sUsageString = "Usage: [Component]:[Subfolder Path]";
		
		FilePath file = getConfig().getBuild().getWorkspace();
		
		ArgumentListBuilder args;
		
		// Process load rules if they exist.
		if (sLoadRules != null && sLoadRules.isEmpty() == false) {
			getConfig().consoleOut("sLoadRules: [" + sLoadRules + "]");
			 
			// Split load rules into a string array.
			String[] aLoadRuleLines = sLoadRules.split("\n");
			
			int iLoadRuleLines_len = aLoadRuleLines.length;
			
			String commandData = "";
			///////////////////
			// Loop through the load rule lines...verify and process.
			///////////////////
			for (int iCount = 1;iCount <= iLoadRuleLines_len; iCount++) {
				// Get a line from the array
				String sLine = aLoadRuleLines[iCount-1];
				
				// Verify the sytax is correct.
				// Line must contain a single ":"
				int iColon1 = sLine.indexOf(":");	// This must exist.
				int iColon2 = sLine.indexOf(":",iColon1+1);  // This should not exist.
				
				// Check for validity of load rule line
				if (iColon1 == -1 || iColon2 != -1) {
					// INVALID
					getConfig().consoleOut("   *** Load Rule syntax error ***");
					getConfig().consoleOut("       Line:[" + sLine + "] must contain 1 and only 1 ':' character ***");
					getConfig().consoleOut("       " + sUsageString);
				} else {
					// OK
					// Split line into 2 pieces by the ":"
					String[] RulePieces = sLine.split(":");
					String sComponent = RulePieces[0];
					String sFolder = RulePieces[1];
					
					if(sFolder.startsWith("/")) {
						sFolder = sFolder.substring(1, sFolder.length());
					}
					
					if( getRemoteSeparator().equals("\\") )
					{
						sFolder = sFolder.replace('/', '\\');
					}
				
					getConfig().consoleOut("   Component: [" + sComponent + "]");
					getConfig().consoleOut("   Folder: [" + sFolder + "]");
					
					String sFileName = getConfig().getJobName() + iCount + ".txt";
					String sFileData = "RootFolderName=" + sFolder;
					getConfig().consoleOut("   Writing to file: [" + sFileName + "]");
					getConfig().consoleOut("   Data: [" + sFileData + "]");
					
					try {
						file.act(new LoadCommand.RemoteFileWriter(file.getRemote() + getRemoteSeparator() + sFileName, sFileData));
					} catch (Exception e) {
						e.printStackTrace();
						getConfig().consoleOut("exception: " + e);
						getConfig().consoleOut("Caused by: " + e.getCause());
					}

					file = getConfig().getBuild().getWorkspace();
					args = new ArgumentListBuilder();
					args.add(jazzExecutable);
					args.add("accept");
					addRepositoryArgument(args);
					addLoginArgument(args);
					args.add("-d", file.getRemote() + getRemoteSeparator() + sFolder);
					list.add(args);
				}
			}
		} else {
			getConfig().consoleOut("");	
			getConfig().consoleOut("No load rules found - OK.");	
			getConfig().consoleOut("");	
		}
		
		getConfig().consoleOut("-------------------------------");	
		getConfig().consoleOut("-- process Load Rules - END --");	
		getConfig().consoleOut("-------------------------------");
	}

	
    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        return parser.parse(reader);
    }
}
