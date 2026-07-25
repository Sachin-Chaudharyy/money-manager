# 💸 Money Manager — Backend

A cozy little Spring Boot API that keeps track of where your money runs off to. Built with Java, secured with JWT, and now smart enough to let you skip the password entirely with Google Sign-In. ✨

---

## 🌱 What this is

Money Manager is a personal finance tracker. This repo is the **brains** of the operation — the backend that handles accounts, authentication, and (soon) all your income and expense data. The frontend (React) talks to this API to bring it all to life on screen.

---

## 🧰 Tech Stack

| Layer | Tech |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT |
| Database | MySQL (local) / PostgreSQL (production) |
| ORM | Spring Data JPA + Hibernate |
| Auth | Email/Password + Google OAuth 2.0 |
| Email | Custom activation email service |
| Deployment | Render 🚀 |

---

## ✨ Features

- 🔐 **Secure signup & login** with JWT-based authentication
- 📧 **Email activation flow** — no fake accounts sneaking in
- 🔵 **Continue with Google** — one-click signup & login, no password needed
- 🚫 **Duplicate email protection** — clean error handling, no ugly stack traces
- 🛡️ **Spring Security** guarding every route that matters
- 🗄️ **Clean layered architecture** — Controller → Service → Repository, the way it should be

---

## 🗂️ Project Structure

```
src/main/java/com/project/moneymanager/
├── controller/     → REST endpoints
├── service/        → Business logic
├── repository/     → Database access (Spring Data JPA)
├── entity/         → JPA entities
├── dto/            → Data transfer objects
├── security/       → JWT filters & config
└── util/           → Helpers (JWT util, etc.)
```

---

## ⚙️ Getting Started Locally

### 1. Clone it
```bash
git clone https://github.com/Sachin-Chaudharyy/money-manager.git
cd money-manager
```

### 2. Set up your database
Create a local MySQL database and update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/moneymanager
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Add your Google OAuth Client ID
```properties
google.oauth.client-id=your_client_id.apps.googleusercontent.com
```

### 4. Run it
```bash
./mvnw spring-boot:run
```

The API will spin up on `http://localhost:8080` 🎉

---

## 🔑 Key Endpoints

| Method | Endpoint | What it does |
|---|---|---|
| `POST` | `/api/v1.0/register` | Create a new account |
| `POST` | `/api/v1.0/login` | Log in with email + password |
| `POST` | `/api/v1.0/auth/google` | Sign in / sign up with Google |
| `GET` | `/api/v1.0/activate` | Activate account via email link |

---

## 🌷 A little note

This project started as a way to learn — Spring Security, JWT, OAuth, real deployment quirks and all. Every bug fixed here (yes, even the CORS ones 😮‍💨) taught something worth keeping. Thanks for stopping by!

---

Made with ☕, a lot of `console.log`s, and mild chaos.
