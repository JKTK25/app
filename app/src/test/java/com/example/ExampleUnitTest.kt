package com.example

import com.example.engine.BlockEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testAdultUrlBlocked() {
    val result = BlockEngine.evaluate("https://www.pornhub.com/video/12345")
    assertTrue(result.isBlocked)
  }

  @Test
  fun testExplicitKeywordBlocked() {
    val result = BlockEngine.evaluate("free adult sex videos streaming")
    assertTrue(result.isBlocked)
  }

  @Test
  fun testSafeUrlAllowed() {
    val result = BlockEngine.evaluate("https://www.wikipedia.org/wiki/Science")
    assertFalse(result.isBlocked)
  }

  @Test
  fun testUserKeywordsBlocked() {
    assertTrue(BlockEngine.evaluate("kiss").isBlocked)
    assertTrue(BlockEngine.evaluate("three sum").isBlocked)
    assertTrue(BlockEngine.evaluate("party").isBlocked)
    assertTrue(BlockEngine.evaluate("shake ass").isBlocked)
    assertTrue(BlockEngine.evaluate("sugua").isBlocked)
    assertTrue(BlockEngine.evaluate("matako").isBlocked)
    assertTrue(BlockEngine.evaluate("toxic masculinity").isBlocked)
    assertTrue(BlockEngine.evaluate("juicy ass").isBlocked)
    assertTrue(BlockEngine.evaluate("penis").isBlocked)
    assertTrue(BlockEngine.evaluate("dick").isBlocked)
  }

  @Test
  fun testSafeSearchUrlTransform() {
    val googleUrl = "https://www.google.com/search?q=space+exploration"
    val safeUrl = BlockEngine.enforceSafeSearch(googleUrl)
    assertTrue(safeUrl.contains("safe=active"))
  }
}
