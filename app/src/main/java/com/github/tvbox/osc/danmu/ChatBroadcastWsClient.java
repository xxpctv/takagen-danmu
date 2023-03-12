package com.github.tvbox.osc.danmu;


import com.github.tvbox.osc.ui.fragment.ChatBroadcastPacketUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ChatBroadcastWsClient {

    private long roomId;

    private String token;

    private WebSocketClient webSocketClient;

    private Timer heartTimer;

    private CallBack callBack;

    public static String WS_CHAT = "wss://broadcastlv.chat.bilibili.com:443/sub";

    public ChatBroadcastWsClient(long roomId, String token){
        this.roomId = roomId;
        this.token = token;
        webSocketClient = new WebSocketClient(URI.create(WS_CHAT), new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                if(callBack != null) callBack.onStart();
            }

            @Override
            public void onMessage(ByteBuffer byteBuffer) {
                if(callBack != null){
                    List<String> msgList = ChatBroadcastPacketUtil.decode(byteBuffer, null);
                    if(!msgList.isEmpty()){
                        for (String msg : msgList) {
                            try{
                                JsonObject jsonObject = new Gson().fromJson(msg, JsonObject.class);
                                String cmd = jsonObject.get("cmd").getAsString();
                                if(cmd.contains(ChatBroadcast.CMD_DANMU_MSG)){
                                    JsonArray infoJsonArray = jsonObject.getAsJsonArray("info");
                                    JsonArray propertyJsonArray = infoJsonArray.get(0).getAsJsonArray();
                                    float textSize = propertyJsonArray.get(2).getAsFloat();
                                    int textColor = (int) (0x00000000ff000000 | propertyJsonArray.get(3).getAsLong());
                                    boolean textShadowTransparent = "true".equalsIgnoreCase(propertyJsonArray.get(11).toString());
                                    String text = infoJsonArray.get(1).getAsString();
                                    callBack.onReceiveDanmu(text, textSize, textColor, textShadowTransparent, msg);
                                }else if(ChatBroadcast.CMD_SUPER_CHAT_MESSAGE.equals(cmd) || ChatBroadcast.CMD_SUPER_CHAT_MESSAGE_JPN.equals(cmd)){
                                    JsonObject dataJsonData = jsonObject.getAsJsonObject("data");
                                    String message = dataJsonData.get("message").getAsString();
                                    String messageFontColor = dataJsonData.get("message_font_color").getAsString();
                                    JsonObject userInfoJsonObject = dataJsonData.get("user_info").getAsJsonObject();
                                    String uname = userInfoJsonObject.get("uname").getAsString();
                                    callBack.onReceiveSuperChatMessage(message, messageFontColor, uname, msg);
                                }else if(ChatBroadcast.CMD_SEND_GIFT.equals(cmd)){
                                    JsonObject dataJsonData = jsonObject.getAsJsonObject("data");
                                    String action = dataJsonData.get("action").getAsString();
                                    String giftName = dataJsonData.get("giftName").getAsString();
                                    Integer num = dataJsonData.get("num").getAsInt();
                                    String uname = dataJsonData.get("uname").getAsString();
                                    callBack.onReceiveSendGift(action, giftName, num, uname , msg);
                                }else{
                                    callBack.onReceiveOtherMessage(msg);
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }

            @Override
            public void onMessage(String message) {
                //System.out.println("Rec: "+ message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if(callBack != null){
                    callBack.onClose(code, reason, remote);
                }
            }

            @Override
            public void onError(Exception ex) {
                //ex.printStackTrace();
            }
        };
    }

    public void start() throws Exception  {
        if(!webSocketClient.isOpen()){
            webSocketClient.connectBlocking();
            String joinRoomJson = String.format(Locale.CHINA, ChatBroadcastPacketUtil.ROOM_AUTH_JSON, roomId, token);
            webSocketClient.send(ChatBroadcastPacketUtil.encode(joinRoomJson, 1, ChatBroadcastPacketUtil.OPERATION_AUTH));
        }
        if(heartTimer == null){
            heartTimer = new Timer();
            heartTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if(webSocketClient.isOpen()){
                        webSocketClient.send(ChatBroadcastPacketUtil.HEART_PACKET);
                    }
                }
            }, 1000, 30000);
        }
    }

    public void close(){
        if(heartTimer != null){
            heartTimer.cancel();
            heartTimer = null;
        }
        if(webSocketClient != null){
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    public boolean isClosed(){
       return webSocketClient.isClosed();
    }

    public boolean isOpen(){
        return webSocketClient.isOpen();
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public interface CallBack{
        void onStart();

        void onReceiveDanmu(String text, float textSize, int textColor, boolean textShadowTransparent, String origin);

        void onReceiveSuperChatMessage(String message, String messageFontColor, String uname, String origin);

        void onReceiveSendGift(String action, String giftName, Integer num, String uname, String origin);

        void onReceiveOtherMessage(String message);

        void onClose(int code, String reason, boolean remote);
    }
}
