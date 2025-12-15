package com.ktb.chatapp.websocket.socketio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Socket User Record
 * @param id user id
 * @param name user name
 * @param authSessionId user auth session id
 * @param socketId user websocket session id
 */
public record SocketUser(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("authSessionId") String authSessionId,
        @JsonProperty("socketId") String socketId
) implements Serializable {

    @JsonCreator
    public SocketUser {
        // Compact constructor for Jackson deserialization
    }
}
