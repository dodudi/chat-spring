package kr.it.rudy.chat.common.websocket;

public record WebSocketEvent<T>(String type, T data) {
    public static <T> WebSocketEvent<T> of(String type, T data) {
        return new WebSocketEvent<>(type, data);
    }
}
