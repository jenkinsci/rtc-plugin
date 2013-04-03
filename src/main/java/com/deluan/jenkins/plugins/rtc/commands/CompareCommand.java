package com.deluan.jenkins.plugins.rtc.commands;

import com.deluan.jenkins.plugins.rtc.JazzConfiguration;
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.util.ArgumentListBuilder;
import hudson.model.TaskListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;

/**
 * @author deluan
 */
public class CompareCommand extends AbstractCommand implements ParseableCommand<Map<String, JazzChangeSet>> {
    private static final Logger logger = Logger.getLogger(CompareCommand.class.getName());

    private static final String DATE_FORMAT = "yyyy-MM-dd-HH:mm:ss";
    private static final String CONTRIBUTOR_FORMAT = "|{name}|{email}|";
    private final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		
    public CompareCommand(JazzConfiguration configurationProvider) {
        super(configurationProvider);
    }

    public ArgumentListBuilder getArguments() {
        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add("compare");
        args.add("ws", getConfig().getWorkspaceName());
        args.add("stream", getConfig().getStreamName());
        addLoginArgument(args);
        addRepositoryArgument(args);
        args.add("-I", "dcsf");
        args.add("-C", '"' + CONTRIBUTOR_FORMAT + '"');
        args.add("-D", "\"|" + DATE_FORMAT + "|\"");

        return args;
    }
	
    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        Map<String, JazzChangeSet> result = new LinkedHashMap<String, JazzChangeSet>();
		String loadRules = getConfig().getLoadRules();
		
        String line;
		String currentComponent = "";
		String direction = "";
		JazzChangeSet changeSet = null;
        while ((line = reader.readLine()) != null) {
            try {
                String[] parts = line.split("\\|");
				//A changeset will not be added until a file is found within that changeset that is also in the load rules
				if (parts[0].trim().startsWith("Component") || parts[0].endsWith("Changes")) {
					if (parts[0].trim().equals("Outgoing Changes")) {
						direction = "Outgoing Changes";
					} else if (parts[0].trim().equals("Incoming Changes")) {
						direction = "Incoming Changes";
					}
					if (direction.equals("Incoming Changes")) {
						if (parts[0].trim().startsWith("Component")) {
							currentComponent = parts[0].substring(parts[0].indexOf("\"")+1, parts[0].length()-1);
						}
						//if there is an added component then add to the map with a null changeset and a string that just says added component
						if (parts[0].trim().startsWith("Component") && parts[0].contains("\" (added)")) {
							result.put("Added component", null);
						}
					}
				} else {
					if (parts[0].trim().startsWith("/")) { // this is a file not a changeset
						//check against load rules
						boolean match = fileContainedInLoadRules(parts[0].trim().substring(1, parts[0].trim().length()), loadRules, currentComponent);
						if (match) {
							result.put(changeSet.getRev(), changeSet);
						}
					} else {
						String rev = parseRevisionNumber(parts[0]);
						changeSet = new JazzChangeSet();
						changeSet.setRev(rev);
						changeSet.setUser(parts[1].trim());
						changeSet.setEmail(parts[2].trim());
						changeSet.setMsg(parseMessage(parts[3]));
						try {
							changeSet.setDate(sdf.parse(parts[4].trim()));
						} catch (ParseException e) {
							logger.log(Level.WARNING, "Error parsing date '" + parts[4].trim() + "' for revision (" + rev + ")");
						}
					}
				}
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error parsing compare output:\n\n" + line + "\n\n", e);
            }
        }

        return result;
    }

    private String parseRevisionNumber(String part) {
        return part.replaceAll("[^0-9]", "");
    }

    private String parseMessage(String string) {
        String msg = string.trim();
        if (msg.startsWith("\"")) {
            int closingQuotes = msg.lastIndexOf('\"');
            msg = msg.substring(1, closingQuotes).trim();
        }
        return msg;
    }
	
	private boolean fileContainedInLoadRules(String combinedFileName, String loadRulesStr, String componentName) {
		boolean returnVal = false;
		if (loadRulesStr == null) {
			returnVal = true;
		} else {
			loadRulesStr = loadRulesStr.replaceAll("\\\\", "/");
			
			if (loadRulesStr.length() < 3) {
				returnVal = true;
			}
			StringTokenizer strtok = new StringTokenizer(loadRulesStr, "\n");
			while(strtok.hasMoreElements()) {
				String nextLine = strtok.nextToken().trim();
				if (nextLine.contains(":")) {
					StringTokenizer strtok2 = new StringTokenizer(nextLine, ":");
					String componentRule = strtok2.nextToken().trim();
					String pathString = strtok2.nextToken().trim();
					
					if (pathString.startsWith("/")) {
						pathString = pathString.substring(1, pathString.length());
					}
					if (pathString.endsWith("/")) {
						pathString = pathString.substring(0, pathString.length()-1);
					}

					if ((combinedFileName.equals(pathString) || combinedFileName.startsWith(pathString + "/")) && componentName.equals(componentRule)) {
						returnVal = true;
					}
				}
			}
		}
		return returnVal;
	}
}
