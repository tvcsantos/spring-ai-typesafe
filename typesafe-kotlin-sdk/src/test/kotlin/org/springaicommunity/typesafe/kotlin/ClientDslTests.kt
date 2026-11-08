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

import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springaicommunity.typesafe.RetryPolicy
import org.springaicommunity.typesafe.TypeSafeModels
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

internal class ClientDslTests {

	@Test
	fun clientIsAssembledFromTheDsl() {
		val client = typeSafeClient {
			apiKey = "test-api-key"
			baseUrl = "https://api.typesafe.ai"
			defaultModel = TypeSafeModels.JEV_1_13_0
			timeout = 3.seconds
			bufferContent = true
			header("X-Tenant", "acme")
			retryPolicy {
				maxRetries = 4
				initialBackoff = 200.milliseconds
				maxBackoff = 2.seconds
				jitter = 0.5
				retryableStatuses(408, 429, 503)
				respectRetryAfter = false
				retryConnectionErrors = false
				totalTimeout = 1.minutes
			}
		}

		assertThat(client.defaultModel()).isEqualTo("jev-1.13.0")
		assertThat(client.timeout()).isEqualTo(Duration.ofSeconds(3))

		val policy = client.retryPolicy()
		assertThat(policy.maxRetries()).isEqualTo(4)
		assertThat(policy.initialBackoff()).isEqualTo(Duration.ofMillis(200))
		assertThat(policy.maxBackoff()).isEqualTo(Duration.ofSeconds(2))
		assertThat(policy.jitter()).isEqualTo(0.5)
		assertThat(policy.retryableStatuses()).containsExactlyInAnyOrder(408, 429, 503)
		assertThat(policy.respectRetryAfter()).isFalse()
		assertThat(policy.retryConnectionErrors()).isFalse()
		assertThat(policy.totalTimeout()).isEqualTo(Duration.ofSeconds(60))
	}

	@Test
	fun unsetPropertiesKeepTheJavaDefaults() {
		val client = typeSafeClient { apiKey = "test-api-key" }
		assertThat(client.defaultModel()).isEqualTo(TypeSafeModels.JEV_LATEST)
		assertThat(client.retryPolicy()).isEqualTo(org.springaicommunity.typesafe.RetryPolicy.defaults())
	}

	@Test
	fun retryPolicyDslStandsAlone() {
		val policy = retryPolicy {
			maxRetries = 0
		}
		assertThat(policy.maxRetries()).isZero()
		assertThat(policy.isRetryable(RuntimeException("boom"))).isFalse()
	}

	@Test
	fun untouchedPropertiesKeepTheJavaBuilderDefaults() {
		// The scope forwards only what was assigned, so an empty block must build
		// exactly what the plain builder builds.
		assertThat(retryPolicy { }).isEqualTo(RetryPolicy.defaults())
	}

	@Test
	fun totalTimeoutCanBeLifted() {
		val policy = retryPolicy { totalTimeout = null }
		assertThat(policy.totalTimeout()).isNull()
	}

	@Test
	fun apiHeadersAccrossRepeatedCallsAllArrive() {
		val httpBuilder = RestClient.builder()
		val server = MockRestServiceServer.bindTo(httpBuilder).build()
		server.expect(requestTo("$BASE_URL/v1/models"))
			.andExpect(header("X-Trace", "on"))
			.andExpect(header("X-Tenant", "acme"))
			.andExpect(header("Authorization", "Bearer test-api-key"))
			.andRespond(withSuccess("""{"models": []}""", MediaType.APPLICATION_JSON))
		val api = typeSafeApi {
			baseUrl = BASE_URL
			apiKey = "test-api-key"
			header("X-Trace", "on")
			header("X-Tenant", "acme")
			restClientBuilder = httpBuilder
		}
		api.listModels()
		server.verify()
	}

	@Test
	fun writeOnlyPropertiesFailFastOnRead() {
		assertThatExceptionOfType(UnsupportedOperationException::class.java)
			.isThrownBy { retryPolicy { maxRetries > 0 } }
			.withMessage("maxRetries is write-only")
		assertThatExceptionOfType(UnsupportedOperationException::class.java)
			.isThrownBy { typeSafeApi { apiKey.isEmpty() } }
			.withMessage("apiKey is write-only")
	}

	companion object {

		private const val BASE_URL = "https://api.typesafe.ai"
	}
}
