# 📱 Guía de Configuración y Pruebas - Club Social y Deportivo Estrella Roja

Esta guía detalla los pasos exactos para abrir, compilar, sincronizar y probar todas las funcionalidades (incluyendo el análisis inteligente con la API de Gemini) de la aplicación.

---

## 🛠️ Requisitos Previos

Antes de comenzar, asegúrate de tener instalado en tu computadora:
* **[Android Studio](https://developer.android.com/studio)** (Versión recomendada: Jellyfish o superior)
* **SDK de Android** instalado (compatible con API 36 / Android 14/15)
* Un **Emulador Android** configurado en Android Studio o un **dispositivo móvil físico** con la *Depuración USB* activada.

---

## 🚀 Paso 1: Configurar las Variables de Entorno (.env)

La aplicación utiliza la API de Google Gemini para realizar visión computacional sobre los comprobantes de pago. 

1. Confirma que el archivo `.env` en la raíz del repositorio contenga tu clave de API válida:
   ```env
   GEMINI_API_KEY=AIzaSyCL... (Tu Clave de Gemini verificada)
   ```
   > [!NOTE]
   > Ya hemos comprobado la validez de tu clave localmente y la conexión con los servidores de Google se realiza con éxito.

---

## 💻 Paso 2: Importar y Compilar el Proyecto

1. Abre **Android Studio**.
2. Selecciona **File > Open** (o **Open An Existing Project**).
3. Navega hasta el directorio de este repositorio (`c:\ReposGitHub\App-CSD-ER`) y selecciónalo.
4. Espera a que Android Studio descargue las dependencias de Gradle y realice el indexado inicial.
   > [!TIP]
   > Ya hemos configurado y comentado la línea `signingConfig` conflictiva en `app/build.gradle.kts`, por lo que el proyecto compilará sin ningún conflicto de firmas digitales en tu máquina de desarrollo.

---

## 🧪 Paso 3: Flujos de Prueba y Cuentas Predeterminadas

Una vez que la aplicación se esté ejecutando en tu emulador o dispositivo móvil, puedes probar los dos roles de usuario principales gracias a los datos simulados ya integrados:

### 👤 Rol 1: Cuenta de Socio (Vista Móvil de Pagos)
* **Correo:** `socio@club.com`
* **Contraseña:** `123`
* **Acciones recomendadas:**
  1. **Visualizar Deudas:** Verás las cuotas pendientes ("Equipamiento Nuevas Camisetas" e "Inscripción Torneo Relámpago").
  2. **Pagar Cuota con IA:** Presiona una cuota pendiente y selecciona subir comprobante. Al estar la clave `.env` activa, la IA de Gemini (`gemini-3.5-flash`) escaneará la imagen ficticia generada y validará los importes de forma automática.
  3. **Historial:** Recibirás notificaciones inmediatas confirmando la acreditación.

### 💼 Rol 2: Cuenta de Administración (Modo Escritorio PC)
* **Correo:** `admin@club.com`
* **Contraseña:** `123`
* **Acciones recomendadas:**
  1. **Dashboard Administrativo:** Tendrás acceso completo al estado financiero local del club.
  2. **SecretaríaClub Pro v3.7 (Desktop PC Simulator):** Dirígete a la pestaña de simulación de PC para abrir la interfaz de escritorio integrada.
  3. **Generación de Socios:** Registra nuevos miembros ficticios del club entregándoles una clave inicial.
  4. **Planilla Sync (Google Sheets Virtual):**
     * Añade nuevas filas ficticias con correos de tus socios de prueba.
     * Presiona **Sincronizar Planilla**.
     * Observa la consola en tiempo real (Terminal Sync Monitor) procesando, validando y emitiendo los estados de deuda y notificaciones a los socios de manera asíncrona.

---

## 🔍 Resolución de Problemas Comunes

* **Error de Gradle (Sync Error):** Ve a `File > Invalidate Caches / Restart` y vuelve a sincronizar Gradle (`Build > Rebuild Project`).
* **Error de conexión con Gemini:** Asegúrate de que tu computadora o emulador tengan conexión a Internet activa para que la app pueda llamar al endpoint de Google Generative Language.
