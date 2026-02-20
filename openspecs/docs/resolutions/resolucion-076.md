# Resolucion Issue #76: Fix billing-service OpenAPI codegen

> **Issue:** [#76 - Fix billing-service OpenAPI codegen](https://github.com/monghithub/guidewire/issues/76)
> **Prioridad:** P0
> **Estado:** Resuelta

## Diagnostico

El `openapi-generator-maven-plugin` en billing-service apuntaba a un archivo inexistente:

```xml
<!-- ANTES (roto) -->
<inputSpec>${project.basedir}/../../contracts/openapi/billing-service.yml</inputSpec>

<!-- Archivo real -->
contracts/openapi/billing-service-api.yml
```

El build de `mvn generate-sources` fallaba silenciosamente y no se generaban las interfaces
en `com.guidewire.billing.api`. El `InvoiceController` era standalone sin implementar contrato.

## Solucion aplicada

Corregido el path en `components/billing-service/pom.xml` (linea 163):

```xml
<!-- DESPUES (correcto) -->
<inputSpec>${project.basedir}/../../contracts/openapi/billing-service-api.yml</inputSpec>
```

La configuracion del plugin ya era correcta:
- `generatorName`: spring
- `apiPackage`: com.guidewire.billing.api
- `interfaceOnly`: true
- `useSpringBoot3`: true

## Siguiente paso

Ejecutar `mvn generate-sources` para generar las interfaces y hacer que
`InvoiceController implements InvoicesApi`.

## Verificacion

```bash
cd components/billing-service
mvn generate-sources -B
ls target/generated-sources/openapi/src/main/java/com/guidewire/billing/api/
```
