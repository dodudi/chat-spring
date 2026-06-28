package kr.it.rudy.chat.common.websocket;

public final class WebSocketDestination {
    private WebSocketDestination() {
    }

    public static String channel(Long channelId) {
        return "/topic/channels/" + channelId;
    }

    public static String dm(Long channelId) {
        return "/topic/dm/" + channelId;
    }
}
