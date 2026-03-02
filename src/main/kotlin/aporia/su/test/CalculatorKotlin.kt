package aporia.su.test

import anidumpproject.api.annotation.Obfuscate

/**
 * Калькулятор на Kotlin для тестирования обфускации
 * Проверяем что обфускатор работает с Kotlin классами
 */
@Obfuscate(level = Obfuscate.Level.EXTREME)
class CalculatorKotlin {
    
    var result: Double = 0.0
        private set
    
    var lastOperation: String = "none"
        private set
    
    fun add(a: Double, b: Double): Double {
        result = a + b
        lastOperation = "addition"
        return result
    }
    
    fun subtract(a: Double, b: Double): Double {
        result = a - b
        lastOperation = "subtraction"
        return result
    }
    
    fun multiply(a: Double, b: Double): Double {
        result = a * b
        lastOperation = "multiplication"
        return result
    }
    
    fun divide(a: Double, b: Double): Double {
        require(b != 0.0) { "Division by zero" }
        result = a / b
        lastOperation = "division"
        return result
    }
    
    fun power(base: Double, exponent: Double): Double {
        result = Math.pow(base, exponent)
        lastOperation = "power"
        return result
    }
    
    fun sqrt(value: Double): Double {
        require(value >= 0) { "Square root of negative number" }
        result = Math.sqrt(value)
        lastOperation = "sqrt"
        return result
    }
    
    fun reset() {
        result = 0.0
        lastOperation = "none"
    }
    
    // Kotlin специфичные фичи
    operator fun plus(other: CalculatorKotlin): Double {
        return this.result + other.result
    }
    
    operator fun minus(other: CalculatorKotlin): Double {
        return this.result - other.result
    }
    
    companion object {
        const val VERSION = "1.0.0"
        
        fun create(): CalculatorKotlin {
            return CalculatorKotlin()
        }
    }
}
