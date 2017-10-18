# Manual

Count alleles can take into account the mapping quality and the read 
groups and outputs the alleles in VCF file format.

Example:
```bash
java -jar countalleles-version.jar \
--inputFile variants.vcf \
--outputFile output.vcf \
--bam original.bam
--min_mapping_quality 85
--referenceFasta reference.fa
--outputReadgroups
```