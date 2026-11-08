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

import org.springaicommunity.typesafe.JsonContent
import org.springaicommunity.typesafe.question.Choice
import org.springaicommunity.typesafe.question.Noul
import org.springaicommunity.typesafe.question.Score

/**
 * Builds a [Noul] - the binary truth question:
 *
 * ```kotlin
 * val urgent = noul {
 *     instructions = "Does this convey urgency?"
 *     whenTrue = "Immediate outage or financial loss"
 *     whenFalse = "Can wait in the queue"
 * }
 * ```
 *
 * Properties forward as they are set and are write-only; the last write to a property
 * or its structured counterpart is the one that counts.
 */
@TypeSafeDsl
class NoulDsl internal constructor() {

	private val builder = Noul.builder()

	/** The question, as plain text. */
	var instructions: String by writeOnly { builder.instructions(it) }

	fun instructions(instructions: JsonContent) {
		builder.instructions(instructions)
	}

	fun instructions(instructions: Map<String, *>) {
		builder.instructions(instructions)
	}

	fun instructions(instructions: List<*>) {
		builder.instructions(instructions)
	}

	/** What counts as true, as plain text. */
	var whenTrue: String by writeOnly { builder.whenTrue(it) }

	fun whenTrue(description: JsonContent) {
		builder.whenTrue(description)
	}

	fun whenTrue(description: Map<String, *>) {
		builder.whenTrue(description)
	}

	/** What counts as false, as plain text. */
	var whenFalse: String by writeOnly { builder.whenFalse(it) }

	fun whenFalse(description: JsonContent) {
		builder.whenFalse(description)
	}

	fun whenFalse(description: Map<String, *>) {
		builder.whenFalse(description)
	}

	internal fun build(): Noul = builder.build()
}

/**
 * @param block the configuration applied to a fresh [NoulDsl]
 * @return the assembled question
 */
fun noul(block: NoulDsl.() -> Unit): Noul = NoulDsl().apply(block).build()

/**
 * Builds a [Choice] - the pick-one-label question. Options keep their declaration order,
 * which the service reads as part of the question:
 *
 * ```kotlin
 * val department = choice {
 *     instructions = "Which team should handle this?"
 *     option("billing", "Payments, invoicing, refunds")
 *     option("technical", "Bugs, outages, integrations")
 *     option("sales")
 * }
 * ```
 */
@TypeSafeDsl
class ChoiceDsl internal constructor() {

	private val builder = Choice.builder()

	var instructions: String by writeOnly { builder.instructions(it) }

	fun instructions(instructions: JsonContent) {
		builder.instructions(instructions)
	}

	fun instructions(instructions: Map<String, *>) {
		builder.instructions(instructions)
	}

	fun instructions(instructions: List<*>) {
		builder.instructions(instructions)
	}

	/** An option carrying no description. */
	fun option(label: String) {
		builder.option(label)
	}

	fun option(label: String, description: String) {
		builder.option(label, description)
	}

	fun option(label: String, description: JsonContent) {
		builder.option(label, description)
	}

	/** Adds options from pairs, in pair order: `options("billing" to "Payments", "sales" to "Deals")`. */
	fun options(vararg options: Pair<String, String>) {
		options.forEach { (label, description) -> builder.option(label, description) }
	}

	fun options(options: Map<String, *>) {
		builder.options(options)
	}

	internal fun build(): Choice = builder.build()
}

/**
 * @param block the configuration applied to a fresh [ChoiceDsl]
 * @return the assembled question
 */
fun choice(block: ChoiceDsl.() -> Unit): Choice = ChoiceDsl().apply(block).build()

/**
 * Builds a [Score] - the ordered-rubric question. Levels are added lowest first and a
 * rubric needs at least two:
 *
 * ```kotlin
 * val severity = score {
 *     instructions = "How severe is the incident?"
 *     level("Cosmetic")
 *     level("Degraded")
 *     level("Full outage")
 * }
 * ```
 */
@TypeSafeDsl
class ScoreDsl internal constructor() {

	private val builder = Score.builder()

	var instructions: String by writeOnly { builder.instructions(it) }

	fun instructions(instructions: JsonContent) {
		builder.instructions(instructions)
	}

	fun instructions(instructions: Map<String, *>) {
		builder.instructions(instructions)
	}

	fun instructions(instructions: List<*>) {
		builder.instructions(instructions)
	}

	fun level(description: String) {
		builder.level(description)
	}

	fun level(description: JsonContent) {
		builder.level(description)
	}

	fun level(description: Map<String, *>) {
		builder.level(description)
	}

	fun level(description: List<*>) {
		builder.level(description)
	}

	/** Adds several levels in order: `level("Calm", "Frustrated", "Furious")`. */
	fun levels(vararg descriptions: String) {
		descriptions.forEach { builder.level(it) }
	}

	internal fun build(): Score = builder.build()
}

/**
 * @param block the configuration applied to a fresh [ScoreDsl]
 * @return the assembled question
 */
fun score(block: ScoreDsl.() -> Unit): Score = ScoreDsl().apply(block).build()
