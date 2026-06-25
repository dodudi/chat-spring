# 로깅 규칙

이 파일은 로그 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 로그 형식

비즈니스 이벤트 로그는 구조화된 태그를 포함한다.

```java
// ✅ 올바른 예
log.info("[USER_SIGNUP] email={}", user.getEmail());
log.info("[LOGIN_SUCCESS] email={}", email);
log.warn("[LOGIN_FAILURE] email={}", email);

// ❌ 잘못된 예
log.info("유저가 가입했습니다: " + user.getEmail());   // 태그 없음, 문자열 연산
log.info("signup success");                            // 식별 불가
```

---

## 로그 레벨 기준

| 레벨 | 기준 |
|------|------|
| `ERROR` | 복구 불가능한 장애 (DB 연결 실패 등) |
| `WARN` | 예상 가능한 비즈니스 예외 (인증 실패, 리소스 없음) |
| `INFO` | 비즈니스 이벤트 (가입, 로그인, 탈퇴) |
| `DEBUG` | 개발 디버깅 전용 — 운영 로그에 남기지 않는다 |

---

## MDC traceId

MDC `traceId`는 `RequestLoggingFilter`가 자동 주입하므로 직접 `MDC.put("traceId", ...)`를 호출하지 않는다.
