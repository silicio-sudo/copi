# Copi en Android Auto sin Play y sin firma de release

Guía rápida para usar Copi en tu pantalla de Android Auto con un APK sideloaded.
No hace falta cuenta de desarrollador, Google Play ni un keystore de release.

> **Importante:** Android **no instala APKs sin firmar**. “Sin firmar” aquí
> significa **sin la firma de Google Play**: el APK debug ya está firmado con la
> clave de depuración que Android genera en tu equipo. Es instalable y válido
> para sideload/Android Auto en modo desarrollador.

## 1. Instala el APK en el móvil

```powershell
adb install -r "C:\Users\NEO\copi\dist\Copi-Rallye-1.0.0.apk"
```

o copia el APK al móvil y ábrelo permitiendo “Instalar apps desconocidas” para
el explorador de archivos.

Comprueba que está instalado en el **perfil actual** del móvil:

```powershell
adb shell pm list packages | findstr copi
```

Debe aparecer:

```text
package:com.silicio.copi.rallye
package:com.silicio.copi
```

La edición Rallye y la estándar pueden convivir. Para Android Auto elige una.

## 2. Activa el modo desarrollador de Android Auto

1. Abre la app **Android Auto** en el móvil.
2. Entra en **Ajustes**.
3. Baja hasta **Versión** (o “Versión y licencia”).
4. Toca **10 veces** la versión hasta que aparezca “Modo desarrollador activado”.
5. Vuelve a Ajustes → menú de tres puntos → **Ajustes de desarrollador**.
6. Activa **Fuentes desconocidas** / **Unknown sources**.
7. Si aparece, activa también **Añadir coches nuevos a Android Auto**.

> En algunas versiones el menú está en Ajustes → **Ajustes de desarrollador**
> directamente, o en el menú de tres puntos de la pantalla principal.

## 3. Conecta al coche

### 3.1 Primer emparejamiento

1. Desbloquea el móvil.
2. Activa **Bluetooth** y **Wi-Fi** en el móvil.
3. Enciende el coche/pantalla y abre su modo **Android Auto**.
4. Acepta el emparejamiento Bluetooth y los permisos que pida la pantalla.
5. En el ASX, haz la **primera conexión por USB** (cable de datos). Así el
   coche registra el teléfono y Copi; después podrás usar el modo inalámbrico.
6. En la genérica, sigue el asistente de la pantalla (Bluetooth + Wi-Fi). Si
   tiene USB, úsalo también para el primer registro.
7. Abre el cajón de aplicaciones del coche y busca **Copi Rallye** o **Copi**.

### 3.2 Inalámbrico: Wi-Fi + Bluetooth

Android Auto inalámbrico usa Bluetooth para el emparejamiento y una red Wi-Fi
creada por el coche para los datos. En el móvil:

- Mantén **Bluetooth** y **Wi-Fi** activos; no hace falta datos móviles para el
  GPS, el roadbook ni la voz local.
- Desactiva el ahorro de batería agresivo para **Android Auto** y **Copi**.
- Mantén el móvil desbloqueado y cerca de la pantalla; el primer inicio puede
  tardar unos segundos.
- Usa preferentemente **Wi-Fi de 5 GHz** si la pantalla lo permite.
- Para un tramo largo, conecta el móvil al cargador: GPS + AA inalámbrico +
  voz consumen batería.

Si el coche no ofrece Wi-Fi propio, algunas pantallas genéricas usan un
**dongle USB** de Android Auto inalámbrico; en ese caso el procedimiento es el
del dongle, no el de la pantalla.

### 3.3 Añadir Copi al lanzador de Android Auto

En el móvil, Android Auto → Ajustes → **Personalizar lanzador** (Customize
launcher). Debe aparecer **Copi Rallye** o **Copi**; actívalo. Si no aparece,
es que Android Auto no está aceptando la app sideloaded: revisa **Fuentes
desconocidas**, reinstala el APK y vuelve a conectar.

### 3.4 ASX oficial frente a pantalla genérica

- **ASX oficial**: usa el Android Auto de Google. Si Copi aparece por cable pero
  no por Wi-Fi, borra caché de Android Auto, reempareja y comprueba que el ASX
  tiene activada la proyección inalámbrica. Para rally, el cable es más estable
  y carga el móvil.
- **Genérica**: muchas usan Zlink/CarPlay/Android Auto propietario. Copi debería
  aparecer si el host respeta las apps sideloaded; algunos firmwares solo
  muestran apps aprobadas. Prueba cable/USB primero, actualiza el firmware y
  revisa el lanzador. Si solo aparece en el móvil/DHU y no en la genérica, es
  una limitación del host, no del APK.

## 4. Qué deberías ver

- Si el host soporta **Car API 7 o superior**: HUD de Copi con velocidad, nota,
  distancia, cronómetro y lista de próximas notas.
- Si el host es más antiguo: **modo lista** con las próximas notas y los botones
  Iniciar/Pausa y Siguiente. La app sigue funcionando; solo falta el HUD.
- La app del móvil y la del coche comparten el mismo motor: puedes iniciar el
  tramo desde el coche o desde el móvil.

`minCarApiLevel` se ha bajado a **1** precisamente para que aparezca en más
hosts. `MapWithContentTemplate` se usa solo cuando el host soporta API 7+.

## 5. Si Copi no aparece

Prueba en este orden:

1. **Reinicia Android Auto**: Ajustes → Forzar detención → borrar caché.
2. Comprueba que **Fuentes desconocidas** sigue activo.
3. Instala el APK en el móvil, no en una tarjeta o perfil de trabajo.
4. Usa un cable de datos y otro puerto USB.
5. Desactiva la optimización de batería para **Android Auto** y **Copi**.
6. Reinicia el móvil y la pantalla del coche.
7. Verifica el manifiesto:

```powershell
aapt dump xmltree app\build\outputs\apk\rallye\debug\app-rallye-debug.apk AndroidManifest.xml | findstr /i "CarAppService minCarApiLevel"
```

Debe aparecer `CopiCarAppService`, la categoría `androidx.car.app.category.NAVIGATION`
y `minCarApiLevel=1`.
8. Prueba el **Desktop Head Unit (DHU)** para saber si el problema es del coche
   o de la app (ver más abajo).
9. Si tu coche tiene **Google integrado (Android Automotive OS)**, no es Android
   Auto proyectado: esta versión no incluye módulo AAOS y no aparecerá. Habría
   que crear un módulo `app-automotive`.

## 6. DHU: probar sin coche

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

## 7. ¿Y la firma?

| Concepto | ¿Necesario? |
|---|---|
| Firma debug de Android | **Sí**, ya incluida; permite instalar por ADB. |
| Firma release propia | No para sideload; sí para publicar/actualizar una release. |
| Aprobación de Google Play | No para modo desarrollador/DHU; sí para distribución pública como app de navegación. |
| Allowlist de hosts | No para sideload (Copi usa `ALLOW_ALL_HOSTS_VALIDATOR`); sí para producción. |

El keystore debug suele estar en:

```text
%USERPROFILE%\.android\debug.keystore
```

Android lo crea automáticamente al compilar/instalar. No lo borres si quieres
que las actualizaciones debug se instalen encima sin desinstalar.

## 8. Límites reales

- Un APK sideloaded funciona en **modo desarrollador/Fuentes desconocidas**.
- No se puede garantizar que aparezca en **todos** los coches de producción sin
  la aprobación de Google para la categoría Navegación.
- Algunos fabricantes bloquean fuentes desconocidas o requieren el móvil
  desbloqueado; es una restricción del coche/host, no de Copi.
- La versión 1.0 es Android Auto **proyectado**; no AAOS.

## 9. Comandos útiles

```powershell
# Reinstalar conservando datos
adb install -r "C:\Users\NEO\copi\dist\Copi-Rallye-1.0.0.apk"

# Ver logs de Android Auto / Copi
adb logcat | findstr /i "car.app copi"

# Desinstalar
adb uninstall com.silicio.copi.rallye
```

Si algo falla, envía el resultado de `adb logcat` filtrando por `CarAppService`,
`Copi` o `NavigationManager`.
