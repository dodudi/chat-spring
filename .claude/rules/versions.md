# 기술 스택 버전

버전 변경 시 이 파일을 반드시 갱신한다.

## 런타임

| 항목 | 버전 | 출처 |
|------|------|------|
| Java | 21 | `build.gradle` — `JavaLanguageVersion.of(21)` |
| Gradle | 9.4.1 | `gradle-wrapper.properties` |

## 프레임워크 (Spring Boot BOM 4.0.6 관리)

| 항목 | 버전 |
|------|------|
| Spring Boot | 4.0.6 |
| Spring Framework | 7.0.7 |
| Spring Security | 7.0.5 |
| Spring Data Redis | 4.0.5 |

## 퍼시스턴스 (Spring Boot BOM 관리)

| 항목 | 버전 |
|------|------|
| Hibernate ORM | 7.2.12.Final |
| Hibernate Validator | 9.0.1.Final |
| Jakarta Validation API | 3.1.1 |
| Flyway | 11.14.1 |
| PostgreSQL Driver | 42.7.10 |
| H2 (테스트용) | 2.4.240 |

## 인프라 / 클라이언트 (Spring Boot BOM 관리)

| 항목 | 버전 |
|------|------|
| Tomcat Embedded | 11.0.21 |
| Lettuce (Redis 클라이언트) | 6.8.2.RELEASE |
| Jackson | 2.21.2 |

## 모니터링 / 문서

| 항목 | 버전 | 출처 |
|------|------|------|
| Micrometer | 1.16.5 | Spring Boot BOM 관리 |
| Micrometer Registry Prometheus | 1.16.5 | Spring Boot BOM 관리 |
| Springdoc OpenAPI | 3.0.2 | `build.gradle` — 명시적 버전 |

## 유틸리티

| 항목 | 버전 | 출처 |
|------|------|------|
| Lombok | 1.18.46 | Spring Boot BOM 관리 |
