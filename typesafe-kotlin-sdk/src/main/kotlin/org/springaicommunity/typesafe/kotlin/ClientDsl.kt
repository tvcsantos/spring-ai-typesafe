/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.typesafe.kotlin

import kotlin.time.Duration
import kotlin.time.toJavaDuration
import org.springaicommunity.typesafe.RetryPolicy
import org.springaicommunity.typesafe.TypeSafeClient
import org.springaicommunity.typesafe.api.TypeSafeApi
import org.springframework.http.HttpHeaders
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestClient

/**
 * Builds a [RetryPolicy] the Kotlin way:
 *
 * ```kotlin
 * val policy = retryPolicy {
 *     maxRetries = 4
 *     initialBackoff = 200.milliseconds
 *     maxBackoff = 2.seconds
 *     jitter = 0.5
 *     totalTimeout = 30.seconds
 * }
 * ```
 *
 * Every property forwards to the policy builder as it is set and is write-only;
 * whatever is left unset keeps its default.
 */
@TypeSafeDsl
class RetryPolicyDsl internal constructor() {

	private val builder = RetryPolicy.builder()

	/** Number of retries after the first attempt. Defaults to 2. */
	var maxRetries: Int by writeOnly { builder.maxRetries(it) }

	/** Backoff before the first retry. Defaults to 500ms. */
	var initialBackoff: Duration by writeOnly { builder.initialBackoff(it.toJavaDuration()) }

	/** Upper bound for the backoff. Defaults to 5s. */
	var maxBackoff: Duration by writeOnly { builder.maxBackoff(it.toJavaDuration()) }

	/** Random factor applied to the backoff, 0..1. Defaults to 0.25. */
	var jitter: Double by writeOnly { builder.jitter(it) }

	/** HTTP statuses worth retrying. Defaults to 408 and 429. */
	var retryableStatuses: Set<Int> by writeOnly { builder.retryableStatuses(it) }

	/** Adds options from varargs: `retryableStatuses(408, 429, 503)`. */
	fun retryableStatuses(vararg statuses: Int) {
		builder.retryableStatuses(statuses.toSet())
	}

	/** Whether a server's `Retry-After` overrides the computed backoff. Defaults to true. */
	var respectRetryAfter: Boolean by writeOnly { builder.respectRetryAfter(it) }

	/** Whether connection failures are retried. Defaults to true. */
	var retryConnectionErrors: Boolean by writeOnly { builder.retryConnectionErrors(it) }

	/** The overall budget across all attempts; set to `null` to lift it. Defaults to 30s. */
	var totalTimeout: Duration? by writeOnly { builder.totalTimeout(it?.toJavaDuration()) }

	internal fun build(): RetryPolicy = builder.build()
}

fun retryPolicy(block: RetryPolicyDsl.() -> Unit): RetryPolicy = RetryPolicyDsl().apply(block).build()

/**
 * Type-safe builder scope for [TypeSafeClient]; entered via [typeSafeClient].
 *
 * Every property forwards to the client builder as it is set and is write-only -
 * read values back from the built client. Whatever is left unset keeps its documented
 * default, several of them through environment variables.
 */
@TypeSafeDsl
class TypeSafeClientDsl internal constructor() {

	private val builder = TypeSafeClient.builder()

	/**
	 * The bearer token used to authenticate. Defaults to the `TYPESAFE_API_KEY` environment
	 * variable.
	 */
	var apiKey: String by writeOnly { builder.apiKey(it) }

	/** Supplies the bearer token per request, so a rotated key takes effect without rebuilding. */
	fun apiKey(apiKey: () -> String) {
		builder.apiKey { apiKey() }
	}

	/** Base URL of the API. Defaults to `TYPESAFE_BASE_URL`, then to `https://api.typesafe.ai`. */
	var baseUrl: String by writeOnly { builder.baseUrl(it) }

	/**
	 * Model used when a request does not name one. Defaults to `TYPESAFE_DEFAULT_MODEL`,
	 * then to `jev-latest`.
	 */
	var defaultModel: String by writeOnly { builder.defaultModel(it) }

	/**
	 * The HTTP timeout for a single attempt. Defaults to 10 seconds when the client builds
	 * its own transport; leave it unset when a custom [restClientBuilder] or [typeSafeApi]
	 * owns the timeouts.
	 */
	var timeout: Duration by writeOnly { builder.timeout(it.toJavaDuration()) }

	/** The retry policy. Defaults to [RetryPolicy.defaults]. */
	var retryPolicy: RetryPolicy by writeOnly { builder.retryPolicy(it) }

	/** Configures the retry policy inline. */
	fun retryPolicy(block: RetryPolicyDsl.() -> Unit) {
		builder.retryPolicy(RetryPolicyDsl().apply(block).build())
	}

	/** Path of the system-one endpoint. Defaults to `/v1/systemone`. */
	var systemOnePath: String by writeOnly { builder.systemOnePath(it) }

	/** Path of the models endpoint. Defaults to `/v1/models`. */
	var modelsPath: String by writeOnly { builder.modelsPath(it) }

	/** Whether streamed content is buffered before it is parsed. Defaults to `false`. */
	var bufferContent: Boolean by writeOnly { builder.bufferContent(it) }

	/**
	 * Derives the HTTP transport from this builder; timeouts, interceptors and
	 * observability stay under your control. Also the hook a test uses to bind a
	 * `MockRestServiceServer`.
	 */
	var restClientBuilder: RestClient.Builder by writeOnly { builder.restClientBuilder(it) }

	/** Supply a pre-built transport layer, bypassing every other transport setting. */
	var typeSafeApi: TypeSafeApi by writeOnly { builder.typeSafeApi(it) }

	/** Adds one header sent on every request. Repeatable. */
	fun header(name: String, value: String) {
		builder.headers(HttpHeaders().apply { add(name, value) })
	}

	/** Adds headers configured on a raw [HttpHeaders] instance. */
	fun headers(block: HttpHeaders.() -> Unit) {
		builder.headers(HttpHeaders().apply(block))
	}

	internal fun build(): TypeSafeClient = builder.build()
}

/**
 * Builds the SDK entry point:
 *
 * ```kotlin
 * val client = typeSafeClient {
 *     apiKey = System.getenv("TYPESAFE_API_KEY")
 *     defaultModel = TypeSafeModels.JEV_LATEST
 *     timeout = 8.seconds
 *     retryPolicy {
 *         maxRetries = 4
 *         totalTimeout = 30.seconds
 *     }
 * }
 * ```
 * @param block the configuration applied to a fresh [TypeSafeClientDsl]
 * @return the assembled client
 */
fun typeSafeClient(block: TypeSafeClientDsl.() -> Unit): TypeSafeClient = TypeSafeClientDsl().apply(block).build()

/**
 * Type-safe builder scope for [TypeSafeApi]; entered via [typeSafeApi]. The transport
 * layer itself rarely needs assembling outside tests and custom wiring - the client DSL
 * covers the usual cases - but when it does, this keeps it declarative.
 *
 * Properties forward as they are set and are write-only. [apiKey] must be set;
 * everything else has a documented default.
 */
@TypeSafeDsl
class TypeSafeApiDsl internal constructor() {

	private val builder = TypeSafeApi.builder()

	private val headers = HttpHeaders()

	/** Base URL of the API. Defaults to `https://api.typesafe.ai`. */
	var baseUrl: String by writeOnly { builder.baseUrl(it) }

	/** The bearer token used to authenticate. Required. */
	var apiKey: String by writeOnly { builder.apiKey(it) }

	/** Supplies the bearer token per request, so a rotated key takes effect without rebuilding. */
	fun apiKey(apiKey: () -> String) {
		builder.apiKey { apiKey() }
	}

	/** Path of the system-one endpoint. Defaults to `/v1/systemone`. */
	var systemOnePath: String by writeOnly { builder.systemOnePath(it) }

	/** Path of the models endpoint. Defaults to `/v1/models`. */
	var modelsPath: String by writeOnly { builder.modelsPath(it) }

	/**
	 * Derives the HTTP transport from this builder; timeouts, interceptors and
	 * observability stay under your control.
	 */
	var restClientBuilder: RestClient.Builder by writeOnly { builder.restClientBuilder(it) }

	/** Decides which responses are errors and how they are reported. */
	var responseErrorHandler: ResponseErrorHandler by writeOnly { builder.responseErrorHandler(it) }

	/** Adds one header sent on every request. Repeatable. */
	fun header(name: String, value: String) {
		headers.add(name, value)
		builder.headers(headers)
	}

	/** Adds headers configured on a raw [HttpHeaders] instance. */
	fun headers(block: HttpHeaders.() -> Unit) {
		headers.apply(block)
		builder.headers(headers)
	}

	internal fun build(): TypeSafeApi = builder.build()
}

/**
 * Builds the transport layer:
 *
 * ```kotlin
 * val api = typeSafeApi {
 *     baseUrl = "https://api.typesafe.ai"
 *     apiKey = System.getenv("TYPESAFE_API_KEY")
 * }
 * ```
 * @param block the configuration applied to a fresh [TypeSafeApiDsl]
 * @return the assembled API instance
 */
fun typeSafeApi(block: TypeSafeApiDsl.() -> Unit): TypeSafeApi = TypeSafeApiDsl().apply(block).build()
