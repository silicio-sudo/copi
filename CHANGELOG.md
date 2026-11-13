# Changelog

## 1.1.0 — 2026-11-13

- Mapa real con OpenStreetMap y caché local de teselas.
- Nueva pestaña **Mapa** con ruta, posición actual y seguimiento.
- Emblema de tres rombos en la edición **Copi Rallye**.
- Icono de coche en la edición **Copi** limpia.
- `versionCode` 3, `versionName` 1.1.0.
- Sigue targetSdk 36.

## 1.0.1 — 2026-11-12

- `compileSdk` y `targetSdk` actualizados a **36** para cumplir el requisito de
  Google Play.
- `versionCode` 2 y `versionName` 1.0.1.
- Sin cambios funcionales.
- AAB firmado con la misma upload key.

## 1.0.0 — 2026-11-12

Primera versión funcional de Copi.

### Añadido

- Dos ediciones con el mismo motor: **Copi Rallye** (arte rallye, LUIS FERNANDO,
  @silicio-sudo · @rsanmart) y **Copi** (limpia, sin personalización).
- Generador automático de pacenotes 1–6 desde GPX o ruta OSRM.
- Detección de crestas cuando el GPX incluye altitud.
- Importación de roadbooks CSV (distancias absolutas o relativas) y exportación.
- Motor de tramo GPS: distancia, velocidad, media, máximo, cronómetro, pausa,
  alineación sobre el trazado, avance automático de notas y meta.
- Voz local con selección de la mejor voz instalada y opción de voz neuronal en
  la nube (Google Cloud TTS / ElevenLabs) con caché en disco.
- Modo demo sin GPS para pruebas y Android Auto.
- Pantalla de Android Auto con `MapWithContentTemplate`, HUD propio, lista de
  próximas notas, controles y `NavigationManager`.
- Notificación de navegación con controles y `CarAppExtender`.
- Planificación online opcional con Nominatim + OSRM y servidores configurables.
- Ajustes persistentes, biblioteca local de tramos, manual integrado y pantalla
  Acerca de.
- Tests unitarios del generador, geometría, CSV y frases de voz.

### Limitaciones conocidas

- La publicación en Google Play como app de navegación requiere aprobación de
  Google; el APK sideloaded funciona en modo desarrollador/DHU.
- Sin módulo Android Automotive OS (AAOS) en esta versión.
- La voz en la nube para distancias exactas necesita conexión; la caché cubre
  frases y anuncios comunes, no todas las combinaciones posibles.
- Los servidores públicos OSRM/Nominatim son de demostración y tienen límites.
