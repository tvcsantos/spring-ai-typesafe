# Kotlin DSL

`typesafe-kotlin-sdk` layers idiomatic Kotlin over `typesafe-java-sdk`: type-safe
builder DSLs for the client, the transport layer and the question model, plus a few
extensions on the response types. It adds nothing the Java API cannot do - it is the
same client, assembled and called the way Kotlin code prefers.

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>typesafe-kotlin-sdk</artifactId>
    <version>0.5.0-SNAPSHOT</version>
</dependency>
```

The Java SDK arrives transitively; Kotlin 2.2+ and Java 17+ are required.

## Building the client

`typeSafeClient { }` wraps `TypeSafeClient.builder()`. Unset values keep the Java
builder's defaults, including the `TYPESAFE_*` environment fallbacks:

```kotlin
import org.springaicommunity.typesafe.kotlin.typeSafeClient
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

val client = typeSafeClient {
    apiKey = System.getenv("TYPESAFE_API_KEY")
    defaultModel = TypeSafeModels.JEV_LATEST
    timeout = 8.seconds
    header("X-Tenant", "acme")
    retryPolicy {
        maxRetries = 4
        initialBackoff = 200.milliseconds
        totalTimeout = 30.seconds
    }
}
```

`retryPolicy { }` also stands alone (`val policy = retryPolicy { ... }`), and so does
the transport layer for tests and custom wiring:

```kotlin
val api = typeSafeApi {
    baseUrl = "https://api.typesafe.ai"
    apiKey = System.getenv("TYPESAFE_API_KEY")
    restClientBuilder = myRestClientBuilder   // e.g. bound to a MockRestServiceServer
}
```

## Asking questions

`client.systemOne { }` assembles the request and sends it in one block. Questions keep
their declaration order - unlike `mapOf(...)` with `Map.of` semantics, nothing is ever
reordered on the way to the wire:

```kotlin
import org.springaicommunity.typesafe.kotlin.systemOne

val response = client.systemOne {
    state = mapOf("message" to ticket)

    noul("is_urgent") {
        instructions = "Does this convey urgency?"
    }

    choice("department") {
        instructions = "Which team should handle this?"
        options(
            "billing" to "Payments, invoicing, refunds",
            "technical" to "Bugs, outages, integrations",
        )
    }

    score("severity") {
        instructions = "How severe is the incident?"
        levels("Cosmetic", "Degraded", "Outage")
    }
}
```

The same block shape works without sending, via `systemOneRequest { }`, which is what
`systemOneAll` batches take:

```kotlin
import org.springaicommunity.typesafe.kotlin.systemOneAll

val requests = passages.map { passage ->
    systemOneRequest {
        state = passage
        noul("relevant") { instructions = "Does this answer the question?" }
    }
}

val results = client.systemOneAll(requests) {
    concurrency = 8
    failFast = true
}
```

## Reading answers

The Java accessors (`response.noulValue(..)`, `choiceValue(..)`, `scores()`, ...) work
unchanged. Kotlin adds:

```kotlin
if (response.noulTrue("is_urgent", 0.8)) {
    // Java records are not destructurable from Kotlin by default; the SDK registers
    // component operators so the answer shapes are anyway:
    val (department, _, confidence) = response.choice("department")
}

// label and its own probability in one step:
val (label, probability) = response.choiceWithProbability("department")
```

For states, `jsonOf("role" to "user", "text" to message)` builds an ordered
`JsonContent` object from pairs, and any value can be wrapped with
`value.asJsonContent()`.

## Notes

- Every DSL is marked with a `@DslMarker`, so a nested `choice { }` cannot silently
  reach the request scope's members.
- Durations are `kotlin.time.Duration` (`8.seconds`, `200.milliseconds`) and are
  converted to `java.time.Duration` at the boundary.
- The DSLs delegate to the Java builders, so validation and failure modes are identical.
