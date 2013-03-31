package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.*;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.model.*;
import org.kohsuke.stapler.framework.io.WriterOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the invocation of RTC's SCM Command Line Interface, "scm".
 *
 * @author deluan
 */
@SuppressWarnings("JavaDoc")
/****************************************************

	public class JazzClient 
	
****************************************************/
public class JazzClient 
{
    public static final String SCM_CMD = "scm";

    private JazzConfiguration configuration = new JazzConfiguration();
    private final Launcher launcher;
    private final TaskListener listener;
    private String jazzExecutable;

	/****************************************************
	
		public JazzClient(Launcher launcher, TaskListener listener, FilePath jobWorkspace, String jazzExecutable,
                      JazzConfiguration configuration)
		
	****************************************************/
    public JazzClient(Launcher launcher, TaskListener listener, FilePath jobWorkspace, String jazzExecutable,
                      JazzConfiguration configuration) {
        this.jazzExecutable = jazzExecutable;
        this.launcher = launcher;
        this.listener = listener;
        this.configuration = new JazzConfiguration(configuration);
        this.configuration.setJobWorkspace(jobWorkspace);
        this.configuration.setTaskListener(listener);
        
    }

	/****************************************************
	
		public boolean hasChanges() throws IOException, InterruptedException {
		
     * Returns true if there is any incoming changes to be accepted.
     *
     * @return <tt>true</tt> if any changes are found
     * @throws IOException
     * @throws InterruptedException
	****************************************************/
    public boolean hasChanges() throws IOException, InterruptedException {
        Map<String, JazzChangeSet> changes = compare();
		
		PrintStream output = listener.getLogger();
    	output.println(changes.values().size() + " change sets.");
		
        return !changes.isEmpty();
    }

	/****************************************************
     public boolean load() throws IOException, InterruptedException {
     
     * Will load the workspace using the parameters defined.
     *
     * @return <tt>true</tt> on success
     * @throws IOException
     * @throws InterruptedException
	****************************************************/
    public boolean load() throws IOException, InterruptedException {
		//output to console.
		PrintStream output = listener.getLogger();
    	output.println("  RTC SCM - Jazz Client: Loading Workspace.");
    	
        Command cmd = new LoadCommand(configuration, listener, jazzExecutable);

        return joinWithPossibleTimeout(run(cmd.getArguments(), null), listener, null) == 0;
    }
	
	/****************************************************
	
		public boolean workspaceExists() throws IOException, InterruptedException {
		
     * Call <tt>scm history</tt> command. <p/>
     * <p/>
     * Will check if the workspace exists.
     *
     * @return <tt>true</tt> on exists
     * @throws IOException
     * @throws InterruptedException
	****************************************************/
    public boolean workspaceExists(AbstractBuild build) throws IOException, InterruptedException {
		//output to console.
		PrintStream output = listener.getLogger();
        Command cmd = new HistoryCommand(configuration);
        output.println("  RTC SCM - Jazz Client: Run History command to determine if workspace exists - it is OK if an error is returned below: (Problem running 'history')");
				
		//Get variables from system.
		String jobName = "";
		
		try {
	        jobName = build.getEnvironment(null).get("JOB_NAME");
		} catch (Exception e) {
			listener.error("" + e);
		}
		// Add the abstract build to the configuration.		
		// This call happens before the load and accept so we can set these items for later use.
		configuration.setBuild(build);
		configuration.setTaskListener(listener);
		configuration.setJobName(jobName);
		configuration.consoleOut("    -- Initializing build object --");
						        
		StringBuffer strBuf = new StringBuffer();
		joinWithPossibleTimeout(run(cmd.getArguments()), listener, strBuf, build, null);
		boolean result = true;
		String stdOut = strBuf.toString();
		
		if (stdOut.contains("did not match any workspaces") || stdOut.contains("Unmatched workspace")) {
			listener.error("The workspace probably doesn't exist.");

			output.println("  RTC SCM - Jazz Client: Specified workspace does not exist...");

			result = false;
		}
		else
		{
			output.println("  RTC SCM - Jazz Client: Specified workspace already exists...");			
		}
        return result;
    }
	
	/****************************************************
		
		public boolean createWorkspace() throws IOException, InterruptedException {
		
     * Call <tt>scm create workspace</tt> command. <p/>
     * <p/>
     * Create the workspace.
     *
     * @return <tt>true</tt> on success
     * @throws IOException
     * @throws InterruptedException
	****************************************************/
    public boolean createWorkspace() throws IOException, InterruptedException {
		//output to console.
		PrintStream output = listener.getLogger();
		output.println("  RTC SCM - Jazz Client: Creating Workspace...");

        Command cmd = new CreateWorkspaceCommand(configuration);
		boolean result = joinWithPossibleTimeout(run(cmd.getArguments()), listener, null) == 0;
        return result;
    }

	/****************************************************
	
		private String getVersion() throws IOException, InterruptedException {
	
     * Call <tt>scm daemon stop</tt> command. <p/>
     * <p/>
     * Will try to stop any daemon associated with the workspace.
     * <p/>
     * This will be executed with the <tt>scm</tt> command, as the <tt>lscm</tt> command
     * does not support this operation.
     *
     * @return <tt>true</tt> on success
     * @throws IOException
     * @throws InterruptedException
	****************************************************/
    public boolean stopDaemon() throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder(SCM_CMD);

        args.add(new StopDaemonCommand(configuration).getArguments().toCommandArray());

        return (joinWithPossibleTimeout(l(args), listener, null) == 0);
    }

	/****************************************************
	
		public List<JazzChangeSet> accept() throws IOException, InterruptedException {
	
     * Call <tt>scm accept</tt> command.<p/>
     *
     * @return all changeSets accepted, complete with affected paths and related work items
     * @throws IOException
     * @throws InterruptedException
	****************************************************/
    public List<JazzChangeSet> accept() throws IOException, InterruptedException {
 		//output to console.
		PrintStream output = listener.getLogger();
		output.println("  RTC SCM - Jazz Client: Accepting changes...");
		String defaultWS = configuration.getWorkspaceName();
				
		//configuration.display("JazzClient - accept(1)");

        Map<String, JazzChangeSet> compareCmdResults = compare();

		output.println("      -------------------------------------");
        if (!compareCmdResults.isEmpty()) {
       		output.println("      -- " + compareCmdResults.values().size() + " Code Changes - Detected --");
			
			//Remove any changesets that are component additions
			//	what happens if the addition is the only change?
			//	does accept command change for components/changesets
			//output.println(compareCmdResults.keySet().size() + " changesets.");
			boolean componentsChanged = false;
			if (compareCmdResults.containsKey("Added component")) {
				compareCmdResults.remove("Added component");
				componentsChanged = true;
			}
			//output.println(compareCmdResults.keySet().size() + " changesets AFTER.");

			accept(compareCmdResults.keySet());
			
			if (componentsChanged) {
				load();
			}

            /*for (Map.Entry<String, JazzChangeSet> entry : compareCmdResults.entrySet()) {
                JazzChangeSet changeSet1 = entry.getValue();
                JazzChangeSet changeSet2 = acceptCmdResult.get(entry.getKey());
                changeSet1.copyItemsFrom(changeSet2);
            }*/
			
            output.println("      -- Code Changes - 'Accept' Complete");
        } else {
       		output.println("      -- NO Code Changes Detected");
        }
		output.println("      -------------------------------------");

        return new ArrayList<JazzChangeSet>(compareCmdResults.values());
    }

	/****************************************************
	
		private String getVersion() throws IOException, InterruptedException {
	
	****************************************************/
    private String getVersion() throws IOException, InterruptedException {
        VersionCommand cmd = new VersionCommand(configuration);
        return execute(cmd);
    }

	/****************************************************
	
		private Map<String, JazzChangeSet> accept(Collection<String> changeSets) throws IOException, InterruptedException {
	
	****************************************************/
    private Map<String, JazzChangeSet> accept(Collection<String> changeSets) throws IOException, InterruptedException {
 		//output to console.
		PrintStream output = listener.getLogger();
		output.println("  RTC SCM - Jazz Client: Accept...");
        String version = getVersion(); // TODO The version should be checked when configuring the Jazz Executable
        
        //configuration.display("JazzClient() - accept(2)");
        
        AcceptCommand cmd = new AcceptCommand(configuration, changeSets, version, listener, jazzExecutable);
        return execute(cmd, null);
		//return joinWithPossibleTimeout(run(cmd.getArguments(), null), true, listener, null) == 0;
    }

	/****************************************************
	
		private Map<String, JazzChangeSet> compare() throws IOException, InterruptedException {
	
	****************************************************/
    private Map<String, JazzChangeSet> compare() throws IOException, InterruptedException {
        CompareCommand cmd = new CompareCommand(configuration);
		return execute(cmd);
    }

	private <T> T execute(ParseableCommand<T> cmd) throws IOException, InterruptedException {
		return execute(cmd, jazzExecutable);
	}
	
	/****************************************************
	
		private <T> T execute(ParseableCommand<T> cmd) throws IOException, InterruptedException {
	
	****************************************************/
    private <T> T execute(ParseableCommand<T> cmd, String jazzExecutable) throws IOException, InterruptedException {
 		//output to console.
		PrintStream output = listener.getLogger();
		//output.println("  RTC SCM - Jazz Client: Execute.");

        BufferedReader in = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(popen(cmd.getArguments(), jazzExecutable).toByteArray())));
        T result;

        try {
            result = cmd.parse(in);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            in.close();
        }

        return result;
    }

	/****************************************************
	
		private ProcStarter l(ArgumentListBuilder args) 
	
	****************************************************/
	private ProcStarter l(ArgumentListBuilder args) {
		String consoleString = "  RTC SCM - Jazz Client: Launching scm.exe...\n";
		boolean[] maskArray = args.toMaskArray();
		String[] cmdArray = args.toCommandArray();
		
		for (int i = 0; i < maskArray.length; i++) {
			if (maskArray[i] == false) {
				consoleString += cmdArray[i] + " ";
			} else {
				consoleString += "**** ";
			}
		}
		
		//output to console.
		PrintStream output = listener.getLogger();
		//output.println(consoleString);
		
		// set the default stdout
		return launcher.launch().cmds(args).stdout(listener);
	}

	/****************************************************
	
		private ProcStarter run(ArgumentListBuilder args) 
	
	****************************************************/
	protected ProcStarter run(ArgumentListBuilder args, String command) {
		if (command != null) {
			args = args.clone().prepend(command);
		}
	    return l(args);
	} // End: run(ArgumentListBuilder args) 
	
	/****************************************************
	
		private ProcStarter run(ArgumentListBuilder args) 
	
	****************************************************/
	private ProcStarter run(ArgumentListBuilder args) {
		return run(args, jazzExecutable);
	} // End: run(ArgumentListBuilder args) 
	
	/****************************************************
	
		private int joinWithPossibleTimeout(ProcStarter proc, final TaskListener listener, StringBuffer strBuf) throws IOException, InterruptedException 
	
	****************************************************/
	private int joinWithPossibleTimeout(ProcStarter proc, final TaskListener listener, StringBuffer strBuf) throws IOException, InterruptedException {
		return joinWithPossibleTimeout(proc, listener, strBuf, null, null);
	}

	/****************************************************
	
		private int joinWithPossibleTimeout(ProcStarter proc, final TaskListener listener, StringBuffer strBuf, AbstractBuild currentBuild) throws IOException, InterruptedException 
	
	****************************************************/
	protected int joinWithPossibleTimeout(ProcStarter proc, final TaskListener listener, StringBuffer strBuf, AbstractBuild currentBuild, String stringToHide) throws IOException, InterruptedException 
	{
	    boolean useTimeout = configuration.isUseTimeout();
	    long timeoutValue = configuration.getTimeoutValue();
	    
		int result = -1;
		
		try {
			PipedInputStream pis = null;
			if(strBuf != null) {
				PipedOutputStream pos = new PipedOutputStream();
				pis = new PipedInputStream(pos, 1000000);
				proc = proc.stdout(pos);
			}
			
            hudson.Proc procStarted = proc.start();
			if (useTimeout) {
				result = procStarted.joinWithTimeout(timeoutValue, TimeUnit.SECONDS, listener);
			} else {
				result = procStarted.join();
			}
			
			if(strBuf != null) {
				byte[] stdoutDataArr = new byte[pis.available()];
				pis.read(stdoutDataArr, 0, stdoutDataArr.length);
				String stdoutStr = new String(stdoutDataArr);
				if(stringToHide != null) {
					stdoutStr = stdoutStr.replaceAll(stringToHide, "****");
				}
				strBuf.append(stdoutStr);
				PrintStream output = listener.getLogger();
				output.println(stdoutStr);
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			if (listener != null) {
				listener.error("Exception caught in joinWithPossibleTimeout: "+e);
			}
		}
		
		return result;
	} // End: joinWithPossibleTimeout(...)

	/****************************************************
	
		private ByteArrayOutputStream popen(ArgumentListBuilder args)
		
		Runs the command and captures the output.
	
	****************************************************/
	private ByteArrayOutputStream popen(ArgumentListBuilder args, String jazzExecutable)
	            throws IOException, InterruptedException 
	{
		try {
			
			// scm produces text in the platform default encoding, so we need to convert it back to UTF-8
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			WriterOutputStream o = new WriterOutputStream(new OutputStreamWriter(baos, "UTF-8"),
					java.nio.charset.Charset.forName("UTF-8"));
					
			PrintStream output = listener.getLogger();
			
			ForkOutputStream fos = new ForkOutputStream(o, output);
			ProcStarter pstarter = run(args, jazzExecutable);

			if (joinWithPossibleTimeout(pstarter.stdout(fos), listener, null) == 0) {
				o.flush();
				return baos;
			} else {
				String errorString = "Failed to run ";
				boolean[] maskArray = args.toMaskArray();
				String[] cmdArray = args.toCommandArray();
				
				for (int i = 0; i < maskArray.length; i++) {
					if (maskArray[i] == false) {
						errorString += cmdArray[i] + " ";
					} else {
						errorString += "**** ";
					}
				}
				
				listener.error(errorString);
				throw new AbortException();
			}
		} catch (Exception e) {
			listener.error("Exception in popen " + e);
			throw new AbortException();
		}
    }

} //End: public class JazzClient(..)
