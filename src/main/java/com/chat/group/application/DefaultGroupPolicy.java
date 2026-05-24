package com.chat.group.application;

import org.springframework.stereotype.Component;

@Component
public class DefaultGroupPolicy implements GroupPolicy {

    private static final int MAX_GROUP_COUNT = 10;

    @Override
    public int maxGroupCount() {
        return MAX_GROUP_COUNT;
    }
}
