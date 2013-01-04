package com.deluan.jenkins.plugins.rtc.commands.accept;

import hudson.scm.EditType;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;

/**
 * @author deluan
 */
public abstract class BaseAcceptOutputParser {
    private static final Logger logger = Logger.getLogger(BaseAcceptOutputParser.class.getName());

    private Pattern startChangesetPattern;
    private Pattern filePattern;
    private Pattern workItemPattern;

    public BaseAcceptOutputParser(String startChangesetPattern, String filePattern, String workItemPattern) {
        this.workItemPattern = Pattern.compile(workItemPattern);
        this.startChangesetPattern = Pattern.compile(startChangesetPattern);
        this.filePattern = Pattern.compile(filePattern);
    }

    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        Map<String, JazzChangeSet> result = new HashMap<String, JazzChangeSet>();
        
        logger.fine("parse()");
        String line;
        JazzChangeSet changeSet = null;
        Matcher matcher;

        while ((line = reader.readLine()) != null) {
        	if(logger.isLoggable(Level.FINER)) {
        		logger.finer("parse(), line: '" + line + "'");
        	}
            if ((matcher = startChangesetPattern.matcher(line)).matches()) {
            	if(logger.isLoggable(Level.FINER)) {
            		logger.finer("startChangeset");
            	}
                if (changeSet != null) {
                    result.put(changeSet.getRev(), changeSet);
                }
                changeSet = new JazzChangeSet();
                changeSet.setRev(matcher.group(1));
            } else if ((matcher = filePattern.matcher(line)).matches()) {
            	if(logger.isLoggable(Level.FINER)) {
            		logger.finer("file");
            	}
                assert changeSet != null;
                String action = parseAction(matcher.group(1));
                String path = parsePath(matcher.group(2));
                changeSet.addItem(path, action);
            } else if ((matcher = workItemPattern.matcher(line)).matches()) {
            	if(logger.isLoggable(Level.FINER)) {
            		logger.finer("workItem");
            	}
                assert changeSet != null;
                changeSet.addWorkItem(parseWorkItem(matcher.group(2)));
            } else {
            	if(logger.isLoggable(Level.FINER)) {
            		logger.finer("line skipped");
            	}
            }
        }

        if (changeSet != null) {
            result.put(changeSet.getRev(), changeSet);
        }

        return result;
    }

    protected abstract String parseWorkItem(String string);

    protected abstract String parseEditFlag(String string);

    protected String parsePath(String string) {
        String path = string.replaceAll("\\\\", "/").trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    protected String parseAction(String string) {
        String flag = parseEditFlag(string);
        String action = EditType.EDIT.getName();
        if ("a".equals(flag)) {
            action = EditType.ADD.getName();
        } else if ("d".equals(flag)) {
            action = EditType.DELETE.getName();
        }
        return action;
    }


}
