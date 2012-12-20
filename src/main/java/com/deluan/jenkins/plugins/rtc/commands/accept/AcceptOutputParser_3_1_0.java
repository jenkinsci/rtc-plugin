package com.deluan.jenkins.plugins.rtc.commands.accept;

/**
 * @author: Chris Cosby <ccosby@gmail.com>
 */

public class AcceptOutputParser_3_1_0 extends BaseAcceptOutputParser {

    public AcceptOutputParser_3_1_0() {
        super("^\\s{6}[^\\d\\s]+(\\d+)[^\\d]+\\s(.*)$",
                "^\\s{10}(.{5})\\s+(.*)$",
                "^\\s{10}[^\\d\\s]+(\\d+)[^\\d]+(.*)$");
    }

    @Override
    protected String parseWorkItem(String string) {
        return string;
    }

    @Override
    protected String parseEditFlag(String string) {
        return string.substring(2, 3);
    }
}
