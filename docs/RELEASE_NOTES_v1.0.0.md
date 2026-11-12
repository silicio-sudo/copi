# Copi 1.0.0 · copiloto de rally

Copi convierte una ruta o un GPX en pacenotes, las canta con voz natural y las
navega con GPS. Funciona sin conexión y tiene pantalla propia para Android Auto.

## Descargas

| Archivo | Qué es |
|---|---|
| `Copi-Rallye-1.0.0.apk` | Edición Rallye: LUIS FERNANDO, GitHub @silicio-sudo · @rsanmart, arte rallye rojo/plata/carbono |
| `Copi-1.0.0.apk` | Edición limpia: grafito/turquesa, sin personalización |
| `Copi-Manual-1.0.0.pdf` | Manual completo en A4, 23 páginas |

Los APK son **debug**, ya firmados con la clave de depuración de Android. No
necesitan Google Play ni firma release. Android no instala APK sin firmar; el
“sin firmar” de Android Auto significa “sin la firma/aprobación de Play”.

## Instalar

```powershell
adb install -r "Copi-Rallye-1.0.0.apk"
```

o copia el APK al móvil y ábrelo permitiendo orígenes desconocidos.

## Android Auto sin Play

1. Instala Copi en el móvil y ábrelo una vez.
2. Android Auto → Ajustes → toca **Versión** 10 veces.
3. Menú de tres puntos → **Ajustes de desarrollador** → **Fuentes desconocidas**.
4. Ajustes → **Personalizar lanzador** → activa Copi.
5. Conecta por cable o inalámbrico (Bluetooth + Wi-Fi) y abre Copi en el coche.

Funciona en Android Auto proyectado, tanto por cable como inalámbrico. El
`minCarApiLevel` es 1 y hay modo lista de reserva si el host no soporta HUD.
No incluye módulo para Android Automotive OS (AAOS).

Guía detallada: [docs/ANDROID_AUTO_SIDELOAD.md](docs/ANDROID_AUTO_SIDELOAD.md)

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
1e8f884f804d638bdd201e3070f8df8872d7d6d7a634a39424a7522bc384c2e0  Copi-Manual-1.0.0.pdf
```
