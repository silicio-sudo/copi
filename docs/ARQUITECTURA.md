# Arquitectura de Copi

## Visión general

Copi es una app Android nativa (Kotlin + Jetpack Compose) con dos productos que
comparten código:

- `rallye`: arte rallye, LUIS FERNANDO, GitHub @silicio-sudo · @rsanmart.
- `standard`: diseño limpio sin personalización.

La personalización se resuelve con **recursos por flavor** (strings, colores,
iconos), no con forks de código. `Brand.from(context)` carga la identidad en
tiempo de ejecución; `LocalBrand` la expone a Compose.

## Capas

```
ui/            Compose, pantallas, componentes, tema
car/           Android Auto (Car App Library)
nav/           motor de tramo, GPS, voz, servicio, controlador
data/          modelos, GPX/CSV, repositorio, generador, OSRM/Nominatim
core/          geometría, unidades, ajustes, red, RoutePath
```

### Flujo principal

```
MainActivity / CopiRoot
        │
        ├── DashboardScreen ── observa RallyState
        ├── RoadbooksScreen ── importa GPX/CSV, activa roadbook
        ├── PlannerScreen ──── Nominatim + OSRM → PacenoteGenerator
        └── SettingsScreen ─── AppSettings + VoiceGuidance
                                   │
                                   ▼
                             Copi (controller)
                    ┌──────────┼───────────┐
                    ▼          ▼           ▼
                RallyEngine  LocationProvider  VoiceGuidance
                    │                            │
                    ▼                            ▼
              RallyState (StateFlow)       TTS local / CloudTTS
                    │
        ┌───────────┼───────────────┐
        ▼           ▼               ▼
     Compose     RallyService    CopiCarScreen
                 (notificación)   (AA + HUD)
```

## Decisiones clave

### Motor sin Android

`RallyEngine` no depende de Android salvo `SystemClock`; recibe fixes y ticks.
Acumula distancia, avanza notas, decide cuándo hablar y emite un `StateFlow`.
Esto permite testear generación y geometría en JVM y mantener la UI tonta.

### Offline-first

- Roadbooks y trazados en `filesDir/roadbooks/*.json`.
- Ajustes en SharedPreferences.
- GPS con `LocationManager` (sin Google Play Services), para funcionar en
  cualquier dispositivo y sin red.
- Voz local con `TextToSpeech`; nube opcional con caché MP3.
- La vista de tramo dibuja el trazado en Canvas (sin teselas), por lo que no hay
  dependencia de mapas ni descargas.

### Voz

`VoiceGuidance` resuelve el modo:

```
AUTO → ElevenLabs (clave) → Google (clave) → local
```

Si la nube falla, se usa el TTS local. La cola es un `Channel` con generación
para descartar locuciones obsoletas. La caché se indexa por hash de
texto+voz+velocidad+tono.

### Android Auto

- `CopiCarAppService` (exportado, categoría `NAVIGATION`).
- `CopiCarScreen` implementa `SurfaceCallback` y dibuja el HUD en la superficie.
- `MapWithContentTemplate` + `ListTemplate` para el contenido.
- `NavigationManager` informa del estado y de un `Trip` con la siguiente nota.
- `minCarApiLevel=7` (MapWithContentTemplate).
- `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` para sideload/DHU. Para Play hay que
  sustituirlo por una allowlist y solicitar aprobación de Google.

## Generador de pacenotes

1. Filtra y deduplica puntos.
2. Remuestrea cada 8 m con distancias acumuladas.
3. Calcula rumbos suavizados (media circular).
4. Calcula el giro en ventanas de ±16 m y lo suaviza.
5. Detecta picos de giro, camina los bordes de la curva y calcula el ángulo neto.
6. Clasifica 1–6 y añade modificador (larga/corta/abre/cierra).
7. Si hay altitud, detecta crestas.
8. Añade salida y meta; ordena y elimina duplicados cercanos.

## Formatos

- GPX: `trkpt`, `rtept`, `wpt`, `<ele>` opcional.
- CSV: `distancia_m,tipo,severidad,modificador,nota`; relativo con `+`.
- JSON interno: `Roadbook` serializado con kotlinx.serialization.

## Tests

- `PacenoteGeneratorTest`: esquina 90°, horquilla, demo, orden.
- `RoadbookCodecTest`: CSV absoluto/relativo, JSON, frases.
- `RoutePathTest`: interpolación y encaje GPS.
- `GeoTest`: distancia, rumbo, offset.

## Extensiones previstas

- Mapas OSM offline (teselas) con atribución.
- Módulo Android Automotive OS (`app-automotive`).
- Splits y sectores.
- Exportación GPX del roadbook.
- Perfiles de voz por idioma.
- Grabación de notas de voz propias.
- Sincronización opcional entre dispositivos.

## Convenciones de build

- `compileSdk 35`, `minSdk 26`, `targetSdk 35`.
- JDK 17/21, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10.01.
- Debug package suffix `.debug`; los dos flavors pueden convivir.
