package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzClient;
import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptCustomOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptNewOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.AcceptOldOutputParser;
import com.deluan.jenkins.plugins.rtc.commands.accept.BaseAcceptOutputParser;
import hudson.util.ArgumentListBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * @author deluan
 */
public class AcceptCommand extends AbstractCommand implements ParseableCommand<Map<String, JazzChangeSet>> {

    public static final String NEW_FORMAT_VERSION = "2.1.0";

    private static final Logger logger = Logger.getLogger(AcceptCommand.class.getName());

    /*
     * Environment properties used for custom rtc command line client output parsing.
     * Have to use following values in jenkins windows start bat file for RTC 3.1.100:
     * set RTC_ACCEPT_OUT_PATTERN_STARTCHANGESET=^^\s{6}\((\d+)\)\s(.*)
     * set RTC_ACCEPT_OUT_PATTERN_FILE=^^\s{10}---(.)-\s+(.*)
     * set RTC_ACCEPT_OUT_PATTERN_WORKITEM=^^\s{10}\((\d+)\)+(.*)
     */
    public static final String RTC_ACCEPT_OUT_PATTERN_STARTCHANGESET = "RTC_ACCEPT_OUT_PATTERN_STARTCHANGESET";
    public static final String RTC_ACCEPT_OUT_PATTERN_FILE = "RTC_ACCEPT_OUT_PATTERN_FILE";
    public static final String RTC_ACCEPT_OUT_PATTERN_WORKITEM = "RTC_ACCEPT_OUT_PATTERN_WORKITEM";
    
    private Collection<String> changeSets;
    private BaseAcceptOutputParser parser;
    protected boolean oldFormat = false;

    public AcceptCommand(JazzConfiguration configurationProvider, Collection<String> changeSets, String version) {
        super(configurationProvider);
        this.changeSets = new LinkedHashSet<String>(changeSets);
        this.oldFormat = (version.compareTo(NEW_FORMAT_VERSION) < 0);
        
        String startChangesetPattern = System.getenv(RTC_ACCEPT_OUT_PATTERN_STARTCHANGESET);
        String filePattern = System.getenv(RTC_ACCEPT_OUT_PATTERN_FILE);
        String workItemPattern = System.getenv(RTC_ACCEPT_OUT_PATTERN_WORKITEM);
        
    	if(StringUtils.hasText(startChangesetPattern) && StringUtils.hasText(filePattern) && StringUtils.hasText(workItemPattern)) {
        	logger.info("RTCOUT_* flags found, using AcceptCustomOutputParser('" + startChangesetPattern + "','" + filePattern + "','" + workItemPattern + "')");
        	parser = new AcceptCustomOutputParser(startChangesetPattern, filePattern, workItemPattern);
        }
        else {
        	logger.info("RTCOUT_* flags not found, oldFormat: " + oldFormat);
        	parser = (oldFormat) ? new AcceptOldOutputParser() : new AcceptNewOutputParser();
        }
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add("accept");
        addLoginArgument(args);
        addLocalWorkspaceArgument(args);
        addSourceStream(args);
        args.add("--flow-components", "-o", "-v");
        if (hasAnyChangeSets()) {
            addChangeSets(args);
        }

        return args;
    }

    private void addSourceStream(ArgumentListBuilder args) {
        args.add("-s", getConfig().getStreamName());
    }

    private boolean hasAnyChangeSets() {
        return changeSets != null && !changeSets.isEmpty();
    }

    private void addChangeSets(ArgumentListBuilder args) {
        args.add("-c");
        for (String changeSet : changeSets) {
            args.add(changeSet);
        }
    }

    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        return parser.parse(reader);
    }
}
