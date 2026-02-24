package ru.files

import java.io.File
import java.util.Comparator
import kotlin.jvm.internal.SourceDebugExtension

// $VF: Class flags could not be determined
@SourceDebugExtension(["SMAP\nComparisons.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Comparisons.kt\nkotlin/comparisons/ComparisonsKt__ComparisonsKt$compareByDescending$1\n+ 2 FilesManager.kt\nru/files/FilesManager\n*L\n1#1,121:1\n503#2:122\n*E\n"])
internal class `FilesManager$clearTemp$$inlined$sortedByDescending$1`<T> : Comparator {
   override final fun compare(a: T, b: T): Int {
      return ComparisonsKt.compareValues((b as File).length(), (a as File).length());
   }
}
