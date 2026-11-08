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

import java.util.concurrent.Executor
import org.springaicommunity.typesafe.JevBatchOptions
import org.springaicommunity.typesafe.JevBatchResult
import org.springaicommunity.typesafe.JsonContent
import org.springaicommunity.typesafe.TypeSafeClient
import org.springaicommunity.typesafe.question.Question
import org.springaicommunity.typesafe.question.SystemOneRequest
import org.springaicommunity.typesafe.response.SystemOneResponse

/**
 * Builds a [SystemOneRequest] the Kotlin way; also the receiver of
 * [TypeSafeClient.systemOne] below, which is the usual entry point:
 *
 * ```kotlin
 * val response = client.systemOne {
 *     state = mapOf("message" to ticket)
 *     noul("is_urgent") { instructions = "Does this convey urgency?" }
 *     choice("department") {
 *         instructions = "Which team should handle this?"
 *         option("billing", "Payments, invoicing, refunds")
 *         option("technical", "Bugs, outages, integrations")
 *     }
 *     score("severity") {
 *         instructions = "How severe is it?"
 *         levels("Cosmetic", "Degraded", "Outage")
 *     }
 * }
 * ```
 *
 * Questions keep their declaration order, and the service reads that order as part of
 * the question.
 */
@TypeSafeDsl
class SystemOneRequestDsl internal constructor() {

	private val builder = SystemOneRequest.builder()

	/** The model to ask; the client's default applies when left unset. */
	var model: String by writeOnly { builder.model(it) }

	/**
	 * The content to evaluate: a [String], a [Map], a [List] or a
	 * [JsonContent] are all accepted and wrapped as-is.
	 */
	var state: Any by writeOnly { builder.state(JsonContent.of(it)) }

	/** Adds a question assembled elsewhere. */
	fun question(name: String, question: Question) {
		builder.question(name, question)
	}

	fun questions(questions: Map<String, Question>) {
		builder.questions(questions)
	}

	/** Adds a binary truth question under [name]. */
	fun noul(name: String, block: NoulDsl.() -> Unit) {
		builder.question(name, noul(block))
	}

	/** Adds a pick-one-label question under [name]. */
	fun choice(name: String, block: ChoiceDsl.() -> Unit) {
		builder.question(name, choice(block))
	}

	/** Adds an ordered-rubric question under [name]. */
	fun score(name: String, block: ScoreDsl.() -> Unit) {
		builder.question(name, score(block))
	}

	internal fun build(): SystemOneRequest = builder.build()
}

/**
 * Assembles a request without sending it, for example to feed [TypeSafeClient.systemOneAll].
 * @param block the configuration applied to a fresh [SystemOneRequestDsl]
 * @return the assembled request
 */
fun systemOneRequest(block: SystemOneRequestDsl.() -> Unit): SystemOneRequest =
	SystemOneRequestDsl().apply(block).build()

/**
 * Assembles and sends the request in one block, using the client's default model when
 * the block does not name one.
 * @param block the request configuration
 * @return the answers
 */
fun TypeSafeClient.systemOne(block: SystemOneRequestDsl.() -> Unit): SystemOneResponse =
	systemOne(systemOneRequest(block))

/**
 * Scope for [JevBatchOptions]; entered via [batchOptions].
 */
@TypeSafeDsl
class JevBatchOptionsDsl internal constructor() {

	var concurrency: Int = JevBatchOptions.DEFAULT_CONCURRENCY

	var failFast: Boolean = false

	var executor: Executor? = null

	internal fun build(): JevBatchOptions = JevBatchOptions(concurrency, failFast, executor)
}

/**
 * ```kotlin
 * client.systemOneAll(requests) {
 *     concurrency = 8
 *     failFast = true
 * }
 * ```
 * @param block the configuration applied to a fresh [JevBatchOptionsDsl]
 * @return the assembled options
 */
fun batchOptions(block: JevBatchOptionsDsl.() -> Unit): JevBatchOptions =
	JevBatchOptionsDsl().apply(block).build()

/** Runs [requests] concurrently with options declared inline. */
fun TypeSafeClient.systemOneAll(
	requests: List<SystemOneRequest>,
	block: JevBatchOptionsDsl.() -> Unit
): List<JevBatchResult<SystemOneResponse>> =
	systemOneAll(requests, batchOptions(block))

/** Runs the given requests concurrently with the default options. */
fun TypeSafeClient.systemOneAll(vararg requests: SystemOneRequest): List<JevBatchResult<SystemOneResponse>> =
	systemOneAll(requests.toList())