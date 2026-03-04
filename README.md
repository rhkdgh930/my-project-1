# 🛡️ my-project
> **Status**: 🛠️ In-Progress (Core Module: Auth & User Domain Completed)

## 📝 Introduction
* "안정적인 백엔드 시스템 구축을 위해 대량 데이터 처리와 보안 로직을 심도 있게 다루는 프로젝트입니다."

* 현재 서비스의 핵심인 유저 관리 및 인증/인가(RBAC, JWT, Redis) 레이어를 우선적으로 구축하였습니다.
* 특히 수백만 건의 데이터를 안정적으로 처리하기 위한 **배치 프로세싱(Keyset Pagination)**과 비동기 이벤트 설계 등, 실제 운영 환경에서 마주할 수 있는 기술적 도전 과제들을 해결하는 데 집중하고 있습니다.

---

## 📌 기술 스택

### 🔹 백엔드
| 기술 | 버전 |
|---|---|
| ![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white) | OpenJDK 17 |
| ![SpringBoot](https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) | 3.5.3 |
| ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white) | 8.0+ |
| ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) | Latest (v6.x) |
| ![JSON Web Tokens](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white) | 0.11.5 |
| ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white) | Latest |
| ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black) | 2.8.8 (Springdoc) |
| ![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white) | Latest |
| ![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white) | 5 |

### 🔹 협업 도구
| 기술 | 용도 |
|---|---|
| ![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white) | 형상 관리 및 코드 통합 (Branch 전략 준수) |
| ![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white) | 백엔드 API 유닛 테스트 및 팀 내 시나리오 공유 |
| ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black) | 프론트엔드 협업을 위한 API 명세 자동화 및 표준화 |

---

## 📜 코드 컨벤션 및 전략

### 🔹 백엔드 핵심 전략
* **Layered Architecture**: Controller - Service - Domain(Entity) - Repository 구조 준수
* **Domain-Driven Validation**: 엔티티 내부에서 자격 검증 로직을 수행하여 응집도 향상
* **Event-Driven Integration**: `ApplicationEventPublisher`를 통한 메일 발송 및 알림 비동기 처리
* **Global Exception Handling**: 표준 예외 처리기를 통한 일관된 에러 응답 제공

---

## 🎯 프로젝트 개요

### ✅ 주요 기능
- **RBAC 인증/인가 (User/Auth)**: 유저 권한(Role) 및 상태(Status)에 따른 정교한 접근 제어
- **고도화된 토큰 관리**: Redis 기반 Token Rotation(AT/RT) 및 로그아웃 블랙리스트 구현
- **대용량 데이터 최적화**: Keyset Pagination을 적용한 휴면 계정 및 탈퇴 데이터 배치 처리
- **보안 및 개인정보**: Soft Delete 정책 적용 및 민감 정보 마스킹을 통한 데이터 보존 규칙 준수
- **비동기 알림 시스템**: `TransactionalEventListener`를 활용한 데이터 정합성 보장형 메일 발송

---

### 📌 기술적 강점
- **Redis 활용 성능 최적화**: 분산 상태 관리자로 Redis를 활용하여 DB 부하 감소 및 보안성 강화
- **운영 안정성 설계**: Chunk 단위 트랜잭션 분리를 통해 대량 데이터 처리 시 시스템 병목 예방
- **Swagger 표준화**: 실시간 API 명세 자동화를 통해 프론트엔드와의 소통 비용 제로화

---

## 🌍 환경 변수 설정 (application.yaml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/secureauth?serverTimezone=UTC&characterEncoding=UTF-8
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate.format_sql: true

  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.gmail.com
    port: 587
    username: YOUR_EMAIL
    password: YOUR_APP_PASSWORD

token:
  secret: YOUR_JWT_SECRET_KEY
  access-token-expiration: 600000  # 10분
  refresh-token-expiration: 3600000 # 1시간
