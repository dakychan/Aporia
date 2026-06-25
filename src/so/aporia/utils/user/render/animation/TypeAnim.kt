/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.animation

import java.util.Random

/**
 * TypeAnim — анимация "взлома сейфа" для текста.
 *
 * Каждый символ целевой строки "подбирается" по одному:
 *  - пока символ не найден, на его месте мелькают случайные символы из алфавита
 *  - как только символ "угадан" — он фиксируется
 *  - лишние символы старой строки медленно стираются справа
 *
 * Использование:
 * ```kotlin
 * val anim = TypeAnim(60, 120) // msPerChar, shuffleMs
 * anim.setTarget("Агрессивный")
 * // каждый кадр:
 * val display = anim.update()
 * renderer.drawText(font, display, x, y, size, color)
 * ```
 */
class TypeAnim(private val msPerChar: Long, private val shuffleMs: Long) {

    private var current = ""
    private var target = ""

    private var startMs = 0L
    private var running = false

    private val rng = Random()

    fun setTarget(newTarget: String) {
        if (newTarget == target && running) return
        current = target
        target = newTarget
        startMs = System.currentTimeMillis()
        running = true
    }

    fun snap(value: String) {
        target = value
        current = value
        running = false
    }

    fun isRunning(): Boolean = running

    fun update(): String {
        if (!running) return target
        val elapsed = System.currentTimeMillis() - startMs
        val fixed = (elapsed / msPerChar).toInt()
        if (fixed >= target.length) {
            running = false
            current = target
            return target
        }
        val sb = StringBuilder()
        for (i in 0 until fixed) {
            sb.append(target[i])
        }
        val charElapsed = elapsed - fixed.toLong() * msPerChar
        val shuffleT = 1f.coerceAtMost(charElapsed.toFloat() / shuffleMs)
        val shuffleInterval = 16L.coerceAtLeast((80 * (1f - shuffleT * 0.7f)).toLong())
        val glitch = ALPHABET[(System.currentTimeMillis() / shuffleInterval % ALPHABET.size).toInt()]
        sb.append(glitch)
        val oldLen = current.length
        val newLen = target.length
        val erased = (elapsed / (msPerChar * 0.6)).toInt()
        val oldTail = 0.coerceAtLeast(oldLen - fixed - 1 - erased)
        val oldStart = fixed + 1
        for (i in 0 until oldTail) {
            if (oldStart + i < oldLen) {
                sb.append(current[oldStart + i])
            }
        }
        return sb.toString()
    }

    fun getTarget(): String = target

    companion object {
        private val ALPHABET: CharArray

        init {
            val src = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
                "0123456789!@#$%&*?<>€£¥¢§©®™°±×÷≠≤≥∞≈∑√∫∆∂∏←↑→↓↔↕↵↖↗↘↙" +
                "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзиклмнопрстуфхцчшщъыьэюя"
            ALPHABET = src.toCharArray()
        }
    }
}
