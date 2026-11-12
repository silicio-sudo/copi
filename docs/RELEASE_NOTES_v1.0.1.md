# Copi 1.0.1 · targetSdk 36

Versión de mantenimiento para cumplir el requisito actual de Google Play.

- `targetSdk` y `compileSdk` actualizados a **36** (Android 16).
- `versionCode` **2**, `versionName` **1.0.1**.
- Sin cambios funcionales respecto a 1.0.0.
- AAB firmado con la misma upload key de Copi.
- Paquete Rallye: `com.silicio.copi.rallye`.
- Paquete estándar: `com.silicio.copi`.

## Descargas

| Archivo | Uso |
|---|---|
| `Copi-Rallye-1.0.1.aab` | Subir a Play Console (Internal testing/Internal App Sharing) |
| `Copi-1.0.1.aab` | Edición estándar |
| `Copi-Rallye-1.0.1-release.apk` | APK release para ADB/DHU |
| `Copi-1.0.1-release.apk` | Edición estándar |

## Play Console

1. Descarta la release con error de API 35.
2. Sube `Copi-Rallye-1.0.1.aab`.
3. Añade tu cuenta a la lista de **Testers** de Internal testing.
4. Revisa y empieza el rollout.
5. Instala desde el enlace de Play y conecta Android Auto.

Las advertencias de archivo de desofuscación y símbolos nativos no bloquean una
prueba interna. Se pueden resolver más adelante activando R8 y subiendo el
mapping/símbolos.
