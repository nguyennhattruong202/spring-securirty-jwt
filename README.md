# Spring Boot JWT Security (Access Token + Refresh Token)

Dự án mẫu xây dựng hệ thống **Authentication/Authorization** với **Spring Security + JWT**, hỗ trợ **Refresh Token theo thiết bị**, **xác thực email**, **quên mật khẩu/đặt lại mật khẩu**, và **RBAC** (USER/ADMIN). Dự án có tích hợp **Swagger/OpenAPI** để test API nhanh và **Spring Mail + FreeMarker** để gửi email template.

---

## Mô tả dự án

- **Mục tiêu**: cung cấp bộ API xác thực người dùng theo chuẩn backend hiện đại, tách cấu hình theo module (`server`, `database`, `jwt`, `mail`, `swagger`).
- **Kiểu ứng dụng**: REST API (Spring Boot), JWT-based stateless auth, refresh token để gia hạn phiên đăng nhập.

---

## Công nghệ sử dụng

- **Java**: 21
- **Spring Boot**: 3.5.x
- **Spring Web**: REST API
- **Spring Security**: Authentication/Authorization + method security (`@PreAuthorize`)
- **JWT**: `io.jsonwebtoken (jjwt)`
- **Spring Data JPA + Hibernate**
- **Database**: MySQL (`mysql-connector-j`)
- **Connection pool**: HikariCP
- **Email**: Spring Mail
- **Template**: FreeMarker (`.ftl`)
- **API Docs**: springdoc-openapi (Swagger UI)
- **Utilities**: Lombok, Guava, ExpiringMap
- **Build**: Maven + Maven Wrapper (`mvnw`)

---

## Các chức năng chính

### Auth (không yêu cầu đăng nhập)

Base path: ` /api/auth `

- **Kiểm tra email/username đã tồn tại**
  - `GET /api/auth/checkEmailInUse?email=...`
  - `GET /api/auth/checkUsernameInUse?username=...`
- **Đăng ký**
  - `POST /api/auth/register`
  - Gửi email xác thực (email verification)
- **Xác thực email**
  - `GET /api/auth/registrationConfirmation?token=...`
  - `GET /api/auth/resendRegistrationToken?token=...`
- **Đăng nhập**
  - `POST /api/auth/login`
  - Trả về **Access Token (JWT)** + **Refresh Token**
- **Refresh token**
  - `POST /api/auth/refresh`
- **Quên mật khẩu / đặt lại mật khẩu**
  - `POST /api/auth/password/resetlink`
  - `POST /api/auth/password/reset`

### User (yêu cầu đăng nhập)

Base path: ` /api/user `

- **Lấy thông tin người dùng hiện tại**
  - `GET /api/user/me` (ROLE_USER)
- **Endpoint yêu cầu ADMIN**
  - `GET /api/user/admins` (ROLE_ADMIN)
- **Đổi mật khẩu**
  - `POST /api/user/password/update` (ROLE_USER)
- **Đăng xuất**
  - `POST /api/user/logout`

---

## Cấu trúc project

Thư mục chính:

- `src/main/java/com/truong/spring/security/jwt/`
  - `JwtApplication.java`: entrypoint
  - `controller/`: REST controllers (`AuthController`, `UserController`)
  - `security/`: JWT filter/validator/provider và cấu hình bảo mật
  - `service/`: business services (auth, user, mail, token...)
  - `repository/`: Spring Data JPA repositories
  - `model/`: entities, enums, token models, payload DTOs
  - `event/` + `event/listener/`: domain events (đăng ký, reset password, logout...) và listeners gửi mail/ xử lý nghiệp vụ
  - `cache/`: cache token logout (blacklist) / expiring cache
  - `advice/`: global exception handler
  - `validation/`: custom annotations/validators

- `src/main/resources/`
  - `application.yml`: cấu hình chính, import các file dưới `config/`
  - `application-dev.yml`, `application-prod.yml`: cấu hình theo profile
  - `config/`
    - `server-config.yml`: server port, error, compression...
    - `database-config.yml`: datasource + JPA + sql init + Jackson
    - `jwt-config.yml`: JWT/refresh token + token TTL + cache
    - `mail-config.yml`: SMTP
    - `swagger-config.yml`: springdoc / Swagger UI
  - `templates/`: FreeMarker templates email (`email-verification.ftl`, `reset-link.ftl`, ...)

---

## Cách chạy project

### Yêu cầu môi trường

- **JDK 21**
- **MySQL** đang chạy (mặc định: `localhost:3306`)
- Không cần cài Maven nếu dùng **Maven Wrapper** (`mvnw`)

### Cấu hình biến môi trường

Tối thiểu cần:

- **DB_PASSWORD**: mật khẩu MySQL (default config đang dùng username `root`)
- **JWT_KEY**: secret ký JWT
- **MAIL_ADDRESS**: email SMTP
- **MAIL_PASSWORD**: mật khẩu SMTP (khuyến nghị app password)

Ví dụ PowerShell:

```powershell
$env:DB_PASSWORD="your_db_password"
$env:JWT_KEY="your_jwt_secret_key"
$env:MAIL_ADDRESS="your_email@gmail.com"
$env:MAIL_PASSWORD="your_mail_password"
```

### Chạy local (dev)

```powershell
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Mặc định server chạy ở:

- **API base**: `http://localhost:9004`

### Build jar

```powershell
.\mvnw -DskipTests package
```

Chạy jar:

```powershell
java -jar .\target\spring-security-jwt-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Swagger / OpenAPI

Nếu bật Swagger (thường dùng cho dev), truy cập:

- Swagger UI: `http://localhost:9004/swagger-ui`
- OpenAPI JSON: `http://localhost:9004/v3/api-docs`

---