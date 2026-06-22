# 💰 Budget Tracker — Backend API

Spring Boot REST API for the Budget Tracker personal finance app. Handles authentication, categories, transactions, and budget management.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen) ![Java](https://img.shields.io/badge/Java-17-orange) ![MySQL](https://img.shields.io/badge/MySQL-8-blue) ![Docker](https://img.shields.io/badge/Docker-ready-2496ED) ![JWT](https://img.shields.io/badge/JWT-secured-yellow)

---

## ✨ Features

- 🔐 JWT Authentication — Register, login, change password
- 👤 User isolation — Every user sees only their own data
- 🏷️ Categories — Income & expense categories per user
- 💸 Transactions — Full CRUD with month/year/type/category filters
- 🎯 Budgets — Monthly spending limits with % used tracking
- 🛡️ Global exception handling — Clean error responses
- 🐳 Docker ready — Deployed on Render free tier

---

## 🛠️ Tech Stack

| Tech | Version | Purpose |
|------|---------|---------|
| Java | 17 | Language |
| Spring Boot | 3.1.5 | Framework |
| Spring Security | 6 | Auth layer |
| jjwt | 0.11.5 | JWT tokens |
| Spring Data JPA | 3.1 | ORM |
| MySQL | 8 | Database |
| Lombok | latest | Boilerplate reduction |
| Docker | - | Containerization |

---

## 🚀 Local Setup

### Prerequisites
- Java 17+
- MySQL 8
- Maven 3.8+

### 1. MySQL Setup
```sql
CREATE DATABASE budget_db;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
FLUSH PRIVILEGES;
```

### 2. Run the app
```bash
./mvnw spring-boot:run
```

Or open in IntelliJ IDEA as a Maven project and run `BudgetApplication.java`.

API runs on `http://localhost:8080`

### 3. application.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/budget_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
app.jwt.secret=your_secret_key_min_32_chars
app.jwt.expiration-ms=86400000
```

---

## 🌍 Deployment on Render

### Environment Variables
| Key | Value |
|-----|-------|
| `DATASOURCE_URL` | `jdbc:mysql://host:port/db?useSSL=true&serverTimezone=UTC` |
| `DATASOURCE_USERNAME` | your db username |
| `DATASOURCE_PASSWORD` | your db password |
| `JWT_SECRET` | random 32+ char string |
| `PORT` | `10000` |

### Steps
1. Push this repo to GitHub
2. Go to [render.com](https://render.com) → New → Web Service
3. Connect your GitHub repo
4. Runtime: **Docker**
5. Add environment variables above
6. Deploy

---

## 📁 Project Structure

```
src/main/java/net/ankan/budget/
├── BudgetApplication.java        # Entry point
├── config/
│   └── SecurityConfig.java       # CORS + JWT filter chain
├── controller/
│   ├── AuthController.java       # /api/auth/**
│   ├── CategoryController.java   # /api/categories/**
│   ├── TransactionController.java# /api/transactions/**
│   └── BudgetController.java     # /api/budgets/**
├── service/impl/
│   ├── AuthServiceImpl.java
│   ├── CategoryServiceImpl.java
│   ├── TransactionServiceImpl.java
│   └── BudgetServiceImpl.java
├── repository/
│   ├── UserRepository.java
│   ├── CategoryRepository.java
│   ├── TransactionRepository.java
│   └── BudgetRepository.java
├── entity/
│   ├── User.java
│   ├── Category.java
│   ├── Transaction.java
│   └── Budget.java
├── dto/                          # Request/Response objects
├── mapper/                       # Entity ↔ DTO mappers
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── CustomUserDetailsService.java
└── exception/
    └── GlobalExceptionHandler.java
```

---

## 🔑 API Endpoints

### Auth
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register new user | ❌ |
| POST | `/api/auth/login` | Login & get JWT | ❌ |
| POST | `/api/auth/change-password` | Change password | ✅ |

### Categories
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | Get all categories |
| GET | `/api/categories?type=EXPENSE` | Filter by type |
| POST | `/api/categories` | Create category |
| PUT | `/api/categories/{id}` | Rename category |
| DELETE | `/api/categories/{id}` | Delete category |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions?month=6&year=2026` | Get transactions |
| GET | `/api/transactions/summary?month=6&year=2026` | Monthly summary |
| POST | `/api/transactions` | Add transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |

### Budgets
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/budgets/status?month=6&year=2026` | Budget status with % |
| POST | `/api/budgets` | Set budget |
| PUT | `/api/budgets/{id}` | Update limit |
| DELETE | `/api/budgets/{id}` | Remove budget |

---

## 🔒 Security Notes

- All `/api/auth/**` endpoints are public
- Every other endpoint requires `Authorization: Bearer <token>` header
- JWT expires in 24 hours (configurable via `app.jwt.expiration-ms`)
- All data is user-scoped — users can only access their own records
- Categories cannot be deleted if linked transactions or budgets exist (409 response)

---

## 🐳 Docker

```bash
# Build
docker build -t budget-tracker-api .

# Run
docker run -p 10000:10000 \
  -e DATASOURCE_URL=jdbc:mysql://host:port/db \
  -e DATASOURCE_USERNAME=user \
  -e DATASOURCE_PASSWORD=pass \
  -e JWT_SECRET=your_secret \
  budget-tracker-api
```

---

## 👤 Author

**Ankan Chakraborty**
- GitHub: [@AnkanIT24](https://github.com/AnkanIT24)
- Email: dasonu24@gmail.com

---

## 📄 License

MIT License — free to use and modify.
