package com.chat.room.application;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

@Component
public class DefaultRoomKeyCreator implements RoomKeyCreator {

    @Override
    public String createDmRoomKey(String userId1, String userId2) {
        String[] users = {userId1, userId2};
        Arrays.sort(users);
        String combined = users[0] + ":" + users[1];
        return "dm:" + sha256(combined);
    }

    @Override
    public String createGroupRoomKey(String userId) {
        return "group:" + userId;
    }

    @Override
    public String createPublicRoomKey(String userId) {
        return "public:" + userId;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
