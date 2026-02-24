package ru.ui.hud

import java.util.Comparator
import kotlin.jvm.internal.SourceDebugExtension

// $VF: Class flags could not be determined
@SourceDebugExtension(["SMAP\nComparisons.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Comparisons.kt\nkotlin/comparisons/ComparisonsKt__ComparisonsKt$compareBy$2\n+ 2 HudManager.kt\nru/ui/hud/HudManager\n*L\n1#1,102:1\n126#2:103\n*E\n"])
internal class `HudManager$render$$inlined$sortedBy$1`<T> : Comparator {
   override final fun compare(a: T, b: T): Int {
      return ComparisonsKt.compareValues((a as HudManager.HudComponent).getZIndex(), (b as HudManager.HudComponent).getZIndex());
   }
}
