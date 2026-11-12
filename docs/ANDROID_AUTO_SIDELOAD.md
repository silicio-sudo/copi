# Copi en Android Auto: por qué no aparece y cómo probarlo

**Actualizado:** 12 de noviembre de 2026

## Resumen

Copi usa la **Android for Cars App Library** (categoría Navegación). Google
restringe estas apps a **fuentes de confianza** (Google Play, Internal App
Sharing o Internal Test Track). La opción **“Fuentes desconocidas”** de Android
Auto **no se aplica** a apps de la Car App Library.

Por eso Copi **no aparece en el lanzador de Android Auto** al instalar el APK
por ADB, ni en el ASX oficial ni en una pantalla genérica. No es un problema del
APK ni de la pantalla.

> Documentación oficial: “This setting applies to media, messaging notifications,
> and parked apps but doesn't apply to apps built using the Android for Cars App
> Library.”
> https://developer.android.com/training/cars/testing#unknown-sources

## Qué sí funciona

### 1. Desktop Head Unit (DHU) — probar la app sin coche

El DHU sí permite ejecutar la app instalada por ADB para desarrollo.

1. SDK Manager → **Android Auto Desktop Head Unit Emulator**.
2. Android Auto del móvil → Ajustes de desarrollador → **Iniciar servidor de
   unidad principal**.
3. Conecta el móvil por USB:

```bash
adb forward tcp:5277 tcp:5277
```

4. Lanza el DHU:

```powershell
%LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe
```

5. En el DHU abre el cajón de apps y elige Copi.

Esto confirma que el APK y la integración funcionan, pero **no lo hace aparecer
en el coche real**.

### 2. Google Play Internal App Sharing — la vía recomendada

Es la forma oficial de instalar una app de coche sin publicarla ni pasar una
revisión pública. La app se instala desde Google Play, así que Android Auto la
considera de confianza.

Requisitos:

- Cuenta de **Google Play Console** (alta única de 25 USD).
- AAB o APK **release firmado** (no debug).
- Un enlace de testers de Internal App Sharing.

Pasos generales:

1. Crea la app en Play Console con el paquete `com.silicio.copi.rallye`.
2. Sube un **AAB release firmado** a **Internal app sharing**.
3. Añade tu cuenta de Google como tester.
4. Abre el enlace de opt-in en el móvil e instala Copi desde Play.
5. Conecta Android Auto; Copi aparecerá en el lanzador.
6. Opcional: crea un **Internal test track** para actualizaciones.

Ventajas: sin revisión pública, sin listado público, sin esperar aprobación de la
categoría Navegación. Para publicar en producción sí hará falta la aprobación de
Google para Navegación.

### 3. Internal Test Track / producción

Si ya tienes Play Console, puedes subir el AAB a un canal de pruebas interno y
distribuirlo a testers. La app se instala desde Play y Android Auto la acepta.
Para que sea pública como app de navegación, Google debe aprobar la categoría.

### 4. Usar Copi en el móvil, no en la pantalla del coche

Copi funciona completo en el móvil: GPS, roadbook, voz local y modo demo. Si no
quieres pasar por Play Console, puedes usar el móvil en un soporte. Es la opción
inmediata, sin depender de Android Auto.

## Lo que no funciona

- Instalar el APK por ADB y activar “Fuentes desconocidas”: Android Auto no
  mostrará una app de Car App Library.
- Instalar el APK en la pantalla genérica si esta solo proyecta Android Auto: la
  pantalla no ejecuta el APK; lo ejecuta el móvil.
- Firmar con la clave debug y esperar que Play lo acepte: Play requiere una
  firma release para Internal App Sharing.
- Usar herramientas de terceros que modifican Android Auto: pueden violar los
  términos de Google, romper la seguridad y no garantizan nada. No se
  recomiendan.

## Sobre “sin firmar”

Android no instala APKs sin firmar. El APK debug ya está firmado con la clave de
depuración y sirve para ADB/DHU. Para Play Internal App Sharing hace falta una
**firma release**:

- crea un keystore de subida;
- configura `signingConfigs` en `app/build.gradle.kts`;
- genera `bundleRallyeRelease`;
- guarda el keystore y las contraseñas fuera del repositorio.

## Si quieres que lo prepare

Puedo dejar listo:

- un **AAB release firmado** de Copi Rallye y de Copi;
- un keystore local con contraseña aleatoria;
- una guía exacta de Play Console para Internal App Sharing;
- el APK release firmado para instalar por ADB/DHU.

Solo hace falta tener o crear la cuenta de Play Console y decidir si el
repositorio sigue siendo público.

## Diferencias ASX / pantalla genérica

No cambian la causa: las dos son pantallas de **Android Auto proyectado**. La
restricción de fuentes de confianza se aplica igual. Si en el futuro alguna
pantalla ejecuta Android completo (AAOS o Android normal), se podría instalar
Copi directamente en ella, pero eso es otro tipo de integración.
