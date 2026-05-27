# Testing

## 테스트 파일 위치 및 네이밍

```
src/
└── test/
    └── java/com/{company}/{project}/
        └── domain/
            └── {domain}/
                ├── controller/
                │   └── UserControllerTest.java
                ├── service/
                │   └── UserServiceTest.java
                └── repository/
                    └── UserRepositoryTest.java
```

- 테스트 클래스명: `{대상클래스명}Test`
- 테스트 메서드명: `{메서드명}_{상황}_{기대결과}` (한글도 허용)

```java
// 영문
void getUser_whenUserExists_returnsUser()

// 한글 (가독성 우선)
void 사용자_조회_성공()
void 존재하지_않는_사용자_조회시_예외발생()
```

## 테스트 종류와 기준

### 단위 테스트 (Service)

- 외부 의존성은 Mockito로 Mock 처리
- 비즈니스 로직의 분기 케이스를 모두 커버
- `@ExtendWith(MockitoExtension.class)` 사용

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Test
    void 존재하지_않는_사용자_조회시_예외발생() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
```

### 통합 테스트 (Controller)

- `@SpringBootTest` + `MockMvc` 또는 `@WebMvcTest` 사용
- 실제 HTTP 요청/응답 흐름 검증
- 인증이 필요한 경우 `@WithMockUser` 또는 테스트용 토큰 사용

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void 사용자_조회_성공() throws Exception {
        // given
        given(userService.getUser(1L)).willReturn(UserResponse.builder()...build());

        // when & then
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

### Repository 테스트

- `@DataJpaTest` 사용 (인메모리 DB)
- 커스텀 쿼리 메서드, JPQL만 테스트
- 기본 CRUD는 테스트 생략

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 이메일로_사용자_조회() {
        // given
        userRepository.save(User.builder().email("test@example.com").build());

        // when
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // then
        assertThat(result).isPresent();
    }
}
```

## 테스트 작성 원칙

- **given / when / then** 구조 필수 주석으로 명시
- 테스트 간 독립성 유지 (테스트 순서에 의존하지 않음)
- `@Transactional` 을 테스트 클래스에 붙여 롤백 처리
- 픽스처(공통 테스트 데이터)는 별도 팩토리 메서드로 분리

```java
// 테스트 픽스처 예시
private User createUser() {
    return User.builder()
            .name("테스트유저")
            .email("test@example.com")
            .build();
}
```

## 반드시 테스트할 것

- 비즈니스 예외 발생 케이스 (존재하지 않는 리소스, 중복 등)
- 입력값 유효성 검증 실패 케이스
- 핵심 비즈니스 로직의 정상/비정상 케이스

## 테스트 생략 가능한 것

- 단순 getter/setter
- JpaRepository 기본 CRUD
- 설정 클래스 (Config)