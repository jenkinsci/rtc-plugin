package com.deluan.jenkins.plugins.rtc.commands.accept;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet;
import hudson.scm.EditType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Chris Cosby <ccosby@gmail.com>
 */

public class JsonAcceptOutputParser extends BaseAcceptOutputParser {

    public JsonAcceptOutputParser() {
        super("", "", "");
        System.err.println("JSON Parser is still very experimental");
    }

    public Map<String, JazzChangeSet> parse(BufferedReader reader) throws ParseException, IOException {
        Map<String, JazzChangeSet> result = new HashMap<String, JazzChangeSet>();

        String line;
        StringBuilder json = new StringBuilder();
        JazzChangeSet changeSet;

        try {
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: Are there multiple items in these arrays? I'm only grabbing #0
        JSONObject jazzAcceptObj = JSONObject.fromObject(json.toString());
        JSONArray jazzChanges = jazzAcceptObj.getJSONArray("repos").getJSONObject(0).getJSONArray("workspaces").getJSONObject(0).getJSONArray("components").getJSONObject(0).getJSONArray("changes");
        for (Iterator<JSONObject> jazzChangesIter = jazzChanges.iterator(); jazzChangesIter.hasNext(); ) {
            JSONObject jazzChange = jazzChangesIter.next();

            //String author = jazzChange.getString("author");
            String rev = jazzChange.getString("uuid");

            changeSet = new JazzChangeSet();
            changeSet.setRev(rev);

            // Iterate through the changes under the changes (stupid stupid JSON) and add to the changeSet
            JSONArray jazzChangeChanges = jazzChange.getJSONArray("changes");
            for (Iterator<JSONObject> jazzChangeChangesIter = jazzChangeChanges.iterator(); jazzChangeChangesIter.hasNext(); ) {
                JSONObject jazzChangeChange = jazzChangeChangesIter.next();

                String path = parsePath(jazzChangeChange.getString("path"));
                String action = parseAction(jazzChangeChange.getJSONObject("state"));
                changeSet.addItem(path, action);

            }

            // Iterate through the workitems under the changes and add to the changeSet
            JSONArray jazzChangeWorkitems = jazzChange.getJSONArray("workitems");
            for (Iterator<JSONObject> jazzChangeWorkitemsIter = jazzChangeWorkitems.iterator(); jazzChangeWorkitemsIter.hasNext(); ) {
                JSONObject jazzChangeWorkItem = jazzChangeWorkitemsIter.next();

                String id = jazzChangeWorkItem.getString("id");
                String workitemLabel = jazzChangeWorkItem.getString("workitem-label");
                String workitem = parseWorkItem(id, workitemLabel);

                changeSet.addWorkItem(workitem);
            }

            result.put(rev, changeSet);
        }

        return result;
    }

    protected String parseWorkItem(String id, String workitemLabel) {
        return id + " \"" + workitemLabel + "\"";
    }

    @Override
    protected String parseWorkItem(String string) {
        return string;
    }

    @Override
    protected String parseEditFlag(String string) {
        return string;
    }

    @Override
    protected String parsePath(String string) {
        String path = string.replaceAll("\\\\", "/").trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    protected String parseAction(JSONObject state) {
        String action = "";
        if (state.getBoolean("content_change")) {
            action = EditType.EDIT.getName();
        } else if (state.getBoolean("add")) {
            action = EditType.ADD.getName();
        } else if (state.getBoolean("delete")) {
            action = EditType.DELETE.getName();
        }
        return action;
    }
}
