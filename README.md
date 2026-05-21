# Backend-for-Frontend (BFF) Gateway

A lightweight, secure API Gateway and Backend-for-Frontend service designed to mediate communication between frontend clients (Mobile/Web) and backend microservices.

## 🚀 Features

- **Centralized Routing** - Proxies requests to internal microservices securely (`/Main/**`, `/AuthForward/**`, etc.).
- **Token Security (Hybrid Mode)** - Converts JSON-based token responses from the Auth Service into HTTP-only, secure cookies for web clients, while simultaneously passing them in the body for mobile clients.
- **Dynamic Method Support** - Fully supports all standard REST HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`).
- **Stateless Authentication Proxying** - Extracts required headers and manages context before passing them to internal services.
- **Cross-Origin Resource Sharing (CORS)** - Configured gateway-level CORS to manage allowed domains and headers safely.

## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.3.5**
  - Spring Web
  - Spring Cloud Gateway (Custom Proxy Routing)
- **Security**: Custom Token Extraction Filters & Cookie Management
- **Build Tool**: Maven

## 📦 Getting Started

### Prerequisites
- JDK 21
- Maven
- Auth Service (running on port `8080` by default)
- Main/Business Service (running on port `8082` by default)

### Configuration
By default, the BFF will run on port `8089` and route traffic based on your internal IP/localhost configurations. No direct database connection is required for the BFF, as it acts purely as a stateless gateway.

### Running the Service

```bash
# Compile and build the project
./mvnw clean install

# Run the Spring Boot application
./mvnw spring-boot:run
```
The service will start on `http://localhost:8089`.

## 🔄 Routing Structure

All API calls from clients should be directed to the BFF. The BFF will route them as follows:

| Client Path | Internal Target | Purpose |
| ----------- | --------------- | ------- |
| `/BFF/api/proxy/AuthForward/**` | `http://localhost:8080/Authservice/**` | Routes to Auth Service (Login, Registration) |
| `/BFF/api/proxy/Main/**` | `http://localhost:8082/**` | Routes to Main Business Service |

## 🍪 Cookie Management
For maximum security on web browsers, this BFF strips raw tokens from standard responses and injects them into the browser via strict, secure `HTTP-Only` cookies (like `AccessToken`, `RefreshToken`, and `Fgp`). It also attaches these cookies as headers when talking to internal backend services so they don't have to deal with cookies.

## 📄 License
This project is part of the Professional Portfolio Module.
