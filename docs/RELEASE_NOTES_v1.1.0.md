# Copi 1.1.0 · mapa y emblemas

Esta versión añade el mapa real y la identidad visual de cada edición.

## Cambios

- **Mapa real OpenStreetMap** en la nueva pestaña **Mapa**:
  - ruta completa del tramo;
  - posición actual y seguimiento;
  - próxima nota y controles;
  - teselas cacheadas para uso posterior sin conexión.
- **Copi Rallye**: emblema de tres rombos en la cabecera.
- **Copi limpia**: icono de coche en la cabecera.
- `versionCode` **3**, `versionName` **1.1.0**.
- Target API **36**.

## AAB para Play

Edición limpia (paquete registrado en Play Console):

```text
Copi-1.1.0-rallye.aab
```

Paquete: `com.silicio.copi.rallye`

Edición Rallye (solo uso privado y GitHub):

```text
Copi-Rallye-1.1.0-edicion.aab
```

Paquete: `com.silicio.copi.rallye.edicion`

> No subas a Play el AAB de la edición Rallye: Play solo acepta
> `com.silicio.copi.rallye`.

## Notas

- El mapa usa © OpenStreetMap contributors. La primera carga necesita internet;
  después se reutilizan las teselas en caché.
- La ruta y la posición se siguen dibujando sin conexión.
- Android Auto mantiene el HUD de Copi; el mapa real está en la app del móvil.
- Las apps de navegación de Android Auto deben instalarse desde Google Play.
