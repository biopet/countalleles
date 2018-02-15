/*
 * Copyright (c) 2016 Biopet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.biopet.tools.countalleles

import java.io.File

import htsjdk.samtools.SamReaderFactory
import htsjdk.variant.vcf.VCFFileReader
import nl.biopet.utils.test.tools.ToolTest
import org.testng.annotations.Test

class CountAllelesTest extends ToolTest[Args] {
  def toolCommand: CountAlleles.type = CountAlleles

  val vcf: String = resourcePath("/chrQ.vcf")
  val bam: String = resourcePath("/single01.bam")
  val vcf2 = new File(resourcePath("/chrQ2.vcf.gz"))

  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      CountAlleles.main(Array())
    }
  }

  @Test
  def testCheckAllelesNone(): Unit = {
    val variant = new File(vcf)
    val samRecord =
      SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val varRecord = new VCFFileReader(variant, false).iterator().next()
    CountAlleles.checkAlleles(samRecord, varRecord) shouldBe None
  }

  @Test
  def testCheckAlleles(): Unit = {
    val samRecord =
      SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val varRecord = new VCFFileReader(vcf2).iterator().next()
    CountAlleles.checkAlleles(samRecord, varRecord) shouldBe Some("T")
  }

}
