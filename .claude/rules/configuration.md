# 환경 설정 규칙

## 파일 구조

```
src/main/resources/
├── application.yml                  # 공통 기본값 (모든 환경 적용)
├── application-local.yml            # 로컬 개발용 — gitignore 대상
├── application-local.yml.example    # 로컬 설정 템플릿 — 커밋 대상
└── application-prod.yml             # 운영용 — 민감 정보는 환경변수로만
```

## 설정 배치 기준

| 조건 | 파일 |
|------|------|
| 모든 환경에서 동일, 민감하지 않음 | `application.yml` |
| 로컬 전용 | `application-local.yml` (+ example 동기화) |
| 운영 민감 정보 | `application-prod.yml` (`${ENV_VAR}` 사용) |

`application.yml`에 `spring.profiles.active`를 고정하지 않는다.

## 운영 환경변수 목록

| 환경변수 | 설명 |
|----------|------|
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `JWT_ISSUER_URI` | OAuth2 JWT 발급자 URI |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 origin 목록 (쉼표 구분, 미설정 시 `*`) |

새 환경변수 추가 시 이 표를 함께 갱신한다.

## 금지 사항

- `application-local.yml` git 커밋 금지
- `application-prod.yml`에 실제 비밀번호·토큰 하드코딩 금지
- 필수 환경변수 누락 시 기동 실패가 올바른 동작
