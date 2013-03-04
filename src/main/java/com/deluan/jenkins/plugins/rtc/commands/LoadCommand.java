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
        ArgumentListBuilder args = new ArgumentListBuilder();
        
        // Get load rules.
        String sLoadRules = getConfig().getLoadRules();
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
	    } else { // Use load rules.
			if (output != null) {
				output.println("     -- Using Load Rules...[");
				output.println(sLoadRules);
				output.println("     ]");
			}
	    	args = processLoadRules(sLoadRules);
	    }
        return args;
    }

	// Process the load rules.
	public ArgumentListBuilder processLoadRules(String sLoadRules) {
		getConfig().consoleOut("-------------------------------");	
		getConfig().consoleOut("-- process Load Rules - START --");	
		getConfig().consoleOut("-------------------------------");	
		String sUsageString = "Usage: [Component]:[Subfolder Path]";
		
		FilePath file = getConfig().getBuild().getWorkspace();
		
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
				
					getConfig().consoleOut("   Component: [" + sComponent + "]");
					getConfig().consoleOut("   Folder: [" + sFolder + "]");
					
					String sFileName = getConfig().getJobName() + iCount + ".txt";
					String sFileData = "RootFolderName=" + sFolder;
					getConfig().consoleOut("   Writing to file: [" + sFileName + "]");
					getConfig().consoleOut("   Data: [" + sFileData + "]");
					
					try {
						file.act(new RemoteFileWriter(file.getRemote() + "\\" + sFileName, sFileData));
					} catch (Exception e) {
						e.printStackTrace();
						getConfig().consoleOut("exception: " + e);
						getConfig().consoleOut("Caused by: " + e.getCause());
					}

					if(sFolder.startsWith("/")) {
						sFolder = sFolder.substring(1, sFolder.length());
					}
					commandData += jazzExecutable + " load -L " + "\"" + file.getRemote() + "\\" + sFileName + "\" " + getConfig().getWorkspaceName() + " -r " + getConfig().getRepositoryLocation() + " -u %1 -P %2 -d " + "\"" + file.getRemote() + "\\" + sFolder + "\" " + sComponent + "\r\n";
				}
			}
			
			try {
				file.act(new RemoteFileWriter(file.getRemote() + "\\" + getConfig().getJobName() + ".bat", "@echo off\n" + commandData));
			} catch (Exception e) {
				e.printStackTrace();
				getConfig().consoleOut("exception: " + e);
				getConfig().consoleOut("Caused by: " + e.getCause());
			}
					
		} else {
			getConfig().consoleOut("");	
			getConfig().consoleOut("No load rules found - OK.");	
			getConfig().consoleOut("");	
		}
		
		getConfig().consoleOut("-------------------------------");	
		getConfig().consoleOut("-- process Load Rules - END --");	
		getConfig().consoleOut("-------------------------------");
		
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add("cmd");
		args.add("/c");
		args.add("\"" + file.getRemote() + "\\" + getConfig().getJobName() + ".bat\"");
        args.addMasked(getConfig().getUsername());
        args.addMasked(getConfig().getPassword());
		return args;
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

