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
import org.springaicommunity.typesafe.response.ChoiceAnswer
import org.springaicommunity.typesafe.response.NoulAnswer
import org.springaicommunity.typesafe.response.ScoreAnswer
import org.springaicommunity.typesafe.response.SystemOneResponse

/**
 * Asks whether the named noul answer cleared [threshold], without going through the raw
 * score: `if (response.noulTrue("is_urgent", 0.8)) escalate()`.
 */
fun SystemOneResponse.noulTrue(name: String, threshold: Double = 0.5): Boolean = noul(name).isTrue(threshold)

/** The complement of [noulTrue]. */
fun SystemOneResponse.noulFalse(name: String, threshold: Double = 0.5): Boolean = !noul(name).isTrue(threshold)

// Java records carry no componentN() functions, so Kotlin destructuring does not work on
// them out of the box. These make the answer shapes destructurable:
// val (department, confidence) = response.choice("department")

operator fun NoulAnswer.component1(): Double = value()

operator fun ChoiceAnswer.component1(): String = value()

operator fun ChoiceAnswer.component2(): Map<String, Double> = probabilities()

operator fun ChoiceAnswer.component3(): Double = confidence()

operator fun ScoreAnswer.component1(): Double = value()

operator fun ScoreAnswer.component2(): Map<Int, JsonContent> = legend()

operator fun ScoreAnswer.component3(): Map<Int, Double> = probabilities()

operator fun ScoreAnswer.component4(): Double = confidence()

/**
 * Ordered [Pair] of a choice answer's label and its probability, which is usually what a
 * threshold check actually wants:
 * `response.choiceWithProbability("department")` over `choiceValue` plus a separate
 * `probabilityOf` call.
 */
fun SystemOneResponse.choiceWithProbability(name: String): Pair<String, Double> {
	val answer = choice(name)
	return answer.value() to answer.probabilityOf(answer.value())
}
