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
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springaicommunity.typesafe.question.QuestionType

internal class QuestionDslTests {

	@Test
	fun noulCarriesInstructionsAndCriteria() {
		val question = noul {
			instructions = "Does this convey urgency?"
			whenTrue = "Immediate outage or financial loss"
			whenFalse = "Can wait in the queue"
		}
		assertThat(question.type()).isEqualTo(QuestionType.NOUL)
		assertThat(question.typeName()).isEqualTo("noul")
		assertThat(question.instructions()?.asText()).isEqualTo("Does this convey urgency?")
		assertThat(question.criteria()?.whenTrue()?.asText()).isEqualTo("Immediate outage or financial loss")
		assertThat(question.criteria()?.whenFalse()?.asText()).isEqualTo("Can wait in the queue")
	}

	@Test
	fun noulRejectsEmptyTextImmediately() {
		assertThatExceptionOfType(IllegalArgumentException::class.java)
			.isThrownBy { noul { instructions = "  " } }
			.withMessageContaining("instructions must not be empty")
	}

	@Test
	fun choiceKeepsOptionDeclarationOrder() {
		val question = choice {
			instructions = "Which team should handle this?"
			option("billing", "Payments, invoicing, refunds")
			option("technical", "Bugs, outages, integrations")
			option("sales")
			options("legal" to "Contracts")
		}
		assertThat(question.typeName()).isEqualTo("choice")
		assertThat(question.criteria().keys).containsExactly("billing", "technical", "sales", "legal")
		assertThat(question.criteria()["billing"]?.asText()).isEqualTo("Payments, invoicing, refunds")
		assertThat(question.criteria()["sales"]?.isNull).isTrue()
	}

	@Test
	fun scoreAddsLevelsInOrder() {
		val question = score {
			instructions = "How severe is the incident?"
			level("Cosmetic")
			level("Degraded")
			levels("Outage", "Catastrophe")
		}
		assertThat(question.typeName()).isEqualTo("score")
		assertThat(question.criteria()).hasSize(4)
		assertThat(question.maxLevel()).isEqualTo(3)
		assertThat(question.levelAt(2)?.asText()).isEqualTo("Outage")
	}

	@Test
	fun aScoreWithOneLevelFailsFast() {
		assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
			score {
				instructions = "Only one level"
				level("Solo")
			}
		}
	}

	@Test
	fun jsonHelpersKeepKeyOrder() {
		val content = jsonOf("b" to 1, "a" to 2)
		assertThat(content.asMap().keys).containsExactly("b", "a")
		assertThat("text".asJsonContent().asText()).isEqualTo("text")
		assertThat((null as Any?).asJsonContent().isNull).isTrue()
	}
}
