package ru.ui.hud.components

import java.util.Comparator
import kotlin.jvm.internal.SourceDebugExtension

// $VF: Class flags could not be determined
@SourceDebugExtension(["SMAP\nComparisons.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Comparisons.kt\nkotlin/comparisons/ComparisonsKt__ComparisonsKt$compareBy$2\n+ 2 KeyBinds.kt\nru/ui/hud/components/KeyBinds\n*L\n1#1,102:1\n41#2:103\n*E\n"])
internal class `KeyBinds$render$$inlined$sortBy$1`<T> : Comparator {
   override final fun compare(a: T, b: T): Int {
      return ComparisonsKt.compareValues((a as Pair).getFirst() as java.lang.String, (b as Pair).getFirst() as java.lang.String);
   }
}
