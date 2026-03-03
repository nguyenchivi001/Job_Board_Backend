# Job Board Backend

Hệ thống backend cho ứng dụng tuyển dụng việc làm, xây dựng theo kiến trúc **Microservices** với Spring Boot.

---

## Tính năng nổi bật

- **JWT Authentication** với refresh token rotation và reuse detection — phát hiện token bị đánh cắp, tự động invalidate toàn bộ session
- **Redis** làm cache cho job listings (TTL 10 phút) và lưu trữ refresh token / blacklist
- **RabbitMQ** publish/subscribe event khi đăng tin, nộp đơn, cập nhật trạng thái → notification-service gửi email tự động
- **API Gateway** tập trung: JWT validation, IP-based rate limiting (10 req/s, burst 20), CORS, token blacklist check
- **OpenFeign** giao tiếp inter-service (application-service → job-service)
- **Scheduler** tự động đóng tin tuyển dụng hết hạn mỗi đêm lúc 0:00
- **Unit Tests** 48 test cases với Mockito — không cần Spring context hay infrastructure

---

## Kiến trúc

```
Client
  │
  ▼
┌──────────────────────────────────────────────┐
│               api-gateway :8080              │
│  JwtAuthFilter · RateLimiter · CorsFilter    │
└───────┬──────────┬──────────┬────────────────┘
        │          │          │          │
        ▼          ▼          ▼          ▼
  auth-service  job-service  application-service  profile-service
     :8081        :8082           :8083               :8084
   MySQL+Redis  MySQL+Redis    MySQL+Feign          MySQL
               +RabbitMQ      +RabbitMQ
                    │               │
                    └───────┬───────┘
                            ▼ (events)
                  notification-service :8085
                    RabbitMQ + MailHog
```

**Luồng dữ liệu chính:**
1. Client gửi request → api-gateway (validate JWT, check blacklist, rate limit)
2. Gateway forward kèm header `X-User-Id` + `X-User-Role` → downstream service
3. Downstream service đọc header qua `HeaderAuthFilter` → build `SecurityContext`
4. job-service / application-service publish event → RabbitMQ → notification-service → gửi email

---

## Công nghệ

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Framework | Spring Boot | 4.0.3 |
| Cloud | Spring Cloud | 2025.1.0 |
| Ngôn ngữ | Java | 17 |
| Database | MySQL | 8.0 |
| Cache / Session | Redis | 7 |
| Message Broker | RabbitMQ | 3 |
| Email (dev) | MailHog | latest |
| Auth | JWT (JJWT) | 0.11.5 |
| Inter-service | OpenFeign | — |
| Container | Docker + Compose | — |

---

## Cấu trúc project

```
Job_Board_Backend/
├── api-gateway/            # Entry point, JWT filter, rate limiting, CORS
├── auth-service/           # Đăng ký, đăng nhập, JWT, refresh token
├── job-service/            # CRUD tin tuyển dụng, Redis cache, scheduler
├── application-service/    # Nộp đơn, cập nhật trạng thái
├── profile-service/        # Hồ sơ candidate / company
├── notification-service/   # Lắng nghe RabbitMQ, gửi email (Thymeleaf)
├── docker-compose.yml
├── init-db.sql             # Tạo 4 database + user jobboard
└── README.md
```

---

## Ports

| Service | Port | Ghi chú |
|---|---|---|
| api-gateway | 8080 | Entry point duy nhất |
| auth-service | 8081 | |
| job-service | 8082 | |
| application-service | 8083 | |
| profile-service | 8084 | |
| notification-service | 8085 | |
| MySQL | 3306 | |
| Redis | 6379 | |
| RabbitMQ AMQP | 5672 | |
| RabbitMQ UI | 15672 | guest / guest |
| MailHog SMTP | 1025 | |
| MailHog UI | 8025 | Xem email gửi đi |

---

## Khởi động

### Yêu cầu

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

### Chạy toàn bộ hệ thống

```bash
git clone <repository-url>
cd Job_Board_Backend
docker-compose up -d --build
```

> Lần đầu mất 5–10 phút để Maven tải dependency và build image.

### Kiểm tra trạng thái

```bash
docker-compose ps
```

Tất cả container phải ở trạng thái `running` hoặc `healthy`.

```bash
# Kiểm tra health từng service
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

### Chỉ chạy infrastructure (phát triển local)

```bash
docker-compose up -d mysql redis rabbitmq mailhog
```

Rồi chạy từng service theo thứ tự: `auth` → `profile` → `job` → `notification` → `application` → `api-gateway`

---

## API Reference

Tất cả request đi qua `http://localhost:8080`. Các endpoint cần xác thực phải kèm header:
```
Authorization: Bearer <access_token>
```

### Auth — `/api/auth`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | ✗ | Đăng ký tài khoản |
| POST | `/api/auth/login` | ✗ | Đăng nhập |
| POST | `/api/auth/refresh` | ✗ | Làm mới access token |
| POST | `/api/auth/logout` | ✓ | Đăng xuất |

**POST /api/auth/register**
```json
// Request
{
  "email": "user@example.com",
  "password": "password123",
  "role": "CANDIDATE"   // hoặc "EMPLOYER"
}

// Response 201
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "userId": 1,
  "email": "user@example.com",
  "role": "CANDIDATE"
}
```

**POST /api/auth/login**
```json
// Request
{ "email": "user@example.com", "password": "password123" }

// Response 200 — cùng cấu trúc AuthResponse
```

**POST /api/auth/refresh**
```json
// Request
{ "refreshToken": "eyJ..." }

// Response 200 — AuthResponse mới (token rotation)
```

**POST /api/auth/logout**
```
// Header: Authorization: Bearer <access_token>
// Response 204 No Content
```

---

### Jobs — `/api/jobs`

| Method | Endpoint | Auth | Role | Mô tả |
|---|---|---|---|---|
| GET | `/api/jobs` | ✗ | — | Tìm kiếm tin tuyển dụng |
| GET | `/api/jobs/filters` | ✗ | — | Lấy danh sách filter |
| GET | `/api/jobs/{id}` | ✗ | — | Chi tiết tin (cached) |
| POST | `/api/jobs` | ✓ | EMPLOYER | Đăng tin mới |
| PUT | `/api/jobs/{id}` | ✓ | EMPLOYER | Cập nhật tin (partial) |
| DELETE | `/api/jobs/{id}` | ✓ | EMPLOYER | Xóa tin |

**GET /api/jobs** — Query params:
```
?title=java&location=hanoi&type=FULL_TIME&category=IT&page=0&size=10
```
Tất cả params đều optional. Chỉ trả về tin có `status=OPEN`.

**POST /api/jobs**
```json
// Request
{
  "title": "Backend Developer",
  "description": "Mô tả công việc...",
  "company": "Tech Corp",
  "location": "Hà Nội",
  "salaryMin": 1000,
  "salaryMax": 2000,
  "type": "FULL_TIME",        // FULL_TIME | PART_TIME | CONTRACT | INTERNSHIP
  "category": "IT",           // IT | MARKETING | FINANCE | DESIGN | ...
  "status": "OPEN",           // OPEN | CLOSED | DRAFT
  "deadline": "2025-12-31T23:59:59"
}
// Response 201 JobResponse
```

**GET /api/jobs/filters**
```json
// Response 200
{
  "locations": ["Hà Nội", "TP.HCM", "Đà Nẵng"],
  "categories": ["IT", "MARKETING", "FINANCE", ...],
  "types": ["FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP"]
}
```

---

### Applications — `/api/applications`

Tất cả endpoints đều yêu cầu xác thực.

| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/applications` | CANDIDATE | Nộp đơn ứng tuyển |
| GET | `/api/applications/my` | CANDIDATE | Danh sách đơn đã nộp |
| GET | `/api/applications/job/{jobId}` | EMPLOYER | Đơn theo tin tuyển dụng |
| PUT | `/api/applications/{id}/status` | EMPLOYER | Cập nhật trạng thái |

**POST /api/applications**
```json
// Request
{
  "jobId": 1,
  "coverLetter": "Thư xin việc..."
}
// Response 201 ApplicationResponse
```

**PUT /api/applications/{id}/status**
```json
// Request
{ "status": "ACCEPTED" }   // PENDING | REVIEWED | ACCEPTED | REJECTED
// Response 200 ApplicationResponse
```

---

### Profiles — `/api/profiles`

| Method | Endpoint | Auth | Role | Mô tả |
|---|---|---|---|---|
| GET | `/api/profiles/candidate/{userId}` | ✗ | — | Xem hồ sơ candidate |
| POST | `/api/profiles/candidate` | ✓ | CANDIDATE | Tạo / cập nhật hồ sơ |
| GET | `/api/profiles/company/{userId}` | ✗ | — | Xem hồ sơ công ty |
| POST | `/api/profiles/company` | ✓ | EMPLOYER | Tạo / cập nhật hồ sơ |

**POST /api/profiles/candidate**
```json
// Request
{
  "fullName": "Nguyễn Văn A",
  "phone": "0912345678",
  "bio": "5 năm kinh nghiệm...",
  "skills": "Java, Spring Boot, MySQL",
  "experience": "...",
  "education": "...",
  "resumeUrl": "https://..."
}
// Response 201 CandidateProfileResponse
```

**POST /api/profiles/company**
```json
// Request
{
  "companyName": "Tech Corp",
  "description": "Công ty công nghệ...",
  "website": "https://techcorp.com",
  "logoUrl": "https://...",
  "address": "123 Nguyễn Huệ, Q1, TP.HCM",
  "employeeSize": 200
}
// Response 201 CompanyProfileResponse
```

---

## Security

### JWT Flow

```
Đăng nhập → access token (1 ngày) + refresh token (7 ngày, lưu Redis)

Refresh token rotation:
  Client gửi refresh token → server validate → issue token mới → lưu Redis
  Token cũ bị thay thế ngay lập tức

Reuse detection:
  Nếu refresh token không khớp Redis → xóa luôn key → throw InvalidTokenException
  Buộc user đăng nhập lại

Logout:
  Access token → blacklist Redis (TTL = thời gian còn lại)
  Refresh token → xóa khỏi Redis
  api-gateway check blacklist trước khi forward request
```

### Rate Limiting

- Thuật toán: **Token Bucket** (Redis)
- Giới hạn: **10 request/giây**, burst tối đa 20
- Key: IP address
- Trả về `429 Too Many Requests` khi vượt giới hạn

---

## Cấu hình môi trường

| Biến | Giá trị mặc định |
|---|---|
| MySQL root password | `root_password` |
| MySQL app user | `jobboard` / `jobboard_password` |
| JWT Expiration | 24 giờ (86400000 ms) |
| Refresh Token Expiration | 7 ngày (604800000 ms) |

> **Lưu ý:** Thay đổi tất cả password và JWT secret trước khi deploy production.

---

## Testing

```bash
# Chạy unit tests trong một service
cd auth-service && ./mvnw test -Dtest="JwtUtilTest,AuthServiceTest"

# Chạy toàn bộ unit tests (bỏ qua contextLoads cần DB)
./mvnw test -Dtest="JwtUtilTest,AuthServiceTest,JobServiceTest,ApplicationServiceTest,CandidateProfileServiceTest,CompanyProfileServiceTest,EmailServiceTest"
```

| Service | Test class | Số tests |
|---|---|---|
| auth-service | `JwtUtilTest` | 8 |
| auth-service | `AuthServiceTest` | 10 |
| job-service | `JobServiceTest` | 9 |
| application-service | `ApplicationServiceTest` | 11 |
| profile-service | `CandidateProfileServiceTest` | 4 |
| profile-service | `CompanyProfileServiceTest` | 4 |
| notification-service | `EmailServiceTest` | 2 |

---

## Các lệnh Docker thường dùng

```bash
# Build lại 1 service sau khi sửa code
docker-compose up -d --build auth-service

# Xem log realtime
docker-compose logs -f job-service

# Restart 1 service
docker-compose restart application-service

# Dừng toàn bộ (giữ data)
docker-compose down

# Reset hoàn toàn (xóa cả data volumes)
docker-compose down -v && docker-compose up -d --build
```

---

## Web UI

| Tool | URL | Đăng nhập |
|---|---|---|
| RabbitMQ Management | http://localhost:15672 | `guest` / `guest` |
| MailHog (xem email) | http://localhost:8025 | Không cần |

---

## Xử lý lỗi thường gặp

**Port 3306 bị chiếm:**
```bash
# Đổi port MySQL trong docker-compose.yml: "3307:3306"
```

**init-db.sql không chạy (database không tạo):**
```bash
# Xóa volume MySQL cũ và khởi động lại
docker-compose down -v && docker-compose up -d
```

**Service crash do chưa kết nối được infrastructure:**
```bash
# Kiểm tra health của mysql / redis / rabbitmq trước
docker-compose ps
# Xem log service bị lỗi
docker-compose logs --tail=50 auth-service
```
