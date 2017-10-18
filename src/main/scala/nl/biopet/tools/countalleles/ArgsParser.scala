package nl.biopet.tools.countalleles

import java.io.File

import nl.biopet.utils.tool.AbstractOptParser

import scala.io.Source

class ArgsParser(cmdName: String) extends AbstractOptParser[Args](cmdName) {
  opt[File]('I', "inputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
    c.copy(inputFile = x)
  } text "VCF file"
  opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
    c.copy(outputFile = x)
  } text "output VCF file name"
  opt[File]('b', "bam") unbounded () action { (x, c) =>
    c.copy(bamFiles = x :: c.bamFiles)
  } text "bam file, from which the variants (VCF files) were called"
  opt[File]("bamList") unbounded () action { (x, c) =>
    c.copy(bamFiles = Source.fromFile(x).getLines().map(new File(_)).toList ::: c.bamFiles)
  } text "bam file, from which the variants (VCF files) were called"
  opt[Int]('m', "min_mapping_quality") maxOccurs 1 action { (x, c) =>
    c.copy(minMapQual = x)
  } text "minimum mapping quality score for a read to be taken into account"
  opt[File]('R', "referenceFasta") required () maxOccurs 1 action { (x, c) =>
    c.copy(referenceFasta = x)
  } text "reference fasta"
  opt[Unit]("outputReadgroups") maxOccurs 1 action { (_, c) =>
    c.copy(outputReadgroups = true)
  } text "Output each readgroup separated"
}
