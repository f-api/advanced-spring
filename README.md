# SPRING ADVANCED

## API 명세서 (프론트엔드용, 현재 구현 기준)

### Auth (인증)

#### 1. 회원가입

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/auth/signup` |
| 인증 | ❌ |

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password1!",
  "userRole": "USER"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| email | String | ✅ | 이메일 형식 |
| password | String | ✅ | 비밀번호 |
| userRole | String | ✅ | `USER` 또는 `ADMIN` |

**Response** `200 OK`

```json
{ "bearerToken": "Bearer eyJhbGciOi..." }
```

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `이미 존재하는 이메일입니다.` | 이메일 중복 |
| `400` | `유효하지 않은 UerRole` | `userRole` 값이 `USER`/`ADMIN` 이 아님 |

> `email`, `password`, `userRole` 누락/형식 오류는 현재 Spring Validation 기본 `400` 응답으로 내려오며, 아래 공통 에러 포맷이 고정적으로 보장되지는 않습니다.

---

#### 2. 로그인

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/auth/signin` |
| 인증 | ❌ |

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password1!"
}
```

**Response** `200 OK`

```json
{ "bearerToken": "Bearer eyJhbGciOi..." }
```

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `가입되지 않은 유저입니다.` | 미등록 이메일 |
| `401` | `잘못된 비밀번호입니다.` | 비밀번호 불일치 |

> `email`, `password` 누락/형식 오류는 현재 Spring Validation 기본 `400` 응답으로 내려오며, 아래 공통 에러 포맷이 고정적으로 보장되지는 않습니다.

---

### User (사용자)

#### 3. 사용자 단건 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/users/{userId}` |
| 인증 | ✅ |

**Response** `200 OK`

```json
{
  "id": 1,
  "email": "user@example.com"
}
```

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `User not found` | 존재하지 않는 사용자 |

---

#### 4. 비밀번호 변경

| 항목 | 값 |
|---|---|
| Method | `PUT` |
| Path | `/users` |
| 인증 | ✅ |

**Request Body**

```json
{
  "oldPassword": "oldPass1!",
  "newPassword": "newPass1!"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| oldPassword | String | 현재 서버는 별도 형식 검증 없이 비교만 수행 |
| newPassword | String | 현재 서버는 길이/숫자/대문자 규칙을 검증하지 않음. 기존 비밀번호와 달라야 함 |

**Response** `200 OK` (본문 없음)

> 현재 구현은 이 엔드포인트에 `@Valid`가 적용되어 있지 않아, `newPassword`의 길이/숫자/대문자 규칙을 서버가 검사하지 않습니다. 프론트엔드에서 별도 검증이 필요합니다.

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `User not found` | 토큰의 사용자 ID 가 DB 에 없음 (계정 삭제 등) |
| `400` | `새 비밀번호는 기존 비밀번호와 같을 수 없습니다.` | 기존 비밀번호와 동일 |
| `400` | `잘못된 비밀번호입니다.` | `oldPassword` 불일치 |

---

### Todo (일정)

#### 5. 일정 생성

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/todos` |
| 인증 | ✅ |

**Request Body**

```json
{
  "title": "할 일 제목",
  "contents": "할 일 내용"
}
```

**Response** `200 OK`

```json
{
  "id": 1,
  "title": "할 일 제목",
  "contents": "할 일 내용",
  "weather": "Sunny",
  "user": { "id": 1, "email": "user@example.com" }
}
```

> 생성 시 외부 Weather API(`https://f-api.github.io/f-api/weather.json`) 를 호출해 오늘의 날씨 정보를 함께 저장합니다.
> `title`, `contents` 누락/blank 는 현재 Spring Validation 기본 `400` 응답으로 내려오며, 공통 에러 포맷이 고정적으로 보장되지는 않습니다.

**Error**

| Status | Message | 상황 |
|---|---|---|
| `500` | `날씨 데이터를 가져오는데 실패했습니다. 상태 코드: {code}` | 외부 날씨 API 가 비정상 응답 |
| `500` | `날씨 데이터가 없습니다.` | 외부 날씨 API 응답이 비어 있음 |
| `500` | `오늘에 해당하는 날씨 데이터를 찾을 수 없습니다.` | 오늘 날짜(MM-dd) 데이터 부재 |

---

#### 6. 일정 목록 조회 (페이징)

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/todos` |
| 인증 | ✅ |

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| page | int | 1 | 페이지 번호 (1부터 시작) |
| size | int | 10 | 페이지 크기 |

> 정렬: `modifiedAt` 기준 내림차순 고정 (최근 수정된 일정이 먼저 나옵니다)
> `page`, `size` 는 그대로 `PageRequest.of(page - 1, size)` 에 전달됩니다. `page < 1` 또는 `size < 1` 같은 값은 별도 커스텀 검증이 없습니다.

**Response** `200 OK` (Spring `Page<TodoResponse>`)

```json
{
  "content": [
    {
      "id": 1,
      "title": "할 일 제목",
      "contents": "할 일 내용",
      "weather": "Sunny",
      "user": { "id": 1, "email": "user@example.com" },
      "createdAt": "2026-04-13T10:00:00",
      "modifiedAt": "2026-04-13T10:00:00"
    }
  ],
  "pageable": { "...": "..." },
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### 7. 일정 단건 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/todos/{todoId}` |
| 인증 | ✅ |

**Response** `200 OK`

```json
{
  "id": 1,
  "title": "할 일 제목",
  "contents": "할 일 내용",
  "weather": "Sunny",
  "user": { "id": 1, "email": "user@example.com" },
  "createdAt": "2026-04-13T10:00:00",
  "modifiedAt": "2026-04-13T10:00:00"
}
```

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `Todo not found` | 존재하지 않는 일정 |

---

### Comment (댓글)

#### 8. 댓글 작성

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/todos/{todoId}/comments` |
| 인증 | ✅ |

**Request Body**

```json
{ "contents": "댓글 내용입니다." }
```

**Response** `200 OK`

```json
{
  "id": 1,
  "contents": "댓글 내용입니다.",
  "user": { "id": 1, "email": "user@example.com" }
}
```

> `contents` 누락/blank 는 현재 Spring Validation 기본 `400` 응답으로 내려오며, 공통 에러 포맷이 고정적으로 보장되지는 않습니다.

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `Todo not found` | 존재하지 않는 일정 |

---

#### 9. 댓글 목록 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/todos/{todoId}/comments` |
| 인증 | ✅ |

**Response** `200 OK`

```json
[
  {
    "id": 1,
    "contents": "댓글 내용입니다.",
    "user": { "id": 1, "email": "user@example.com" }
  }
]
```

> 현재 구현은 `todoId` 존재 여부를 별도로 검증하지 않습니다. 존재하지 않는 일정 ID 여도 빈 배열(`[]`)을 반환할 수 있습니다.

---

### Manager (담당자)

#### 10. 담당자 등록

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Path | `/todos/{todoId}/managers` |
| 인증 | ✅ (일정 작성자만 가능) |

**Request Body**

```json
{ "managerUserId": 2 }
```

**Response** `200 OK`

```json
{
  "id": 1,
  "user": { "id": 2, "email": "manager@example.com" }
}
```

> `managerUserId` 누락은 현재 Spring Validation 기본 `400` 응답으로 내려오며, 공통 에러 포맷이 고정적으로 보장되지는 않습니다.
> 현재 구현은 동일한 사용자를 중복 담당자로 등록하는 것을 막지 않습니다.

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `Todo not found` | 존재하지 않는 일정 |
| `400` | `일정을 생성한 유저만 담당자를 지정할 수 있습니다.` | 일정 작성자가 아님 |
| `400` | `등록하려고 하는 담당자 유저가 존재하지 않습니다.` | 존재하지 않는 사용자 |
| `400` | `일정 작성자는 본인을 담당자로 등록할 수 없습니다.` | 본인을 담당자로 지정 |

---

#### 11. 담당자 목록 조회

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/todos/{todoId}/managers` |
| 인증 | ✅ |

**Response** `200 OK`

```json
[
  {
    "id": 1,
    "user": { "id": 1, "email": "user@example.com" }
  },
  {
    "id": 2,
    "user": { "id": 2, "email": "manager@example.com" }
  }
]
```

> 일정 생성 시 작성자는 `managers` 테이블에 기본 담당자로 자동 등록됩니다. 따라서 새 일정의 담당자 목록은 비어 있지 않을 수 있습니다.

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `Todo not found` | 존재하지 않는 일정 |

---

#### 12. 담당자 삭제

| 항목 | 값 |
|---|---|
| Method | `DELETE` |
| Path | `/todos/{todoId}/managers/{managerId}` |
| 인증 | ✅ (일정 작성자만 가능) |

**Response** `200 OK` (본문 없음)

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `User not found` | 요청 유저 없음 |
| `400` | `Todo not found` | 존재하지 않는 일정 |
| `400` | `Manager not found` | 존재하지 않는 담당자 |
| `400` | `해당 일정을 만든 유저가 유효하지 않습니다.` | 일정 작성자가 아님 |
| `400` | `해당 일정에 등록된 담당자가 아닙니다.` | 다른 일정의 담당자 ID |

---

### Admin (관리자)

> `/admin/**` 경로는 **ADMIN 권한**이 필요합니다. 일반 사용자가 접근하면 `403` 을 반환합니다.

#### 13. 사용자 권한 변경

| 항목 | 값 |
|---|---|
| Method | `PATCH` |
| Path | `/admin/users/{userId}` |
| 인증 | ✅ (ADMIN) |

**Request Body**

```json
{ "role": "ADMIN" }
```

**Response** `200 OK` (본문 없음)

**Error**

| Status | Message | 상황 |
|---|---|---|
| `400` | `User not found` | 존재하지 않는 사용자 |
| `400` | `유효하지 않은 UerRole` | `role` 값이 `USER`/`ADMIN` 이 아님 |
| `403` | `접근 권한이 없습니다.` | ADMIN 이 아닌 사용자 접근 |

---

#### 14. 댓글 삭제

| 항목 | 값 |
|---|---|
| Method | `DELETE` |
| Path | `/admin/comments/{commentId}` |
| 인증 | ✅ (ADMIN) |

**Response** `200 OK` (본문 없음)

> 존재하지 않는 `commentId` 를 보내도 `200 OK` 를 반환합니다 (멱등).

**Error**

| Status | Message | 상황 |
|---|---|---|
| `403` | `접근 권한이 없습니다.` | ADMIN 이 아닌 사용자 접근 |

---

## ✅ 공통 사항

### 인증 헤더

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy
```

> 현재 구현은 `Authorization` 헤더의 `Bearer ` 접두사가 빠지거나 형식이 잘못된 경우, 안정적인 공통 JSON 에러 포맷을 보장하지 않습니다. 프론트엔드는 반드시 정확한 `Bearer {JWT}` 형식으로 전송해야 합니다.

### 커스텀 에러 응답

| HTTP Status | 상황 |
|---|---|
| `400 Bad Request` | 비즈니스 규칙 위반, 일부 JWT 오류 |
| `401 Unauthorized` | 토큰 누락 / 만료 / 로그인 비밀번호 불일치 |
| `403 Forbidden` | 관리자 전용 API 에 일반 사용자가 접근 |
| `500 Internal Server Error` | 서버 내부 오류 |

**에러 응답 형식**

```json
{
  "status": "BAD_REQUEST",
  "code": 400,
  "message": "에러 메시지"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| status | String | HTTP 상태명 (`BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL_SERVER_ERROR`) |
| code | int | HTTP 상태 코드 숫자 |
| message | String | 사용자에게 노출 가능한 에러 메시지 |

### Spring Boot 기본 오류 응답이 내려올 수 있는 경우

다음 경우에는 위 커스텀 에러 포맷 대신 Spring Boot 기본 `400` 오류 응답이 내려올 수 있습니다.

- `@Valid` 가 적용된 요청 DTO 검증 실패
- 일부 서블릿/필터 단계 예외

영향 범위가 확인된 대표 API 는 아래와 같습니다.

- `POST /auth/signup`
- `POST /auth/signin`
- `POST /todos`
- `POST /todos/{todoId}/comments`
- `POST /todos/{todoId}/managers`

프론트엔드는 이 케이스에서 오류 응답 바디 스키마를 강하게 가정하지 말고, 우선 HTTP 상태 코드를 기준으로 처리하는 편이 안전합니다.

## ERD

![ERD](docs/erd.png)
