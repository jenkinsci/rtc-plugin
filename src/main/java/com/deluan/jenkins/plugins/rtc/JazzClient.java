package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.*;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.model.*;
import hudson.util.*;
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
public class JazzClient {
    public static final String SCM_CMD = "scm";

    private static final int TIMEOUT = 60 * 60; // in seconds

    private JazzConfiguration configuration = new JazzConfiguration();
    private final Launcher launcher;
    private final TaskListener listener;
    private String jazzExecutable;

    public JazzClient(Launcher launcher, TaskListener listener, FilePath jobWorkspace, String jazzExecutable,
                      JazzConfiguration configuration) {
        this.jazzExecutable = jazzExecutable;
        this.launcher = launcher;
        this.listener = listener;
        this.configuration = configuration.clone();
        this.configuration.setJobWorkspace(jobWorkspace);
    }

    /**
     * Returns true if there is any incoming changes to be accepted.
     *
     * @return <tt>true</tt> if any changes are found
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean hasChanges() throws IOException, InterruptedException {
		try {
			FileOutputStream fos = new FileOutputStream("N:\\temp\\RTCPluginLog.txt", true);
			fos.write("has changes\n".getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
		}
        Map<String, JazzChangeSet> changes = compare();

        return !changes.isEmpty();
    }

    /**
     * Call <tt>scm load</tt> command. <p/>
     * <p/>
     * Will load the workspace using the parameters defined.
     *
     * @return <tt>true</tt> on success
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean load() throws IOException, InterruptedException {
        Command cmd = new LoadCommand(configuration);

        return joinWithPossibleTimeout(run(cmd.getArguments()), true, listener, null) == 0;
    }
	
	/**
     * Call <tt>scm history</tt> command. <p/>
     * <p/>
     * Will check if the workspace exists.
     *
     * @return <tt>true</tt> on exists
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean workspaceExists() throws IOException, InterruptedException {
        Command cmd = new HistoryCommand(configuration);
		StringBuffer strBuf = new StringBuffer();
		joinWithPossibleTimeout(run(cmd.getArguments()), true, listener, strBuf);
		boolean result = true;
		String stdOut = strBuf.toString();
		if (stdOut.contains("did not match any workspaces")) {
			listener.error("The workspace probably doesn't exist.");
			result = false;
		}
        return result;
    }
	
	/**
     * Call <tt>scm create workspace</tt> command. <p/>
     * <p/>
     * Create the workspace.
     *
     * @return <tt>true</tt> on success
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean createWorkspace() throws IOException, InterruptedException {
        Command cmd = new CreateWorkspaceCommand(configuration);
		//StringBuffer strBuf = new StringBuffer();
		boolean result = joinWithPossibleTimeout(run(cmd.getArguments()), true, listener, null) == 0;
		//String stdOut = strBuf.toString();
		//if (stdOut.contains("Problem running")) {
		//	listener.error("The workspace probably doesn't exist.");
		//	result = false;
		//}
        return result;
    }

    /**
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
     */
    public boolean stopDaemon() throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder(SCM_CMD);

        args.add(new StopDaemonCommand(configuration).getArguments().toCommandArray());

        return (joinWithPossibleTimeout(l(args), true, listener, null) == 0);
    }

    /**
     * Call <tt>scm accept</tt> command.<p/>
     *
     * @return all changeSets accepted, complete with affected paths and related work itens
     * @throws IOException
     * @throws InterruptedException
     */
    public List<JazzChangeSet> accept() throws IOException, InterruptedException {
        Map<String, JazzChangeSet> compareCmdResults = compare();

        if (!compareCmdResults.isEmpty()) {
            Map<String, JazzChangeSet> acceptCmdResult = accept(compareCmdResults.keySet());

            for (Map.Entry<String, JazzChangeSet> entry : compareCmdResults.entrySet()) {
                JazzChangeSet changeSet1 = entry.getValue();
                JazzChangeSet changeSet2 = acceptCmdResult.get(entry.getKey());
                changeSet1.copyItemsFrom(changeSet2);
            }
        }

        return new ArrayList<JazzChangeSet>(compareCmdResults.values());
    }

    private String getVersion() throws IOException, InterruptedException {
        VersionCommand cmd = new VersionCommand(configuration);
        return execute(cmd);
    }

    private Map<String, JazzChangeSet> accept(Collection<String> changeSets) throws IOException, InterruptedException {
        String version = getVersion(); // TODO The version should be checked when configuring the Jazz Executable
        AcceptCommand cmd = new AcceptCommand(configuration, changeSets, version);
        return execute(cmd);
    }

    private Map<String, JazzChangeSet> compare() throws IOException, InterruptedException {
		listener.error("compare in JazzClient");
		try {
			FileOutputStream fos = new FileOutputStream("N:\\temp\\RTCPluginLog.txt", true);
			fos.write("compare in JazzClient\n".getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
		}
        CompareCommand cmd = new CompareCommand(configuration);
        return execute(cmd);
    }

    private <T> T execute(ParseableCommand<T> cmd) throws IOException, InterruptedException {
		listener.error("execute in JazzClient");
		try {
			FileOutputStream fos = new FileOutputStream("N:\\temp\\RTCPluginLog.txt", true);
			fos.write("execute in JazzClient\n".getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
		}
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(popen(cmd.getArguments()).toByteArray())));
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

    private ProcStarter l(ArgumentListBuilder args) {
		String errorString = "Trying to run with args ";
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
		
		try {
			FileOutputStream fos = new FileOutputStream("N:\\temp\\RTCPluginLog.txt", true);
			fos.write(args.toStringWithQuote().getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
		}
		
        // set the default stdout
		return launcher.launch().cmds(args).stdout(listener);
    }

    private ProcStarter run(ArgumentListBuilder args) {
		listener.error("run in JazzClient");
		try {
			FileOutputStream fos = new FileOutputStream("N:\\temp\\RTCPluginLog.txt", true);
			fos.write("run in JazzClient\n".getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
		}
        ArgumentListBuilder cmd = args.clone().prepend(jazzExecutable);
        return l(cmd);
    }

    private int joinWithPossibleTimeout(ProcStarter proc, boolean useTimeout, final TaskListener listener, StringBuffer strBuf) throws IOException, InterruptedException {
		int result = -1;
		
		if(strBuf == null) {
			strBuf = new StringBuffer();
		}
		
		AbstractBuild currentBuild = JazzSCM.getAbstractBuild();

		try {
			//proc = proc.readStdout();		
			if (useTimeout) {
				hudson.Proc procStarted = proc.start();
				result = procStarted.joinWithTimeout(TIMEOUT, TimeUnit.SECONDS, listener);
				//The following line was added to force the scm.exe process to exit while using lscm.bat instead
				//	of scm.exe. This is commented out because we now only use scm.exe
				//procStarted.kill();
			} else {
				hudson.Proc procStarted = proc.start();
				result = procStarted.join();
				//The following line was added to force the scm.exe process to exit while using lscm.bat instead
				//	of scm.exe. This is commented out because we now only use scm.exe
				//procStarted.kill();
			}
			InputStream logStream = currentBuild.getLogInputStream();
			byte[] data2 = new byte[logStream.available()];
			logStream.read(data2);
			strBuf.append(new String(data2));
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			listener.error("Exeption caught in joinWithPossibleTimeout: "+e);
		}
		
		return result;
		//return useTimeout ? proc.start().joinWithTimeout(TIMEOUT, TimeUnit.SECONDS, listener) : proc.join();
    }

    /**
     * Runs the command and captures the output.
     */
    private ByteArrayOutputStream popen(ArgumentListBuilder args)
            throws IOException, InterruptedException {
		listener.error("popen in JazzClient");
		try {
			
			// scm produces text in the platform default encoding, so we need to convert it back to UTF-8
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			listener.error("popen in JazzClient 1");
			listener.error("popen in JazzClient 1.1 " + baos);
			listener.error("popen in JazzClient 1.3 " + Computer.currentComputer());
			//listener.error("popen in JazzClient 1.4 " + Computer.currentComputer().getDefaultCharset());
//			WriterOutputStream o = new WriterOutputStream(new OutputStreamWriter(baos, "UTF-8"),
//					Computer.currentComputer().getDefaultCharset());
			WriterOutputStream o = new WriterOutputStream(new OutputStreamWriter(baos, "UTF-8"),
					java.nio.charset.Charset.forName("UTF-8"));
					
listener.error("popen in JazzClient 2");
			PrintStream output = listener.getLogger();
			listener.error("popen in JazzClient 3");
			ForkOutputStream fos = new ForkOutputStream(o, output);
			listener.error("popen in JazzClient 4");
			ProcStarter pstarter = run(args);
			listener.error("popen in JazzClient 5");
			if (joinWithPossibleTimeout(pstarter.stdout(fos), true, listener, null) == 0) {
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
			listener.error("exception in popen " + e);
			throw new AbortException();
		}
    }
}
