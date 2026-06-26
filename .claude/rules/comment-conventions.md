# 주석 규칙

이 파일은 인라인 주석과 Javadoc 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 기본 원칙

주석은 기본적으로 작성하지 않는다.
잘 지어진 이름이 WHAT을 설명하므로, 주석은 이름만으로 전달할 수 없는 **WHY**만 담는다.

```java
// ✅ 올바른 예 — 이름으로 설명되지 않는 이유
// proceed() 이전에 캡처 — 실행 중 객체 상태가 바뀌어도 진입 시점 값을 기록
String args = formatArgs(method, joinPoint.getArgs());

// ❌ 잘못된 예 — 코드가 이미 설명하고 있음
// 사용자를 조회한다
User user = userRepository.findById(id);

// ❌ 잘못된 예 — 작업 맥락은 주석이 아닌 커밋 메시지에 담는다
// 로그인 흐름에서 호출됨
public TokenResponse login(LoginRequest request) { ... }
```

---

## 인라인 주석 vs Javadoc

| 종류 | 위치 | 사용 시점 |
|------|------|----------|
| 인라인 주석 `//` | 메서드 **내부** | 특정 코드 라인의 WHY가 비명확할 때 |
| Javadoc `/** */` | 메서드 **외부** | 메서드 전체의 설계 의도·제약이 비명확할 때 |

둘 다 필요하면 각자 위치에 작성한다. 인라인 주석을 메서드 외부에, Javadoc을 메서드 내부에 쓰지 않는다.

---

## 인라인 주석

메서드 내부에서 특정 라인의 이유가 비명확할 때만 작성한다.

작성 기준 — 아래 중 하나에 해당할 때:
- 숨겨진 제약 (외부 시스템, 프레임워크 버그 회피 등)
- 직관에 반하는 구현 선택 (더 단순해 보이는 방법을 쓰지 않은 이유)
- 미래 독자가 "왜 이렇게 했지?" 하고 바꾸려 할 만한 코드

```java
// ✅ 올바른 예 — 직관에 반하는 선택의 이유
// 스트림 대신 인덱스 루프 — args[i]와 paramAnnotations[i]를 동시에 참조해야 함
for (int i = 0; i < args.length; i++) { ... }

// ✅ 올바른 예 — 프레임워크 제약
// Thymeleaf 3.1+: th:onclick은 숫자·불리언만 허용 — 문자열은 data-* 속성으로 전달
<button th:data-id="${user.id}" onclick="openModal(this.dataset.id)">

// ❌ 잘못된 예 — WHAT 설명
// 리스트를 순회한다
for (int i = 0; i < args.length; i++) { ... }
```

---

## Javadoc

메서드 외부에 `/** */`로 작성한다. 메서드 전체의 설계 의도나 비명확한 제약이 있을 때만 작성한다.

### 형식

한 줄로 끝나면 한 줄로 쓴다. 태그가 필요하거나 설명이 길면 여러 줄로 쓴다.

```java
// ✅ 한 줄 — 설명이 짧고 태그가 없을 때
/** @see Masked */

// ✅ 여러 줄 — 설명이 있거나 태그가 붙을 때
/**
 * 설명 문장.
 * 이어지는 설명은 * 뒤 한 칸 띄고 작성한다.
 *
 * @param  name   설명
 * @return        설명
 * @throws AuthException 조건 (ErrorCode)
 */
```

태그 순서는 `@param` → `@return` → `@throws` → `@see` 순으로 작성한다.
태그가 여러 개일 때는 태그 블록 앞에 빈 줄을 하나 둔다.

```java
// ❌ 잘못된 예 — /** 와 */ 를 본문과 같은 줄에 혼용
/** 설명 문장.
 * 이어지는 설명
 */

// ❌ 잘못된 예 — 태그 순서 뒤섞임
/**
 * @throws AuthException 조건
 * @param registrationId 설명
 * @return 설명
 */
```

### 작성 기준

아래 중 하나에 해당할 때만 Javadoc을 작성한다:
- **설계 의도**: 왜 이 메서드가 이런 방식으로 동작하는지
- **숨겨진 제약**: 호출 순서, 사이드 이펙트, 스레드 안전성 등
- **연관 타입 링크**: `@see`로 관련 클래스·어노테이션을 연결

```java
// ✅ 올바른 예 — 설계 의도 + 제약
/**
 * args는 proceed() 이전에 캡처하므로 실행 중 객체 상태가 바뀌어도 진입 시점 값을 기록한다.
 * 인덱스 루프를 사용하는 이유: args[i]와 paramAnnotations[i]를 동시에 참조해야 하기 때문이다.
 */
private String formatArgs(Method method, Object[] args) { ... }

// ✅ 올바른 예 — @see로 연관 타입 링크
/** @see Masked */
private boolean isMasked(Annotation[] annotations) { ... }

// ❌ 잘못된 예 — 메서드 이름과 동일한 설명
/**
 * 프레임워크 객체인지 확인한다.
 */
private boolean isFrameworkObject(Object arg) { ... }

// ❌ 잘못된 예 — 파라미터·반환값 나열 (자명할 때)
/**
 * @param id 사용자 ID
 * @return 사용자 응답
 */
public UserResponse findById(Long id) { ... }
```

### @param / @return / @throws 사용 기준

자명한 파라미터·반환값에는 달지 않는다.
비명확한 계약이 있을 때만 작성한다.

```java
// ✅ 올바른 예 — 비명확한 계약
/**
 * @param registrationId application.yaml에 등록된 OAuth2 클라이언트 ID
 * @throws AuthException 클라이언트가 등록되지 않은 경우 (CL001)
 */
public ClientDetail getDetail(String registrationId) { ... }
```

---

## AOP 어드바이스 메서드

`@Around`, `@Before`, `@AfterThrowing` 어드바이스 메서드에는 Javadoc을 달지 않는다.
어드바이스 타입과 Pointcut 표현식이 이미 역할과 적용 범위를 설명하기 때문이다.

```java
// ✅ 올바른 예 — Javadoc 없음
@Around("@within(org.springframework.web.bind.annotation.RestController)")
public Object logController(ProceedingJoinPoint joinPoint) throws Throwable { ... }

// ❌ 잘못된 예 — 어드바이스가 이미 설명하는 내용을 중복 작성
/**
 * RestController 메서드 실행 전후에 로그를 남긴다.
 */
@Around("@within(org.springframework.web.bind.annotation.RestController)")
public Object logController(ProceedingJoinPoint joinPoint) throws Throwable { ... }
```

---

## 금지 사항

```java
// ❌ WHAT 주석 — 코드가 이미 설명함
// 사용자를 저장한다
userRepository.save(user);

// ❌ 작업·PR 맥락 주석 — 커밋 메시지에 남긴다
// 이슈 #123 대응으로 추가
private static final int MAX_RETRY = 3;

// ❌ 제거한 코드를 주석으로 보존 — git history로 확인
// userRepository.deleteById(id);
userRepository.softDelete(id);

// ❌ TODO/FIXME 방치 — 해결하거나 이슈로 등록한다
// TODO: 나중에 캐시 추가
public User findById(Long id) { ... }
```
