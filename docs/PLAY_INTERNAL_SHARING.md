# Publicar Copi en Play Console (Internal App Sharing)

Guía para que Copi aparezca en Android Auto real sin publicar la app al público.
Android Auto no acepta apps de navegación instaladas por ADB; la app debe venir
de **Google Play**. Internal App Sharing es la vía oficial sin revisión pública.

## Archivos ya preparados

```text
C:\Users\NEO\copi\dist\Copi-Rallye-1.0.0.aab
C:\Users\NEO\copi\dist\Copi-1.0.0.aab
C:\Users\NEO\copi\dist\Copi-Rallye-1.0.0-release.apk
C:\Users\NEO\copi\dist\Copi-1.0.0-release.apk
```

- Los **AAB** son los que se suben a Play Console.
- Los **release APK** son la misma build firmada, para instalar por ADB/DHU.
- Los APK debug siguen existiendo para pruebas locales.

Firma:

```text
Keystore:  C:\Users\NEO\copi\keystore\copi-upload.jks
Alias:     copi-upload
Password:  C:\Users\NEO\copi\keystore\CREDENTIALS.txt
Certificado: CN=Copi, O=silicio-sudo
```

> No subas el keystore ni `CREDENTIALS.txt` a GitHub. Guarda una copia en un
> gestor de contraseñas o en un disco seguro. Sin esa clave no podrás firmar
> actualizaciones de la misma app (aunque Play permite resetear la upload key).

## 1. Crear la cuenta de Play Console

1. Entra en https://play.google.com/console.
2. Crea una cuenta de desarrollador.
3. Paga la cuota única de aproximadamente 25 USD (el precio depende del país).
4. Verifica tu identidad si Play lo pide.

No hace falta publicar la app ni pagar por descarga. La cuota es de la cuenta.

## 2. Crear la app

En Play Console → **Crear app**:

- Nombre: `Copi Rallye` (o `Copi`).
- Idioma predeterminado: español.
- Tipo: aplicación.
- Gratuita.
- Acepta las declaraciones.

Cuando pida el **nombre del paquete**, usa exactamente:

```text
com.silicio.copi.rallye
```

Para la edición limpia, crea otra app con:

```text
com.silicio.copi
```

Puedes subir solo la edición Rallye primero.

## 3. Activar Play App Signing

Al subir el primer AAB, Play ofrecerá **Play App Signing**. Acepta.

- La **upload key** es tu `copi-upload.jks`; con ella firmas los AAB.
- Google guarda la **app signing key** y firma la versión distribuida.
- No pierdas el keystore: lo necesitas para subir actualizaciones.

## 4. Internal App Sharing

1. En Play Console entra en **Probar y lanzar → Test interno → Uso compartido
   interno de apps** (Internal app sharing).
2. Sube `Copi-Rallye-1.0.0.aab`.
3. Añade tu dirección de Google como tester.
4. Copia el enlace de opt-in.
5. Abre el enlace en el móvil con esa misma cuenta.
6. Instala Copi desde Play.
7. Conecta Android Auto por cable o inalámbrico. Copi debe aparecer en el
   lanzador, sin “Fuentes desconocidas”.

Internal App Sharing no genera un listado público. Es una distribución solo
para testers.

## 5. Internal Test Track (alternativa)

Si prefieres el canal de pruebas normal:

1. **Probar y lanzar → Pruebas → Prueba interna**.
2. Crea la pista y sube el AAB.
3. Añade testers por email.
4. Copia el enlace de opt-in e instala desde Play.
5. Play puede pedir **Contenido de la app**: privacidad, anuncios, seguridad de
   datos y clasificación. Rellénalo.

Para producción pública como app de Navegación, Google debe aprobar la categoría
y la allowlist de hosts. Internal App Sharing no requiere esa aprobación pública.

## 6. Actualizar Copi

Cada subida a Play necesita un `versionCode` mayor:

```kotlin
// app/build.gradle.kts
versionCode = 2
versionName = "1.0.1"
```

Luego:

```powershell
.\gradlew.bat :app:bundleRallyeRelease
```

Sube el nuevo AAB al mismo canal. Firma siempre con el mismo keystore.

## 7. Comprobar la firma

```powershell
# Certificado del AAB
keytool -printcert -jarfile Copi-Rallye-1.0.0.aab

# Certificado del APK release
apksigner verify --print-certs Copi-Rallye-1.0.0-release.apk
```

Debe aparecer `CN=Copi, O=silicio-sudo`.

## 8. Si Android Auto no lo muestra después de instalar desde Play

- Confirma que instalaste desde el enlace de Play, no por ADB.
- Comprueba que la app está en el **perfil principal** del móvil.
- Fuerza la detención de Android Auto y borra caché.
- Reinicia el móvil y la pantalla.
- Prueba primero por USB y después inalámbrico.
- Revisa que la build subida incluye la categoría `NAVIGATION` y
  `minCarApiLevel=1`.

## 9. Privacidad y Play

Para el canal de pruebas interno o producción, Play suele pedir una **URL
pública de política de privacidad**. Puedes usar el contenido de
[PRIVACY.md](../PRIVACY.md) en GitHub Pages o en tu web. Para Internal App
Sharing puede que no lo pida, pero tenlo preparado.

## 10. Qué no hacer

- Subir el keystore al repositorio.
- Subir un APK debug a Play.
- Cambiar el nombre del paquete entre builds.
- Instalar el AAB por ADB: Android no instala AAB; para ADB usa el release APK.
- Usar herramientas que parchean Android Auto.
