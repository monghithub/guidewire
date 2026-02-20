# Resolucion Issue #75: Activar security scans reales en CI

> **Issue:** [#75 - Activar security scans reales en CI (OWASP, npm audit, Trivy, Gitleaks)](https://github.com/monghithub/guidewire/issues/75)
> **Prioridad:** P2
> **Estado:** Resuelta

## Diagnostico

El job `security-scan` en `.github/workflows/ci.yml` era solo un placeholder que imprimia
TODOs con `echo`. Los scans reales estaban comentados.

## Solucion aplicada

Se reemplazo el placeholder completo por 3 scans reales:

### 1. npm audit (Node.js)

```yaml
- name: npm audit (Node.js dependencies)
  continue-on-error: true
  run: cd components/customers-service && npm audit --audit-level=high
```

- Escanea dependencias de Node.js del customers-service
- `--audit-level=high` — solo reporta vulnerabilidades HIGH y CRITICAL
- Gratis, sin configuracion adicional

### 2. Trivy (filesystem)

```yaml
- name: Trivy vulnerability scan (filesystem)
  uses: aquasecurity/trivy-action@master
  continue-on-error: true
  with:
    scan-type: 'fs'
    scan-ref: '.'
    severity: 'CRITICAL,HIGH'
    format: 'table'
```

- Escanea **todo el repositorio** (Java, Node.js, Dockerfiles, configs)
- Detecta CVEs en dependencias Maven y npm
- Identifica misconfiguraciones en Dockerfiles
- `format: table` — salida legible en los logs del CI
- Gratuito, open source (Aqua Security)

### 3. Gitleaks (secrets)

```yaml
- name: Gitleaks secret scan
  uses: gitleaks/gitleaks-action@v2
  continue-on-error: true
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- Escanea el repositorio buscando secretos hardcodeados
- Detecta: API keys, passwords, tokens, private keys
- Analiza el historial de git, no solo el working tree
- Requiere `GITHUB_TOKEN` (ya disponible en GitHub Actions)

### Decisiones de diseno

| Decision | Razon |
|----------|-------|
| `continue-on-error: true` en todos | No bloquear el pipeline durante la adopcion inicial |
| Sin OWASP Dependency-Check | Muy lento (~10 min), Trivy cubre el mismo caso de uso mas rapido |
| Sin Snyk/Semgrep | Requieren cuenta/API key, se pueden agregar despues |
| `needs: [build-*]` | Solo escanear si el build pasa |

### Scans descartados (por ahora)

| Scan | Razon de exclusion |
|------|--------------------|
| OWASP Dependency-Check | Trivy es mas rapido y cubre Java + Node.js |
| Snyk | Requiere API key y plan (freemium) |
| Semgrep | Analisis estatico avanzado, overkill para un POC |
| Trivy container scan | Requiere imagenes Docker construidas en CI |

## Siguiente paso: Quitar continue-on-error

Cuando el equipo este comodo con los resultados de los scans, se puede:

1. Revisar los findings iniciales y crear un baseline
2. Configurar `.trivyignore` para falsos positivos
3. Configurar `.gitleaksignore` para secretos aceptados (ej: `.env.example`)
4. Quitar `continue-on-error: true` para bloquear el pipeline ante vulnerabilidades

## Verificacion

```bash
# Verificar que el job ya no es placeholder
grep "Security Scan" .github/workflows/ci.yml
# Debe mostrar: "Security Scan" (sin "placeholder")

# Verificar que los 3 scans estan configurados
grep -E "npm audit|trivy-action|gitleaks-action" .github/workflows/ci.yml
```

## Antes vs Despues

### Antes
```yaml
security-scan:
  name: "Security Scan (placeholder)"
  steps:
    - name: Dependency check placeholder
      run: echo "TODO: Enable..."
```

### Despues
```yaml
security-scan:
  name: "Security Scan"
  steps:
    - name: npm audit (Node.js dependencies)
    - name: Trivy vulnerability scan (filesystem)
    - name: Gitleaks secret scan
```
