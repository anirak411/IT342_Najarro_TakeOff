# TradeOff — Regression Test Report

**Project:** TradeOff Preloved Marketplace  
**Branch:** `feature/vertical-slice-refactoring`  
**Date:** 2026-05-05  
**Author:** Monica A. Najarro  

---

## 1. Project Information

| Field | Value |
|-------|-------|
| Course | IT342 — System Integration and Architecture |
| Stack | Spring Boot 3.2 · React + Vite · Android (Kotlin) |
| Database | PostgreSQL (Supabase) / H2 fallback |
| Image Storage | Cloudinary |

---

## 2. Refactoring Summary

The entire project was restructured from a technical-layer layout to **Vertical Slice Architecture**, where each feature owns all its related files.

### What changed

| Layer | Before | After |
|-------|--------|-------|
| Backend | `controller/`, `model/`, `dto/`, `repository/` | `features/auth/`, `features/item/`, `features/user/`, `features/chat/`, `features/transaction/`, `features/common/` |
| Web | `pages/`, `components/`, `css/` (flat) | `features/auth/`, `features/dashboard/`, `features/item/`, `features/profile/`, `features/chat/`, `features/transaction/`, `features/admin/`, `features/common/` |
| Mobile | `model/`, `ui/`, `network/`, `utils/` | `features/auth/`, `features/item/`, `features/chat/`, `features/user/`, `features/common/`, `core/network/`, `core/utils/` |

### Additional fixes applied during refactoring
- Created `backend/src/main/resources/application.properties` (was missing)
- Created `web/.env` with `VITE_API_BASE_URL` and Supabase credentials
- Fixed `web/src/main.jsx` axios interceptor — replaced broken URL-rewrite logic with `axios.defaults.baseURL`
- Removed hardcoded `http://localhost:8080` from all 17 web source files
- Updated all package declarations and import paths in mobile Kotlin files

---

## 3. Updated Project Structure

```
backend/src/main/java/com/it342/backend/
├── features/
│   ├── auth/         AuthController, UserService, LoginRequest, RegisterRequest, AuthSessionResponse
│   ├── item/         Item, ItemController, ItemRepository, ItemService
│   ├── user/         User, UserController, UserRepository, UserRole, UserProfileResponse, ...
│   ├── chat/         ChatMessage, ChatController, ChatMessageRepository
│   ├── transaction/  EscrowTransaction, TransactionController, EscrowTransactionRepository, ...
│   └── common/       ApiResponse, HealthController
├── security/         SessionService, SessionPrincipal
└── config/           SecurityConfig, CloudinaryConfig, AdaptiveDataSourceConfig

web/src/
├── features/
│   ├── auth/         Login.jsx, Register.jsx, AuthCallback.jsx, global.css
│   ├── dashboard/    Dashboard.jsx, LandingPage.jsx, Sidebar.jsx, *.css
│   ├── item/         ItemDetails.jsx, MyItems.jsx, details.css
│   ├── profile/      Profile.jsx, SellerProfile.jsx, Settings.jsx, *.css
│   ├── chat/         ChatWidget.jsx, chat.css
│   ├── transaction/  Transactions.jsx
│   ├── admin/        AdminLayout.jsx, AdminOverview.jsx, AdminTransactions.jsx, AdminListings.jsx, AdminUsers.jsx, admin.css
│   └── common/       BackButton.jsx
└── utils/            session.js, supabaseClient.js, itemImages.js, itemTime.js, ownership.js, seller.js

mobile/app/src/main/java/com/example/tradeoff/
├── features/
│   ├── auth/         AuthRequest.kt, AuthResponse.kt, LoginRequest.kt
│   ├── item/         Item.kt, ItemListAdapter.kt
│   ├── chat/         ChatMessage.kt, ChatInboxThread.kt, SendMessageRequest.kt, ChatMessageAdapter.kt, ChatInboxAdapter.kt
│   ├── user/         UserProfile.kt, UserSummary.kt
│   └── common/       NotificationItem.kt, NotificationAdapter.kt
└── core/
    ├── network/      ApiService.kt, RetrofitClient.kt
    └── utils/        SessionManager.kt, PriceFormatter.kt
```

---

## 4. Test Plan

### 4.1 Functional Requirements Coverage

| # | Requirement | Test Type | Covered |
|---|-------------|-----------|---------|
| FR-01 | User Registration | Automated + Manual | ✅ |
| FR-02 | User Login | Automated + Manual | ✅ |
| FR-03 | Session token issued on login | Automated | ✅ |
| FR-04 | Duplicate email rejected | Automated | ✅ |
| FR-05 | Username validation (min 8, alphanumeric+_) | Automated | ✅ |
| FR-06 | Wrong password rejected | Automated | ✅ |
| FR-07 | Admin-exists check | Automated | ✅ |
| FR-08 | Browse all listings | Automated + Manual | ✅ |
| FR-09 | Search listings by keyword | Automated + Manual | ✅ |
| FR-10 | Filter listings by category/price/location | Manual | ✅ |
| FR-11 | Post listing with image upload | Manual | ✅ |
| FR-12 | Edit own listing | Manual | ✅ |
| FR-13 | Delete own listing | Manual | ✅ |
| FR-14 | View item details | Manual | ✅ |
| FR-15 | View seller profile | Manual | ✅ |
| FR-16 | Chat with other users | Manual | ✅ |
| FR-17 | Profile management (pic, cover, name) | Manual | ✅ |
| FR-18 | Admin can delete any listing | Manual | ✅ |
| FR-19 | Admin can view all users | Manual | ✅ |

### 4.2 Test Cases

#### TC-01: Register with valid data
- **Steps:** POST `/api/auth/register` with fullName, displayName (≥8 chars), email, password
- **Expected:** 200 OK, `success: true`
- **Result:** ✅ PASS

#### TC-02: Register with duplicate email
- **Steps:** POST `/api/auth/register` twice with same email
- **Expected:** 400 Bad Request, `success: false`
- **Result:** ✅ PASS

#### TC-03: Register with short username
- **Steps:** POST `/api/auth/register` with displayName = "short"
- **Expected:** 400 Bad Request
- **Result:** ✅ PASS

#### TC-04: Login with correct credentials
- **Steps:** Register user, then POST `/api/auth/login`
- **Expected:** 200 OK, `data.sessionToken` present
- **Result:** ✅ PASS

#### TC-05: Login with wrong password
- **Steps:** POST `/api/auth/login` with incorrect password
- **Expected:** 400 Bad Request, `success: false`
- **Result:** ✅ PASS

#### TC-06: Admin-exists endpoint
- **Steps:** GET `/api/auth/admin-exists`
- **Expected:** 200 OK, `data` is boolean
- **Result:** ✅ PASS

#### TC-07: Get all items
- **Steps:** GET `/api/items`
- **Expected:** 200 OK, JSON array
- **Result:** ✅ PASS

#### TC-08: Search items (no params)
- **Steps:** GET `/api/items/search`
- **Expected:** 200 OK, JSON array
- **Result:** ✅ PASS

#### TC-09: Search items with keyword
- **Steps:** GET `/api/items/search?q=phone`
- **Expected:** 200 OK, JSON array (filtered)
- **Result:** ✅ PASS

#### TC-10: Context loads
- **Steps:** Spring application context startup
- **Expected:** No startup errors
- **Result:** ✅ PASS

---

## 5. Automated Test Evidence

```
[INFO] Tests run: 1,  Failures: 0, Errors: 0 — BackendApplicationTests
[INFO] Tests run: 6,  Failures: 0, Errors: 0 — AuthControllerTest
[INFO] Tests run: 3,  Failures: 0, Errors: 0 — ItemControllerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
```

Test files:
- `backend/src/test/.../features/auth/AuthControllerTest.java` — 6 tests
- `backend/src/test/.../features/item/ItemControllerTest.java` — 3 tests
- `backend/src/test/.../BackendApplicationTests.java` — 1 test (context load)

---

## 6. Regression Test Results

| Feature | Before Refactor | After Refactor | Status |
|---------|----------------|----------------|--------|
| Register / Login | ✅ Working | ✅ Working | No regression |
| Browse listings | ✅ Working | ✅ Working | No regression |
| Search / Filter | ✅ Working | ✅ Working | No regression |
| Post listing | ✅ Working | ✅ Working | No regression |
| Edit / Delete listing | ✅ Working | ✅ Working | No regression |
| Item details page | ✅ Working | ✅ Working | No regression |
| Seller profile | ✅ Working | ✅ Working | No regression |
| Chat | ✅ Working | ✅ Working | No regression |
| Profile management | ✅ Working | ✅ Working | No regression |
| Admin panel | ✅ Working | ✅ Working | No regression |
| Web build | ✅ Success | ✅ Success | No regression |
| Backend compile | ✅ Success | ✅ Success | No regression |

---

## 7. Issues Found

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| B-01 | `application.properties` missing — backend started in H2 offline mode silently | High | ✅ Fixed |
| B-02 | `web/.env` missing — Supabase chat disabled, API URL not configurable | High | ✅ Fixed |
| B-03 | Axios interceptor only rewrote `localhost:8080` URLs — deployed URL ignored | High | ✅ Fixed |
| B-04 | 17 web files had hardcoded `http://localhost:8080` — broke deployed environments | High | ✅ Fixed |
| B-05 | Backend launched from IntelliJ without env vars — ran against empty H2 DB | Medium | ✅ Documented (fix: add env vars to IntelliJ run config) |
| B-06 | `WebConfig.java` is empty class with no annotations | Low | ⚠️ Left as-is (harmless) |

---

## 8. Fixes Applied

| Fix | File(s) Changed |
|-----|----------------|
| Created `application.properties` | `backend/src/main/resources/application.properties` |
| Created `web/.env` | `web/.env` |
| Fixed axios baseURL | `web/src/main.jsx` |
| Stripped hardcoded localhost URLs | All 17 `web/src/**/*.jsx` files |
| Backend vertical slice refactor | 31 files moved/renamed under `features/` |
| Web vertical slice refactor | All pages/components moved to `features/` |
| Mobile vertical slice refactor | All files moved to `features/` and `core/` |
| Fixed all broken import paths post-refactor | Multiple `.jsx` and `.kt` files |
