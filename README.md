# Hüma — Hasta Memnuniyet Anket ve Geri Bildirim Sistemi

**Hüma**, hastaneler ve sağlık kuruluşları için geliştirilmiş; anonim hasta memnuniyet anketlerini, birim bazlı kalite metriklerini, anket versiyonlamasını, Hasta Hakları onay akışını ve KVKK uyumlu yapay zekâ destekli yorum özetlemeyi tek çatı altında yöneten tam yığın (full-stack) web uygulamasıdır.

> **Temel İlke:** Sistem hasta kimliğini toplamadan güvenilir hizmet kalitesi ölçümü yapmak üzere tasarlanmıştır. Doktor puanlama veya sıralama mekanizması içermez; birim bazlı sürekli iyileştirme ve kurumsal Hasta Hakları sürecini destekler.

---

## Teknoloji Yığını ve Mimari

| Katman | Teknolojiler |
|---|---|
| **Backend** | Java 25 LTS (`pom.xml` / Temurin 25), Spring Boot 3.5.5, Spring Web, Spring Security, Spring Data JPA (Hibernate), Maven |
| **Veritabanı & Migration** | PostgreSQL 17, Flyway (`V1`–`V6`), üretim profilinde `ddl-auto: validate` |
| **API Dokümantasyonu** | Springdoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`, `/v3/api-docs`) |
| **Frontend** | React 19, Vite, JavaScript, Modüler CSS, Lucide Icons, Nginx Reverse Proxy |
| **Altyapı & Test** | Docker & Docker Compose, JUnit 5, Mockito, Spring MockMvc (H2), Vitest, React Testing Library, Playwright |

### Katmanlı / Onion Mimari Yapısı

```text
src/main/java/com/hbys/survey/
├── presentation/      # REST Controller'lar ve GlobalExceptionHandler (standart JSON hata formatı)
├── application/       # Use-case servisleri, DTO'lar, mapper'lar, AI port/PII maskeleme arayüzleri
├── domain/            # JPA Entity'leri ve iş kuralları (SurveyVersion, QuestionScope, AISummary vb.)
└── infrastructure/    # Spring Data JPA Repository'ler, Spring Security/Session/RateLimit filtreleri, AI adaptörleri (Mock & OpenAI)

frontend/src/
├── api.js             # CSRF destekli API istemcisi ve oturum zaman aşımı yönetimi
├── components/        # SurveyView, TrackingView, DashboardPanel, SubmissionsPanel, PatientRightsPanel, ReportPanel, SummaryPanel, AdminPanel
└── utils/             # Türkiye saat dilimi (Europe/Istanbul) tarih ve durum biçimleyicileri
```

---

## Temel Özellikler

### 1. Dinamik Departman, Ortak + Birime Özel Anket ve Versiyonlama
- **Varsayılan Hazır Kurulum (`V5`–`V6`):** Sistem ilk açıldığında 8 standart hastane birimi (*Acil Servis, Poliklinik, Yatan Hasta, Laboratuvar, Radyoloji, Kadın Doğum, Çocuk Sağlığı ve Hastalıkları, Ameliyathane ve Yoğun Bakım*) ve yayında olan **`Hüma Genel Hasta Memnuniyet Anketi` (`v1`)** (5 ortak + 14 birime özel soru) kullanıma hazır gelir.
- **Ortak (`COMMON`) ve Departmana Özel (`DEPARTMENT_SPECIFIC`) Sorular:** Ortak sorular tüm birimlerde tekil olarak paylaşılır (`department_id IS NULL`); birime özel sorular yalnızca ilgili birimde görünür ve veritabanı `CHECK` kısıtı (`chk_question_scope_department`) ile korunur.
- **Anket Versiyonlama (`SurveyVersion`):** Yayımlanan (`PUBLISHED`) sürümler değiştirilemez. Yeni `DRAFT` sürüm oluşturulduğunda aktif sorular otomatik klonlanır; yeni sürüm yayımlandığında önceki sürüm `ARCHIVED` durumuna geçer ve eski gönderimler/raporlar kendi sürümünde korunur.

### 2. Anonimlik, QR Erişim ve Güvenlik Mimarisi
- **Hasta Kimliği Yoktur:** Ad, soyad, T.C. kimlik numarası veya browser fingerprint toplanmaz.
- **Hash Tabanlı Token & 24 Saat Tekrar Engeli:** QR erişim kodları (`SurveyAccess`), takip numaraları (`TRK-...`) ve `HttpOnly` anonim istemci çerezi (`HBYS_CLIENT_TOKEN`) veritabanında yalnızca **SHA-256 hash** olarak saklanır. Aynı anonim istemci 24 saat içinde aynı ankete tekrar gönderim yapamaz.
- **Spring Security & CSRF:** Oturum tabanlı kimlik doğrulama, `X-XSRF-TOKEN` CSRF koruması, BCrypt parola hashleme ve backend zorunlu RBAC (`ADMIN`, `STAFF`, `PATIENT_RIGHTS_OFFICER`).
- **Tek Cihaz Oturum Koruması & Rate Limiting:** Aynı hesapla başka cihazdan giriş yapıldığında önceki oturum anında sonlandırılır (`401 SESSION_EXPIRED`). `/api/public/**` ve `POST /api/auth/login` uç noktaları bellek içi IP hız sınırlaması (`429 RATE_LIMIT_EXCEEDED`) ile korunur.
- **Aynı Birimde Taslak Sahipliği:** Bir personelin oluşturduğu yanıt taslağını aynı birimdeki başka bir personel değiştiremez; yalnızca taslağı oluşturan kullanıcı veya `ADMIN` düzenleyebilir.

### 3. Raporlama, Dashboard, Bildirimler ve Hasta Hakları Akışı
- **Dashboard ve Metrik Raporlama:** Departman bazlı memnuniyet ortalamaları, ortak/özel metrik kırılımları, 1–5 puan dağılımı, günlük zaman trendi, `n < 3` yetersiz veri uyarısı ve tekrarlayan şikâyet temaları.
- **Puan Bazlı Bildirimler:** Genel puan `≤ 3` düşük puan bildirimi, `≤ 2` kritik bildirim (`SYSTEM_ALERT`) üretir ve ilgili birim personeli ile adminlere iletilir.
- **Hasta Hakları Onay Süreci:** Personel yanıt taslağı yazar → Hasta Hakları birimi onaylar, revize ister veya reddeder → yalnızca onaylı yanıt hastanın takip ekranında yayımlanır → hasta yanıtı yalnızca bir kez değerlendirebilir.

### 4. KVKK Uyumlu AI Yorum Özetleme Altyapısı
- Serbest metin yorumlardaki TCKN, telefon, e-posta, IBAN ve takip kodları AI sağlayıcısına ulaşmadan önce `PiiMasker` ile maskelenir.
- Sentiment skoru, bildirim veya otomatik yetki üretmez; tekrarlayan sorunları ve güçlü alanları doğal dilde özetler.
- Varsayılan `HBYS_AI_PROVIDER=mock` dış anahtar olmadan çalışır. Gerçek sağlayıcı için `HBYS_AI_PROVIDER=openai` ve `OPENAI_API_KEY` tanımlanabilir (anahtar yoksa sahte başarı üretmez, `FAILED` olarak kaydeder).

---

## Docker Compose ile Tek Adımda Kurulum ve Çalıştırma

Ön koşul: Bilgisayarınızda **Docker Desktop** (veya Docker Engine + Compose) kurulu olmalıdır. Yerel JDK veya Node.js kurulumuna gerek yoktur.

### 1. Ortam dosyasını oluşturun
```bash
cp .env.example .env
```
*(Windows PowerShell için: `Copy-Item .env.example .env`)*

Ardından `.env` dosyasını açıp şu iki alanı kendi yerel parolalarınızla güncelleyin:
- `POSTGRES_PASSWORD`
- `HBYS_ADMIN_PASSWORD`

### 2. Tüm sistemi derleyip ayağa kaldırın
```bash
docker compose up -d --build
```

Bu komut PostgreSQL 17, Spring Boot backend (Flyway `V1–V6` migration dahil) ve React/Nginx frontend konteynerlerini sağlık kontrolleriyle sırayla başlatır.

### 3. Sistemi durdurma
```bash
docker compose down
```
*(Veritabanı verileri `hbys-postgres-data` volume'unda saklanır; tamamen sıfırlamak için `docker compose down -v` kullanılır.)*

---

## Erişim Adresleri ve Giriş Bilgileri

| Bileşen | Adres |
|---|---|
| **Web Uygulaması (Hüma Arayüzü)** | http://localhost:3000 |
| **Swagger / OpenAPI UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON Şeması** | http://localhost:8080/v3/api-docs |
| **Backend API** | http://localhost:8080 |

### Başlangıç Yönetici Hesabı
İlk açılışta `.env` dosyasında belirlediğiniz `HBYS_ADMIN_USERNAME` (varsayılan şablonda `admin`) ve `HBYS_ADMIN_PASSWORD` değerleriyle otomatik olarak bir `ADMIN` hesabı oluşturulur.

1. http://localhost:3000 adresini açıp üst menüden **Personel** sekmesine tıklayın ve `.env`'deki admin bilgileriyle giriş yapın.
2. **Yönetim → Kullanıcılar & Roller** sekmesinden `STAFF` (birim personeli) ve `PATIENT_RIGHTS_OFFICER` (Hasta Hakları) test hesapları oluşturabilirsiniz.
3. **Yönetim → Anketler & QR Erişim** sekmesinden hazır gelen **`Hüma Genel Hasta Memnuniyet Anketi`** ve istediğiniz departman için bağlantı üretip **Anketi yeni sekmede test et** butonuyla hasta akışını deneyebilirsiniz.

---

## Test ve Derleme Komutları (Yerel Geliştirme)

```bash
# Backend derleme, birim ve entegrasyon testleri (22 test - H2 PostgreSQL modu)
mvn verify

# Frontend birim/bileşen testleri ve production build
cd frontend
npm ci
npm test
npm run build
```

---

## Temel API Uç Noktaları

- **Kimlik Doğrulama:** `GET /api/auth/csrf`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`
- **Public Anket & Takip:** `GET /api/public/surveys/access/{token}`, `GET /api/public/surveys/access/{token}/questions`, `POST /api/public/surveys/{token}/submissions`, `GET /api/public/surveys/tracking/{trackingNumber}`, `GET /api/public/surveys/tracking/{trackingNumber}/feedback`, `POST /api/public/surveys/tracking/{trackingNumber}/feedback/satisfaction`
- **Admin Yönetimi:** `/api/admin/departments`, `/api/admin/users`, `/api/admin/surveys`, `/api/admin/survey-accesses`, `/api/admin/surveys/{surveyId}/questions`, `/api/admin/surveys/{surveyId}/versions`
- **Personel & Hasta Hakları:** `/api/staff/submissions`, `/api/staff/feedback`, `/api/notifications`, `/api/patient-rights/evaluations`, `GET /api/patient-rights/evaluations/queue`
- **Raporlama & AI Özeti:** `GET /api/reports/dashboard`, `POST /api/reports/departments/summary`, `/api/ai-summaries`
