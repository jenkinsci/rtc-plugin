package com.deluan.jenkins.plugins.rtc.commands.accept;

/**
 * Configurable with three environment variables:
 * RTC_ACCEPT_OUT_PATTERN_STARTCHANGESET, RTC_ACCEPT_OUT_PATTERN_FILE and RTC_ACCEPT_OUT_PATTERN_WORKITEM.
 * (proper values for RTC 3.1.100: "^\s{6}\((\d+)\)\s(.*)", "^\s{10}---(.)-\s+(.*)" and "^\s{10}\((\d+)\)+(.*)".
 * 
 * @author vvidovic
 */
public class AcceptCustomOutputParser extends BaseAcceptOutputParser {
	public AcceptCustomOutputParser(String startChangesetPattern, String filePattern, String workItemPattern) {
        super(startChangesetPattern, filePattern, workItemPattern);
    }

    @Override
    protected String parseWorkItem(String string) {
        return string;
    }

    @Override
    protected String parseEditFlag(String string) {
        return string;
    }
}
