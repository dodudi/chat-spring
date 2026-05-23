package com.chat.user.application;

public interface UserInitializer {

    /**
     * 최초 API 호출 시 users upsert + 기본 프로필 + 기본 그룹 생성. 멱등.
     * HandlerInterceptor에서 인증된 모든 요청마다 호출된다.
     */
    void initUser(String userId);
}
