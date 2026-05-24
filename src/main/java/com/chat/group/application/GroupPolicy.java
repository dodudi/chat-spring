package com.chat.group.application;

public interface GroupPolicy {

    /** 사용자가 생성 가능한 최대 그룹 수 (기본 그룹 제외). */
    int maxGroupCount();
}
