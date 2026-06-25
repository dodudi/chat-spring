# Controller 규칙

이 파일은 Controller 작성 시 항상 따라야 할 규칙을 정의한다.

---

## 컨트롤러 종류 구분

| 어노테이션 | 용도 | 응답 |
|-----------|------|------|
| `@RestController` | REST API | JSON |
| `@Controller` | Thymeleaf 뷰 렌더링 | HTML |

---

## @RestController — REST API

클래스 선언부에 `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor` 세 어노테이션을 함께 사용한다.
의존성은 `private final` 필드로만 선언한다.

```java
// ✅ 올바른 예
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }
}
```

```java
// ❌ 잘못된 예
public class UserApiController {

    @Autowired                       // 필드 주입 금지
    private UserService userService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {   // Entity 직접 반환
        return userRepository.findById(id).get();  // Repository 직접 호출
    }
}
```

---

## @Controller — Thymeleaf 뷰

`@Controller`는 뷰 이름(String)을 반환하며, 데이터는 `Model`에 담아 전달한다.

```java
// ✅ 올바른 예
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/user/list";   // templates/admin/user/list.html
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "admin/user/detail";
    }
}
```

```java
// ❌ 잘못된 예
@Controller
@RequestMapping("/admin/users")
public class UserController {

    @GetMapping
    @ResponseBody                    // @Controller에 @ResponseBody 혼용 금지 — @RestController 사용
    public List<User> list() { ... }
}
```
