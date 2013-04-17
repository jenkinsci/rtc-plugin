package com.deluan.jenkins.plugins.rtc.commands;

import hudson.model.TaskListener;
import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import hudson.util.ArgumentListBuilder;
import java.io.*;
import java.util.*;
import hudson.model.*;
import hudson.FilePath;


/**
 * @author deluan
 */
public class LoadCommand extends AbstractCommand {

	public TaskListener listener;
	private String jazzExecutable = null;
	
    public LoadCommand(JazzConfiguration configurationProvider, TaskListener listener, String jazzExecutable) {
        super(configurationProvider);
        
        this.listener = listener;
		this.jazzExecutable = jazzExecutable;
    }

    public ArgumentListBuilder getArguments() {
		return(new ArgumentListBuilder());
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
        
        // check to see if load rules are specified.
        if (sLoadRules == null || sLoadRules.isEmpty()) {
			if (output != null) {
				output.println("     -- No load rules specified - ok -- ");
			}
			args.add(jazzExecutable);
	        args.add("load", getConfig().getWorkspaceName());
	        addLoginArgument(args);
	        addRepositoryArgument(args);
	        addLocalWorkspaceArgument(args);
	    	args.add("-f");
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
						file.act(new RemoteFileWriter(file.getRemote() + getRemoteSeparator() + sFileName, sFileData));
					} catch (Exception e) {
						e.printStackTrace();
						getConfig().consoleOut("exception: " + e);
						getConfig().consoleOut("Caused by: " + e.getCause());
					}

					file = getConfig().getBuild().getWorkspace();
					args = new ArgumentListBuilder();
					args.add(jazzExecutable);
					args.add("load", "-L", file.getRemote() + getRemoteSeparator() + sFileName);
					args.add(getConfig().getWorkspaceName());
					addRepositoryArgument(args);
					addLoginArgument(args);
					args.add("-d", file.getRemote() + getRemoteSeparator() + sFolder);
					args.add(sComponent);
					args.add("-f");
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
	
	public static class RemoteFileWriter implements FilePath.FileCallable<Void>, Serializable {
		String fileName = null;
		String data = null;
		
		public RemoteFileWriter(String fileName, String data) {
			this.fileName = fileName;
			this.data = data;
		}
		public Void invoke(File f, hudson.remoting.VirtualChannel channel) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(fileName);
				fos.write(data.getBytes(), 0, data.length());
				fos.close();
			} catch (Exception e) {
				try {
					if (fos != null) {
						fos.close();
					}
				} catch (Exception ee) {}
			}
			return null;
		}
	}
} //end - class

