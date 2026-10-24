package com.example

import com.example.ui.screens.parseScreenshotText
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFreeFireBooyahOcrParsing() {
    val ocrText = """
      BOOYAH!
      MATCH SUMMARY
      RANK #1
      KILLS: 8
      DAMAGE: 1540
    """.trimIndent()
    val result = parseScreenshotText(ocrText)
    assertEquals(8, result.kills)
    assertEquals(1, result.rank)
    assertTrue(result.isBooyahOrWinner)
  }

  @Test
  fun testBgmiChickenDinnerOcrParsing() {
    val ocrText = """
      WINNER WINNER CHICKEN DINNER!
      Finishes: 11
      #1 / 100
      Survival Time: 28:40
    """.trimIndent()
    val result = parseScreenshotText(ocrText)
    assertEquals(11, result.kills)
    assertEquals(1, result.rank)
    assertTrue(result.isBooyahOrWinner)
  }

  @Test
  fun testRunnerUpScoreboardOcrParsing() {
    val ocrText = """
      MATCH COMPLETED
      Rank #3
      Eliminations - 5
    """.trimIndent()
    val result = parseScreenshotText(ocrText)
    assertEquals(5, result.kills)
    assertEquals(3, result.rank)
    assertFalse(result.isBooyahOrWinner)
  }
}

