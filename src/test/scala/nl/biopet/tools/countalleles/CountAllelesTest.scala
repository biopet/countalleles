package nl.biopet.tools.countalleles

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

class CountAllelesTest extends BiopetTest {
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      CountAlleles.main(Array())
    }
  }
}
