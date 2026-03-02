package aporia.su.test;

import anidumpproject.api.annotation.Obfuscate;

/**
 * Калькулятор для тестирования LIGHT обфускации
 * Только имена классов меняются, методы остаются читаемыми
 */
@Obfuscate(level = Obfuscate.Level.LIGHT)
public class CalculatorLight {
    
    private double result;
    private String lastOperation;
    
    public CalculatorLight() {
        this.result = 0.0;
        this.lastOperation = "none";
    }
    
    public double add(double a, double b) {
        result = a + b;
        lastOperation = "addition";
        return result;
    }
    
    public double subtract(double a, double b) {
        result = a - b;
        lastOperation = "subtraction";
        return result;
    }
    
    public double multiply(double a, double b) {
        result = a * b;
        lastOperation = "multiplication";
        return result;
    }
    
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        result = a / b;
        lastOperation = "division";
        return result;
    }
    
    public double power(double base, double exponent) {
        result = Math.pow(base, exponent);
        lastOperation = "power";
        return result;
    }
    
    public double sqrt(double value) {
        if (value < 0) {
            throw new ArithmeticException("Square root of negative number");
        }
        result = Math.sqrt(value);
        lastOperation = "sqrt";
        return result;
    }
    
    public double getResult() {
        return result;
    }
    
    public String getLastOperation() {
        return lastOperation;
    }
    
    public void reset() {
        result = 0.0;
        lastOperation = "none";
    }
}
