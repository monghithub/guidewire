# Guía de Preparación — Perfil de Integración Guidewire

## Objetivo

Documento de referencia técnica para la preparación de entrevista en un perfil de arquitectura de integración sobre plataforma Guidewire. Incluye fundamentos teóricos, patrones de diseño, herramientas, ciclos de vida y una POC demostrativa del stack tecnológico completo.

---

## Stack Tecnológico del Perfil

| Área | Tecnología |
|------|-----------|
| Plataforma CORE | Guidewire InsuranceSuite + Integration Gateway |
| Arquitecturas | SOA, MSA, EDA |
| Especificación API | OpenAPI, AsyncAPI, AVRO — API First |
| API Management | Red Hat 3Scale |
| Integración EIP | Red Hat Fuse (Apache Camel + Spring Boot/Quarkus) |
| Event Driven | Red Hat AMQ Streams (Kafka), Red Hat AMQ Broker (ActiveMQ) |
| Reglas de Negocio | IBM BAM Open Editions (Drools) |
| Registro de Servicios | Apicurio Service Registry |
| Herramientas API | Postman, API Quality |
| Plataforma PaaS/CaaS | Red Hat OpenShift 4.x |

---

## 1. Guidewire — Plataforma CORE de Seguros

### 1.1 Qué es Guidewire

Guidewire es la plataforma core del sector asegurador. Funciona como el ERP de referencia para grandes aseguradoras a nivel mundial, cubriendo los tres pilares operativos del negocio: pólizas, siniestros y facturación.

### 1.2 InsuranceSuite — Módulos Principales

**PolicyCenter** gestiona el ciclo de vida completo de la póliza: cotización, emisión, renovación, cancelación y endorsements. Toda operación contractual entre aseguradora y cliente pasa por este módulo.

**ClaimCenter** gestiona el ciclo completo de siniestros: apertura, asignación de peritos, constitución de reservas económicas, flujos de aprobación y resolución hasta el pago.

**BillingCenter** controla la facturación, cobros, planes de pago a plazos y comisiones a corredores y mediadores.

Los tres módulos comparten un modelo de datos común y se comunican entre sí. Ejemplo: cuando PolicyCenter emite una póliza, BillingCenter genera automáticamente el plan de cobro asociado.

### 1.3 Tecnología Base

Guidewire está construido sobre **Gosu**, un lenguaje propietario que corre sobre la JVM. Es tipado estáticamente, orientado a objetos, y fue diseñado para que los equipos de negocio configuren reglas, validaciones y flujos sin modificar código Java directamente.

### 1.4 On-Premise vs Cloud

**Guidewire Cloud Platform (GWCP)** es la evolución hacia cloud-native, desplegada sobre AWS como SaaS.

- **Cloud API**: REST con estándar JSON:API. Es el modelo actual.
- **Legacy API**: SOAP/XML del modelo on-premise.

En contexto de entrevista, asumir siempre el modelo cloud salvo indicación contraria.

### 1.5 Integration Gateway

El Integration Gateway es el componente central de integración, construido sobre **Apache Camel**. Actúa como mediador entre Guidewire y todos los sistemas externos.

#### Integración Síncrona

Guidewire expone Cloud APIs REST que sistemas externos consumen directamente. Portales web, apps móviles, CRMs o ERPs financieros llaman a estas APIs para consultar pólizas, abrir siniestros o consultar estados de pago. El Integration Gateway transforma, enruta y aplica políticas antes de que la petición llegue al core.

#### Integración Asíncrona (EDA)

Guidewire genera eventos de negocio cuando ocurre algo relevante: emisión de póliza, apertura de siniestro, cambio de estado de pago. Estos eventos se publican en **Kafka** (Red Hat AMQ Streams). Los sistemas downstream se suscriben a los topics que les interesan:

- Un sistema de detección de fraude escucha eventos de siniestros nuevos.
- Un data lake consume todo para analytics.
- Un sistema de notificaciones envía SMS/email al cliente.

### 1.6 Patrones de Integración con Guidewire

**Patrón Outbound — Guidewire consume datos externos:**
Durante la cotización, PolicyCenter llama a un servicio externo de scoring crediticio. El Integration Gateway realiza la llamada, transforma la respuesta al formato esperado por Guidewire y la devuelve.

**Patrón Inbound — Sistemas externos operan sobre Guidewire:**
Un portal de mediadores crea una cotización. La petición entra por el API Management (3Scale), pasa por el Integration Gateway que valida, transforma y enruta hacia la Cloud API correspondiente de PolicyCenter.

**Patrón Event — Flujo asíncrono:**
Guidewire publica eventos, Kafka los distribuye, los consumidores procesan. Los schemas AVRO registrados en Apicurio garantizan la compatibilidad entre productor y consumidor.

### 1.7 Preguntas Típicas de Entrevista

- **¿Cómo se integra Guidewire con sistemas legacy?** — A través del Integration Gateway usando Apache Camel, exponiendo APIs REST con especificación OpenAPI y mensajería asíncrona con Kafka para eventos.
- **¿Qué papel juega el Integration Gateway en una arquitectura EDA?** — Actúa como puente que transforma eventos de negocio de Guidewire en eventos Kafka consumibles por otros sistemas.
- **¿Diferencia entre Cloud API y Legacy API?** — Cloud API es RESTful basada en JSON:API orientada a GWCP. Las legacy APIs son SOAP/XML del modelo on-premise.
- **¿Cuál es tu rol en este perfil respecto a Guidewire?** — No dominar Guidewire a nivel funcional de seguros, sino construir y gobernar la capa de integración: Integration Gateway, APIs, eventos Kafka, schemas AVRO, API Management.

---

## 2. Arquitecturas: SOA, MSA y EDA

Las tres arquitecturas no son excluyentes; en este stack conviven simultáneamente.

### 2.1 SOA (Service Oriented Architecture)

Base conceptual del modelo de integración. Nació para romper monolitos exponiendo funcionalidades como servicios reutilizables con contratos bien definidos (WSDL/XSD en SOAP, OpenAPI en REST). El concepto central es el **ESB** (Enterprise Service Bus), que centraliza mediación, transformación y enrutamiento. En este stack, Red Hat Fuse cumple ese rol.

SOA no está muerto, está evolucionado. Muchas aseguradoras mantienen servicios SOAP legacy que hay que integrar. El Integration Gateway de Guidewire tiene que hablar ambos mundos.

### 2.2 MSA (Microservices Architecture)

Evolución natural de SOA. Servicios pequeños, autónomos, con su propia base de datos, desplegables de forma independiente. En OpenShift cada microservicio es un pod. La comunicación es directa (REST) o por eventos (Kafka). No hay ESB centralizado; cada servicio gestiona su propia lógica de integración, apoyándose en sidecars o service mesh.

En el contexto de la oferta, los nuevos desarrollos alrededor de Guidewire siguen MSA: microservicios en Spring Boot o Quarkus desplegados en OpenShift que consumen las Cloud APIs de Guidewire.

### 2.3 EDA (Event Driven Architecture)

Modelo de comunicación asíncrono. No reemplaza SOA ni MSA, los complementa. En lugar de "llamo y espero respuesta", el productor emite un evento y se desentiende. Los consumidores reaccionan cuando les llega. Kafka es el backbone (la autopista central por la que fluyen todos los eventos). Los schemas AVRO en Apicurio garantizan que productor y consumidor hablen el mismo idioma.

En Guidewire, EDA es fundamental: cada cambio de estado en una póliza, siniestro o pago genera un evento que alimenta el ecosistema.

### 2.4 Convivencia de las Tres Arquitecturas

El core Guidewire expone APIs REST (SOA evolucionado). Los microservicios alrededor (MSA) consumen esas APIs y se comunican entre sí por REST o Kafka. Los eventos de negocio fluyen por Kafka (EDA). Red Hat Fuse actúa como mediador cuando hay transformación o enrutamiento complejo. 3Scale gobierna todo el tráfico API. OpenShift orquesta el despliegue.

### 2.5 Aclaraciones Conceptuales

**Pod vs contenedor:** Un pod es la unidad mínima de despliegue en Kubernetes/OpenShift. Un pod contiene uno o más contenedores Docker que comparten red y almacenamiento. En la práctica, el 90% de las veces un pod tiene un solo contenedor. La diferencia importa con sidecars: un pod puede tener un contenedor con tu microservicio y otro con un proxy Envoy (service mesh). Ambos comparten la misma IP y se ven como localhost.

**Conversión SOAP a REST/JSON:** Escenario habitual en integración con aseguradoras. Lo hace la capa de mediación (Red Hat Fuse / Apache Camel). Camel tiene componentes nativos para ambos mundos: `cxf` para SOAP y `rest` para REST. Un flujo típico: el microservicio llama a un endpoint REST/JSON → Camel transforma el JSON a XML/SOAP → invoca el servicio SOAP legacy → transforma la respuesta XML a JSON → devuelve al microservicio. Es un patrón EIP clásico: Message Translator + Protocol Bridge.

**Quarkus vs Spring Boot — Curva de aprendizaje:** Baja viniendo de Spring Boot. Quarkus soporta Spring Compatibility Extensions (`@RestController`, `@Autowired`, `@ConfigurationProperties`). La inyección de dependencias usa CDI (Jakarta EE) en lugar de Spring IoC: `@Inject` en lugar de `@Autowired`, `@ApplicationScoped` en lugar de `@Component`. La ventaja real es el arranque en milisegundos y consumo mínimo de memoria gracias a compilación nativa con GraalVM, crítico para escalado horizontal en OpenShift.

**Backbone (concepto):** Cuando se dice que Kafka es el backbone del EDA, significa que es la columna vertebral por la que circulan todos los eventos. Kafka no procesa nada, solo transporta y garantiza que nada se pierde y que todo llega en orden. Si cae Kafka, los sistemas siguen vivos pero dejan de comunicarse.

**AVRO vs alternativas:** Kafka es agnóstico respecto al formato de serialización. Se puede usar JSON plano (lo más habitual al empezar), Protobuf o AVRO. AVRO es el estándar de facto en ecosistemas Kafka enterprise porque soporta evolución de schemas de forma nativa, es binario/compacto y se integra nativamente con Schema Registry (Apicurio). Protobuf es más rápido pero sin soporte nativo de evolución de schemas tan elegante. JSON Schema es más legible pero significativamente más pesado y sin gobernanza automática de compatibilidad. No es obligatorio usar AVRO con Kafka; la mayoría de proyectos empiezan con JSON y migran a AVRO cuando el ecosistema crece y necesitan gobernanza formal de contratos.

### 2.6 Preguntas Típicas de Entrevista

- **¿Cuándo usarías REST síncrono vs Kafka asíncrono?** — REST para operaciones que requieren respuesta inmediata (consulta de póliza, cotización). Kafka para operaciones donde el productor no necesita esperar (notificaciones, auditoría, alimentar data lake).
- **¿Cómo gestionas transacciones distribuidas en MSA?** — Patrón Saga (orquestada o coreografiada), compensaciones en caso de fallo, consistencia eventual.
- **¿Cómo manejas la compatibilidad de schemas?** — AVRO + Apicurio con reglas de compatibilidad FULL. Validación en tres niveles: registro, compilación y runtime.

---

## 3. Especificación API: OpenAPI, AsyncAPI, AVRO y API First

### 3.1 API First — Metodología

API First es la metodología que vertebra todo el desarrollo. El principio: **antes de escribir una sola línea de código, se define el contrato de la API**. El contrato es la fuente de verdad, no la implementación.

Flujo API First:

1. Se diseña el contrato (OpenAPI para REST, AsyncAPI para eventos).
2. Se registra en Apicurio.
3. Se genera el esqueleto de código (stubs) a partir del contrato.
4. Se implementa la lógica de negocio sobre los stubs generados.

La ventaja es que equipos de frontend, backend, QA y terceros trabajan en paralelo desde el minuto uno, porque todos comparten el mismo contrato.

### 3.2 OpenAPI (antes Swagger)

Especificación para APIs **síncronas REST**. Un fichero YAML/JSON que define endpoints, métodos HTTP, parámetros, request/response bodies, códigos de error, autenticación y modelos de datos. Versión actual: 3.1.

En este stack, las Cloud APIs de Guidewire están especificadas en OpenAPI y todas las APIs REST que se expongan deben tener su especificación OpenAPI registrada en Apicurio y publicada en 3Scale.

**OpenAPI Generator** es la herramienta que genera código a partir de la especificación. Disponible como:

- **Plugin Maven** (lo más habitual en proyectos Java):

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.19.0</version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <apiPackage>com.example.api</apiPackage>
                <modelPackage>com.example.model</modelPackage>
                <configOptions>
                    <interfaceOnly>true</interfaceOnly>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- **CLI local** via npm: `npm install -g @openapitools/openapi-generator-cli`
- **JAR descargable** desde Maven Central
- **Imagen Docker**: `docker run openapitools/openapi-generator-cli generate ...`
- **Web oficial**: https://openapi-generator.tech
- **Repositorio GitHub**: https://github.com/OpenAPITools/openapi-generator
- **Editor visual online**: Swagger Editor en https://editor.swagger.io

OpenAPI Generator soporta más de 30 lenguajes para clients y más de 20 frameworks para server stubs.

### 3.3 Concepto de Stub

Un **stub** es un esqueleto de código generado automáticamente que define la estructura (interfaces, clases, firmas de métodos) pero sin lógica de negocio. El desarrollador solo implementa lo que falta.

Ejemplo. A partir de este OpenAPI YAML:

```yaml
openapi: 3.0.3
info:
  title: API de Pólizas
  version: 1.0.0
paths:
  /polizas/{id}:
    get:
      operationId: getPoliza
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Póliza encontrada
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Poliza'
components:
  schemas:
    Poliza:
      type: object
      properties:
        id:
          type: string
        titular:
          type: string
        estado:
          type: string
          enum: [ACTIVA, CANCELADA, SUSPENDIDA]
        prima:
          type: number
          format: double
```

OpenAPI Generator con `interfaceOnly=true` genera:

```java
// GENERADO — NO TOCAR
public interface PolizasApi {
    @GetMapping("/polizas/{id}")
    ResponseEntity<Poliza> getPoliza(@PathVariable("id") String id);
}

// GENERADO — NO TOCAR
public class Poliza {
    private String id;
    private String titular;
    private EstadoEnum estado;
    private Double prima;
    // getters, setters, equals, hashCode, toString...
}
```

El desarrollador solo escribe la implementación:

```java
// ESTO SÍ LO ESCRIBES TÚ
@RestController
public class PolizasController implements PolizasApi {
    @Override
    public ResponseEntity<Poliza> getPoliza(String id) {
        Poliza poliza = polizaService.findById(id);
        return ResponseEntity.ok(poliza);
    }
}
```

El generador hace el trabajo mecánico (interfaces, modelos, anotaciones, validaciones), el desarrollador pone la inteligencia.

### 3.4 AsyncAPI

Equivalente de OpenAPI pero para APIs **asíncronas y eventos**. Define canales (topics de Kafka), mensajes, schemas del payload, protocolos (Kafka, AMQP, MQTT) y bindings específicos del broker. Versión actual: 3.0.

Un fichero AsyncAPI describe: qué topics existen, qué eventos circulan por cada topic, qué schema tiene cada evento, quién produce y quién consume, y qué protocolo se usa.

Ejemplo detallado:

```yaml
asyncapi: 3.0.0
info:
  title: Eventos de Siniestros Guidewire
  version: 1.0.0
  description: |
    Eventos emitidos por ClaimCenter cuando se producen
    cambios en el ciclo de vida de un siniestro.

servers:
  produccion:
    host: kafka-broker-prod:9092
    protocol: kafka
    description: Cluster Kafka de producción

channels:
  siniestroAbierto:
    address: guidewire.claims.siniestro-abierto
    messages:
      SiniestroAbiertoMessage:
        $ref: '#/components/messages/SiniestroAbierto'

  siniestroResuelto:
    address: guidewire.claims.siniestro-resuelto
    messages:
      SiniestroResueltoMessage:
        $ref: '#/components/messages/SiniestroResuelto'

operations:
  publicarSiniestroAbierto:
    action: send
    channel:
      $ref: '#/channels/siniestroAbierto'
    summary: ClaimCenter publica cuando se abre un siniestro

  consumirSiniestroAbierto:
    action: receive
    channel:
      $ref: '#/channels/siniestroAbierto'
    summary: Sistemas downstream consumen el evento

components:
  messages:
    SiniestroAbierto:
      payload:
        schemaFormat: application/vnd.apache.avro;version=1.9.0
        schema:
          $ref: './schemas/siniestro-abierto.avsc'
      headers:
        type: object
        properties:
          correlationId:
            type: string
          source:
            type: string
            enum: [ClaimCenter]

    SiniestroResuelto:
      payload:
        schemaFormat: application/vnd.apache.avro;version=1.9.0
        schema:
          $ref: './schemas/siniestro-resuelto.avsc'
```

El `payload` referencia un schema AVRO externo (`.avsc`). AsyncAPI define el "sobre" (canales, operaciones, quién produce/consume) y AVRO define el "contenido" (estructura del mensaje).

**Herramientas para diseñar y validar AsyncAPI:**

- **AsyncAPI Studio** (https://studio.asyncapi.com): herramienta online oficial para validar, previsualizar documentación y generar templates.
- **AsyncAPI CLI**: herramienta de línea de comandos (`npm install -g @asyncapi/cli`). Permite validar documentos y abrir Studio localmente con `asyncapi start studio`.
- **Spectral**: herramienta open source para definir reglas de linting personalizadas y aplicar estándares de gobernanza corporativa sobre documentos AsyncAPI.
- **Microcks** (https://microcks.io): mocking y testing de APIs asíncronas. Lee el AsyncAPI y genera un mock que publica mensajes fake en Kafka.

### 3.5 AVRO

AVRO define el esquema de los datos que viajan por Kafka. Los schemas se escriben en ficheros `.avsc` (JSON):

```json
{
  "type": "record",
  "name": "SiniestroAbierto",
  "namespace": "com.seguros.eventos",
  "fields": [
    {"name": "siniestroId", "type": "string"},
    {"name": "polizaId", "type": "string"},
    {"name": "fechaApertura", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "tipoDano", "type": {"type": "enum", "name": "TipoDano",
        "symbols": ["VEHICULO", "HOGAR", "SALUD", "VIDA"]}},
    {"name": "descripcion", "type": ["null", "string"], "default": null},
    {"name": "importeEstimado", "type": ["null", "double"], "default": null}
  ]
}
```

La generación de clases Java se hace con el plugin Maven de AVRO:

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.11.3</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals><goal>schema</goal></goals>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/avro</sourceDirectory>
                <outputDirectory>${project.build.directory}/generated-sources/avro</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Esto genera una clase `SiniestroAbierto.java` con todos los campos tipados, builder pattern y serialización AVRO integrada.

### 3.6 Ciclo de Vida de una API REST (API First)

```
1. DISEÑO
   └─ Arquitecto define el contrato OpenAPI (YAML)
   └─ Herramienta: Swagger Editor o Apicurio UI
   └─ Se valida con Spectral (linting corporativo)

2. REGISTRO
   └─ El YAML se sube a Apicurio Service Registry
   └─ Apicurio valida estructura y compatibilidad
   └─ Se versiona (v1.0.0, v1.1.0...)

3. GENERACIÓN DE CÓDIGO (build time)
   └─ Plugin Maven openapi-generator-maven-plugin
   └─ Genera interfaces Java (stubs) + modelos (POJOs)
   └─ Se ejecuta en `mvn compile`
   └─ Las clases generadas van a target/generated-sources/

4. IMPLEMENTACIÓN
   └─ El desarrollador implementa las interfaces generadas
   └─ Escribe la lógica de negocio en el Controller
   └─ Las firmas de métodos están fijadas por el contrato

5. VALIDACIÓN EN BUILD
   └─ Si el contrato cambia y el código no compila → fallo en CI
   └─ Tests de contrato (Pact/Microcks) validan cumplimiento

6. PUBLICACIÓN EN API MANAGEMENT
   └─ El YAML OpenAPI se importa en Red Hat 3Scale
   └─ 3Scale genera el portal de desarrolladores
   └─ Se configuran políticas: rate limiting, API keys, OAuth

7. RUNTIME
   └─ Los consumidores generan su client SDK desde el mismo YAML
   └─ 3Scale valida tokens y aplica throttling
   └─ El tráfico llega al microservicio en OpenShift
```

### 3.7 Ciclo de Vida de un Evento AVRO (API First)

```
1. DISEÑO
   └─ Se define el schema AVRO (.avsc)
   └─ Se define la especificación AsyncAPI (.yaml)
   └─ Herramientas: editor + AsyncAPI Studio para validar

2. REGISTRO
   └─ El .avsc se sube a Apicurio Service Registry
   └─ Apicurio valida compatibilidad (BACKWARD/FORWARD/FULL)
   └─ Si rompe compatibilidad → RECHAZO (no se registra)
   └─ El AsyncAPI también se registra en Apicurio

3. GENERACIÓN DE CÓDIGO (build time)
   └─ Plugin Maven avro-maven-plugin
   └─ Genera clases Java (SpecificRecord) desde el .avsc
   └─ Se ejecuta en `mvn generate-sources`

4. IMPLEMENTACIÓN
   └─ Producer: crea instancias de la clase generada y las envía
   └─ Consumer: recibe objetos ya deserializados y tipados
   └─ El serializer/deserializer maneja AVRO automáticamente

5. VALIDACIÓN EN BUILD
   └─ Si el schema cambia en Apicurio, el plugin descarga
      la nueva versión y regenera las clases
   └─ Si el código no compila → fallo en CI

6. VALIDACIÓN EN RUNTIME
   └─ KafkaAvroSerializer contacta Apicurio (1ª vez, luego caché)
   └─ Serializa el mensaje según el schema registrado
   └─ Mete el schema ID en la cabecera del mensaje
   └─ KafkaAvroDeserializer del consumer resuelve el schema ID
   └─ Si el mensaje no cumple el schema → excepción en runtime

7. GOBERNANZA CONTINUA
   └─ Cada cambio de schema pasa por Apicurio
   └─ Las reglas de compatibilidad protegen a los consumidores
   └─ El histórico de versiones queda registrado
```

### 3.8 Cuadro Resumen de Validación de Contratos

| Momento | Qué se valida | Quién valida |
|---------|--------------|-------------|
| Diseño | Estructura y linting | Swagger Editor / AsyncAPI Studio / Spectral |
| Registro | Compatibilidad con versiones anteriores | Apicurio Service Registry |
| Build | Compilación contra el contrato | Maven plugins (openapi-generator / avro-maven-plugin) |
| CI/CD | Tests de contrato | Pact / Microcks |
| Runtime | Mensajes contra schema | KafkaAvroSerializer/Deserializer |
| API Gateway | Tokens, rate limits, políticas | Red Hat 3Scale |

### 3.9 Preguntas Típicas de Entrevista

- **¿Qué es API First y por qué es importante?** — Diseñar el contrato antes del código. Permite trabajo en paralelo, gobernanza desde el diseño y detección temprana de incompatibilidades.
- **¿Diferencia entre OpenAPI y AsyncAPI?** — OpenAPI para REST síncrono, AsyncAPI para mensajería asíncrona. Ambos definen contratos pero para paradigmas de comunicación distintos.
- **¿Cómo garantizas que la implementación cumple el contrato?** — Generación de código contract-first, validación en build y tests de contrato con Pact o Microcks.
- **¿Qué es un stub?** — Esqueleto de código generado automáticamente (interfaces + modelos) a partir del contrato. El desarrollador solo implementa la lógica de negocio.

---

## 4. API Management: Red Hat 3Scale

### 4.1 Qué es 3Scale

Red Hat 3Scale API Management es la plataforma de gestión de APIs del stack. Se sitúa como puerta de entrada a todas las APIs del ecosistema. Cualquier petición que venga de fuera (portales, apps, partners) pasa por 3Scale antes de llegar a los microservicios o al Integration Gateway de Guidewire.

### 4.2 Funciones Principales

**Gestión del tráfico:** Rate limiting (limitar peticiones por segundo/minuto/día por consumidor), throttling (degradar en lugar de rechazar), circuit breaking.

**Seguridad:** Autenticación (API Keys, OAuth 2.0, OpenID Connect, JWT), autorización por plan, TLS termination, protección contra ataques (DDoS básico, inyección).

**Portal de desarrolladores:** Se autogenera a partir de la especificación OpenAPI. Los consumidores de la API pueden registrarse, obtener sus credenciales, leer la documentación interactiva (try-it-out), ver ejemplos y descargar SDKs.

**Planes y monetización:** Se definen planes de uso (free, basic, premium) con distintos límites. Útil cuando terceros consumen las APIs (brokers de seguros, comparadores, partners).

**Analytics y monitorización:** Dashboard con métricas de uso: peticiones por API, latencia, errores, consumo por cliente, tendencias.

**Políticas (policies):** Plugins que se encadenan en el pipeline de procesamiento de cada petición. Ejemplos: CORS, transformación de headers, IP whitelisting, caché de respuestas, validación de JWT.

### 4.3 Arquitectura de 3Scale

3Scale tiene tres componentes principales:

- **APIcast (Gateway)**: Proxy nginx-based que intercepta todas las peticiones. Se despliega como pod en OpenShift. Puede estar en modo staging (para pruebas) o production.
- **Admin Portal**: Interfaz web de administración donde se configuran APIs, planes, políticas y se revisan analytics.
- **Developer Portal**: Portal público autogenerado para consumidores de APIs.

### 4.4 Equivalente Community para POC

3Scale no tiene versión community completa. Para la POC, las alternativas son:

- **Kong** (https://konghq.com): API Gateway open source muy extendido. Soporta plugins equivalentes a las policies de 3Scale. Tiene versión community con Docker.
- **Gravitee** (https://gravitee.io): Plataforma de API Management open source con portal de desarrolladores incluido.

Los conceptos son idénticos: gateway, políticas, planes, portal, analytics. Lo que cambia es la UI y la configuración.

### 4.5 Flujo de una Petición a través de 3Scale

```
Consumidor → 3Scale APIcast → Valida credenciales → Aplica rate limit
    → Aplica políticas (CORS, transformación, caché)
    → Enruta al backend (Integration Gateway / Microservicio)
    → Recibe respuesta → La devuelve al consumidor
    → Registra métricas en analytics
```

### 4.6 Preguntas Típicas de Entrevista

- **¿Qué diferencia hay entre un API Gateway y un API Manager?** — El gateway es el componente técnico que intercepta tráfico (APIcast). El API Manager es la plataforma completa: gateway + portal + planes + analytics + gobernanza.
- **¿Cómo protegerías una API expuesta a terceros?** — OAuth 2.0 / OpenID Connect para autenticación, rate limiting por plan, TLS, validación de JWT en el gateway, IP whitelisting si aplica.
- **¿Dónde se publica la especificación OpenAPI?** — En Apicurio como fuente de verdad y en 3Scale para generar el portal de desarrolladores con documentación interactiva.

---

## 5. Integración EIP: Red Hat Fuse (Apache Camel + Spring Boot/Quarkus)

### 5.1 Qué es Red Hat Fuse

Red Hat Fuse es la distribución enterprise de **Apache Camel**. Es exactamente el mismo motor de integración pero con soporte Red Hat, parches certificados y operadores para OpenShift. El código que escribes en Camel community es idéntico al que corres en Fuse.

### 5.2 Apache Camel — Fundamentos

Apache Camel es un framework de integración que implementa los **Enterprise Integration Patterns (EIP)** del libro de Gregor Hohpe y Bobby Woolf. Proporciona un DSL (Domain Specific Language) para definir rutas de integración de forma declarativa.

Concepto central: la **ruta**. Una ruta define un flujo de mensajes desde un origen (consumer) a un destino (producer), con transformaciones y lógica intermedia.

```java
from("kafka:guidewire.claims.siniestro-abierto")
    .unmarshal().avro(SiniestroAbierto.class)
    .filter(simple("${body.tipoDano} == 'VEHICULO'"))
    .marshal().json()
    .to("rest:post:http://fraud-service/api/check");
```

Esta ruta: consume un evento AVRO de Kafka → lo deserializa → filtra solo siniestros de vehículos → lo convierte a JSON → lo envía a un servicio REST de fraude.

### 5.3 EIP Más Relevantes para este Perfil

**Message Translator:** Transformar entre formatos. JSON ↔ XML, AVRO ↔ JSON, SOAP ↔ REST. Es el patrón más usado en la mediación entre Guidewire y el ecosistema.

**Content-Based Router:** Enrutar un mensaje a destinos distintos según su contenido.

```java
from("kafka:guidewire.claims.siniestro-abierto")
    .choice()
        .when(simple("${body.tipoDano} == 'VEHICULO'"))
            .to("direct:procesarVehiculo")
        .when(simple("${body.tipoDano} == 'HOGAR'"))
            .to("direct:procesarHogar")
        .otherwise()
            .to("direct:procesarGenerico");
```

**Protocol Bridge:** Conectar dos protocolos distintos. Ejemplo: recibir REST y llamar SOAP, o recibir Kafka y llamar REST.

**Splitter:** Dividir un mensaje compuesto en partes individuales. Ejemplo: un batch de pólizas se divide para procesarlas una a una.

**Aggregator:** Reunir múltiples mensajes en uno solo. Ejemplo: recoger respuestas de varios servicios de scoring y combinarlas.

**Dead Letter Channel:** Manejar errores. Los mensajes que fallan tras N reintentos se envían a una cola de errores para revisión.

**Wire Tap:** Enviar una copia del mensaje a otro destino sin afectar el flujo principal. Útil para auditoría y logging.

### 5.4 Componentes Camel Relevantes

| Componente | Uso en este stack |
|---|---|
| `camel-kafka` | Producir/consumir eventos Kafka |
| `camel-rest` | Exponer y consumir APIs REST |
| `camel-cxf` | Conectar con servicios SOAP legacy |
| `camel-jackson` | Serialización JSON |
| `camel-avro` | Serialización AVRO |
| `camel-jms` / `camel-amqp` | Conectar con ActiveMQ Artemis |
| `camel-http` | Llamadas HTTP genéricas |
| `camel-bean` | Invocar lógica Java (Drools, servicios) |
| `camel-quarkus-*` | Extensiones específicas para Quarkus |

### 5.5 Camel con Spring Boot vs Quarkus

Con **Spring Boot**: se usa `camel-spring-boot-starter`. Las rutas son `@Component` que extienden `RouteBuilder`. Configuración en `application.properties` con prefijo `camel.*`.

Con **Quarkus**: se usa `camel-quarkus-*`. Las rutas son `@ApplicationScoped` que extienden `RouteBuilder`. La ventaja principal es el arranque en milisegundos y la compilación nativa con GraalVM.

Ambos producen un fat JAR o una imagen Docker idéntica desde el punto de vista funcional.

### 5.6 Preguntas Típicas de Entrevista

- **¿Qué es una ruta Camel?** — Un flujo de integración definido como una secuencia de procesadores entre un consumer (from) y un producer (to), con transformaciones y lógica intermedia.
- **¿Cómo mediarías entre un servicio SOAP legacy y un microservicio REST?** — Ruta Camel con `from("rest:...")`, transformación con Message Translator (XSLT o marshal/unmarshal), llamada SOAP con `cxf`, transformación de la respuesta a JSON.
- **¿Cómo manejas errores en Camel?** — Dead Letter Channel para errores definitivos, reintentos con backoff exponencial para errores transitorios, onException para manejo específico por tipo de excepción.
- **¿Diferencia entre Red Hat Fuse y Apache Camel?** — Mismo motor. Fuse añade soporte enterprise, parches certificados, operadores OpenShift y consola de gestión Hawtio.

---

## 6. Event Driven Architecture: Kafka y ActiveMQ

### 6.1 Apache Kafka (Red Hat AMQ Streams)

Red Hat AMQ Streams es Apache Kafka con soporte Red Hat y operadores para OpenShift. El código y la configuración son idénticos.

**Conceptos fundamentales:**

- **Topic**: canal con nombre donde se publican eventos. Ejemplo: `guidewire.claims.siniestro-abierto`.
- **Partition**: subdivisión de un topic para paralelismo. Cada partición es una cola ordenada.
- **Producer**: quien publica mensajes en un topic.
- **Consumer**: quien lee mensajes de un topic.
- **Consumer Group**: grupo de consumers que se reparten las particiones. Cada mensaje lo procesa un solo consumer del grupo.
- **Offset**: posición de lectura de un consumer dentro de una partición.
- **Retention**: cuánto tiempo se guardan los mensajes (configurable: horas, días, ilimitado con log compaction).

**Kafka en este stack:** Es el backbone del EDA. Guidewire publica eventos de negocio, los microservicios los consumen. Los schemas AVRO se registran en Apicurio. La comunicación es completamente desacoplada.

**KRaft vs ZooKeeper:** Las versiones modernas de Kafka (3.3+) usan KRaft para la gestión de metadatos, eliminando la dependencia de ZooKeeper. En la POC se usa KRaft.

### 6.2 Apache ActiveMQ Artemis (Red Hat AMQ Broker)

Red Hat AMQ Broker es Apache ActiveMQ Artemis con soporte Red Hat. Se usa para mensajería punto a punto y pub/sub donde Kafka no encaja.

**¿Cuándo usar ActiveMQ en lugar de Kafka?**

| Escenario | Kafka | ActiveMQ |
|---|---|---|
| Streaming de eventos a gran escala | Sí | No |
| Retención de mensajes largo plazo | Sí | No |
| Múltiples consumers leyendo el mismo mensaje | Sí | Con topics JMS |
| Mensajería request/reply | Incómodo | Sí |
| Transacciones JTA/XA | No soporta | Sí |
| Colas con prioridad | No soporta | Sí |
| Protocolo AMQP 1.0 | No nativo | Sí |
| Integración con Jakarta EE/JMS | No nativo | Sí |

En este stack, **Kafka** se usa para eventos de negocio (EDA) y **ActiveMQ** se usa para comunicación entre servicios que necesitan request/reply, transacciones distribuidas o integración con sistemas Jakarta EE legacy.

### 6.3 Preguntas Típicas de Entrevista

- **¿Cuándo usarías Kafka vs ActiveMQ?** — Kafka para event streaming a gran escala con retención y múltiples consumidores. ActiveMQ para mensajería transaccional, request/reply y cuando se necesita JMS/AMQP.
- **¿Cómo garantizas que un mensaje no se pierde en Kafka?** — `acks=all` en el producer, `min.insync.replicas=2` en el broker, consumer con commit manual de offsets tras procesamiento exitoso.
- **¿Qué pasa si un consumer falla?** — El consumer group rebalancea y otro consumer del grupo asume las particiones del que falló. Los mensajes no procesados se reprocesan desde el último offset commiteado.

---

## 7. Reglas de Negocio: IBM BAM Open Editions (Drools)

### 7.1 Qué es Drools

IBM Business Automation Manager Open Editions es Drools con soporte IBM. Drools es un motor de reglas de negocio (BRMS) que permite separar la lógica de negocio del código Java.

En lugar de codificar decisiones con `if/else` en Java, se definen reglas declarativas que el motor evalúa. Esto permite que los analistas de negocio modifiquen reglas sin tocar código.

### 7.2 Para Qué se Usa en este Stack

En el ecosistema Guidewire, Drools se usa para decisiones que cambian frecuentemente:

- **Scoring de riesgo**: evaluar el riesgo de un nuevo asegurado.
- **Detección de fraude**: reglas que identifican patrones sospechosos en siniestros.
- **Cálculo de primas**: reglas que ajustan la prima base según el perfil del asegurado.
- **Derivación de siniestros**: decidir a qué equipo/perito se asigna un siniestro.
- **Validaciones de negocio**: reglas sobre normativa vigente.

### 7.3 Cómo se Integra con Camel

Drools se invoca desde una ruta Camel como un servicio más:

```java
from("kafka:guidewire.claims.siniestro-abierto")
    .unmarshal().avro(SiniestroAbierto.class)
    .to("direct:evaluarFraude")

from("direct:evaluarFraude")
    .bean(droolsService, "evaluar")
    .choice()
        .when(simple("${body.nivelRiesgo} == 'ALTO'"))
            .to("kafka:alertas.fraude")
        .otherwise()
            .to("kafka:siniestros.procesados");
```

### 7.4 DRL — Lenguaje de Reglas

Las reglas se escriben en ficheros `.drl`:

```
rule "Siniestro sospechoso - múltiples en 30 días"
when
    $s : Siniestro(fechaApertura > hace30Dias)
    $count : Number(intValue > 2) from accumulate(
        Siniestro(polizaId == $s.polizaId, fechaApertura > hace30Dias),
        count(1)
    )
then
    $s.setNivelRiesgo("ALTO");
    $s.setMotivo("Más de 2 siniestros en 30 días");
end
```

### 7.5 Decision Model and Notation (DMN)

Además de DRL, Drools soporta **DMN** (Decision Model and Notation), un estándar OMG que permite definir decisiones en tablas visuales que los analistas de negocio pueden editar directamente sin conocer DRL.

### 7.6 Preguntas Típicas de Entrevista

- **¿Por qué usar un motor de reglas en lugar de codificar en Java?** — Separación de responsabilidades. Las reglas cambian frecuentemente y un motor permite modificarlas sin redesplegar la aplicación.
- **¿Cómo se despliega Drools?** — Como un microservicio (KIE Server) en OpenShift que expone una API REST. Los ficheros DRL/DMN se empaquetan en un KJAR (Knowledge JAR) y se despliegan en caliente.
- **¿Qué diferencia hay entre Drools y un simple if/else?** — Drools usa el algoritmo Rete para evaluación eficiente de muchas reglas simultáneas, soporta encadenamiento y permite modificar reglas en caliente.

---

## 8. Registro de Servicios: Apicurio Service Registry

### 8.1 Qué es Apicurio

Apicurio Service Registry es un servicio independiente que actúa como el catálogo centralizado de todos los schemas y contratos API del ecosistema. Se despliega como un pod más en OpenShift y todos los servicios se conectan a él por HTTP REST.

Soporta múltiples formatos: AVRO, Protobuf, JSON Schema, OpenAPI, AsyncAPI, GraphQL y XML Schema (WSDL/XSD). Es el punto único de verdad para todos los contratos del stack.

### 8.2 Origen y Contexto

El nombre "Apicurio" viene del latín, relacionado con "el que cuida con esmero". Fue creado por **Red Hat**, liderado por **Eric Wittmann**. Es open source bajo licencia Apache 2.0, alojado en GitHub bajo la organización `Apicurio`. Red Hat lo integró como parte de **Red Hat Integration**. La versión community es funcionalmente idéntica a la enterprise.

### 8.3 Patrones que Implementa

- **Schema Registry Pattern**: catálogo centralizado con versionado y validación de compatibilidad.
- **Contract-First / API First**: repositorio de contratos que habilita el diseño antes de la implementación.
- **Schema Evolution**: gestión controlada de cambios con reglas de compatibilidad.
- **Schema Validation**: validación de mensajes contra el schema registrado en runtime.
- **Content Negotiation**: múltiples formatos de serialización bajo un mismo registry.

### 8.4 Funciones Generales

Gestión de artefactos con grupos, versionado semántico, reglas de validación globales y por artefacto, búsqueda y filtrado, API REST completa, UI web de administración, webhooks, export/import masivo, integración con Kafka Connect, soporte multi-tenant y roles de acceso.

### 8.5 Tres Momentos de Validación

**Tiempo de registro (el más importante):** Al subir una nueva versión de un schema, Apicurio aplica las reglas de compatibilidad. Si rompe compatibilidad, se rechaza.

Reglas de compatibilidad:

- **BACKWARD**: consumidores con schema nuevo pueden leer datos escritos con schema viejo.
- **FORWARD**: consumidores con schema viejo pueden leer datos escritos con schema nuevo.
- **FULL**: backward + forward simultáneamente. La más restrictiva y segura.
- **NONE**: sin validación.

**Tiempo de serialización/deserialización (runtime):** El `KafkaAvroSerializer` contacta Apicurio, serializa y mete el schema ID en la cabecera. El `KafkaAvroDeserializer` resuelve el ID y deserializa.

**Tiempo de compilación/build (opcional pero recomendable):** El plugin Maven descarga schemas y genera clases Java automáticamente.

```xml
<plugin>
    <groupId>io.apicurio</groupId>
    <artifactId>apicurio-registry-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>download</goal></goals>
        </execution>
    </executions>
</plugin>
```

### 8.6 Latencia Introducida

Mínima. Caché local en el serializer tras la primera resolución.

- **Primera llamada**: 5-20ms (HTTP GET al registry).
- **Llamadas posteriores**: 0ms adicional.
- **Registro de schema**: 10-50ms (solo en CI/CD).

Si Apicurio cae, producers/consumers siguen funcionando con schemas cacheados.

### 8.7 Preguntas Típicas de Entrevista

- **¿Cómo garantizas la compatibilidad entre servicios?** — Apicurio con reglas FULL y validación en tres niveles.
- **¿Qué ocurre si un producer cambia el schema sin Apicurio?** — El serializer falla porque el schema no está registrado.
- **¿Cómo gestionas múltiples entornos?** — Una instancia por entorno o grupos de artifacts separados.
- **¿Qué latencia introduce?** — Prácticamente cero gracias a la caché local.

---

## 9. Herramientas API: Postman y API Quality

### 9.1 Postman

Postman es la herramienta estándar para diseñar, probar y documentar APIs.

**Testing manual:** Probar endpoints REST del Integration Gateway, Cloud APIs de Guidewire y microservicios.

**Colecciones:** Agrupar peticiones relacionadas, compartir con el equipo, versionar en Git.

**Entornos:** Variables por entorno (dev/pre/pro). `{{base_url}}` apunta a distintos servidores sin modificar peticiones.

**Tests automatizados:** Scripts JavaScript que validan respuestas:

```javascript
pm.test("Status 200", () => pm.response.to.have.status(200));
pm.test("Poliza tiene ID", () => {
    const body = pm.response.json();
    pm.expect(body.id).to.not.be.undefined;
});
```

**Newman (CLI):** Ejecutor de colecciones desde línea de comandos. Se integra en pipelines CI/CD.

**Mock servers:** Genera mocks a partir de colecciones con ejemplos. Consumidores trabajan antes de que la API real esté implementada.

**Import de OpenAPI:** Importa ficheros OpenAPI YAML/JSON y genera automáticamente la colección.

### 9.2 API Quality

Herramienta de análisis estático y dinámico de APIs.

**Análisis estático:** Evalúa especificaciones OpenAPI/AsyncAPI contra reglas de buenas prácticas: nomenclatura, respuestas de error documentadas, schemas referenciados, versionado, seguridad, paginación.

**Análisis dinámico:** Ejecuta peticiones contra la API real y compara con la especificación. Detecta discrepancias entre contrato e implementación.

**Governance:** Define estándares corporativos de calidad API. Se integra en CI/CD para bloquear despliegues que no cumplan el umbral.

### 9.3 Preguntas Típicas de Entrevista

- **¿Cómo pruebas APIs en un entorno de integración?** — Postman para testing manual, Newman para CI/CD, Microcks para contract testing, API Quality para análisis estático.
- **¿Cómo compartes la configuración de testing?** — Colecciones Postman en Git, entornos compartidos, Newman en pipeline.

---

## 10. Plataforma PaaS/CaaS: Red Hat OpenShift 4.x

### 10.1 Qué es OpenShift

Red Hat OpenShift es una plataforma de contenedores enterprise basada en **Kubernetes**. Es Kubernetes con seguridad reforzada, operadores, consola web, pipelines CI/CD integrados (Tekton), service mesh (Istio/Envoy), monitorización (Prometheus/Grafana) y gestión de builds.

### 10.2 Conceptos Clave

**Pod:** Unidad mínima de despliegue. Uno o más contenedores que comparten red y almacenamiento.

**Deployment:** Define cómo se despliega un pod: imagen, réplicas, estrategia de actualización, variables, volúmenes.

**Service:** Abstracción que expone pods bajo un nombre DNS interno y un puerto.

**Route:** Expone un Service al exterior con hostname público y TLS.

**Operator:** Patrón que automatiza gestión de aplicaciones complejas. Hay operadores para Kafka (Strimzi), Apicurio, 3Scale y Drools.

**Namespace / Project:** Aislamiento lógico. Cada equipo o entorno tiene su propio project con cuotas y permisos.

**ConfigMap y Secret:** Configuración externalizada. ConfigMaps para datos no sensibles, Secrets para credenciales.

**HPA (Horizontal Pod Autoscaler):** Escala réplicas automáticamente según métricas (CPU, memoria, consumer lag de Kafka).

### 10.3 Qué Aporta OpenShift/Kubernetes a la Gestión de Pods

#### Service Discovery (Descubrimiento de Servicios)

Kubernetes tiene un DNS interno integrado. Al crear un Service llamado `apicurio-registry` en el namespace `integracion`, automáticamente se resuelve como `apicurio-registry.integracion.svc.cluster.local`. Los microservicios no necesitan saber IPs, solo nombres. Si un pod muere y se recrea con otra IP, el Service lo detecta y redirige el tráfico transparentemente.

OpenShift añade **Routes** encima de los Services para exponer al exterior con hostname público, TLS y balanceo.

#### Resiliencia y Auto-Reparación (Self-Healing)

Kubernetes monitoriza constantemente el estado de cada pod con tres mecanismos:

**Liveness Probe:** "¿Estás vivo?" — Kubernetes hace un HTTP GET cada N segundos al pod. Si falla X veces consecutivas, mata el pod y levanta uno nuevo automáticamente. Ejemplo: un microservicio Camel entra en deadlock, la liveness probe falla, Kubernetes lo reinicia sin intervención humana.

**Readiness Probe:** "¿Estás listo para recibir tráfico?" — Mientras el pod arranca o está sobrecargado, la readiness probe dice "no estoy listo" y Kubernetes deja de enviarle tráfico. Cuando se recupera, lo vuelve a incluir. Evita que los consumidores reciban errores 503.

**Startup Probe:** Para aplicaciones que tardan en arrancar (típico en Java/Spring Boot). Evita que la liveness probe mate el pod antes de que haya terminado de iniciar.

Si un pod muere inesperadamente, el **ReplicaSet** detecta que hay menos réplicas de las deseadas y crea una nueva automáticamente. Si un nodo entero cae, Kubernetes reubica todos sus pods en otros nodos.

#### Escalado Automático

**HPA (Horizontal Pod Autoscaler):** Escala el número de réplicas basándose en métricas. Si la CPU supera el 70%, HPA crea más réplicas. Soporta métricas custom como consumer lag de Kafka: si se acumulan mensajes, crea más consumers.

**VPA (Vertical Pod Autoscaler):** Ajusta los recursos (CPU/memoria) asignados a cada pod según uso real.

**Cluster Autoscaler:** Si no hay capacidad en el cluster, añade nodos automáticamente (en cloud).

#### Balanceo de Carga

Cada Service actúa como balanceador interno. Con 3 réplicas de un microservicio Camel, el Service distribuye peticiones entre las tres (round-robin por defecto). OpenShift soporta también balanceo por IP source, least connections y random a nivel de Route.

#### Gestión de Configuración y Secretos

**ConfigMaps:** Configuración externalizada. En lugar de meter `application.properties` dentro de la imagen Docker, se monta como ConfigMap. Cambiar configuración sin reconstruir la imagen.

**Secrets:** Lo mismo pero cifrado en etcd. Para contraseñas, API keys, certificados TLS. Se montan como variables de entorno o ficheros.

**Gestión por entorno:** Cada namespace (dev/pre/pro) tiene sus propios ConfigMaps y Secrets. El mismo Deployment usa configuraciones distintas según entorno sin cambiar código.

#### Despliegues sin Downtime

**Rolling Update (por defecto):** Kubernetes crea pods nuevos con la versión nueva mientras los viejos siguen sirviendo tráfico. Cuando los nuevos pasan las readiness probes, redirige el tráfico y mata los viejos. Cero downtime.

**Blue-Green:** Dos versiones completas en paralelo. Se cambia el tráfico de golpe. Si falla, se vuelve instantáneamente.

**Canary:** Porcentaje pequeño del tráfico (5-10%) a la versión nueva. Si todo va bien, se incrementa gradualmente hasta el 100%.

#### Service Mesh (Istio/Envoy)

Capa avanzada de gestión de tráfico entre pods. OpenShift integra **Istio** con sidecars **Envoy** que se inyectan automáticamente en cada pod.

**mTLS automático:** Toda la comunicación entre pods se cifra con certificados mutuos sin tocar código. El sidecar Envoy maneja el TLS transparentemente.

**Circuit Breaker:** Si un servicio downstream falla, el circuit breaker corta las peticiones tras N fallos y devuelve error rápido en lugar de saturar el servicio caído. Tras un timeout, prueba de nuevo.

**Retry automático:** Si una petición falla con 503, Envoy reintenta N veces con backoff. Configurable por servicio.

**Timeout:** Si un servicio no responde en X milisegundos, Envoy corta la conexión. Evita cascadas de timeouts.

**Traffic splitting:** Enviar el 90% del tráfico a v1 y el 10% a v2 (canary a nivel de mesh).

**Observabilidad:** Tracing distribuido automático con Jaeger. Cada petición que cruza varios microservicios se traza de extremo a extremo sin instrumentar código.

#### Monitorización y Logging Integrados

**Prometheus + Grafana:** OpenShift viene con Prometheus preinstalado que recolecta métricas de todos los pods (CPU, memoria, peticiones, latencia). Grafana para dashboards.

**EFK Stack (Elasticsearch + Fluentd + Kibana):** Logging centralizado. Todos los logs de todos los pods se recolectan e indexan automáticamente.

**Alerting:** Reglas de alerta en Prometheus. Si un pod reinicia más de 3 veces en 5 minutos, si la latencia supera un umbral, si Kafka tiene consumer lag alto → alerta automática.

#### Gestión de Recursos y Cuotas

**Resource Requests y Limits:** Cada pod declara cuánta CPU/memoria necesita (request) y cuánta puede usar como máximo (limit). Kubernetes usa esto para decidir dónde colocar pods y evitar que uno acapare recursos.

**ResourceQuotas:** Por namespace. "El equipo de integración puede usar máximo 32 CPUs y 64GB de RAM."

**LimitRanges:** Restricciones por pod. "Ningún pod puede pedir más de 4 CPUs o 8GB de RAM."

### 10.4 Resumen de Capacidades de la Plataforma

| Capacidad | Qué resuelve | Componente |
|---|---|---|
| Service Discovery | Servicios se encuentran por nombre, no por IP | DNS interno + Services |
| Self-Healing | Pods muertos se recrean automáticamente | Liveness/Readiness Probes + ReplicaSet |
| Escalado automático | Más carga → más pods → menos carga → menos pods | HPA / VPA |
| Balanceo de carga | Distribuir tráfico entre réplicas | Services + Routes |
| Configuración externalizada | Cambiar config sin reconstruir imagen | ConfigMaps + Secrets |
| Despliegue sin downtime | Actualizar sin cortar servicio | Rolling Update / Blue-Green / Canary |
| Seguridad inter-servicio | Cifrado mTLS automático entre pods | Service Mesh (Istio/Envoy) |
| Resiliencia de red | Circuit breaker, retry, timeout | Service Mesh (Istio/Envoy) |
| Observabilidad | Tracing distribuido, métricas, logs | Jaeger + Prometheus + EFK |
| Gobernanza de recursos | Cuotas por equipo, límites por pod | ResourceQuotas + LimitRanges |

### 10.5 Despliegue del Stack en OpenShift

| Componente | Mecanismo de despliegue |
|---|---|
| Kafka | Operador Strimzi/AMQ Streams → CRD `Kafka` |
| Apicurio | Operador Apicurio → CRD `ApicurioRegistry` |
| 3Scale | Operador 3Scale → CRD `APIManager` |
| Drools (KIE Server) | Operador KIE → CRD `KieApp` |
| ActiveMQ Artemis | Operador AMQ Broker → CRD `ActiveMQArtemis` |
| Microservicios (Camel) | Deployment + Service + Route |
| PostgreSQL | Operador o Deployment estándar |

### 10.6 Equivalente Local para POC

- **CRC (Red Hat OpenShift Local):** Plataforma elegida para la POC. OpenShift 4.x real de un solo nodo con Operators, Routes, BuildConfigs y Service Discovery. Requiere 9GB RAM mínimo y 4 CPUs. Proporciona el entorno más fiel a producción.
- **Minikube / Kind:** Alternativas con Kubernetes puro. Suficiente para validar workloads pero sin las capacidades enterprise de OpenShift (Operators, SCC, Routes).

### 10.7 Preguntas Típicas de Entrevista

- **¿Diferencia entre OpenShift y Kubernetes?** — OpenShift es Kubernetes con seguridad reforzada (SCC), consola web, operadores preinstalados, pipelines CI/CD (Tekton), service mesh y gestión de builds out-of-the-box.
- **¿Qué es un Operator?** — Un controlador que extiende Kubernetes para gestionar aplicaciones complejas automáticamente. El operador de Kafka sabe cómo escalar brokers, gestionar particiones y hacer rolling updates sin downtime.
- **¿Cómo escalarías un microservicio?** — HPA basado en métricas de CPU/memoria o métricas custom (consumer lag de Kafka). Si el lag aumenta, se crean más réplicas del consumer automáticamente.
- **¿Cómo garantizas la resiliencia entre microservicios?** — Service Mesh con Istio/Envoy: circuit breaker, retry automático, timeout, mTLS. A nivel de aplicación: Dead Letter Channel en Camel, consumer groups en Kafka.
- **¿Cómo se despliega sin downtime?** — Rolling Update por defecto: pods nuevos arrancan mientras los viejos sirven, readiness probes validan antes de recibir tráfico. Blue-Green o Canary para despliegues de mayor riesgo.

---

## 11. Equivalencia Enterprise vs Community

Los productos Red Hat son wrappers enterprise sobre proyectos open source. El código core es idéntico.

| Componente Enterprise | Equivalente Community | Idéntico |
|---|---|---|
| Red Hat Fuse | Apache Camel | Sí |
| Red Hat AMQ Streams | Apache Kafka | Sí |
| Red Hat AMQ Broker | Apache ActiveMQ Artemis | Sí |
| IBM BAM Open Editions | Drools community | Sí |
| Apicurio (Red Hat) | Apicurio community | Sí |
| Red Hat OpenShift 4.x | Minikube / Docker Compose | No, mismos conceptos |
| Red Hat 3Scale | Kong / Gravitee | No, mismos conceptos |
| Guidewire InsuranceSuite | Mock Spring Boot | Sin equivalente |

---

## 12. POC — Prueba de Concepto

### 12.1 Objetivo

Demostrar el dominio del stack completo montando un flujo de integración end-to-end que simule el ecosistema Guidewire con todos los componentes community.

### 12.2 Arquitectura de la POC

```
┌──────────────────────────────────────────────────────────┐
│  OpenShift Local (CRC) — Single Node Cluster             │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Namespace: guidewire-infra                        │  │
│  │                                                    │  │
│  │  ┌──────────────┐  ┌──────────┐  ┌────────────┐   │  │
│  │  │ Kafka (KRaft) │  │ Apicurio │  │ PostgreSQL │   │  │
│  │  │ Strimzi Op.  │  │ Registry │  │            │   │  │
│  │  └──────┬───────┘  └──────────┘  └────────────┘   │  │
│  │         │                                          │  │
│  │  ┌──────┴───────┐  ┌──────────┐  ┌────────────┐   │  │
│  │  │ Kafdrop      │  │ ActiveMQ │  │ 3Scale     │   │  │
│  │  │ (Kafka UI)   │  │ Artemis  │  │ APIcast    │   │  │
│  │  └──────────────┘  └──────────┘  └──────┬─────┘   │  │
│  └─────────────────────────────────────────┼─────────┘  │
│                                            │             │
│  ┌─────────────────────────────────────────┼─────────┐  │
│  │  Namespace: guidewire-apps              │         │  │
│  │                                         ▼         │  │
│  │  ┌──────────────┐  ┌──────────┐  ┌────────────┐   │  │
│  │  │ Camel        │  │ Drools   │  │ Billing    │   │  │
│  │  │ Gateway      │  │ Engine   │  │ Service    │   │  │
│  │  └──────────────┘  └──────────┘  └────────────┘   │  │
│  │                                                    │  │
│  │  ┌──────────────┐  ┌──────────┐                    │  │
│  │  │ Incidents    │  │ Customers│                    │  │
│  │  │ Service      │  │ Service  │                    │  │
│  │  └──────────────┘  └──────────┘                    │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 12.3 Stack OpenShift/CRC

| Servicio | Imagen / Operator | RAM estimada |
|---|---|---|
| Kafka (KRaft, 1 broker) | `confluentinc/cp-kafka` | ~1.5GB |
| Apicurio Registry | `apicurio/apicurio-registry` | ~512MB |
| PostgreSQL | `postgres:16` | ~256MB |
| ActiveMQ Artemis | `apache/activemq-artemis` | ~512MB |
| Kong API Gateway | `kong:latest` | ~256MB |
| Drools (KIE Server) | `quay.io/kiegroup/kie-server` | ~1GB |
| Mock Guidewire (Spring Boot) | Custom | ~512MB |
| Microservicio Camel | Custom | ~512MB |
| Kafdrop (UI Kafka) | `obsidiandynamics/kafdrop` | ~256MB |
| **Total estimado** | | **~5.5GB** |

Con la BOSGAME M5 (96/128GB RAM), el stack completo ocupa menos del 6% de la memoria disponible.

### 12.4 Flujo End-to-End

1. **API First:** Se definen contratos OpenAPI (API de pólizas/siniestros) y AsyncAPI (eventos) + schemas AVRO. Se registran en Apicurio.
2. **Generación de código:** Los plugins Maven generan stubs REST y clases AVRO.
3. **Mock Guidewire:** Spring Boot expone Cloud APIs REST fake y publica eventos en Kafka al crear siniestros.
4. **Integration Gateway (Camel):** Ruta que consume la API REST del mock, transforma y publica en Kafka. Otra ruta que consume eventos y los enruta según tipo.
5. **Drools:** Evalúa reglas de detección de fraude sobre los siniestros recibidos.
6. **Kafka:** Transporta todos los eventos con schemas AVRO validados contra Apicurio.
7. **Consumer de notificaciones:** Microservicio que consume eventos procesados y simula envío de notificaciones.
8. **Kong:** Protege las APIs con API Key, aplica rate limiting y expone documentación.
9. **Postman:** Colección de tests que validan todo el flujo end-to-end.

---

## 13. Laboratorio Aislado — Red Hat OpenShift Local (CRC)

### 13.1 Enfoque

Entorno OpenShift real de un solo nodo en tu máquina local. CRC (CodeReady Containers) ejecuta un cluster OpenShift 4.x completo con todas las capacidades enterprise: Operators, Routes, BuildConfigs, Service Discovery.

### 13.2 Requisitos del Host

| Requisito | Mínimo | Recomendado |
|-----------|--------|-------------|
| RAM | 16 GB | 32 GB+ |
| CPU | 4 cores | 8 cores+ |
| Disco | 50 GB libres | 100 GB+ |
| SO | Linux (Fedora/RHEL/Ubuntu), macOS, Windows | Linux |
| Cuenta Red Hat | Sí (gratuita) | — |

### 13.3 Instalación

```bash
# 1. Descargar CRC desde https://console.redhat.com/openshift/create/local
# 2. Descomprimir y mover al PATH
tar xvf crc-linux-amd64.tar.xz
sudo mv crc-linux-*-amd64/crc /usr/local/bin/

# 3. Setup inicial (descarga ~4GB, configura hipervisor)
crc setup

# 4. Arrancar el cluster
crc start --cpus 8 --memory 20480 --disk-size 80

# 5. Configurar oc CLI
eval $(crc oc-env)
oc login -u developer -p developer https://api.crc.testing:6443
```

### 13.4 Arquitectura del Laboratorio

```
┌─────────────────────────────────────────────────────────┐
│  HOST (tu máquina)                                      │
│                                                         │
│  ┌─────────────────┐  ┌──────────────────────────────┐  │
│  │ IDE + Navegador  │  │ oc CLI                       │  │
│  │                  │  │ oc get pods -n guidewire-apps │  │
│  └─────────────────┘  └──────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CRC (OpenShift 4.x — single node)              │   │
│  │                                                  │   │
│  │  ┌─────────────────────────────────────┐         │   │
│  │  │  Namespace: guidewire-infra         │         │   │
│  │  │  PostgreSQL · Kafka (Strimzi)       │         │   │
│  │  │  ActiveMQ (AMQ Broker) · Apicurio   │         │   │
│  │  │  Kafdrop · 3Scale APIcast           │         │   │
│  │  └─────────────────────────────────────┘         │   │
│  │                                                  │   │
│  │  ┌─────────────────────────────────────┐         │   │
│  │  │  Namespace: guidewire-apps          │         │   │
│  │  │  Camel Gateway · Drools Engine      │         │   │
│  │  │  Billing · Incidents · Customers    │         │   │
│  │  └─────────────────────────────────────┘         │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 13.5 Despliegue

```bash
cd lab/openshift
./deploy-all.sh
```

El script despliega en orden:
1. Namespaces
2. Operators (Strimzi, AMQ Broker, Apicurio)
3. Infraestructura (PostgreSQL, Kafka, ActiveMQ, Apicurio, Kafdrop, APIcast)
4. Aplicaciones (BuildConfig + Deployment × 5 servicios)

### 13.6 Acceso a Servicios

```bash
# Ver todas las routes
oc get routes -n guidewire-infra
oc get routes -n guidewire-apps

# Acceder a la consola OpenShift
crc console
# URL: https://console-openshift-console.apps-crc.testing
# Credenciales: developer / developer (o kubeadmin)
```

### 13.7 Operaciones Día a Día

| Acción | Comando |
|--------|---------|
| Arrancar CRC | `crc start` |
| Detener CRC | `crc stop` |
| Consola web | `crc console` |
| Estado | `crc status` |
| Login | `eval $(crc oc-env) && oc login -u developer` |
| Ver pods infra | `oc get pods -n guidewire-infra` |
| Ver pods apps | `oc get pods -n guidewire-apps` |
| Logs de un pod | `oc logs -f deploy/<service> -n guidewire-apps` |
| Rebuild service | `oc start-build <service> --from-dir=components/<service>` |
| Eliminar todo | `oc delete project guidewire-infra guidewire-apps` |

### 13.8 Recursos Consumidos

| Recurso | CRC | Stack Guidewire | Descripción |
|---------|-----|-----------------|-------------|
| RAM | 20 GB | ~5.5 GB | Queda ~14.5 GB para workloads |
| CPU | 8 cores | ~4 cores | Queda ~4 cores libres |
| Disco | 80 GB | ~15 GB | Imágenes + PVCs |

### 13.9 Alternativa Legacy: Podman Compose

Para entornos sin CRC, se mantiene `lab/podman/podman-compose.yml` como alternativa funcional. Consultar `lab/podman/README.md`.
