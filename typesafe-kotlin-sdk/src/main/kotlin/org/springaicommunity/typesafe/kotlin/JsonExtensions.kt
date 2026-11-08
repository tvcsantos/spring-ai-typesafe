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

/**
 * Builds an ordered JSON object from pairs, mirroring `JsonContent.object(..)` in a way
 * Kotlin reads naturally: `jsonOf("role" to "user", "text" to message)`.
 */
fun jsonOf(vararg pairs: Pair<String, *>): JsonContent = JsonContent.of(mapOf(*pairs))

/** Wraps this value as [JsonContent]; `null` becomes the explicit JSON `null`. */
fun Any?.asJsonContent(): JsonContent = JsonContent.of(this)
