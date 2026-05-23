# v2 Troubleshooting

v2 브랜치 구현 중 발생한 문제와 해결 방법을 기록한다.

---

## #001 2026-05-24 — `SecurityConfig`에서 `ObjectMapper` 직접 주입 시 빈 누락 문제

**증상**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'ObjectMapper'
```
`SecurityConfig`가 `@RequiredArgsConstructor`로 `ObjectMapper`를 주입받는 구조에서 `@SpringBootTest` 컨텍스트 로딩 실패.

**원인**
`@Configuration` 클래스인 `SecurityConfig`는 초기화 순서가 앞서기 때문에,
`JacksonAutoConfiguration`이 `ObjectMapper` 빈을 아직 등록하지 않은 시점에 주입이 시도돼 실패.

**해결**
두 단계로 처리:

1. `ObjectMapper` 의존성을 `SecurityErrorResponder`(`@Component`)로 분리
   ```java
   @Component
   @RequiredArgsConstructor
   public class SecurityErrorResponder {
       private final ObjectMapper objectMapper;
   
       public void writeUnauthorized(HttpServletResponse response) throws IOException { ... }
       public void writeForbidden(HttpServletResponse response) throws IOException { ... }
   }
   ```
   `SecurityConfig`는 `SecurityErrorResponder`만 주입받고 `ObjectMapper`를 직접 참조하지 않는다.

2. `JacksonConfig`로 `ObjectMapper` 빈 명시 등록 (→ #002 참고)
   `@Component` 레벨에서도 `ObjectMapper`가 없으면 여전히 실패하므로 필수.

**패턴**
Security 설정 클래스에서 직렬화 관련 의존성이 필요하면 전용 클래스로 분리한다.
`@Configuration`은 가능한 한 프레임워크 빈만 의존하도록 유지한다.

---

## #002 2026-05-24 — Spring Boot 4.x / Jackson 3.x — `spring.jackson.serialization.*` YAML 바인딩 실패

**증상**
```
BindException: Failed to bind properties under 'spring.jackson.serialization'
  to java.util.Map<tools.jackson.databind.SerializationFeature, java.lang.Boolean>
Caused by: No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```
`application.yml`에 `spring.jackson.serialization.write-dates-as-timestamps: false` 설정 시 컨텍스트 로딩 실패.

**원인**
Spring Boot 4.x는 Jackson 3.x(`tools.jackson` 패키지)를 사용한다.
Jackson 3.x에서 `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`가 제거됐고,
`JacksonProperties` 바인딩 타입이 `Map<tools.jackson.databind.SerializationFeature, Boolean>`으로 변경됐다.
`LenientObjectToEnumConverterFactory`가 `write-dates-as-timestamps` → `WRITE_DATES_AS_TIMESTAMPS` 변환을 시도하지만
해당 상수가 존재하지 않아 `IllegalArgumentException` 발생.

**해결**
`application.yml`에서 해당 항목을 제거하고 `JacksonConfig` 빈에서 직접 설정:
```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();  // com.fasterxml.jackson (shim)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

**주의**
`com.fasterxml.jackson` 패키지는 Jackson 3.x에서 하위 호환 shim으로 남아 있어 컴파일은 통과한다.
YAML 프로퍼티 바인딩만 `tools.jackson`을 직접 참조하므로 충돌이 발생하는 것이다.
`spring.jackson.*` 프로퍼티 대신 `JacksonConfig` `@Bean`으로 `ObjectMapper`를 직접 구성하는 방식이 Spring Boot 4.x에서 안전하다.
