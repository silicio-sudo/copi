# Política de privacidad de Copi

**Última actualización:** 12 de noviembre de 2026

Copi es una aplicación local de copiloto de rally. No hay servidores propios de
Copi, no hay cuentas, analítica, publicidad ni venta de datos.

## 1. Datos que se tratan en el dispositivo

- **Ubicación**: se usa para calcular velocidad, distancia, rumbo y avance de las
  pacenotes. Se procesa en el móvil y no se transmite a Copi.
- **Tramos y ajustes**: roadbooks, trazados GPX, preferencias y claves de voz se
  guardan en el almacenamiento privado de la app.
- **Caché de voz**: archivos de audio sintetizados, borrables desde Ajustes.
- **Historial de posición**: la estela del tramo se mantiene en memoria y no se
  guarda al cerrar la app.

## 2. Permisos

- **Ubicación precisa**: necesaria para navegar. Sin ella se puede usar el modo
  demo.
- **Notificaciones**: para el servicio en primer plano y los controles del tramo.
- **Internet**: solo para planificación online, geocodificación y voz en la nube.

## 3. Servicios de terceros (solo si los usas)

| Servicio | Datos enviados | Cuándo |
|---|---|---|
| **Nominatim / OpenStreetMap** | Texto de búsqueda y coordenadas | Al buscar un destino o un origen |
| **OSRM** | Coordenadas de origen y destino | Al generar una ruta online |
| **Google Cloud Text-to-Speech** | Texto de la nota | Si configuras una API key de Google |
| **ElevenLabs** | Texto de la nota | Si configuras una API key de ElevenLabs |

Estos servicios tienen sus propias políticas de privacidad y términos. Copi no
controla sus servidores. Si no configuras claves de voz, el texto nunca sale del
dispositivo.

## 4. Claves de API

Las claves se guardan en las preferencias privadas de la app, sin cifrado
adicional. Cualquier persona con acceso físico/root al dispositivo podría
leerlas. Se recomienda:

- crear claves específicas para Copi;
- restringirlas por API y, si es posible, por app Android;
- revocarlas si se filtran;
- no compartir capturas con las claves visibles.

## 5. Conservación y borrado

- Los tramos se borran desde la ficha del tramo.
- La caché de voz se borra desde Ajustes → Voces en la nube.
- Los ajustes se restablecen desde Ajustes → Aplicación.
- Desinstalar la app elimina todos los datos locales.

## 6. Seguridad

Copi no transmite datos a servidores propios. Las comunicaciones con
OpenStreetMap/OSRM/Google/ElevenLabs usan HTTPS. No se puede garantizar la
seguridad de redes o servicios de terceros.

## 7. Menores

Copi no está dirigida a menores de edad ni recoge datos personales de forma
consciente.

## 8. Cambios

Esta política puede actualizarse con nuevas funciones. La fecha de la cabecera
indica la última revisión.

## 9. Contacto

GitHub: [github.com/silicio-sudo](https://github.com/silicio-sudo) · @rsanmart

---

**Resumen:** Copi funciona en tu móvil, no tiene backend y no vende datos. Solo
salen datos si tú activas la planificación online o una voz en la nube.
