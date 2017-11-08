package nl.biopet.tools.countalleles

import htsjdk.samtools.{SAMReadGroupRecord, SAMRecord, SamReader}
import htsjdk.variant.variantcontext.writer.{
  AsyncVariantContextWriter,
  VariantContextWriterBuilder
}
import htsjdk.variant.variantcontext._
import htsjdk.variant.vcf._
import nl.biopet.utils.ngs.{bam, fasta}
import nl.biopet.utils.tool.ToolCommand

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._

object CountAlleles extends ToolCommand[Args] {
  def emptyArgs: Args = Args()
  def argsParser = new ArgsParser(toolName)
  def main(args: Array[String]): Unit = {
    val cmdArgs = cmdArrayToArgs(args)

    logger.info("Start")

    val dict = fasta.getCachedDict(cmdArgs.referenceFasta)

    logger.info(cmdArgs.bamFiles.size + " bamfiles found")

    val bamReaders = bam.sampleBamReaderMap(cmdArgs.bamFiles)
    val sampleReadergroup = bam.sampleReadGroups(bamReaders)

    logger.info(s"Samples found: ${sampleReadergroup.size}")
    logger.info(s"Readgroups found: ${sampleReadergroup.map(_._2.size).sum}")

    val reader = new VCFFileReader(cmdArgs.inputFile, false)
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setReferenceDictionary(dict)
        .setOutputFile(cmdArgs.outputFile)
        .build)

    val headerLines: Set[VCFHeaderLine] = Set(
      new VCFFormatHeaderLine("GT",
                              VCFHeaderLineCount.R,
                              VCFHeaderLineType.String,
                              "Genotype of position"),
      new VCFFormatHeaderLine(
        "AD",
        VCFHeaderLineCount.R,
        VCFHeaderLineType.Integer,
        "Allele depth, ref and alt on order of vcf file"),
      new VCFFormatHeaderLine("DP",
                              1,
                              VCFHeaderLineType.Integer,
                              "Depth of position")
    )
    val sampleNames =
      (if (cmdArgs.outputReadgroups)
         sampleReadergroup
           .flatMap(x =>
             x._1 :: x._2.map(rg => rg.getSample + "-" + rg.getReadGroupId))
           .toList
       else sampleReadergroup.keys.toList).sorted

    val header = new VCFHeader(headerLines, sampleNames)
    header.setSequenceDictionary(dict)
    writer.writeHeader(header)

    val it = for (vcfRecord <- reader.iterator().buffered) yield {
      val countReports = bamReaders.map(
        x =>
          x._1 -> Future(
            countAlleles(vcfRecord, x._2._1, sampleReadergroup(x._1))))

      val builder = new VariantContextBuilder()
        .chr(vcfRecord.getContig)
        .start(vcfRecord.getStart)
        .alleles(vcfRecord.getAlleles)
        .computeEndFromAlleles(vcfRecord.getAlleles, vcfRecord.getStart)
      val genotypes = for ((sampleName, countsFuture) <- countReports) yield {
        val counts = Await.result(countsFuture, Duration.Inf)
        val sampleGenotype =
          counts.values
            .fold(AlleleCounts())(_ + _)
            .toGenotype(sampleName, vcfRecord)
        if (cmdArgs.outputReadgroups)
          sampleGenotype :: counts
            .map(
              x =>
                x._2.toGenotype(x._1.getSample + "-" + x._1.getReadGroupId,
                                vcfRecord))
            .toList
        else List(sampleGenotype)
      }
      builder.genotypes(genotypes.flatten).make
    }
    var c = 0L
    it.buffered.foreach { record =>
      c += 1
      if (c % 1000 == 0) logger.info(s"$c variants done")
      writer.add(record)
    }
    logger.info(s"$c variants done")
    bamReaders.foreach(_._2._1.close())
    reader.close()
    writer.close()

    logger.info("Done")
  }

  protected case class AlleleCounts(count: Map[Allele, Int] = Map(),
                                    dp: Int = 0) {
    def +(other: AlleleCounts): AlleleCounts = {
      val alleles = this.count.keySet ++ other.count.keySet
      val map = alleles.map(a =>
        a -> (this.count.getOrElse(a, 0) + other.count.getOrElse(a, 0)))
      AlleleCounts(map.toMap, other.dp + this.dp)
    }

    def toGenotype(sampleName: String, vcfRecord: VariantContext): Genotype = {
      val alleles =
        if (count.forall(_._2 == 0)) List(Allele.NO_CALL, Allele.NO_CALL)
        else {
          val f = count.toList.map(x => (x._2.toDouble / dp) -> x._1)
          val maxF = f.map(_._1).max
          val firstAllele = f.find(_._1 == maxF).get._2
          if (maxF > 0.8) List(firstAllele, firstAllele)
          else {
            val leftOver = f.filter(_._2 != firstAllele)
            val maxF2 = (0.0 :: leftOver.map(_._1)).max
            val secondAllele =
              leftOver.find(_._1 == maxF2).map(_._2).getOrElse(Allele.NO_CALL)
            List(firstAllele, secondAllele)
          }
        }
      new GenotypeBuilder(sampleName)
        .alleles(alleles.sortBy(a => vcfRecord.getAlleleIndex(a)))
        .DP(dp)
        .AD(vcfRecord.getAlleles.map(count.getOrElse(_, 0)).toArray)
        .make()
    }
  }

  def countAlleles(vcfRecord: VariantContext,
                   samReader: SamReader,
                   readGroups: List[SAMReadGroupRecord])
    : Map[SAMReadGroupRecord, AlleleCounts] = {
    val map = samReader
      .query(vcfRecord.getContig, vcfRecord.getStart, vcfRecord.getEnd, false)
      .toList
      .groupBy(_.getReadGroup)
      .map {
        case (readGroup, rs) =>
          val count = rs
            .flatMap(checkAlleles(_, vcfRecord))
            .groupBy(x => x)
            .map(x => vcfRecord.getAllele(x._1) -> x._2.size)
          readGroup -> AlleleCounts(count, rs.size)
      }
    readGroups.map(rg => rg -> map.getOrElse(rg, AlleleCounts())).toMap
  }

  def checkAlleles(samRecord: SAMRecord,
                   vcfRecord: VariantContext): Option[String] = {
    val readStartPos = List
      .range(0, samRecord.getReadBases.length)
      .find(x =>
        samRecord
          .getReferencePositionAtReadPosition(x + 1) == vcfRecord.getStart) getOrElse {
      return None
    }
    val readBases = samRecord.getReadBases
    val alleles = vcfRecord.getAlleles.map(x => x.getBaseString)
    val refAllele = alleles.head
    var maxSize = 1
    for (allele <- alleles if allele.length > maxSize) maxSize = allele.length
    val readC = for (t <- readStartPos until readStartPos + maxSize
                     if t < readBases.length)
      yield readBases(t).toChar
    val allelesInRead =
      mutable.Set(alleles.filter(readC.mkString.startsWith): _*)

    // Removal of insertions that are not really in the cigarstring
    for (allele <- allelesInRead if allele.length > refAllele.length) {
      val refPos = for (t <- refAllele.length until allele.length)
        yield
          samRecord.getReferencePositionAtReadPosition(readStartPos + t + 1)
      if (refPos.exists(_ > 0)) allelesInRead -= allele
    }

    // Removal of alleles that are not really in the cigarstring
    for (allele <- allelesInRead) {
      val readPosAfterAllele =
        samRecord.getReferencePositionAtReadPosition(
          readStartPos + allele.length + 1)
      val vcfPosAfterAllele = vcfRecord.getStart + refAllele.length
      if (readPosAfterAllele != vcfPosAfterAllele &&
          (refAllele.length != allele.length || (refAllele.length == allele.length && readPosAfterAllele < 0)))
        allelesInRead -= allele
    }

    for (allele <- allelesInRead if allele.length >= refAllele.length) {
      if (allelesInRead.exists(_.length > allele.length))
        allelesInRead -= allele
    }
    if (allelesInRead.contains(refAllele) && allelesInRead.exists(
          _.length < refAllele.length))
      allelesInRead -= refAllele
    if (allelesInRead.isEmpty) None
    else if (allelesInRead.size == 1) Some(allelesInRead.head)
    else {
      logger.warn("vcfRecord: " + vcfRecord)
      logger.warn("samRecord: " + samRecord.getSAMString)
      logger.warn("Found multiple options: " + allelesInRead.toString)
      logger.warn(
        "ReadStartPos: " + readStartPos + "  Read Length: " + samRecord.getReadLength)
      logger.warn("Read skipped, please report this")
      None
    }
  }

}
