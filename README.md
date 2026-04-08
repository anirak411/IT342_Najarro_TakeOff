# TradeOff

TradeOff is a multi-platform preloved marketplace project for IT342 (System Integration and Architecture).  
It includes a Spring Boot backend, a React web app, and an Android app.

## Project Overview
TradeOff lets users:
- Register and log in
- Post, edit, browse, and delete listings
- View seller information and listing details
- Chat with other users
- Manage profile media and user listings

## Current Feature Status
Implemented:
- Authentication (register/login)
- Listing CRUD with image upload
- Marketplace browsing with search/filter/sort
- Basic profile management
- Web and mobile messaging

Not yet implemented:
- JWT auth and refresh token flow
- Admin moderation panel
- Escrow transaction workflow
- Trade request and transaction history modules

## Tech Stack
Backend:
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL (local or Supabase)
- Cloudinary (image storage)

Web:
- React + Vite
- Axios
- React Router

Mobile:
- Kotlin (Android, minSdk 24)
- Retrofit
- RecyclerView + Coil

## Repository Structure
- `backend/` - Spring Boot API
- `web/` - React web application
- `mobile/` - Android application
- `docs/` - project documents and references

## Setup Instructions

1. Clone:
```bash
git clone https://github.com/anirak411/IT342_G5_Najarro_Lab1
cd IT342_G5_Najarro_Lab1
```

2. Backend:
```bash
cd backend
# Optional: online database (Supabase/PostgreSQL)
export SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require
export SPRING_DATASOURCE_USERNAME=postgres.<your-project-ref>
export SPRING_DATASOURCE_PASSWORD=<your-db-password>
mvn clean install
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`.

Database behavior is automatic:
- If online PostgreSQL is reachable, backend uses it.
- If it is unreachable (no internet), backend falls back to a local file database (`H2`) with no manual switching.
- Offline DB path can be customized with `APP_DATASOURCE_OFFLINE_URL`.

3. Web:
```bash
cd web
npm install
npm run dev
```
Web app runs on Vite's default local port.

4. Mobile:
- Open `mobile/` in Android Studio
- Sync Gradle
- Run on emulator/device

Ensure backend is running before testing web/mobile API features.

## Contributors
IT342 - G5  
Monica A. Najarro
