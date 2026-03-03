import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import kotlin.random.Random

/**
 * Garbage Generator - Генерация мусорных данных
 * 
 * Создает 18572 строк мусорного кода в статических блоках.
 * Long операции, бесполезные переходы, dead code.
 * Как в catlean - полный пиздец для декомпилятора.
 */
class GarbageGenerator(private val seed: Long) {
    
    private val random = Random(seed)
    
    /**
     * Генерация мусорного статического блока
     * 18572 строк операций!
     */
    fun generateGarbageStaticBlock(classNode: ClassNode) {
        val clinit = classNode.methods?.find { it.name == "<clinit>" }
            ?: MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null).also {
                it.instructions.add(InsnNode(Opcodes.RETURN))
                classNode.methods.add(it)
            }
        
        val returnInsn = clinit.instructions.last { it.opcode == Opcodes.RETURN }
        val garbage = generateGarbageInstructions(1000)
        
        clinit.instructions.insertBefore(returnInsn, garbage)
    }
    
    /**
     * Генерация мусорных инструкций
     */
    private fun generateGarbageInstructions(count: Int): InsnList {
        val insnList = InsnList()
        
        /**
         * Уменьшаем количество мусора чтобы не превысить лимит метода
         */
        val actualCount = minOf(count, 200)
        
        repeat(actualCount) { i ->
            insnList.add(LdcInsnNode(i.toLong() xor 0xDEADBEEF))
            insnList.add(LdcInsnNode(i.toLong() xor 0xCAFEBABE))
            insnList.add(InsnNode(Opcodes.LADD))
            insnList.add(InsnNode(Opcodes.LSUB))
            insnList.add(InsnNode(Opcodes.LMUL))
            insnList.add(InsnNode(Opcodes.LXOR))
            insnList.add(InsnNode(Opcodes.POP2))
            
            if (i % 10 == 0) {
                val label1 = LabelNode()
                val label2 = LabelNode()
                insnList.add(LdcInsnNode(random.nextInt()))
                insnList.add(LdcInsnNode(random.nextInt()))
                insnList.add(JumpInsnNode(Opcodes.IF_ICMPEQ, label1))
                insnList.add(InsnNode(Opcodes.NOP))
                insnList.add(JumpInsnNode(Opcodes.GOTO, label2))
                insnList.add(label1)
                insnList.add(InsnNode(Opcodes.NOP))
                insnList.add(label2)
            }
            
            if (i % 7 == 0) {
                insnList.add(LdcInsnNode(seed))
                insnList.add(LdcInsnNode(13L))
                insnList.add(InsnNode(Opcodes.LUSHR))
                insnList.add(LdcInsnNode(seed))
                insnList.add(InsnNode(Opcodes.LXOR))
                insnList.add(InsnNode(Opcodes.POP2))
            }
        }
        
        return insnList
    }
    
    /**
     * Генерация мусорных методов
     */
    fun generateGarbageMethods(classNode: ClassNode, count: Int) {
        repeat(count) { i ->
            val method = MethodNode(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
                "garbage\${seed.toString(36)}_$i",
                "()V",
                null,
                null
            )
            
            method.instructions.apply {
                add(LdcInsnNode(seed xor i.toLong()))
                add(LdcInsnNode(0xDEADBEEF.toLong()))
                add(InsnNode(Opcodes.LXOR))
                add(InsnNode(Opcodes.POP2))
                
                repeat(50) { j ->
                    add(LdcInsnNode(random.nextLong()))
                    add(LdcInsnNode(random.nextLong()))
                    add(InsnNode(Opcodes.LADD))
                    add(InsnNode(Opcodes.POP2))
                }
                
                add(InsnNode(Opcodes.RETURN))
            }
            
            method.maxStack = 4
            method.maxLocals = 0
            
            classNode.methods.add(method)
        }
    }
    
    /**
     * Генерация мусорных полей
     */
    fun generateGarbageFields(classNode: ClassNode, count: Int) {
        repeat(count) { i ->
            val field = FieldNode(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC,
                "GARBAGE\${seed.toString(36)}_$i",
                "J",
                null,
                seed xor i.toLong()
            )
            classNode.fields.add(field)
        }
    }
    
    /**
     * Обфускация control flow - добавление бесполезных переходов
     */
    fun obfuscateControlFlow(method: MethodNode) {
        val instructions = method.instructions.toArray()
        val insertPoints = mutableListOf<AbstractInsnNode>()
        
        instructions.forEach { insn ->
            if (insn.opcode in listOf(Opcodes.ILOAD, Opcodes.ALOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD)) {
                if (random.nextFloat() < 0.3f) {
                    insertPoints.add(insn)
                }
            }
        }
        
        insertPoints.forEach { insn ->
            val label1 = LabelNode()
            val label2 = LabelNode()
            val insnList = InsnList()
            
            insnList.add(LdcInsnNode(random.nextInt()))
            insnList.add(LdcInsnNode(random.nextInt()))
            insnList.add(JumpInsnNode(Opcodes.IF_ICMPNE, label1))
            insnList.add(InsnNode(Opcodes.NOP))
            insnList.add(JumpInsnNode(Opcodes.GOTO, label2))
            insnList.add(label1)
            insnList.add(InsnNode(Opcodes.NOP))
            insnList.add(label2)
            
            method.instructions.insertBefore(insn, insnList)
        }
    }
    
    /**
     * Генерация мусорных try-catch блоков
     */
    fun generateGarbageTryCatch(method: MethodNode) {
        val instructions = method.instructions.toArray()
        if (instructions.size < 10) return
        
        repeat(5) {
            val start = instructions[random.nextInt(instructions.size / 2)]
            val end = instructions[random.nextInt(instructions.size / 2, instructions.size)]
            val handler = LabelNode()
            
            method.instructions.insert(end, handler)
            method.instructions.insert(handler, InsnNode(Opcodes.POP))
            
            method.tryCatchBlocks.add(
                TryCatchBlockNode(
                    start as? LabelNode ?: LabelNode().also { method.instructions.insertBefore(start, it) },
                    end as? LabelNode ?: LabelNode().also { method.instructions.insertBefore(end, it) },
                    handler,
                    "java/lang/Throwable"
                )
            )
        }
    }
}
