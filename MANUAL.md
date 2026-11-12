# Copi · Manual de uso y configuración

**Versión 1.0.0 · Android 8.0+ · dos ediciones: Copi Rallye y Copi**

Copi es un copiloto de rally para el móvil y para Android Auto. Genera
pacenotes a partir de un GPX o de una ruta, las canta con voz natural y las
navega con GPS. El motor del tramo, el roadbook y la voz local funcionan sin
conexión.

> **Seguridad primero.** Copi es una ayuda para conducción deportiva en circuitos
> cerrados o tramos autorizados. Respeta las normas de tráfico, no manipules el
> móvil conduciendo y no confíes ciegamente en una nota: la responsabilidad es
> siempre del piloto.

---

## Índice

1. [Qué es Copi y qué edición elegir](#1-qué-es-copi-y-qué-edición-elegir)
2. [Requisitos e instalación](#2-requisitos-e-instalación)
3. [Primer arranque](#3-primer-arranque)
4. [Conceptos: tramo, roadbook y pacenote](#4-conceptos-tramo-roadbook-y-pacenote)
5. [Crear o importar un tramo](#5-crear-o-importar-un-tramo)
6. [Navegar el tramo](#6-navegar-el-tramo)
7. [La voz del copiloto](#7-la-voz-del-copiloto)
8. [Offline y online](#8-offline-y-online)
9. [Android Auto](#9-android-auto)
10. [Ajustes, uno a uno](#10-ajustes-uno-a-uno)
11. [Formatos de archivo](#11-formatos-de-archivo)
12. [Privacidad y datos](#12-privacidad-y-datos)
13. [Solución de problemas (FAQ)](#13-solución-de-problemas-faq)
14. [Compilar, firmar y publicar](#14-compilar-firmar-y-publicar)
15. [Avisos legales](#15-avisos-legales)

---

## 1. Qué es Copi y qué edición elegir

Copi convierte una carretera en un roadbook:

1. Importas un **GPX** o pides una ruta online.
2. Copi analiza la geometría y detecta cada curva.
3. Cada curva se convierte en una **pacenote** con dirección (izquierda/derecha),
   severidad (1–6), modificador (larga, corta, cierra, abre) y distancia.
4. Al pulsar **INICIAR TRAMO**, el GPS avanza por la lista y la voz canta las
   notas con la antelación configurada.

### Ediciones

| | **Copi Rallye** | **Copi** |
|---|---|---|
| Nombre visible | Copi Rallye | Copi |
| Diseño | Arte rallye: carbono, rojo, plata, franjas diagonales | Grafito y turquesa, sobrio |
| Personalización | **LUIS FERNANDO** como piloto | Ninguna |
| Pie de pantalla | GitHub **@silicio-sudo · @rsanmart** | Solo versión |
| Paquete Android | `com.silicio.copi.rallye` | `com.silicio.copi` |
| APK debug | `app-rallye-debug.apk` | `app-standard-debug.apk` |

Comparten el 100 % del motor. La edición solo cambia recursos de marca
(`app/src/rallye/res` frente a `app/src/standard/res` y `app/src/main/res`), no
la lógica.

**Nombre recomendado:** Copi. Es corto, se pronuncia igual en español e inglés,
se asocia a “copiloto” y funciona bien como marca (“Copi Rallye”). Alternativas
si quieres otro registro: *Tramo*, *Pacenote*, *Copi Cero*, *Roadbook*.

---

## 2. Requisitos e instalación

### Móvil

- Android 8.0 (API 26) o superior.
- GPS y brújula recomendables; el rumbo también se deduce del movimiento.
- 100 MB libres, aproximadamente.
- Android Auto: móvil con la app Android Auto y un coche compatible, o el
  emulador **Desktop Head Unit (DHU)** de Google.
- Voz: cualquier motor TTS instalado. Recomendado Google Speech Services con la
  voz de español descargada.

### Instalar el APK

1. Copia el APK al móvil o conéctalo por USB.
2. Permite la instalación desde esa fuente (Ajustes → Seguridad → Orígenes
   desconocidos) o instala con ADB.
3. Abre Copi y concede ubicación.

```powershell
# Windows, con el móvil conectado y depuración USB activa
adb install -r app\build\outputs\apk\rallye\debug\app-rallye-debug.apk
adb install -r app\build\outputs\apk\standard\debug\app-standard-debug.apk
```

Las dos ediciones pueden convivir. Los APK debug se firman con la clave de
depuración de Android y usan los identificadores definitivos
(`com.silicio.copi.rallye` y `com.silicio.copi`); si más adelante instalas una
versión release firmada con otra clave, desinstala antes la debug.

### Compilar desde el código

Requisitos: JDK 17 o 21, Android SDK Platform 35 y conexión para descargar
dependencias.

```bash
./gradlew :app:assembleRallyeDebug :app:assembleStandardDebug :app:testRallyeDebugUnitTest
```

En Windows:

```powershell
.\gradlew.bat :app:assembleRallyeDebug :app:assembleStandardDebug :app:testRallyeDebugUnitTest
```

Si Gradle no encuentra el SDK, crea `local.properties`:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
```

El workflow de GitHub Actions (`.github/workflows/android.yml`) compila ambos
APK y publica los artefactos descargables en cada push.

---

## 3. Primer arranque

1. **Ubicación**: concede “Mientras se usa la app” y activa la ubicación precisa.
   Sin GPS puedes usar el modo demo.
2. **Notificaciones**: en Android 13+ se piden para el servicio en primer plano;
   si las deniegas, el tramo sigue funcionando, pero el aviso permanente puede
   no mostrarse.
3. **Voz**: Copi detecta las voces en español instaladas. En Ajustes → Voz puedes
   probar, elegir una voz concreta y abrir los ajustes de TTS del sistema.
4. **Tramo demo**: la primera vez se crea “Tramo demo · Puerto sintético” para
   que puedas probar sin importar nada.

### Probar sin coche

- Activa **Modo demo** en Ajustes. Simula unos 60 km/h siguiendo el trazado.
- Pulsa **INICIAR TRAMO**. Verás velocidad, cuenta atrás, cambio de nota y voz.
- Es también la forma más rápida de probar Android Auto en el DHU.

---

## 4. Conceptos: tramo, roadbook y pacenote

### Tramo

Un recorrido con notas. En Copi, el **kilómetro 0** es el punto en el que pulsas
INICIAR (o donde alineas con el GPS). Las distancias del roadbook se miden desde
la salida del tramo.

### Pacenote

La información que el copiloto canta antes de una curva. Ejemplo:

> “En 120 metros, izquierda 4, larga.”

- **Dirección**: izquierda / derecha / horquilla / recto / cresta / salto.
- **Severidad 1–6**: 1 es la curva más cerrada (horquilla) y 6 la más rápida.
- **Modificador**: `larga`, `corta`, `cierra` (se cierra), `abre` (se abre),
  `no cortar`, etc.
- **Distancia**: metros hasta la entrada de la curva.

| Severidad | Ángulo aproximado | Interpretación |
|---|---|---|
| 1 | ≥ 160° | Horquilla |
| 2 | 135–160° | Muy lenta |
| 3 | 105–135° | Lenta |
| 4 | 75–105° | Media (90° típica) |
| 5 | 45–75° | Rápida |
| 6 | 22–45° | Muy rápida / casi recta |

La severidad se calcula con el giro neto entre la entrada y la salida de la
curva, no con un único punto GPS. Si el GPX es pobre (pocos puntos, trazado
suavizado), las notas pueden salir menos precisas: usa GPX con un punto cada
5–15 m.

### Roadbook

Lista ordenada de notas. Copi guarda los roadbooks en el almacenamiento privado
de la app, con el trazado si existe. Puedes tener varios y cambiar el activo.

---

## 5. Crear o importar un tramo

### 5.1 Importar GPX (recomendado)

1. Pestaña **Tramo** → botón **+** → **Importar GPX**.
2. Elige el archivo (track o ruta).
3. Copi lee los puntos, calcula distancias, detecta curvas y crestas y guarda el
   roadbook.
4. Abre el tramo, revisa el trazado y las notas, y pulsa **USAR TRAMO**.

Consejos para un GPX de calidad:

- Un punto cada 5–15 m; evita tracks con miles de puntos duplicados.
- Si incluye `<ele>`, Copi añade notas de **cresta** cuando detecta un cambio de
  rasante claro.
- Un GPX de coche suele venir suavizado: la app vuelve a muestrear cada 8 m y
  suaviza rumbos para no generar ruido.
- Si el archivo tiene nombres de waypoints, se usan como nombre del tramo; si no,
  se usa el nombre del archivo.

### 5.2 Importar un roadbook CSV

1. Pestaña **Tramo** → **+** → **Importar roadbook CSV**.
2. Elige el archivo.
3. Revisa las notas y elige el tramo.

Formato mínimo (la cabecera es opcional):

```csv
distancia_m,tipo,severidad,modificador,nota
0,salida,0,,
180,izquierda,4,larga,
340,derecha,3,cierra,
520,horquilla izquierda,1,no cortar,
700,recto,0,,
1150,meta,0,,
```

- La distancia puede ser absoluta desde la salida (`520`) o relativa a la nota
  anterior (`+120`).
- Si falta la nota de meta, Copi añade una al final.
- Si falta la salida, Copi añade una a 0 m.
- El campo `nota` es opcional y, si se rellena, **sustituye** la frase generada
  por la voz. Úsalo para indicaciones especiales.
- También puedes exportar el roadbook activo con el icono de compartir de la
  ficha del tramo (texto CSV).

Tipos aceptados (sin distinguir mayúsculas): `salida`, `izquierda`/`left`/`l`,
`derecha`/`right`/`r`, `horquilla izquierda`/`hl`/`hairpin_l`,
`horquilla derecha`/`hr`/`hairpin_r`, `recto`/`straight`/`s`, `cresta`,
`salto`, `peligro`/`caution`, `chicane`, `meta`/`finish`.

### 5.3 Planificar una ruta online

1. Pestaña **Ruta**.
2. Escribe el destino (dirección, lugar o `lat,lon`).
3. Opcional: escribe un origen. Si lo dejas vacío, se usa tu GPS o la última
   posición conocida.
4. **Buscar** enseña resultados; **Generar tramo** pide la ruta a OSRM y genera
   las pacenotes.
5. Revisa la vista previa y pulsa **USAR ESTE TRAMO**.

La planificación usa Nominatim (búsqueda) y OSRM (ruta). Los servidores públicos
son de demostración y tienen límites; para uso intensivo configura tu propia
instancia en **Ajustes → Avanzado**. El tramo generado se guarda y ya funciona
offline.

> Sin internet, el botón de planificación se desactiva. Importa un GPX o un CSV
> para trabajar completamente offline.

### 5.4 Biblioteca de tramos

- **Tramo activo**: se marca con `ACTIVO` y se abre en Inicio.
- **Usar tramo**: lo carga en el motor y te lleva a Inicio.
- **Alinear**: activa el tramo y encaja tu posición GPS actual en el trazado.
- **Compartir**: envía el CSV por la app que elijas.
- **Borrar**: elimina el tramo del dispositivo (con confirmación).

---

## 6. Navegar el tramo

### Antes de salir

1. Sitúate en la salida real del tramo.
2. Activa el tramo en la pestaña Tramo.
3. Comprueba GPS (indicador `GPS OK`), voz y unidades.
4. Pulsa **INICIAR TRAMO**.

### Durante el tramo

| Elemento | Qué hace |
|---|---|
| **INICIAR / PAUSA / REANUDAR** | Controla el motor y el cronómetro. |
| **Anterior / Siguiente** | Retrocede o salta una nota manualmente. |
| **Reiniciar** | Vuelve a 0, borra tiempo y avance. |
| **Alinear** | Encaja tu posición en el trazado y recoloca la siguiente nota. |
| **Voz ON/OFF** | Silencia o reactiva la voz sin perder el tramo. |
| **Barra de progreso** | Puntos = notas; el punto grande es la siguiente. |

- La distancia se acumula con GPS. Se ignoran saltos absurdos (>250 m entre
  fixes) para evitar que un fallo de GPS dispare el tramo.
- Si el GPS se pierde, el cronómetro sigue y la velocidad baja a 0; al recuperar
  señal, la distancia se reanuda. Usa **Alinear** si el desfase es grande.
- Al pasar una nota suena un pitido (si está activado) y avanza la siguiente.
- Al llegar al final suena “Meta” con el tiempo total.

### Antelación y avisos

- **Antelación de la voz** (3–10 s): a 100 km/h y 6 s, la nota se canta unos
  170 m antes. A baja velocidad manda la **distancia mínima** (40–200 m).
- **Recordatorio a 30 m**: repite la nota corta justo antes de la curva.
- **Pitido al pasar la nota**: confirma que la nota quedó atrás.
- Si llegas a una nota sin haberla oído, revisa la antelación, el volumen
  multimedia y el modo de voz.

### Alineación sobre el trazado

**Alinear** busca el punto más cercano del trazado y ajusta el origen del tramo.
Úsalo:

- si empiezas a mitad de tramo;
- si has tenido que retroceder o dar un rodeo;
- si el GPS acumuló error;
- si has importado un roadbook cuya salida no coincide con tu posición.

Si el roadbook no tiene trazado (CSV manual), la alineación coloca la siguiente
nota a 60 m.

### Modo demo

Ajustes → **Modo demo**. Reinicia el tramo al activarlo/desactivarlo. Simula el
trazado a velocidad constante y sirve para probar voz, HUD y Android Auto sin
mover el coche.

---

## 7. La voz del copiloto

Copi tiene tres capas de voz, en este orden:

1. **Voz neuronal en la nube** (Google Cloud TTS o ElevenLabs), con caché.
2. **Mejor voz local instalada** en español (prefiere calidad alta y voces
   neuronales offline).
3. **Motor TTS del sistema** como último recurso.

Nunca se queda mudo: si la nube falla, usa la voz local.

### 7.1 Voz local (offline, sin claves)

La calidad depende de las voces instaladas en el móvil. Para la mejor experiencia:

1. Instala **Google Speech Services** (Google TTS) desde Play Store.
2. Ajustes del sistema → Idiomas → **Salida de texto a voz** → Google.
3. Descarga la voz de **español (España)** de alta calidad.
4. En Copi: Ajustes → Voz → **Voces locales** → elige la voz.
5. Ajusta velocidad (0,7×–1,4×) y tono. Para que suene menos robótica, prueba
   velocidad 0,95–1,05 y tono 1,00–1,05.

Consejos si suena metálica:

- Usa una voz “neural”, “wavenet” o de alta calidad, no la voz compacta.
- Evita velocidades extremas (0,7× y 1,4×).
- Comprueba que el audio sale por el canal multimedia, no por el de llamadas.
- Prueba el botón **Probar voz**.
- En algunos Samsung, la voz de Samsung TTS es más robótica que la de Google.

### 7.2 Google Cloud TTS (voz natural online)

1. Crea un proyecto en Google Cloud Console.
2. Activa la **Cloud Text-to-Speech API** y la facturación.
3. Crea una **API key**; restíngela a esa API (y, si puedes, a la app Android).
4. En Copi: Ajustes → Voces en la nube → pega la clave.
5. Voz recomendada: `es-ES-Neural2-F` (femenina) o `es-ES-Neural2-B`
   (masculina). También puedes usar una voz `Studio` si tu proyecto tiene acceso.
6. Pulsa **Probar voz**. El modo **Auto** elegirá Google si no hay ElevenLabs.

La voz se sintetiza por HTTPS y se guarda en caché local. Cada frase nueva
consume cuota; consulta precios y límites de Google.

### 7.3 ElevenLabs (voz clonada o de catálogo)

1. Crea una cuenta y una API key.
2. Copia el **voice ID** de la voz que quieras usar.
3. Pega clave y voice ID en Ajustes → Voces en la nube.
4. Modelo usado: `eleven_multilingual_v2` (español natural).
5. Prueba la voz. En modo **Auto**, ElevenLabs tiene prioridad sobre Google.

Ideal si quieres una voz concreta o clonar la tuya. Revisa los términos, la
latencia y el coste por carácter.

### 7.4 Modos de voz

| Modo | Comportamiento |
|---|---|
| **Auto** | ElevenLabs si hay clave; si no, Google; si no, local. |
| **Local** | Fuerza la mejor voz instalada. Funciona sin internet. |
| **Google** | Usa Google Cloud TTS si hay clave y red; si no, local. |
| **ElevenLabs** | Usa ElevenLabs si hay clave y red; si no, local. |

### 7.5 Precarga y uso offline

La caché de voz permite repetir frases ya sintetizadas sin internet. El botón
**Precargar voces del tramo** sintetiza:

- la frase de cada nota (recordatorios);
- “Ahora, …”;
- el anuncio a 100 m;
- la salida y la meta.

Las distancias exactas (“En 170 metros…”) se sintetizan cuando hace falta. Si no
hay red, Copi usa la voz local para esa frase. Para un rally **sin cobertura**,
la opción más fiable es una buena **voz neuronal local**; la nube es un extra.

La caché se puede borrar desde Ajustes. Se guarda en el almacenamiento privado de
la app.

### 7.6 Frases que canta

- “Copi listo. Tramo X. En 120 metros, izquierda 4, larga.”
- “En 80 metros, derecha 3, cierra.”
- “Ahora, horquilla izquierda, no cortar.”
- “Cresta.”
- “Meta. Tiempo 3:42.”

La voz no lee la pantalla: solo las notas y avisos del tramo.

---

## 8. Offline y online

Copi está diseñado **offline-first**.

| Función | Sin conexión | Con conexión |
|---|---|---|
| GPS, velocidad, distancia, cronómetro | ✅ | ✅ |
| Motor de tramo y avance de notas | ✅ | ✅ |
| Roadbook y trazado guardados | ✅ | ✅ |
| Voz local neuronal | ✅ | ✅ |
| Voz en la nube ya cacheada | ✅ (parcial) | ✅ |
| Voz en la nube para frases nuevas | ❌ (usa local) | ✅ |
| Importar GPX/CSV | ✅ | ✅ |
| Planificar ruta y buscar lugares | ❌ | ✅ |
| Precache de voz | ❌ | ✅ |

No hay mapas de calles descargados: la vista de tramo es un **mapa esquemático**
del trazado, con notas, salida, meta y tu posición. Eso mantiene la app ligera y
100 % funcional sin cobertura. La navegación real la dan las pacenotes y el GPS.

---

## 9. Android Auto

### 9.1 Qué verás en el coche

- HUD de carrera dibujado por Copi: velocidad, siguiente nota, distancia,
  cronómetro, media y progreso.
- En hosts con Car API 7 o superior. En hosts más antiguos Copi muestra un
  **modo lista** con las próximas notas y los controles esenciales, sin HUD.
- Lista de las próximas notas.
- Acciones: Iniciar/Pausa/Reanudar, Anterior, Siguiente, Silenciar.
- Integración con `NavigationManager` para clusters/HUD del coche compatibles.
- Notificación de navegación con controles (también en el rail de Android Auto).

### 9.2 Activación en el móvil

1. Instala y abre Copi en el móvil al menos una vez.
2. Abre **Android Auto** en el móvil.
3. Toca 10 veces la versión de Android Auto para activar el modo desarrollador.
4. Menú de tres puntos → **Ajustes de desarrollador** → activa
   **Fuentes desconocidas** (Unknown sources).
5. Conecta el móvil al coche por USB (mejor cable de datos) o por wifi si tu
   coche lo soporta.

> **Ojo:** “Fuentes desconocidas” sirve para apps de media, mensajería y
> aparcado. **No se aplica a apps de la Car App Library** como Copi. En un coche
> real, Copi debe estar instalada desde Google Play (Internal App Sharing,
> Internal Test Track o producción). Para desarrollo se usa el DHU.

### 9.2.1 Sin Play y sin firma de release

Android no instala APKs sin firmar. El APK **debug** ya está firmado con la
clave de depuración (`~/.android/debug.keystore`), así que se instala por ADB o
desde el móvil sin keystore release. “Sin firmar” significa aquí **sin la firma
de Play**, no sin ninguna firma.

- El APK debug sirve para probar en el **móvil** y en el **DHU**.
- En el coche real **no aparece solo por ADB/Fuentes desconocidas**: Google
  exige que las apps de la Car App Library vengan de una fuente de confianza.
- La vía oficial sin publicación pública es **Google Play Internal App
  Sharing** (requiere cuenta de Play Console y un AAB/APK **release firmado**).
- Para producción pública hace falta, además, la aprobación de la categoría
  Navegación.

Guía paso a paso y alternativas:
[docs/ANDROID_AUTO_SIDELOAD.md](docs/ANDROID_AUTO_SIDELOAD.md).

Para subir el AAB a Play:
[docs/PLAY_INTERNAL_SHARING.md](docs/PLAY_INTERNAL_SHARING.md).

### 9.3 Desktop Head Unit (DHU)

El DHU permite probar Android Auto sin coche:

1. SDK Manager → **Android Auto Desktop Head Unit Emulator**.
2. En Android Auto del móvil: Ajustes de desarrollador → **Iniciar servidor de
   unidad principal** (Head unit server).
3. Conecta el móvil por USB y ejecuta:

```bash
adb forward tcp:5277 tcp:5277
```

4. Lanza el DHU:

```powershell
# Windows
%LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe
```

```bash
# macOS/Linux
$ANDROID_HOME/extras/google/auto/desktop-head-unit
```

5. En el DHU abre el cajón de apps y elige Copi.

### 9.4 Limitación importante de Google Play

Copi implementa la **Android for Cars App Library** oficial:

- `CarAppService` con categoría `androidx.car.app.category.NAVIGATION`.
- Permisos `NAVIGATION_TEMPLATES`, `MAP_TEMPLATES` y `ACCESS_SURFACE`.
- `MapWithContentTemplate`, `SurfaceCallback`, `NavigationManager`.
- `minCarApiLevel=1`; `MapWithContentTemplate` se usa si el host soporta Car API
  7 o superior y, si no, se cae a `ListTemplate` (modo lista).

Para que una app de navegación aparezca en un coche real, Android Auto exige
que esté instalada desde una **fuente de confianza**: Google Play, Internal App
Sharing o Internal Test Track. La opción “Fuentes desconocidas” **no se aplica**
a la Car App Library. Para publicar en Google Play hace falta, además, aprobar
la categoría Navegación y la allowlist de hosts.

`HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` está pensado para desarrollo/DHU. Para
publicar hay que usar `HostValidator.Builder` con la allowlist de hosts y firmar
la app. La versión 1.0 no incluye módulo Android Automotive OS (AAOS), solo
Android Auto proyectado.

### 9.5 Problemas típicos en Android Auto

| Síntoma | Solución |
|---|---|
| La app no aparece | Es lo esperado con un APK sideloaded: las apps Car App Library no usan Fuentes desconocidas. Instálala desde Google Play Internal App Sharing/Internal Test Track o pruébala en el DHU. |
| Solo veo una lista, sin HUD | El host no soporta Car API 7; es el modo lista de compatibilidad. |
| HUD en negro | La superficie aún no está lista; espera, cambia de app y vuelve. Reporta el coche/host. |
| No hay sonido | Sube el volumen multimedia; revisa que la voz no esté silenciada; prueba la voz en el móvil. |
| Se corta el audio | Puede ser el host; baja la antelación o usa voz local. |
| Cluster sin datos | No todos los coches muestran `Trip`; es opcional del vehículo. |

---

## 10. Ajustes, uno a uno

### Navegación

| Ajuste | Valores | Para qué |
|---|---|---|
| Unidades | km/h, mph | Velocidad y distancias. |
| Antelación de la voz | 3–10 s | Segundos antes de la curva; se combina con la velocidad. |
| Distancia mínima de aviso | 40–200 m | Suelo de distancia para velocidades bajas. |
| Recordatorio a 30 m | Sí/No | Repite la nota antes de entrar. |
| Pitido al pasar | Sí/No | Confirma que la nota quedó atrás. |
| Mantener pantalla encendida | Sí/No | Útil con el móvil a la vista. |
| Modo demo | Sí/No | Simula el tramo sin GPS. Reinicia el tramo al cambiarlo. |

### Voz

| Ajuste | Para qué |
|---|---|
| Voz activada | Silencia/activa todo. |
| Modo de voz | Auto, Local, Google, ElevenLabs. |
| Velocidad | 0,7×–1,4×. |
| Tono | 0,8–1,3. |
| Voces locales | Selector de voces instaladas. |
| Probar voz | Locución de prueba. |
| Ajustes de voz del sistema | Abre el panel TTS de Android. |
| Precargar | Sintetiza frases del tramo activo para uso offline. |
| Borrar caché | Elimina los MP3 sintetizados. |

### Voces en la nube

- **Servicios online**: si se desactiva, Copi fuerza voz local y bloquea la
  planificación online (el resto del tramo sigue).
- **Google Cloud TTS**: API key y nombre de voz.
- **ElevenLabs**: API key y voice ID.
- Las claves se guardan en las preferencias privadas de la app, **sin cifrado
  adicional**. Restríngelas y no las compartas.

### Avanzado

- **Servidor OSRM**: por defecto `https://router.project-osrm.org`.
- **Servidor Nominatim**: por defecto `https://nominatim.openstreetmap.org`.

Los servidores públicos limitan el uso (1 petición/s en Nominatim, sin descargas
masivas). Para un uso intensivo o comercial, monta tu propia instancia y cambia
las URLs.

---

## 11. Formatos de archivo

### 11.1 CSV

- Separador `,` o `;`.
- Cabecera opcional: `distancia_m,tipo,severidad,modificador,nota`.
- Distancias en metros, absolutas o relativas con `+`.
- La coma decimal se acepta (`120,5`).
- Si la última nota no es `meta`, se añade.
- Si la primera no es `salida`, se añade a 0.

Ejemplo: [docs/ejemplo-roadbook.csv](docs/ejemplo-roadbook.csv).

### 11.2 GPX

- Se aceptan `trkpt`, `rtept` y `wpt`.
- Se usa el nombre del track/ruta si existe.
- La altitud (`<ele>`) activa la detección de crestas.
- Se descartan puntos inválidos y duplicados a menos de 1 m.
- El trazado se guarda en el roadbook para el mapa esquemático y la alineación.

Ejemplo: [docs/ejemplo-tramo.gpx](docs/ejemplo-tramo.gpx).

### 11.3 JSON interno

Los roadbooks se guardan en `filesDir/roadbooks/*.json` con el esquema:

```json
{
  "id": "uuid",
  "name": "Tramo",
  "createdAt": 1700000000000,
  "distanceM": 1960.0,
  "notes": [
    {"distanceM": 0.0, "type": "START", "severity": 0, "modifier": "", "instruction": "Salida", "extra": ""}
  ],
  "route": [{"lat": 40.84, "lon": -3.94, "alt": 1200.0}],
  "source": "gpx",
  "version": 1
}
```

Puedes copiar/restaurar estos JSON manualmente con una herramienta de backup,
pero no es una API estable.

---

## 12. Privacidad y datos

- **Ubicación**: se procesa en el dispositivo. No se envía a ningún backend de
  Copi (no existe backend). El trazado y el progreso se mantienen en memoria; el
  roadbook guarda el trazado que tú importas.
- **Búsqueda y rutas online**: si usas la pestaña Ruta, el texto y las
  coordenadas se envían a Nominatim y OSRM (OpenStreetMap). No se envían a
  Google salvo que uses la voz de Google Cloud.
- **Voz**: si configuras Google/ElevenLabs, el texto de las notas se envía a esos
  servicios para sintetizarlo. Sin claves, todo es local.
- **Claves**: se guardan en `SharedPreferences` privadas, sin cifrado adicional.
  Restringe las claves y revócalas si se filtran.
- **Caché de voz**: MP3 en la caché privada; se puede borrar.
- **Sin analítica, publicidad ni cuentas**.

Más detalle: [PRIVACY.md](PRIVACY.md).

---

## 13. Solución de problemas (FAQ)

### No aparece el GPS

- Concede **Ubicación precisa** y activa el GPS del móvil.
- Sal al exterior; el primer fix puede tardar 10–30 s.
- Sin permiso, el botón Iniciar muestra un aviso. Usa **Modo demo** o concede el
  permiso en Ajustes de Android → Apps → Copi → Permisos.

### La voz no suena

- Ajustes → Voz → **Probar voz**.
- Sube el volumen **multimedia** (no el de llamadas).
- Comprueba el motor TTS y descarga la voz de español.
- Si el modo es Google/ElevenLabs y no hay red, cambia a **Local** o espera al
  fallback.
- Algunos coches silencian la navegación si otra app tiene el foco de audio.

### Suena robótica

- Instala una voz neuronal local y selecciónala.
- Ajusta velocidad ~1,0 y tono ~1,0.
- Configura Google Cloud TTS con `es-ES-Neural2-F` o ElevenLabs.
- Evita el altavoz del móvil a volumen alto; usa el audio del coche.

### Las notas van adelantadas o atrasadas

- Pulsa **Alinear** al empezar o tras un desvío.
- Aumenta/reduce la antelación y la distancia mínima.
- Revisa la calidad del GPX (puntos cada 5–15 m).
- La distancia GPS puede variar en túneles o zonas urbanas; usa el cronómetro y
  las notas como referencia, no solo el número.

### El CSV no importa

- Comprueba que hay al menos dos columnas y una nota válida.
- Usa `,` o `;` de forma consistente.
- Revisa la codificación UTF-8 y que la primera línea no sea basura.
- Prueba con el [ejemplo](docs/ejemplo-roadbook.csv).

### La ruta online falla

- Comprueba internet y que **Servicios online** esté activo.
- Los servidores públicos pueden limitar o caer; prueba más tarde o configura
  tu propia instancia.
- Si el origen está vacío y no hay GPS, escribe un origen.

### Android Auto no muestra Copi

- Es lo esperado si instalaste el APK por ADB: las apps de la Car App Library no
  se habilitan con “Fuentes desconocidas”.
- Para el coche real, instala desde **Google Play Internal App Sharing** o un
  **Internal Test Track** (requiere cuenta de Play Console y AAB firmado).
- Para desarrollo, usa el **DHU** con el APK debug.
- Comprueba que la app está instalada en el **móvil** y en el perfil actual.
- Recuerda que la publicación pública como Navegación requiere aprobación de
  Google.

### ¿Puedo usar las dos ediciones?

Sí, tienen paquetes distintos. Solo una puede ser la app de navegación activa en
Android Auto a la vez. Los datos (tramos y ajustes) son independientes.

### ¿Dónde están mis tramos?

En el almacenamiento privado de la app. Exporta el CSV desde la ficha del tramo
para conservar las notas. El trazado solo se conserva en el JSON interno; si
quieres conservarlo, guarda el GPX original.

### ¿Hay mapas de calles?

No en 1.0. La vista de tramo es un mapa esquemático del trazado. La navegación se
basa en pacenotes, GPS, distancia y voz. Está en el roadmap añadir mapas
offline.

---

## 14. Compilar, firmar y publicar

### Debug

```bash
./gradlew :app:assembleRallyeDebug :app:assembleStandardDebug
```

### Release

```bash
./gradlew :app:bundleRallyeRelease :app:bundleStandardRelease
```

Sin `signingConfig`, el release sale sin firmar. Para firmar, crea un keystore y
añade un bloque `signingConfigs` en `app/build.gradle.kts` (no subas el keystore
ni las contraseñas al repositorio).

### Publicación en Google Play

- Sube un **AAB** firmado.
- Declara la categoría **Navegación** y solicita la aprobación de Google.
- Implementa la allowlist de hosts (`HostValidator.Builder`) y revisa las
  guideline de calidad para navegación.
- Política de privacidad: usa [PRIVACY.md](PRIVACY.md) como base.
- No prometas compatibilidad universal con Android Auto sin la aprobación.

### Tests

```bash
./gradlew :app:testRallyeDebugUnitTest
```

Cubren geometría, generador de pacenotes, CSV y frases de voz.

---

## 15. Avisos legales

- Copi es un proyecto personal de **Luis Fernando (silicio-sudo / @rsanmart)**.
- **Copi no está afiliado a Mitsubishi Motors ni a Ralliart.** El diseño de la
  edición Rallye es un homenaje al arte rallye (rojo, plata, carbono y franjas),
  sin usar logotipos de terceros.
- Mapas, geocodificación y datos: © OpenStreetMap contributors, ODbL. Rutas
  OSRM. Respeta las políticas de uso de los servidores públicos.
- Google Cloud TTS y ElevenLabs se usan bajo tus cuentas y términos.
- Licencia del código: MIT (ver [LICENSE](LICENSE)).
- Conduce con prudencia. Copi no sustituye al copiloto humano ni exime de
  respetar las normas de circulación.

---

**Copi · pacenotes, tramo y meta.**
