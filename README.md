<div align="center">

# ⚽ App CSD Estrella Roja

### Ecosistema Digital del **Club Social y Deportivo Estrella Roja**

*Plataforma de gestión de socios y consulta de cuentas corrientes*

---

[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android)](https://developer.android.com)
[![Desktop](https://img.shields.io/badge/Escritorio-Compose%20for%20Desktop-4285F4?logo=kotlin)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Room](https://img.shields.io/badge/Persistencia-Room%20%2B%20SQLite-FF6F00?logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Gemini](https://img.shields.io/badge/IA-Gemini%20API-8E24AA?logo=google)](https://ai.google.dev)

</div>

---

## 🏟️ ¿Qué es este proyecto?

Este repositorio contiene el **ecosistema de aplicaciones del Club Social y Deportivo Estrella Roja**, un club de fútbol que necesita digitalizar la gestión de su padrón de socios y las cuentas corrientes de cuotas mensuales.

El ecosistema se compone de **dos aplicaciones complementarias** que comparten una base de datos SQLite local:

| Módulo | Plataforma | Público Objetivo |
|---|---|---|
| **App Móvil** (`:app`) | Android (Jetpack Compose) | Socios del club |
| **App de Escritorio** (`:desktop`) | JVM (Compose for Desktop) | Administradores del club |

---

## 📱 App Móvil — Socio (`app/`)

La aplicación Android permite a cada socio acceder a su cuenta personal y consultar el estado de sus cuotas.

### Funcionalidades implementadas

- **Pantalla de Autenticación** — Inicio de sesión con RUT y contraseña (integrado con Room).
- **Dashboard del Socio** — Vista de bienvenida con resumen del perfil personal.
- **Consulta de Saldos** — Listado de cuotas mensuales con estado **Pagada / Impaga**, saldo total deudor y situación financiera.
- **Carga automática del Padrón** — Al primer arranque, `DatabaseSeeder` parsea el archivo `padron_socios.csv` y puebla la base de datos Room sin intervención del usuario.

### Stack técnico (Android)

- **UI:** Jetpack Compose + Material 3 (paleta deportiva Rojo Albirrojo `#D32F2F` / Carbono `#212121`)
- **Arquitectura:** MVVM con `ClubViewModel` + `StateFlow`
- **Persistencia:** Room Database sobre SQLite
- **IA (opcional):** Gemini API mediante variable de entorno `GEMINI_API_KEY`

---

## 🖥️ App de Escritorio — Administrador (`desktop/`)

Aplicación JVM independiente para la gestión del padrón de socios desde la oficina del club.

### Funcionalidades implementadas

- **Vista Maestro-Detalle** del padrón de socios.
- **Búsqueda en tiempo real** por nombre, RUT o ID.
- **Panel de estado financiero** por socio.
- **Conexión directa** al mismo archivo SQLite compartido con la app móvil.

### Stack técnico (Escritorio)

- **UI:** Compose for Desktop (JVM)
- **Persistencia:** `org.xerial:sqlite-jdbc` (conexión JDBC directa al archivo `club_social_futbol_db`)
- **Arquitectura:** ViewModel reactivo con `MutableStateFlow`

---

## 🗂️ Estructura del Repositorio

```
App-CSD-ER/
├── app/                          # Módulo Android (socios)
│   └── src/main/java/com/example/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── database/         # ClubDatabase.kt, DatabaseSeeder.kt
│       │   ├── dao/              # DAOs de Room
│       │   ├── model/            # Entidades Room
│       │   ├── repository/       # Repositorios de datos
│       │   └── service/          # Servicios de dominio
│       └── ui/
│           ├── MainApp.kt        # Navegación principal
│           ├── auth/             # AuthScreens.kt (pantalla de login)
│           ├── dues/             # MemberDashboardScreen.kt, MemberSaldosTab.kt
│           ├── theme/            # Paleta de colores y tipografía
│           └── viewmodel/        # ClubViewModel.kt
├── desktop/                      # Módulo JVM (administradores)
│   └── src/main/kotlin/com/example/
│       ├── data/database/        # DesktopDatabaseConnector.kt
│       └── ui/admin/desktop/     # AdminDesktopMain.kt, AdminDesktopViewModel.kt
├── padron_socios.csv             # Fuente de verdad del padrón inicial
├── DEPLOYMENT_INSTRUCTIONS.md   # Guía detallada de despliegue
├── INSTRUCCIONES.md             # Instrucciones operativas adicionales
├── build.gradle.kts             # Configuración raíz de Gradle
├── settings.gradle.kts          # Registro de módulos (:app, :desktop)
└── .env.example                 # Plantilla de variables de entorno
```

---

## 🚀 Cómo ejecutar localmente

### Prerrequisitos

- [Android Studio](https://developer.android.com/studio) con el SDK de Android instalado
- JDK 17
- Un dispositivo Android físico (con Depuración USB activa) **o** un AVD configurado en Android Studio

### 1. Configuración inicial

```bash
# Clonar el repositorio
git clone https://github.com/<tu-usuario>/App-CSD-ER.git
cd App-CSD-ER

# Crear el archivo de variables de entorno
cp .env.example .env
# Editar .env y agregar la GEMINI_API_KEY si se desea usar la IA
```

### 2. App Android

```bash
# Compilar el APK de depuración
./gradlew :app:assembleDebug

# Instalar en el dispositivo o emulador conectado
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Lanzar la actividad principal
adb shell am start -n com.example.app/.MainActivity
```

> **Nota:** Si Android Studio muestra un error de `signingConfig`, comenta la línea `signingConfig = signingConfigs.getByName("debugConfig")` en `app/build.gradle.kts`.

### 3. App de Escritorio (Administrador)

```bash
# Compilar y lanzar la ventana de escritorio
./gradlew :desktop:run
```

Para instrucciones más detalladas, consulta [`DEPLOYMENT_INSTRUCTIONS.md`](DEPLOYMENT_INSTRUCTIONS.md).

---

## 🗺️ Hoja de Ruta — Próximos Pasos hacia Producción

### Fase 2 — Sincronización en la Nube ☁️
- [ ] Migrar la base de datos SQLite local a **Firebase Firestore** o un servidor PostgreSQL compartido.
- [ ] Implementar **autenticación real** con Firebase Auth (reemplazando el login local de Room).
- [ ] Sincronización bidireccional: cambios del administrador → visibles en la app del socio en tiempo real.

### Fase 3 — Funcionalidades del Socio 📲
- [ ] **Carnet Digital** con código QR generado dinámicamente.
- [ ] **Notificaciones Push** cuando una cuota vence o se registra un pago.
- [ ] **Historial de pagos** con descarga de comprobantes en PDF.
- [ ] **Reserva de instalaciones** (canchas, salones) desde la app.

### Fase 4 — Administración Avanzada 🖥️
- [ ] **Alta y baja de socios** desde la app de escritorio con validación de RUT.
- [ ] **Generación de cobros masivos** (emitir cuotas para todos los socios activos en un clic).
- [ ] **Reportes y exportación** a Excel/PDF del estado de morosidad.
- [ ] **Roles de usuario:** Tesorero, Secretario, Presidente.

### Fase 5 — Despliegue en Producción 🏁
- [ ] Firmar el APK con un keystore de producción y publicar en **Google Play Store**.
- [ ] Empaquetar la app de escritorio como instalador `.exe` (Windows) usando `./gradlew :desktop:packageMsi`.
- [ ] Configurar CI/CD con **GitHub Actions** para compilaciones automáticas en cada push a `main`.
- [ ] Implementar monitoreo de errores con **Firebase Crashlytics**.

---

## 📄 Variables de Entorno

| Variable | Descripción | Requerida |
|---|---|---|
| `GEMINI_API_KEY` | Clave de API de Google Gemini para funcionalidades de IA | Opcional |

Copia `.env.example` a `.env` y completa los valores necesarios.

---

## 👥 Contribuciones

Este proyecto es de uso interno del **Club Social y Deportivo Estrella Roja**. Para reportar errores o proponer mejoras, abre un Issue en este repositorio.

---

<div align="center">
<sub>Desarrollado con ❤️ para el Club Social y Deportivo Estrella Roja · 2025–2026</sub>
</div>
