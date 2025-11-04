# URL Shortener - Angular Frontend Plan

## 🎨 Screen Layout & Component Plan

### 1. Authentication Module

#### Login Screen (`/login`)
**Fields:**
- Username input
- Password input
- "Remember me" checkbox
- Login button
- "Don't have an account? Sign up" link

**Angular Concepts:** Reactive Forms, Form Validation, HTTP Client, Router

#### Registration Screen (`/register`)
**Fields:**
- Username input
- Email input
- Password input
- Confirm password input
- Terms & conditions checkbox
- Register button
- "Already have an account? Login" link

**Angular Concepts:** Form Validators, Password matching validation

---

### 2. Main Dashboard (`/dashboard`)

#### URL Shortening Section (Top of page)
**Fields:**
- Original URL input (large text box)
- Custom expiration dropdown (7 days, 14 days, 30 days, 90 days, 1 year)
- "Shorten URL" button
- Result display area (shortened URL with copy button)

#### My URLs Section (Below shortening form)
**Features:**
- Table/card grid showing:
  - Short URL (clickable)
  - Original URL (truncated with tooltip)
  - Created date
  - Expiration date
  - Click count
  - Status (Active/Expired/Deactivated)
  - Actions: View Analytics, Copy, Deactivate, Delete
- Pagination
- Search/filter functionality
- Sort by: Date, Clicks, Status

---

### 3. Analytics Screen (`/analytics/:shortCode`)

**Sections:**

**A. Overview Cards (Top row)**
- Total Clicks
- Today's Clicks
- Average Daily Clicks
- Active Days

**B. Charts Section**
- **Click Trends**: Line chart (last 7/30 days)
- **Geographic Distribution**: World map or bar chart by country
- **Device Types**: Pie chart (Desktop, Mobile, Tablet)
- **Browsers**: Horizontal bar chart
- **Referrer Sources**: Bar chart

**C. Recent Clicks Table**
- Timestamp
- Location (Country, City)
- Device
- Browser
- Referrer

**Angular Concepts:** Chart.js/ng2-charts, Pipes for date formatting

---

### 4. Navigation & Layout

#### Header/Navbar (Always visible)
- Logo/Brand name
- Navigation links: Dashboard, My URLs
- User menu (dropdown): Profile, Logout
- User avatar/username

#### Sidebar (Optional, for larger screens)
- Quick stats
- Recent URLs
- Quick actions

---

## 🧩 Angular Component Structure

```
src/app/
├── core/                          # Core module (singleton services)
│   ├── services/
│   │   ├── auth.service.ts        # Authentication logic, JWT handling
│   │   ├── url.service.ts         # URL CRUD operations
│   │   ├── analytics.service.ts   # Analytics data fetching
│   │   └── token-interceptor.ts   # HTTP interceptor for JWT
│   ├── guards/
│   │   └── auth.guard.ts          # Route protection
│   └── models/
│       ├── user.model.ts
│       ├── url.model.ts
│       └── analytics.model.ts
│
├── shared/                        # Shared module (reusable components)
│   ├── components/
│   │   ├── navbar/                # Top navigation bar
│   │   ├── footer/                # Footer
│   │   ├── loading-spinner/       # Loading indicator
│   │   ├── toast-notification/    # Success/error messages
│   │   └── confirm-dialog/        # Confirmation dialogs
│   ├── pipes/
│   │   ├── truncate.pipe.ts       # Truncate long URLs
│   │   └── time-ago.pipe.ts       # Relative time display
│   └── directives/
│       └── copy-clipboard.directive.ts  # Copy to clipboard
│
├── features/
│   ├── auth/                      # Authentication feature module
│   │   ├── login/
│   │   │   ├── login.component.ts
│   │   │   ├── login.component.html
│   │   │   └── login.component.css
│   │   ├── register/
│   │   │   ├── register.component.ts
│   │   │   ├── register.component.html
│   │   │   └── register.component.css
│   │   └── auth.module.ts
│   │
│   ├── dashboard/                 # Dashboard feature module
│   │   ├── url-shortener-form/    # URL creation form
│   │   ├── url-list/              # List of URLs (table/cards)
│   │   ├── url-card/              # Individual URL display
│   │   └── dashboard.module.ts
│   │
│   └── analytics/                 # Analytics feature module
│       ├── analytics-overview/    # Summary cards
│       ├── click-chart/           # Click trend chart
│       ├── geo-chart/             # Geographic chart
│       ├── device-chart/          # Device distribution
│       ├── browser-chart/         # Browser stats
│       ├── recent-clicks/         # Recent clicks table
│       └── analytics.module.ts
│
├── app-routing.module.ts          # Main routing
├── app.component.ts               # Root component
└── app.module.ts                  # Root module
```

---

## 📋 Angular Concepts You'll Learn

1. **Modules**: Organizing code (Core, Shared, Feature modules)
2. **Components**: Building UI blocks
3. **Services**: Business logic and API calls
4. **Routing**: Navigation between pages
5. **Guards**: Protecting routes (auth required)
6. **Interceptors**: Adding JWT to requests
7. **Reactive Forms**: Form handling with validation
8. **Pipes**: Data transformation in templates
9. **Directives**: Custom behaviors (copy to clipboard)
10. **RxJS**: Observables for async operations
11. **HttpClient**: Making API calls
12. **Dependency Injection**: Service management

---

## 🎨 UI/UX Design Approach

**Style Framework:** Angular Material (Google's Material Design)
- Well-documented and beginner-friendly
- Ready-to-use components (buttons, forms, tables, dialogs)
- Consistent design system
- Built specifically for Angular

**Alternative Options:**
- **Bootstrap** - Popular, easy to learn
- **PrimeNG** - Rich component library
- **Tailwind CSS** - Utility-first CSS

---

## 📱 Responsive Design Considerations

- Mobile-first approach
- Collapsible sidebar on small screens
- Responsive tables (convert to cards on mobile)
- Touch-friendly buttons and inputs

---

## 🎯 Implementation Approach

**Learning Strategy:** Build one complete functionality at a time, implementing only what's needed for that feature to work. This approach helps you understand each piece before moving to the next.

---

## 📋 Functionality-Based Implementation Plan

### ✅ Setup (COMPLETED)
- [x] Angular project created
- [x] Angular Material installed and configured
- [x] Project structure (core, shared, features folders)
- [x] TypeScript models (User, URL, Analytics)
- [x] Environment configuration

---

### 🎯 Functionality 1: User Login

**What we'll build:** Complete login flow where users can authenticate and access the app.

**Implementation Steps:**
1. Create Login Component with Angular CLI
2. Build Login Form UI with Angular Material (username, password fields)
3. Add Form Validation with Reactive Forms (required, minLength validators)
4. Create Auth Service with only `login()` method
5. Connect Form to Service (handle success, errors, loading state)
6. Setup Basic Routing (`/login`, `/dashboard` placeholder)

**What you'll learn:** Components, Reactive Forms, Services, HttpClient, Observables, Router

**Deliverables:**
- ✅ Working login screen with validation
- ✅ API integration with `/api/v1/auth/login`
- ✅ JWT token stored in localStorage
- ✅ Navigation to dashboard after login

---

### 🎯 Functionality 2: User Registration

**What we'll build:** Registration form for new users to sign up.

**Implementation Steps:**
1. Create Register Component
2. Build Registration Form UI (username, email, password, confirm password)
3. Add Custom Validators (email format, password match)
4. Add `register()` method to Auth Service
5. Connect Form to Service
6. Update Routing (add `/register` route, navigation between login/register)

**What you'll learn:** Complex forms, Custom validators, Cross-field validation

**Deliverables:**
- ✅ Working registration form with validation
- ✅ API integration with `/api/v1/auth/register`
- ✅ Password matching validation
- ✅ Navigation between login and register

---

### 🎯 Functionality 3: Protected Routes & Auth Guard

**What we'll build:** Prevent unauthenticated users from accessing protected pages.

**Implementation Steps:**
1. Create Auth Guard (check token, redirect to login if not authenticated)
2. Create HTTP Interceptor (automatically add JWT to all API requests)
3. Add Logout Functionality to Auth Service
4. Apply Guard to Routes (protect dashboard and future routes)

**What you'll learn:** Route guards, HTTP interceptors, Route protection

**Deliverables:**
- ✅ Auth guard prevents unauthorized access
- ✅ JWT automatically added to API requests
- ✅ Logout functionality works

---

### 🎯 Functionality 4: Navigation Bar

**What we'll build:** Navbar with branding, navigation links, and user menu.

**Implementation Steps:**
1. Create Navbar Component in shared/components
2. Build Navbar UI (toolbar, logo, navigation links, user menu)
3. Show/Hide based on Auth State (display username, conditional rendering)
4. Implement Logout button

**What you'll learn:** Shared components, Angular Material toolbar/menu, Conditional rendering

**Deliverables:**
- ✅ Navbar visible only when authenticated
- ✅ Shows current username
- ✅ Logout button works

---

### 🎯 Functionality 5: URL Shortening Form

**What we'll build:** Form to create shortened URLs.

**Implementation Steps:**
1. Create Dashboard Component
2. Create URL Shortener Form Component (URL input, expiration dropdown)
3. Create URL Service with `createShortUrl()` method
4. Add Form Validation (URL format, required fields)
5. Connect Form to Service (submit, display result)
6. Add Copy Button for shortened URL
7. Add Loading & Error States

**What you'll learn:** Select dropdowns, HTTP POST, Displaying async results, Clipboard API

**Deliverables:**
- ✅ Working URL shortening form
- ✅ API integration with `/api/v1/urls`
- ✅ Display shortened URL with copy button
- ✅ Loading and error states

---

### 🎯 Functionality 6: Display URL List

**What we'll build:** Table showing all user's shortened URLs.

**Implementation Steps:**
1. Create URL List Component
2. Add `getUserUrls()` method to URL Service (if backend supports)
3. Build Table UI with Angular Material table
4. Display: short URL, original URL, expiration, click count
5. Add Loading State (skeleton/spinner)
6. Handle Empty State (no URLs message)

**What you'll learn:** Material tables, Data binding, Loading states, Empty states

**Deliverables:**
- ✅ Table displays user's URLs
- ✅ Shows all relevant information
- ✅ Loading and empty states

---

### 🎯 Functionality 7: URL Actions (Copy, Deactivate)

**What we'll build:** Actions for each URL in the list.

**Implementation Steps:**
1. Add Copy Button to table rows (clipboard API, success feedback)
2. Add Deactivate Button with confirmation dialog
3. Implement `deactivateUrl()` in URL Service
4. Refresh list after deactivation
5. Add Material Snackbar for user feedback

**What you'll learn:** Material dialog, Material snackbar, HTTP DELETE, Data refresh

**Deliverables:**
- ✅ Copy URL functionality
- ✅ Deactivate URL with confirmation
- ✅ User feedback notifications

---

### 🎯 Functionality 8: Analytics View

**What we'll build:** Analytics page showing URL statistics with charts.

**Implementation Steps:**
1. Create Analytics Component with route parameter (`/analytics/:shortCode`)
2. Create Analytics Service with `getAnalytics(shortCode)` method
3. Build Overview Cards (total clicks, key metrics)
4. Install Chart Library (ng2-charts, chart.js)
5. Create Click Trends Chart (line chart)
6. Create Distribution Charts (countries, devices, browsers)
7. Add "View Analytics" button in URL list
8. Implement navigation to analytics page

**What you'll learn:** Route parameters, Third-party libraries, Chart.js, Data transformation

**Deliverables:**
- ✅ Analytics page with interactive charts
- ✅ API integration with `/api/v1/analytics/{shortCode}`
- ✅ Multiple chart types
- ✅ Navigation from URL list

---

### 🎯 Functionality 9: Polish & Production Ready

**What we'll build:** Error handling, loading states, responsive design, animations.

**Implementation Steps:**
1. Create Global Error Handling (error interceptor, user-friendly messages)
2. Create Reusable Loading Spinner Component
3. Centralize Toast Notifications (notification service)
4. Make Responsive (test on mobile, adjust table to cards)
5. Improve Form Validation Messages
6. Add Animations (route transitions, list animations)

**What you'll learn:** Global error handling, Reusable components, Responsive design, Animations, UX best practices

**Deliverables:**
- ✅ Comprehensive error handling
- ✅ Loading states everywhere
- ✅ Responsive on all devices
- ✅ Smooth animations
- ✅ Production-ready UX

---

### 🎯 Functionality 10: Password & Security Enhancements

**What we'll build:** Enhanced password security features and HTTPS configuration for production.

**Implementation Steps:**

#### A. Password Strength Indicator
1. Install password strength library (`npm install zxcvbn @types/zxcvbn`)
2. Create Password Strength Component
3. Add visual feedback (weak/medium/strong/very strong)
4. Display color-coded progress bar (red → yellow → green)
5. Show strength percentage
6. Integrate with Registration form

#### B. Enhanced Password Validation
1. Update password validators to require:
   - Minimum 8 characters (increase from current 6)
   - At least one uppercase letter (A-Z)
   - At least one lowercase letter (a-z)
   - At least one number (0-9)
   - At least one special character (!@#$%^&*)
2. Create custom validator for password complexity
3. Display validation rules checklist with visual feedback
4. Show green checkmarks as rules are satisfied
5. Prevent form submission until all rules pass

#### C. Show/Hide Password Toggle
1. Add eye icon button to password fields
2. Toggle between `type="password"` and `type="text"`
3. Change icon between eye-open and eye-closed
4. Apply to both password and confirm password fields
5. Improve UX while maintaining security

#### D. HTTPS Configuration & Security Headers
1. Generate self-signed certificate for local development
2. Configure Angular dev server for HTTPS
3. Update environment files with HTTPS URLs
4. Add security headers documentation
5. Create production deployment checklist

#### E. Additional Security Measures
1. Add rate limiting on login/registration forms (prevent spam)
2. Implement client-side input sanitization
3. Add CAPTCHA for registration (optional, using reCAPTCHA)
4. Create security best practices document
5. Add XSS protection reminders in code comments

**What you'll learn:**
- Third-party security libraries
- Custom form validators
- Password security best practices
- HTTPS configuration
- Client-side security measures
- Production security considerations

**Deliverables:**
- ✅ Password strength indicator with visual feedback
- ✅ Enhanced password validation (8+ chars, complexity requirements)
- ✅ Password visibility toggle
- ✅ HTTPS configuration for development
- ✅ Security documentation for production
- ✅ Rate limiting implementation
- ✅ Input sanitization

**Security Notes:**

⚠️ **CRITICAL: HTTPS is Required for Production**

Currently, passwords are sent over HTTP in plain text. This is **ONLY acceptable for local development**. For production:

1. **MUST use HTTPS/TLS** to encrypt data in transit
2. **MUST get SSL certificate** (Let's Encrypt is free)
3. **MUST configure Spring Boot for HTTPS**
4. **MUST update Angular environment** to use `https://` URLs

**Current Security Status:**
- ✅ Backend hashes passwords with BCrypt (protects at rest)
- ✅ Frontend validates password strength
- ❌ Passwords sent over HTTP (visible to attackers)
- ❌ No rate limiting (vulnerable to brute force)
- ❌ No HTTPS in development (bad practice to ignore)

**What Hashing Does:**
- Protects passwords **stored in database**
- One-way encryption (can't be reversed)
- Salted (prevents rainbow table attacks)

**What HTTPS Does:**
- Protects passwords **in transit over network**
- Encrypts entire request/response
- Prevents man-in-the-middle attacks
- **YOU NEED BOTH!**

---

## 📊 Progress Tracker

| Functionality | Status | Components | Services |
|--------------|--------|------------|----------|
| Setup | ✅ Done | - | - |
| 1. Login | 🔄 Next | login.component | auth.service (partial) |
| 2. Registration | ⏳ Pending | register.component | auth.service (extend) |
| 3. Auth Guard | ⏳ Pending | - | auth.guard, token.interceptor |
| 4. Navbar | ⏳ Pending | navbar.component | - |
| 5. URL Shortening | ⏳ Pending | dashboard, url-form | url.service |
| 6. URL List | ⏳ Pending | url-list | url.service (extend) |
| 7. URL Actions | ⏳ Pending | - | url.service (extend) |
| 8. Analytics | ⏳ Pending | analytics, charts | analytics.service |
| 9. Polish | ⏳ Pending | spinner, dialogs | notification.service |
| 10. Password Security | ⏳ Pending | password-strength | security validators |

---

## 🔌 API Integration Points

### Authentication Endpoints
```
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### URL Management Endpoints
```
POST   /api/v1/urls                    # Create short URL
GET    /api/v1/urls/{shortUrl}         # Get URL details
DELETE /api/v1/urls/{shortUrl}         # Deactivate URL
GET    /api/v1/urls/{shortCode}/stats  # Get URL analytics
```

### Analytics Endpoints
```
GET /api/v1/analytics/{shortCode}      # Detailed analytics
```

---

## 🛠️ Required Dependencies

```json
{
  "dependencies": {
    "@angular/animations": "^17.x",
    "@angular/common": "^17.x",
    "@angular/compiler": "^17.x",
    "@angular/core": "^17.x",
    "@angular/forms": "^17.x",
    "@angular/material": "^17.x",
    "@angular/platform-browser": "^17.x",
    "@angular/platform-browser-dynamic": "^17.x",
    "@angular/router": "^17.x",
    "rxjs": "~7.8.0",
    "chart.js": "^4.x",
    "ng2-charts": "^5.x"
  }
}
```

---

## 📝 Environment Configuration

### Development (environment.ts)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  appUrl: 'http://localhost:4200'
};
```

### Production (environment.prod.ts)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.yourdomain.com/api/v1',
  appUrl: 'https://yourdomain.com'
};
```

---

## 🎓 Learning Path

As you implement each phase, you'll progressively learn:

1. **Phase 1**: Angular basics, components, services, routing, HTTP
2. **Phase 2**: Forms, data binding, component communication
3. **Phase 3**: Third-party libraries, complex data visualization
4. **Phase 4**: Best practices, optimization, user experience

---

## ✅ Success Criteria

The frontend will be considered complete when:
- ✅ Users can register and login
- ✅ Users can create shortened URLs
- ✅ Users can view their URL list
- ✅ Users can copy shortened URLs
- ✅ Users can deactivate URLs
- ✅ Users can view detailed analytics
- ✅ All charts display correctly
- ✅ Application is responsive
- ✅ Error handling is comprehensive
- ✅ Loading states are shown appropriately

---

## 🚀 Getting Started

**Prerequisites:**
- Node.js (v18 or higher)
- npm (v9 or higher)
- Angular CLI (`npm install -g @angular/cli`)

**Initial Setup:**
```bash
# Create new Angular project
ng new url-shortener-frontend

# Navigate to project
cd url-shortener-frontend

# Install Angular Material
ng add @angular/material

# Start development server
ng serve
```

Access application at: `http://localhost:4200`
