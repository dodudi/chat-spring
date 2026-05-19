# 테스트 규칙

## 어노테이션 선택 기준

| 어노테이션 | 용도 |
|-----------|------|
| `@ExtendWith(MockitoExtension.class)` | Service 단위 테스트. Spring 컨텍스트 없음, 가장 빠름 |
| `@WebMvcTest` | Controller 단위 테스트. Web 레이어만 로딩 |
| `@DataJpaTest` | Repository 단위 테스트. H2 인메모리 DB |
| `@SpringBootTest` | 통합 테스트. 꼭 필요할 때만 |

## Mock 어노테이션

| 어노테이션 | 사용 위치 |
|-----------|----------|
| `@Mock` | `@ExtendWith(MockitoExtension.class)` |
| `@MockitoBean` | `@WebMvcTest`, `@SpringBootTest` |

`@WebMvcTest`에서 `@Mock`을 쓰면 Spring 빈으로 등록되지 않아 NPE 발생.

## Spring Boot 4.x 주의사항

- `@WebMvcTest` import: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- `@MockBean` 제거됨 → `@MockitoBean` 사용
- `ObjectMapper`는 `@WebMvcTest` slice에서 자동 등록되지 않음 → `new ObjectMapper()` 직접 생성
- `@WithMockUser`는 `Jwt` 타입 principal이 필요한 컨트롤러에서 null 반환 → `SecurityMockMvcRequestPostProcessors.jwt()` 사용

## 테스트 메서드 네이밍

`메서드명_상황_기대결과` 형식으로 작성한다.

```java
// ✅
void findById_존재하는_사용자_조회시_UserResponse_반환()
void findById_존재하지_않는_id_조회시_AppException_발생()

// ❌
void testFindById()
void test1()
```

## 테스트 구조

`// given / when / then` 주석으로 단계를 구분한다.

```java
@Test
void findById_존재하는_채팅방_조회시_RoomSummaryResponse_반환() {
    // given
    given(roomRepository.findById(1L)).willReturn(Optional.of(room));

    // when
    RoomSummaryResponse response = roomService.findById(1L);

    // then
    assertThat(response.id()).isEqualTo(1L);
}
```

## AssertJ

JUnit `assertEquals` 대신 AssertJ `assertThat`을 사용한다.

```java
// ✅
assertThat(response.name()).isEqualTo("테스트방");
assertThatThrownBy(() -> roomService.findById(99L)).isInstanceOf(AppException.class);

// ❌
assertEquals("테스트방", response.name());
```

## 테스트 클래스 위치

대상 클래스와 **동일한 패키지**에 위치시킨다.

```
src/main/java/com/chat/room/application/RoomServiceImpl.java
src/test/java/com/chat/room/application/RoomServiceImplTest.java  ✅
```
