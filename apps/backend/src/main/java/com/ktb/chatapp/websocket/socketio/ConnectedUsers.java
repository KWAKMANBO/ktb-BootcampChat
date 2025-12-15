package com.ktb.chatapp.websocket.socketio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "socketio.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ConnectedUsers {

    private static final String USER_SOCKET_KEY_PREFIX = "conn_users:userid:";
    private static final String SOCKET_USER_KEY_PREFIX = "conn_users:socketid:";

    private final ChatDataStore chatDataStore;

    /**
     * userId로 사용자 조회
     */
    public SocketUser get(String userId) {
        return chatDataStore.get(buildUserKey(userId), SocketUser.class).orElse(null);
    }

    /**
     * socketId로 사용자 조회 (다중 서버 환경 지원)
     */
    public SocketUser getBySocketId(String socketId) {
        return chatDataStore.get(buildSocketKey(socketId), SocketUser.class).orElse(null);
    }

    /**
     * 사용자 정보 저장 (userId, socketId 양방향 인덱스)
     */
    public void set(String userId, SocketUser socketUser) {
        chatDataStore.set(buildUserKey(userId), socketUser);
        // socketId로도 조회 가능하도록 추가 인덱스 저장
        if (socketUser.socketId() != null) {
            chatDataStore.set(buildSocketKey(socketUser.socketId()), socketUser);
            log.debug("Stored user {} with socketId {} in Redis", userId, socketUser.socketId());
        }
    }

    /**
     * 사용자 정보 삭제 (양방향 인덱스 모두 삭제)
     */
    public void del(String userId) {
        SocketUser user = get(userId);
        if (user != null && user.socketId() != null) {
            chatDataStore.delete(buildSocketKey(user.socketId()));
        }
        chatDataStore.delete(buildUserKey(userId));
    }

    /**
     * socketId로 사용자 정보 삭제
     */
    public void delBySocketId(String socketId) {
        SocketUser user = getBySocketId(socketId);
        if (user != null) {
            chatDataStore.delete(buildUserKey(user.id()));
        }
        chatDataStore.delete(buildSocketKey(socketId));
    }

    public int size() {
        return chatDataStore.size();
    }

    private String buildUserKey(String userId) {
        return USER_SOCKET_KEY_PREFIX + userId;
    }

    private String buildSocketKey(String socketId) {
        return SOCKET_USER_KEY_PREFIX + socketId;
    }
}
