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

import kotlin.reflect.KProperty

/**
 * Property delegate for scopes that forward straight to a builder: assigning runs
 * [setter]; reading fails fast, naming the property.
 *
 * ```kotlin
 * var maxRetries: Int by writeOnly { builder.maxRetries(it) }
 * ```
 */
class WriteOnly<T> internal constructor(private val setter: (T) -> Unit) {

	operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
		throw UnsupportedOperationException("${property.name} is write-only")

	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		setter(value)
	}
}

/** Creates a [WriteOnly] delegate that forwards assignments to [setter]. */
internal fun <T> writeOnly(setter: (T) -> Unit): WriteOnly<T> = WriteOnly(setter)
