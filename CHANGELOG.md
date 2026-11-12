# Changelog

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
