/**
 * Copyright 2024 Karl Kegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.driftool.reporting.DistanceRelation
import io.driftool.reporting.MatrixResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RelationConversationTests {

    val relationCompleteWithIdentity = mutableSetOf(
        Triple("a", "a", 0.0f),
        Triple("a", "b", 1.0f),
        Triple("a", "c", 2.0f),

        Triple("b", "b", 0f),
        Triple("b", "a", 3.0f),
        Triple("b", "c", 4.0f),

        Triple("c", "c", 0f),
        Triple("c", "a", 5.0f),
        Triple("c", "b", 6.0f)
    )

    @Test
    fun testFromDistanceRelationCompleteWithIdentityEnsureNoSymmetry() {
        val matrixResult = MatrixResult.fromDistanceRelation(
            DistanceRelation(relationCompleteWithIdentity),
            listOf("a", "b", "c"), isComplete = true, ensureSymmetry = false, zeroIdentities = false)
        assertEquals(listOf(
            listOf(0.0f, 1.0f, 2.0f),
            listOf(3.0f, 0.0f, 4.0f),
            listOf(5.0f, 6.0f, 0.0f)
        ), matrixResult.data)
    }

    @Test
    fun testFromDistanceRelationCompleteWithIdentityEnsureSymmetry() {
        val matrixResult = MatrixResult.fromDistanceRelation(
            DistanceRelation(relationCompleteWithIdentity),
            listOf("a", "b", "c"), isComplete = true, ensureSymmetry = true, zeroIdentities = false)
        assertEquals(listOf(
            listOf(0.0f, (1.0f+3.0f)/2f, (2.0f+5.0f)/2f),
            listOf((3.0f+1.0f)/2f, 0.0f, (4.0f+6.0f)/2f),
            listOf((5.0f+2-0f)/2f, (6.0f+4.0f)/2f, 0.0f)
        ), matrixResult.data)
    }

    val relationCompleteWithIdentityMissingValue = mutableSetOf(
        Triple("a", "a", 0.0f),
        Triple("a", "b", 1.0f),
        Triple("a", "c", 2.0f),

        Triple("b", "b", 0f),
        Triple("b", "c", 4.0f),

        Triple("c", "c", 0f),
        Triple("c", "a", 5.0f),
        Triple("c", "b", 6.0f)
    )

    @Test
    fun testFromDistanceRelationCompleteWithIdentityEnsureNoSymmetryMissingValue() {
        try {
            val matrixResult = MatrixResult.fromDistanceRelation(
                DistanceRelation(relationCompleteWithIdentityMissingValue),
                listOf("a", "b", "c"), isComplete = true, ensureSymmetry = true, zeroIdentities = false)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Complete matrix is missing value for b -> a", e.message)
        }
    }

    val relationCompleteWithIdentityMissingValue2 = mutableSetOf(
        Triple("a", "a", 0.0f),
        Triple("a", "b", 1.0f),
        Triple("a", "c", 2.0f),

        Triple("b", "a", 1.0f),
        Triple("b", "b", 0f),
        Triple("b", "c", 4.0f),

        Triple("c", "c", 0f),
        Triple("c", "b", 6.0f)
    )

    @Test
    fun testFromDistanceRelationCompleteWithIdentityEnsureNoSymmetryMissingValue2() {
        try {
            val matrixResult = MatrixResult.fromDistanceRelation(
                DistanceRelation(relationCompleteWithIdentityMissingValue2),
                listOf("a", "b", "c"), isComplete = true, ensureSymmetry = false, zeroIdentities = false)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Complete matrix is missing value for c -> a", e.message)
        }
    }

    val relationCompleteWithoutIdentity = mutableSetOf(
        Triple("a", "b", 1.0f),
        Triple("a", "c", 2.0f),

        Triple("b", "a", 3.0f),
        Triple("b", "c", 4.0f),

        Triple("c", "a", 5.0f),
        Triple("c", "b", 6.0f)
    )

    @Test
    fun testFromDistanceRelationCompleteWithoutIdentityEnsureNoSymmetry() {
        val matrixResult = MatrixResult.fromDistanceRelation(
            DistanceRelation(relationCompleteWithoutIdentity),
            listOf("a", "b", "c"), isComplete = true, ensureSymmetry = false, zeroIdentities = true)
        assertEquals(listOf(
            listOf(0.0f, 1.0f, 2.0f),
            listOf(3.0f, 0.0f, 4.0f),
            listOf(5.0f, 6.0f, 0.0f)
        ), matrixResult.data)
    }

    @Test
    fun testFromDistanceRelationCompleteWithoutIdentityEnsureNoSymmetryMissingValue() {
        try {
            val matrixResult = MatrixResult.fromDistanceRelation(
                DistanceRelation(relationCompleteWithoutIdentity),
                listOf("a", "b", "c"), isComplete = true, ensureSymmetry = false, zeroIdentities = false)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Complete matrix is missing value for a -> a", e.message)
        }
    }

    val relationNotCompleteWithoutIdentity = mutableSetOf(
        Triple("a", "b", 1.0f),
        Triple("a", "c", 2.0f),
        Triple("b", "c", 4.0f),
    )

    @Test
    fun testFromDistanceRelationNotCompleteWithoutIdentityEnsureNoSymmetry() {
        val matrixResult = MatrixResult.fromDistanceRelation(
            DistanceRelation(relationNotCompleteWithoutIdentity),
            listOf("a", "b", "c"), isComplete = false, ensureSymmetry = false, zeroIdentities = true)
        assertEquals(listOf(
            listOf(0.0f, 1.0f, 2.0f),
            listOf(0.0f, 0.0f, 4.0f),
            listOf(0.0f, 0.0f, 0.0f)
        ), matrixResult.data)
    }

    @Test
    fun testFromDistanceRelationNotCompleteWithoutIdentityEnsureSymmetry() {
        val matrixResult = MatrixResult.fromDistanceRelation(
            DistanceRelation(relationNotCompleteWithoutIdentity),
            listOf("a", "b", "c"), isComplete = false, ensureSymmetry = true, zeroIdentities = true)
        assertEquals(listOf(
            listOf(0.0f, 1.0f, 2.0f),
            listOf(1.0f, 0.0f, 4.0f),
            listOf(2.0f, 4.0f, 0.0f)
        ), matrixResult.data)
    }

    val relationNotCompleteWithoutIdentityInvalid = mutableSetOf(
        Triple("a", "b", 1.0f),
        Triple("a", "c", 2.0f),
        Triple("b", "c", 4.0f),
        Triple("c", "b", 4.0f)
    )

    @Test
    fun testFromDistanceRelationNotCompleteWithoutIdentityEnsureSymmetryInvalid() {
        try {
            val matrixResult = MatrixResult.fromDistanceRelation(
                DistanceRelation(relationNotCompleteWithoutIdentityInvalid),
                listOf("a", "b", "c"), isComplete = false, ensureSymmetry = true, zeroIdentities = true
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException){
            assertEquals("Incomplete matrix cannot have both directions!", e.message)
        }
    }

    val relationNotCompleteWithoutIdentityMissingValue = mutableSetOf(
        Triple("a", "b", 1.0f),
        Triple("b", "c", 4.0f),
    )

    @Test
    fun testFromDistanceRelationNotCompleteWithoutIdentityEnsureSymmetryMissingValue() {
        val matrixResult = MatrixResult.fromDistanceRelation(
            DistanceRelation(relationNotCompleteWithoutIdentityMissingValue),
            listOf("a", "b", "c"), isComplete = false, ensureSymmetry = true, zeroIdentities = true
        )
        assertEquals(listOf(
            listOf(0.0f, 1.0f, 0.0f),
            listOf(1.0f, 0.0f, 4.0f),
            listOf(0.0f, 4.0f, 0.0f)
        ), matrixResult.data)
    }

}