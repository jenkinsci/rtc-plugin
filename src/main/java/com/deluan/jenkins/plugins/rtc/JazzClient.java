package com.deluan.jenkins.plugins.rtc;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.*;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.util.LogTaskListener;
import org.kohsuke.stapler.framework.io.WriterOutputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the invocation of RTC's SCM Command Line Interface, "scm".
 *
 * @author deluan
 */
@SuppressWarnings("JavaDoc")
public class JazzClient {
    public static final String SCM_CMD = "scm";

    private static final Logger logger = Logger.getLogger(JazzClient.class.getName());

    private JazzConfiguration configuration = new JazzConfiguration();
    private final Launcher launcher;
    private final TaskListener listener;
    private String jazzExecutable;
    private String version;

    public JazzClient(String jazzExecutable, FilePath jobWorkspace,
                      JazzConfiguration configuration) throws IOException, InterruptedException {
        FilePath executable = new FilePath(new File(jazzExecutable));
        this.listener = new LogTaskListener(logger, Level.FINEST);
        this.launcher = executable.createLauncher(listener);
        this.jazzExecutable = jazzExecutable;
        this.configuration = new JazzConfiguration(configuration);
        this.configuration.setJobWorkspace(jobWorkspace);
    }

    public JazzClient(String jazzExecutable, FilePath jobWorkspace, JazzConfiguration configuration, Launcher launcher, TaskListener listener) {
        this.jazzExecutable = jazzExecutable;
        this.launcher = launcher;
        this.listener = listener;
        this.configuration = new JazzConfiguration(configuration);
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

        return (joinWithPossibleTimeout(run(cmd.getArguments())) == 0);
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
        ArgumentListBuilder args = new ArgumentListBuilder(findSCMExecutable());

        args.add(new StopDaemonCommand(configuration).getArguments().toCommandArray());

        return (joinWithPossibleTimeout(l(args)) == 0);
    }

    protected String findSCMExecutable() {
        File file = new File(jazzExecutable);

        // First, check if we can use the configured jazz executable
        if (file.getName().startsWith("scm")) {
            return jazzExecutable;
        }

        // Else, try to find a suitable scm command in the same directory as the configured one
        String[] cmds = {"scm", "scm.exe", "scm.sh"};
        String installDir = file.getParent();
        if (installDir != null) {
            for (String cmd : cmds) {
                String fullCmdPath = installDir + '/' + cmd;
                if (canExecute(fullCmdPath)) {
                    return fullCmdPath;
                }
            }
        }

        // If not found, hope that there is a scm command in the system's PATH
        return SCM_CMD;
    }

    protected boolean canExecute(String fullCmdPath) {
        return new File(fullCmdPath).canExecute();
    }

    /**
     * Disable scm's version auto detection and use this specific version
     *
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() throws IOException, InterruptedException {
        if (this.version == null) {
            VersionCommand cmd = new VersionCommand(configuration);
            this.version = execute(cmd);
        }
        return this.version;
    }

    /**
     * Call <tt>scm accept</tt> command.<p/>
     *
     * @return all changeSets accepted, complete with affected paths and related work items
     * @throws IOException
     * @throws InterruptedException
     */
    public List<JazzChangeSet> accept() throws IOException, InterruptedException {
    	logger.fine("accept()");
        Map<String, JazzChangeSet> compareCmdResults = compare();

        if (!compareCmdResults.isEmpty()) {
            Map<String, JazzChangeSet> acceptCmdResult = accept(compareCmdResults.keySet());

            for (Map.Entry<String, JazzChangeSet> entry : compareCmdResults.entrySet()) {
                JazzChangeSet changeSet1 = entry.getValue();
                JazzChangeSet changeSet2 = acceptCmdResult.get(entry.getKey());
                if (changeSet2 == null) {
                    throw new IOException("'scm accept' output invalid");
                }
                changeSet1.copyItemsFrom(changeSet2);
            }
        }

    	logger.fine("accept() end");
        return new ArrayList<JazzChangeSet>(compareCmdResults.values());
    }

    protected Map<String, JazzChangeSet> accept(Collection<String> changeSets) throws IOException, InterruptedException {
        AcceptCommand cmd = new AcceptCommand(configuration, changeSets, getVersion());
        return execute(cmd);
    }

    protected Map<String, JazzChangeSet> compare() throws IOException, InterruptedException {
        CompareCommand cmd = new CompareCommand(configuration);
        return execute(cmd);
    }

    private <T> T execute(ParseableCommand<T> cmd) throws IOException, InterruptedException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(popen(cmd.getArguments()).toByteArray())));
        T result;

        try {
            result = cmd.parse(in);
        } catch (Exception e) {
        	logger.log(Level.SEVERE, "Error parsing command!", e);
            throw new IOException(e);
        } finally {
            in.close();
        }

        return result;
    }

    private ProcStarter l(ArgumentListBuilder args) {
        // set the default stdout
        return launcher.launch().cmds(args).stdout(listener);
    }

    private ProcStarter run(ArgumentListBuilder args) {
        ArgumentListBuilder cmd = args.clone().prepend(jazzExecutable);
        return l(cmd);
    }

    private int joinWithPossibleTimeout(ProcStarter proc) throws IOException, InterruptedException {
        return configuration.isUseTimeout() ? proc.start().joinWithTimeout(configuration.getTimeoutValue(), TimeUnit.SECONDS, listener) : proc.join();
    }

    /**
     * Runs the command and captures the output.
     */
    private ByteArrayOutputStream popen(ArgumentListBuilder args)
            throws IOException, InterruptedException {

        // scm produces text in the platform default encoding, so we need to convert it back to UTF-8
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WriterOutputStream o = new WriterOutputStream(new OutputStreamWriter(baos, "UTF-8"),
                getDefaultCharset());

        PrintStream output = listener.getLogger();
        ForkOutputStream fos = new ForkOutputStream(o, output);
        if (joinWithPossibleTimeout(run(args).stdout(fos)) == 0) {
            o.flush();
            return baos;
        } else {
            listener.error("Failed to run " + toMaskedCommandLine(args));
            throw new AbortException();
        }
    }

    /**
     * A version of ArgumentListBuilder.toStringWithQuote() that also masks fields marked as 'masked'
     */
    protected String toMaskedCommandLine(ArgumentListBuilder argsBuilder) {
        StringBuilder buf = new StringBuilder();
        List<String> args = argsBuilder.toList();
        boolean[] masks = argsBuilder.toMaskArray();

        for (int i = 0; i < args.size(); i++) {
            String arg;
            if (masks[i]) {
                arg = "********";
            } else {
                arg = args.get(i);
            }

            if (buf.length() > 0) buf.append(' ');

            if (arg.indexOf(' ') >= 0 || arg.length() == 0)
                buf.append('"').append(arg).append('"');
            else
                buf.append(arg);
        }
        return buf.toString();
    }


    private Charset getDefaultCharset() {
        // First check if we can get currentComputer. See issue JENKINS-11874
        if (Computer.currentComputer() != null) {
            return Computer.currentComputer().getDefaultCharset();
        } else {
            return Charset.forName("UTF-8");
        }
    }
}
