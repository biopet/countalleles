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

import nl.biopet.utils.tool.{AbstractOptParser, ToolCommand}

import scala.io.Source

class ArgsParser(toolCommand: ToolCommand[Args])
    extends AbstractOptParser[Args](toolCommand) {
  opt[File]('I', "inputFile") required () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(inputFile = x)
  } text "input VCF filename"
  opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(outputFile = x)
  } text "output VCF filename"
  opt[File]('b', "bam") unbounded () action { (x, c) =>
    c.copy(bamFiles = x :: c.bamFiles)
  } text "bam file, from which the variants (VCF files) were called"
  opt[File]("bamList") unbounded () action { (x, c) =>
    c.copy(
      bamFiles = Source
        .fromFile(x)
        .getLines()
        .map(new File(_))
        .toList ::: c.bamFiles)
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
