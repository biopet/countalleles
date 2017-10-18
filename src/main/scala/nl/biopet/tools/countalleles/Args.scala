package nl.biopet.tools.countalleles

import java.io.File

case class Args(inputFile: File = null,
                outputFile: File = null,
                bamFiles: List[File] = Nil,
                minMapQual: Int = 1,
                referenceFasta: File = null,
                outputReadgroups: Boolean = false)
