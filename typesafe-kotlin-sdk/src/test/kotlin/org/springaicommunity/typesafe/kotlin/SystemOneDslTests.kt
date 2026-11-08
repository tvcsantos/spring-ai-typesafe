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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springaicommunity.typesafe.TypeSafeClient
import org.springaicommunity.typesafe.TypeSafeModels
import org.springaicommunity.typesafe.exception.TypeSafeApiException
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

internal class SystemOneDslTests {

	private val restClientBuilder: RestClient.Builder = RestClient.builder()

	private val server: MockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
		.bufferContent()
		.build()

	private fun client(): TypeSafeClient = typeSafeClient {
		apiKey = "test-api-key"
		baseUrl = BASE_URL
		defaultModel = TypeSafeModels.JEV_LATEST
		restClientBuilder = this@SystemOneDslTests.restClientBuilder
	}

	@Test
	fun systemOneDslRoundTripsStateAndQuestions() {
		server.expect(requestTo("$BASE_URL/v1/systemone"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(
				content().json(
					"""
					{
					  "model": "jev-latest",
					  "state": { "message": "Help! My payouts have been failing for 3 days." },
					  "questions": {
					    "is_urgent": { "type": "noul", "instructions": "Does this convey urgency?" },
					    "department": {
					      "type": "choice",
					      "instructions": "Which team should handle this?",
					      "criteria": {
					        "billing": "Payments, invoicing, refunds",
					        "technical": "Bugs, outages, integrations"
					      }
					    },
					    "severity": {
					      "type": "score",
					      "instructions": "How severe is the incident?",
					      "criteria": ["Cosmetic", "Degraded", "Outage"]
					    }
					  }
					}
					"""
				)
			)
			.andRespond(
				withSuccess(ANSWER_BODY, MediaType.APPLICATION_JSON)
					.header(TypeSafeApiException.REQUEST_ID_HEADER, "req_0123456789")
			)

		val response = client().systemOne {
			state = mapOf("message" to "Help! My payouts have been failing for 3 days.")
			noul("is_urgent") { instructions = "Does this convey urgency?" }
			choice("department") {
				instructions = "Which team should handle this?"
				options(
					"billing" to "Payments, invoicing, refunds",
					"technical" to "Bugs, outages, integrations"
				)
			}
			score("severity") {
				instructions = "How severe is the incident?"
				levels("Cosmetic", "Degraded", "Outage")
			}
		}

		assertThat(response.model()).isEqualTo("jev-1.13.0")
		assertThat(response.requestId()).isEqualTo("req_0123456789")
		assertThat(response.noulValue("is_urgent")).isEqualTo(0.92)
		assertThat(response.noulTrue("is_urgent", 0.8)).isTrue()
		assertThat(response.noulFalse("is_urgent", 0.95)).isTrue()

		// Kotlin destructuring via the component operators in ResponseExtensions.kt.
		val (department, probabilities, confidence) = response.choice("department")
		assertThat(department).isEqualTo("technical")
		assertThat(probabilities).containsEntry("billing", 0.08)
		assertThat(confidence).isEqualTo(0.82)
		assertThat(response.choiceWithProbability("department")).isEqualTo("technical" to 0.85)

		val (severity) = response.score("severity")
		assertThat(severity).isEqualTo(1.6)
		assertThat(response.score("severity").nearestLabel()).isEqualTo("Very angry")

		server.verify()
	}

	@Test
	fun standaloneRequestDslFeedsTheBatchApi() {
		val requests = listOf(
			systemOneRequest {
				model = "jev-latest"
				state = "passage one"
				noul("relevant") { instructions = "Does this answer the question?" }
			},
			systemOneRequest {
				state = jsonOf("text" to "passage two")
				question("relevant", org.springaicommunity.typesafe.question.Noul.of("Does this answer the question?"))
			}
		)
		assertThat(requests).hasSize(2)
		assertThat(requests[1].model()).isNull()
		assertThat(requests[0].questions().keys).containsExactly("relevant")

		val options = batchOptions {
			concurrency = 2
			failFast = true
		}
		assertThat(options.concurrency()).isEqualTo(2)
		assertThat(options.failFast()).isTrue()
	}

	companion object {

		private const val BASE_URL = "https://api.typesafe.ai"

		private const val ANSWER_BODY = """
			{
			  "model": "jev-1.13.0",
			  "answers": {
			    "is_urgent": { "type": "noul", "noul": 0.92 },
			    "department": {
			      "type": "choice",
			      "choice": "technical",
			      "probabilities": { "billing": 0.08, "technical": 0.85 },
			      "confidence": 0.82
			    },
			    "severity": {
			      "type": "score",
			      "score": 1.6,
			      "legend": { "0": "Cosmetic", "1": "Degraded", "2": "Very angry" },
			      "probabilities": { "0": 0.05, "1": 0.3, "2": 0.65 },
			      "confidence": 0.78
			    }
			  },
			  "usage": { "input_tokens": 312, "output_tokens": 48 }
			}"""
	}
}
