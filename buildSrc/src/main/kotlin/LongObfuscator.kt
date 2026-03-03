import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import kotlin.random.Random

/**
 * Long-арифметика - Обфускация чисел через long операции
 * 
 * Превращает простые int константы в сложные long вычисления.
 * Как в j0 - 42 операции для каждого числа.
 */
class LongObfuscator(private val seed: Long) {
    
    private val random = Random(seed)
    
    /**
     * Превращает int в long сдвиги и XOR операции
     */
    fun obfuscateInt(value: Int): Long {
        var result = seed
        repeat(42) {
            result = result xor (value.toLong() shl (it % 56))
            result = result.rotateRight(13)
            result = result xor (result shl 7)
        }
        return result
    }
    
    /**
     * Генерация long-массива с обфусцированными значениями
     */
    fun generateLongArray(values: List<Long>): List<Long> {
        return values.map { it xor seed }
    }
    
    /**
     * Замена int константы на long вычисление
     */
    fun replaceIntConstant(method: MethodNode, intInsn: AbstractInsnNode, value: Int) {
        val obfuscated = obfuscateInt(value)
        val insnList = InsnList()
        
        insnList.add(LdcInsnNode(obfuscated))
        
        repeat(42) { i ->
            insnList.add(LdcInsnNode(seed))
            insnList.add(InsnNode(Opcodes.LXOR))
            
            insnList.add(InsnNode(Opcodes.DUP2))
            insnList.add(LdcInsnNode(13L))
            insnList.add(InsnNode(Opcodes.LUSHR))
            insnList.add(InsnNode(Opcodes.LXOR))
            
            if (i % 7 == 0) {
                insnList.add(InsnNode(Opcodes.DUP2))
                insnList.add(LdcInsnNode(7L))
                insnList.add(InsnNode(Opcodes.LSHL))
                insnList.add(InsnNode(Opcodes.LXOR))
            }
        }
        
        insnList.add(InsnNode(Opcodes.L2I))
        
        method.instructions.insert(intInsn, insnList)
        method.instructions.remove(intInsn)
    }
    
    /**
     * Обфускация всех int констант в методе
     */
    fun obfuscateMethodConstants(method: MethodNode) {
        val toReplace = mutableListOf<Pair<AbstractInsnNode, Int>>()
        
        method.instructions.forEach { insn ->
            when (insn.opcode) {
                Opcodes.ICONST_M1 -> toReplace.add(insn to -1)
                Opcodes.ICONST_0 -> toReplace.add(insn to 0)
                Opcodes.ICONST_1 -> toReplace.add(insn to 1)
                Opcodes.ICONST_2 -> toReplace.add(insn to 2)
                Opcodes.ICONST_3 -> toReplace.add(insn to 3)
                Opcodes.ICONST_4 -> toReplace.add(insn to 4)
                Opcodes.ICONST_5 -> toReplace.add(insn to 5)
            }
            
            if (insn is IntInsnNode && (insn.opcode == Opcodes.BIPUSH || insn.opcode == Opcodes.SIPUSH)) {
                toReplace.add(insn to insn.operand)
            }
            
            if (insn is LdcInsnNode && insn.cst is Int) {
                toReplace.add(insn to (insn.cst as Int))
            }
        }
        
        toReplace.forEach { (insn, value) ->
            if (random.nextFloat() < 0.7f) {
                replaceIntConstant(method, insn, value)
            }
        }
    }
    
    /**
     * Генерация статического long массива в классе
     */
    fun generateStaticLongArray(classNode: ClassNode, values: List<Long>): String {
        val fieldName = "LONGS\${seed.toString(36)}"
        
        val field = FieldNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC,
            fieldName,
            "[J",
            null,
            null
        )
        classNode.fields.add(field)
        
        var clinit = classNode.methods?.find { it.name == "<clinit>" }
        if (clinit == null) {
            clinit = MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
            clinit.instructions.add(InsnNode(Opcodes.RETURN))
            classNode.methods.add(clinit)
        }
        
        val returnInsn = clinit.instructions.last { it.opcode == Opcodes.RETURN }
        val insnList = InsnList()
        
        insnList.add(LdcInsnNode(values.size))
        insnList.add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_LONG))
        
        values.forEachIndexed { index, value ->
            insnList.add(InsnNode(Opcodes.DUP))
            insnList.add(LdcInsnNode(index))
            insnList.add(LdcInsnNode(value xor seed))
            insnList.add(InsnNode(Opcodes.LASTORE))
        }
        
        insnList.add(FieldInsnNode(
            Opcodes.PUTSTATIC,
            classNode.name,
            fieldName,
            "[J"
        ))
        
        clinit.instructions.insertBefore(returnInsn, insnList)
        
        return fieldName
    }
    
    private fun Long.rotateRight(bits: Int): Long {
        return (this ushr bits) or (this shl (64 - bits))
    }
}
