package com.deluan.jenkins.plugins.rtc.changelog;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author deluan
 */
public class JazzChangeSetList extends ChangeLogSet<JazzChangeSet> {
    private final List<JazzChangeSet> changeSets;

    public JazzChangeSetList(AbstractBuild build, List<JazzChangeSet> logs) {
        super(build);
        Collections.reverse(logs);  // put new things first
        this.changeSets = Collections.unmodifiableList(logs);
        for (JazzChangeSet log : logs) {
            log.setParent(this);
        }
    }

    public boolean isEmptySet() {
        return changeSets.isEmpty();
    }

    public Iterator<JazzChangeSet> iterator() {
        return changeSets.iterator();
    }

    @SuppressWarnings("unused")
    public List<JazzChangeSet> getLogs() {
        return changeSets;
    }

    @Override
    public String getKind() {
        return "rtc";
    }

}
