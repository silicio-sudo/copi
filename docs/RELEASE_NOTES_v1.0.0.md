# Copi 1.0.0 · copiloto de rally

Copi convierte una ruta o un GPX en pacenotes, las canta con voz natural y las
navega con GPS. Funciona sin conexión y tiene pantalla propia para Android Auto.

## Descargas

| Archivo | Qué es |
|---|---|
| `Copi-Rallye-1.0.0.aab` | Bundle firmado para Google Play / Internal App Sharing |
| `Copi-1.0.0.aab` | Bundle de la edición limpia |
| `Copi-Rallye-1.0.0-release.apk` | APK release firmado, para ADB/DHU |
| `Copi-1.0.0-release.apk` | APK release de la edición limpia |
| `Copi-Rallye-1.0.0.apk` | APK debug de Rallye, para móvil/DHU |
| `Copi-1.0.0.apk` | APK debug de la edición limpia |
| `Copi-Manual-1.0.0.pdf` | Manual completo en A4, 23 páginas |

Los APK son **debug**, ya firmados con la clave de depuración de Android. Sirven
para instalar en el móvil y probar en el **DHU**. Para que Copi aparezca en la
pantalla de un coche real, Android Auto exige una instalación desde Google Play
(Internal App Sharing o Internal Test Track); “Fuentes desconocidas” no se
aplica a la Car App Library.

## Instalar

```powershell
adb install -r "Copi-Rallye-1.0.0.apk"
```

o copia el APK al móvil y ábrelo permitiendo orígenes desconocidos.

## Android Auto sin Play

**Importante:** Android Auto no muestra apps de navegación instaladas por ADB o
con “Fuentes desconocidas”. Google exige una fuente de confianza para la Car App
Library. El APK debug sirve para el móvil y el DHU; para la pantalla del coche
hay que usar **Google Play Internal App Sharing** o un **Internal Test Track**
(requiere cuenta de Play Console y un AAB release firmado).

Si solo quieres probar la integración, usa el **Desktop Head Unit (DHU)**.
Guía detallada y alternativas:
[docs/ANDROID_AUTO_SIDELOAD.md](docs/ANDROID_AUTO_SIDELOAD.md)

## Funciones

- Pacenotes automáticas 1–6 desde GPX o ruta online, con crestas y modificadores.
- Roadbook CSV, biblioteca local, exportación y tramo demo.
- GPS: velocidad, media, máximo, cronómetro, progreso y alineación.
- Voz local neuronal + Google Cloud TTS / ElevenLabs opcionales con caché.
- Offline real para GPS, motor, roadbook, voz local y Android Auto.
- Android Auto: HUD, próximas notas, controles y notificación de navegación.
- Dos ediciones desde el mismo código.

## Verificación

- Compilan `Copi Rallye` y `Copi`.
- 21 tests OK (generador, geometría, CSV, voz, arranque y capturas).
- Lint sin errores.

## Aviso

Copi es un homenaje al arte rallye. No está afiliado a Mitsubishi Motors ni a
Ralliart. Conduce con prudencia y respeta las normas de tráfico. El manual y la
política de privacidad están en el repositorio.

## SHA-256

```text
7f297d792e1ccc04b5206da4d4fb596bc3dd1fe44ae492dcb61adea0d59764a9  Copi-Rallye-1.0.0.apk
2052bdaebbcbd6e86db3e36447530c292cd535a340f3034234b6f2e7cdf866af  Copi-1.0.0.apk
8aeb23446dbd741f4a3001b1668bc7a456aeee729d2b6337bb350761e3a1eb67  Copi-Manual-1.0.0.pdf
```
