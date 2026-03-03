import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

/**
 * MethodHandle + CallSite - Динамическая подмена методов
 * 
 * Генерирует bootstrap методы для invokedynamic инструкций.
 * Все вызовы методов идут через MutableCallSite который можно подменить в runtime.
 */
class MethodHandleGenerator(private val seed: Long) {
    
    /**
     * Генерация bootstrap метода для invokedynamic
     */
    fun generateBootstrapMethod(classNode: ClassNode): String {
        val bootstrapName = "bootstrap\$${seed.toString(36)}"
        
        val bootstrap = MethodNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            bootstrapName,
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            null,
            null
        )
        
        bootstrap.instructions.apply {
            add(TypeInsnNode(Opcodes.NEW, "java/lang/invoke/MutableCallSite"))
            add(InsnNode(Opcodes.DUP))
            add(VarInsnNode(Opcodes.ALOAD, 2))
            add(MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/invoke/MutableCallSite",
                "<init>",
                "(Ljava/lang/invoke/MethodType;)V",
                false
            ))
            
            add(VarInsnNode(Opcodes.ASTORE, 3))
            
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(VarInsnNode(Opcodes.ALOAD, 1))
            add(VarInsnNode(Opcodes.ALOAD, 2))
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                classNode.name,
                "resolve\$${seed.toString(36)}",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            ))
            
            add(VarInsnNode(Opcodes.ASTORE, 4))
            add(VarInsnNode(Opcodes.ALOAD, 3))
            add(VarInsnNode(Opcodes.ALOAD, 4))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MutableCallSite",
                "setTarget",
                "(Ljava/lang/invoke/MethodHandle;)V",
                false
            ))
            
            add(VarInsnNode(Opcodes.ALOAD, 3))
            add(InsnNode(Opcodes.ARETURN))
            
            add(LabelNode())
        }
        
        bootstrap.maxStack = 4
        bootstrap.maxLocals = 5
        
        classNode.methods.add(bootstrap)
        
        generateResolveMethod(classNode)
        
        return bootstrapName
    }
    
    /**
     * Генерация resolve метода для получения MethodHandle
     */
    private fun generateResolveMethod(classNode: ClassNode) {
        val resolveName = "resolve\$${seed.toString(36)}"
        
        val resolve = MethodNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            resolveName,
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
            null,
            null
        )
        
        resolve.instructions.apply {
            add(VarInsnNode(Opcodes.ALOAD, 2))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodType",
                "returnType",
                "()Ljava/lang/Class;",
                false
            ))
            
            add(LdcInsnNode(""))
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/invoke/MethodHandles",
                "constant",
                "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
                false
            ))
            
            add(InsnNode(Opcodes.ARETURN))
        }
        
        resolve.maxStack = 2
        resolve.maxLocals = 3
        
        classNode.methods.add(resolve)
    }
    
    /**
     * Добавление BootstrapMethods атрибута
     */
    fun addBootstrapMethodsAttribute(classNode: ClassNode, bootstrapName: String) {
        val handle = org.objectweb.asm.Handle(
            Opcodes.H_INVOKESTATIC,
            classNode.name,
            bootstrapName,
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        )
        
        classNode.methods?.forEach { method ->
            method.instructions?.forEach { insn ->
                if (insn is LdcInsnNode && insn.cst is String) {
                    val invokeDynamic = InvokeDynamicInsnNode(
                        "decrypt",
                        "()Ljava/lang/String;",
                        handle
                    )
                }
            }
        }
    }
}
