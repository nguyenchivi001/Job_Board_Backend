# Job Board Backend

Hệ thống backend cho ứng dụng tuyển dụng việc làm, xây dựng theo kiến trúc **Microservices** với Spring Boot.

---

## Kiến trúc tổng quan

```
Client
  │
  ▼
┌─────────────────┐
│   api-gateway   │  :8080  ← entry point duy nhất
└────────┬────────┘
         │ route theo path
    ┌────┴──────────────────────────┐
    │                               │
    ▼                               ▼
┌──────────────┐           ┌──────────────────────┐
│ auth-service │  :8081    │    job-service        │ :8082
│ MySQL+Redis  │           │ MySQL+Redis+RabbitMQ  │
└──────────────┘           └──────────┬───────────┘
                                      │ publish event
┌──────────────────────┐              ▼
│ application-service  │  :8083  ┌──────────────────────┐
│ MySQL+RabbitMQ+Feign │         │ notification-service │ :8085
└──────────────────────┘         │ RabbitMQ+MailHog     │
                                 └──────────────────────┘
┌──────────────┐
│profile-service│ :8084
│    MySQL      │
└──────────────┘
```

---

## Công nghệ sử dụng

| Thành phần | Công nghệ | Phiên bản |
|-----------|-----------|-----------|
| Framework | Spring Boot | 4.0.3 |
| Cloud | Spring Cloud | 2025.1.0 |
| Ngôn ngữ | Java | 17 |
| Database | MySQL | 8.0 |
| Cache | Redis | 7 |
| Message Broker | RabbitMQ | 3 |
| Email (dev) | MailHog | latest |
| Auth | JWT (JJWT) | 0.11.5 |
| Container | Docker + Compose | - |

---

## Cấu trúc project

```
Job_Board_Backend/
├── api-gateway/                 # Cổng vào, routing tới các service
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── auth-service/                # Đăng ký, đăng nhập, JWT
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── job-service/                 # Quản lý tin tuyển dụng
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── application-service/         # Quản lý hồ sơ ứng tuyển
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── profile-service/             # Quản lý hồ sơ người dùng
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── notification-service/        # Gửi email thông báo
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml
├── init-db.sql                  # Khởi tạo database MySQL
└── README.md
```

---

## Ports

| Service | Port | Ghi chú |
|---------|------|---------|
| api-gateway | 8080 | Entry point chính |
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

## Yêu cầu

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Java 17+ (nếu chạy local không dùng Docker)
- Maven 3.9+ (nếu chạy local không dùng Docker)

---

## Chạy với Docker (khuyến nghị)

### 1. Clone project

```bash
git clone <repository-url>
cd Job_Board_Backend
```

### 2. Khởi động toàn bộ hệ thống

```bash
docker-compose up -d --build
```

> Lần đầu sẽ mất 5–10 phút để Maven tải dependency và build image.

### 3. Kiểm tra trạng thái

```bash
docker-compose ps
```

Tất cả container phải ở trạng thái `running` hoặc `healthy`.

### 4. Kiểm tra database đã tạo chưa

```bash
docker exec -it jobboard-mysql mysql -u root -proot_password -e "SHOW DATABASES;"
```

Kết quả mong đợi:
```
+--------------------+
| Database           |
+--------------------+
| application_db     |
| auth_db            |
| job_db             |
| profile_db         |
+--------------------+
```

### 5. Kiểm tra health các service

```bash
curl http://localhost:8080/actuator/health   # api-gateway
curl http://localhost:8081/actuator/health   # auth-service
curl http://localhost:8082/actuator/health   # job-service
curl http://localhost:8083/actuator/health   # application-service
curl http://localhost:8084/actuator/health   # profile-service
curl http://localhost:8085/actuator/health   # notification-service
```

---

## Chạy local (không dùng Docker)

### 1. Khởi động infrastructure

```bash
# Chỉ chạy MySQL, Redis, RabbitMQ, MailHog bằng Docker
docker-compose up -d mysql redis rabbitmq mailhog
```

### 2. Build và chạy từng service

Thứ tự khởi động:

```bash
# 1. auth-service
cd auth-service && mvn spring-boot:run

# 2. profile-service
cd profile-service && mvn spring-boot:run

# 3. job-service
cd job-service && mvn spring-boot:run

# 4. notification-service
cd notification-service && mvn spring-boot:run

# 5. application-service (cần job-service + profile-service chạy trước)
cd application-service && mvn spring-boot:run

# 6. api-gateway (khởi động cuối cùng)
cd api-gateway && mvn spring-boot:run
```

---

## Các lệnh Docker thường dùng

```bash
# Build lại 1 service sau khi sửa code
docker-compose up -d --build auth-service

# Xem log realtime
docker-compose logs -f auth-service

# Restart 1 service
docker-compose restart job-service

# Dừng toàn bộ (giữ data)
docker-compose down

# Reset hoàn toàn (xóa cả data)
docker-compose down -v && docker-compose up -d --build
```

---

## Web UI

| Tool | URL | Thông tin đăng nhập |
|------|-----|---------------------|
| RabbitMQ Management | http://localhost:15672 | `guest` / `guest` |
| MailHog (xem email) | http://localhost:8025 | Không cần đăng nhập |

---

## API Gateway Routes

Tất cả request đi qua `http://localhost:8080`:

| Path | Chuyển tới |
|------|-----------|
| `/api/auth/**` | auth-service:8081 |
| `/api/jobs/**` | job-service:8082 |
| `/api/applications/**` | application-service:8083 |
| `/api/profiles/**` | profile-service:8084 |

---

## Cấu hình môi trường

Các giá trị mặc định dùng cho môi trường **development**:

| Biến | Giá trị mặc định |
|------|-----------------|
| MySQL root password | `root_password` |
| MySQL app user | `jobboard` / `jobboard_password` |
| JWT Secret | xem `docker-compose.yml` |
| JWT Expiration | 24 giờ (86400000ms) |
| Refresh Token Expiration | 7 ngày (604800000ms) |

> **Lưu ý:** Thay đổi tất cả password và JWT secret trước khi deploy lên môi trường production.

---

## Xử lý lỗi thường gặp

**Port 3306 bị chiếm:**
```bash
# Đổi port MySQL trong docker-compose.yml
ports:
  - "3307:3306"
```

**init-db.sql không chạy (database không tạo):**
```bash
# Xóa volume MySQL cũ và tạo lại
docker-compose down -v
docker-compose up -d
```

**Docker Desktop chưa khởi động:**
```bash
# Kiểm tra Docker đang chạy chưa
docker info
```
