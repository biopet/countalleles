package nl.biopet.tools.countalleles

import java.io.File

import htsjdk.samtools.SamReaderFactory
import htsjdk.variant.vcf.VCFFileReader
import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

class CountAllelesTest extends BiopetTest {

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
    val samRecord = SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val varRecord = new VCFFileReader(variant, false).iterator().next()
    CountAlleles.checkAlleles(samRecord, varRecord) shouldBe None
  }

  @Test
  def testCheckAlleles(): Unit = {
    val samRecord = SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val varRecord = new VCFFileReader(vcf2).iterator().next()
    CountAlleles.checkAlleles(samRecord, varRecord) shouldBe Some("T")
  }

}
