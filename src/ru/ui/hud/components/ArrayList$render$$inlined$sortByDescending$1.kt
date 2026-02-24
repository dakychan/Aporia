package ru.ui.hud.components

import java.util.Comparator
import kotlin.jvm.internal.SourceDebugExtension
import ru.module.Module

// $VF: Class flags could not be determined
@SourceDebugExtension(["SMAP\nComparisons.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Comparisons.kt\nkotlin/comparisons/ComparisonsKt__ComparisonsKt$compareByDescending$1\n+ 2 ArrayList.kt\nru/ui/hud/components/ArrayList\n*L\n1#1,121:1\n34#2:122\n*E\n"])
internal class `ArrayList$render$$inlined$sortByDescending$1`<T> : Comparator {
   fun `ArrayList$render$$inlined$sortByDescending$1`(var1: ArrayList) {
      this.this$0 = var1;
   }

   override final fun compare(a: T, b: T): Int {
      return ComparisonsKt.compareValues(
         ArrayList.access$getTextRenderer$p(this.this$0).measureWidth((b as Module).getName(), 16.0F),
         ArrayList.access$getTextRenderer$p(this.this$0).measureWidth((a as Module).getName(), 16.0F)
      );
   }
}
