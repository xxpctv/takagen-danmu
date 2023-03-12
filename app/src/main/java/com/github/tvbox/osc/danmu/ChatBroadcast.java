package com.github.tvbox.osc.danmu;


public class ChatBroadcast {
    public static final String CMD_DANMU_MSG = "DANMU_MSG";
    public static final String CMD_SUPER_CHAT_MESSAGE = "SUPER_CHAT_MESSAGE";
    public static final String CMD_SUPER_CHAT_MESSAGE_JPN = "SUPER_CHAT_MESSAGE_JPN";
    public static final String CMD_SEND_GIFT = "SEND_GIFT";

    private String cmd;

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
}
