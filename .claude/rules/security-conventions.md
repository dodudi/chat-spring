# Security 인증 규칙

이 파일은 Security 설정과 인증 방식 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 인증 구조 개요

이 애플리케이션은 두 가지 OAuth2 흐름을 동시에 사용한다.

| 흐름 | Registration ID | 용도 |
|------|----------------|------|
| Authorization Code + OIDC | `auth-server` | 관리자 브라우저 로그인 |
| Client Credentials | `m2m-client` | 인증 서버 관리 API 호출 (서버 간) |

외부 인증 서버(Spring Authorization Server)가 토큰을 발급한다.
이 앱은 Authorization Server를 **운영**하지 않고 **클라이언트**로서 연동한다.

---

## 관리자 로그인 — Authorization Code + OIDC

브라우저 사용자는 OAuth2 Authorization Code 흐름으로 로그인한다.

```
브라우저 → /oauth2/authorization/auth-server
        → 인증 서버 로그인 페이지
        → 콜백 /login/oauth2/code/auth-server
        → 세션 저장 → 애플리케이션 접근
```

### SecurityConfig 설정 원칙

```java
// ✅ 올바른 예
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                )
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );
        return http.build();
    }
}
```

- `permitAll()` 대상: `/actuator/**`(헬스·메트릭), `/h2-console/**`(로컬 개발)만 허용
- 나머지 모든 경로는 `authenticated()` — 명시적 allowlist 방식
- `csrf().disable()`: 관리자 내부망 전용 앱이므로 비활성화. 외부 공개 서비스 전환 시 반드시 활성화
- `frameOptions().sameOrigin()`: H2 콘솔 iframe 허용 (로컬 전용)

### OIDC 로그아웃

로그아웃 시 인증 서버의 end_session_endpoint로 리다이렉트하여 SSO 세션까지 종료한다.

```java
private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    handler.setPostLogoutRedirectUri("{baseUrl}");  // 로그아웃 후 앱 루트로 복귀
    return handler;
}
```

- `{baseUrl}`은 Spring Security가 런타임에 현재 서버 주소로 치환한다.
- 하드코딩 URL 사용 금지 — 포트·도메인 변경 시 수동 수정이 필요해짐.

---

## 서버 간 API 호출 — Client Credentials

인증 서버의 관리 API(`/api/clients/**`)를 호출할 때는 Client Credentials 흐름으로 액세스 토큰을 발급받아 Bearer 헤더에 실어 보낸다.

```
AuthServerClientConfig (RestClient)
    → OAuth2AuthorizedClientManager.authorize()
    → 캐싱된 토큰이 없거나 만료 시 인증 서버에 토큰 요청
    → Authorization: Bearer {token}
    → 인증 서버 관리 API
```

### 설정 위치

M2M 관련 빈은 `client/config/AuthServerClientConfig`에 둔다.
`common/config/SecurityConfig`에 혼재시키지 않는다.

```java
// ✅ 올바른 예
@Bean
public OAuth2AuthorizedClientManager m2mAuthorizedClientManager(...) {
    AuthorizedClientServiceOAuth2AuthorizedClientManager manager = ...;
    manager.setAuthorizedClientProvider(
            OAuth2AuthorizedClientProviderBuilder.builder()
                    .clientCredentials()
                    .build()
    );
    return manager;
}

@Bean
public RestClient authServerRestClient(OAuth2AuthorizedClientManager m2mAuthorizedClientManager) {
    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor((request, body, execution) -> {
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                        .withClientRegistrationId("m2m-client")
                        .principal("m2m-client")
                        .build();
                OAuth2AuthorizedClient client = m2mAuthorizedClientManager.authorize(authorizeRequest);
                if (client != null && client.getAccessToken() != null) {
                    request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
                }
                return execution.execute(request, body);
            })
            .build();
}
```

- `AuthorizedClientServiceOAuth2AuthorizedClientManager` 사용: 요청 컨텍스트(HttpServletRequest) 없이 토큰을 관리하기 위해 사용. 서버 간 호출에서는 `DefaultOAuth2AuthorizedClientManager` 대신 이 구현체를 써야 한다.
- 토큰은 `OAuth2AuthorizedClientService`가 인메모리로 캐싱한다. 만료 시 자동 재발급.

### 인증 서버 API 호출

```java
// ✅ 올바른 예
@Service
@RequiredArgsConstructor
public class AuthServerClientService {

    private final RestClient authServerRestClient;

    public ClientDetail getDetail(String id) {
        AuthApiResponse<ClientDetail> response = authServerRestClient.get()
                .uri("/api/clients/{id}", id)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response != null ? response.data() : null;
    }
}

// ❌ 직접 RestClient.builder()로 생성 금지 — 토큰이 주입되지 않음
RestClient.builder().baseUrl(...).build().get()...
```

---

## application.yaml 등록 구조

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          auth-server:                          # 브라우저 로그인용
            client-id: local-admin-server
            client-secret: ${ADMIN_CLIENT_SECRET:change-me}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid, profile
          m2m-client:                           # 서버 간 API 호출용
            client-id: local-m2m-client
            client-secret: ${M2M_CLIENT_SECRET:local-m2m-secret}
            authorization-grant-type: client_credentials
            scope: client:manage
            provider: auth-server
        provider:
          auth-server:
            issuer-uri: http://localhost:8080   # OIDC 메타데이터 자동 조회
```

- `client-secret`은 환경 변수로만 주입한다. 평문을 yaml에 하드코딩하지 않는다.
- `issuer-uri`를 지정하면 Spring Security가 `/.well-known/openid-configuration`을 조회해 엔드포인트를 자동으로 구성한다.
- `m2m-client`에 `provider: auth-server`를 명시해야 인증 서버 엔드포인트를 찾을 수 있다.

---

## 금지 사항

```java
// ❌ @RestController 경로를 permitAll() 대신 인증 없이 방치
.anyRequest().permitAll()

// ❌ 로그아웃 후 리다이렉트 URL 하드코딩
handler.setPostLogoutRedirectUri("http://localhost:8090");

// ❌ M2M RestClient를 SecurityConfig에서 선언
// → common/config는 인프라 공통 설정만, 도메인 설정은 해당 도메인 config에

// ❌ authServerRestClient 대신 직접 RestClient 생성으로 인증 서버 호출
// → Bearer 토큰이 누락되어 401 응답
```
