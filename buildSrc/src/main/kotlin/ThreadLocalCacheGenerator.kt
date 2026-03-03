import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

/**
 * ThreadLocal Cache - Кэширование Cipher объектов
 * 
 * Создает ThreadLocal кэш для Cipher объектов чтобы избежать
 * повторной инициализации при каждой расшифровке строки.
 * Как в catlean - один Cipher на поток.
 */
class ThreadLocalCacheGenerator(private val seed: Long) {
    
    /**
     * Генерация ThreadLocal поля для кэша
     */
    fun generateThreadLocalCache(classNode: ClassNode) {
        val field = FieldNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC,
            "CACHE\${seed.toString(36)}",
            "Ljava/lang/ThreadLocal;",
            "Ljava/lang/ThreadLocal<[Ljava/lang/Object;>;",
            null
        )
        classNode.fields.add(field)
        
        initializeThreadLocalInClinit(classNode, field.name)
        generateCacheAccessor(classNode, field.name)
    }
    
    /**
     * Инициализация ThreadLocal в статическом блоке
     */
    private fun initializeThreadLocalInClinit(classNode: ClassNode, fieldName: String) {
        var clinit = classNode.methods?.find { it.name == "<clinit>" }
        
        if (clinit == null) {
            clinit = MethodNode(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
            )
            clinit.instructions.add(InsnNode(Opcodes.RETURN))
            classNode.methods.add(clinit)
        }
        
        val returnInsn = clinit.instructions.last { it.opcode == Opcodes.RETURN }
        val insnList = InsnList()
        
        insnList.add(TypeInsnNode(Opcodes.NEW, "java/lang/ThreadLocal"))
        insnList.add(InsnNode(Opcodes.DUP))
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/ThreadLocal",
            "<init>",
            "()V",
            false
        ))
        insnList.add(FieldInsnNode(
            Opcodes.PUTSTATIC,
            classNode.name,
            fieldName,
            "Ljava/lang/ThreadLocal;"
        ))
        
        clinit.instructions.insertBefore(returnInsn, insnList)
    }
    
    /**
     * Генерация метода для доступа к кэшу
     */
    private fun generateCacheAccessor(classNode: ClassNode, fieldName: String) {
        val accessor = MethodNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            "getCache\${seed.toString(36)}",
            "()[Ljava/lang/Object;",
            null,
            null
        )
        
        accessor.instructions.apply {
            add(FieldInsnNode(
                Opcodes.GETSTATIC,
                classNode.name,
                fieldName,
                "Ljava/lang/ThreadLocal;"
            ))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/ThreadLocal",
                "get",
                "()Ljava/lang/Object;",
                false
            ))
            add(TypeInsnNode(Opcodes.CHECKCAST, "[Ljava/lang/Object;"))
            add(VarInsnNode(Opcodes.ASTORE, 0))
            
            add(VarInsnNode(Opcodes.ALOAD, 0))
            val notNullLabel = LabelNode()
            add(JumpInsnNode(Opcodes.IFNONNULL, notNullLabel))
            
            add(InsnNode(Opcodes.ICONST_2))
            add(TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"))
            add(VarInsnNode(Opcodes.ASTORE, 0))
            
            add(FieldInsnNode(
                Opcodes.GETSTATIC,
                classNode.name,
                fieldName,
                "Ljava/lang/ThreadLocal;"
            ))
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/ThreadLocal",
                "set",
                "(Ljava/lang/Object;)V",
                false
            ))
            
            add(notNullLabel)
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(InsnNode(Opcodes.ARETURN))
        }
        
        accessor.maxStack = 3
        accessor.maxLocals = 1
        
        classNode.methods.add(accessor)
    }
    
    /**
     * Генерация метода для получения Cipher из кэша
     */
    fun generateCipherGetter(classNode: ClassNode): String {
        val methodName = "getCipher\${seed.toString(36)}"
        
        val method = MethodNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            methodName,
            "(I)Ljavax/crypto/Cipher;",
            null,
            arrayOf("java/lang/Exception")
        )
        
        method.instructions.apply {
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                classNode.name,
                "getCache\${seed.toString(36)}",
                "()[Ljava/lang/Object;",
                false
            ))
            add(VarInsnNode(Opcodes.ASTORE, 1))
            
            add(VarInsnNode(Opcodes.ALOAD, 1))
            add(VarInsnNode(Opcodes.ILOAD, 0))
            add(InsnNode(Opcodes.AALOAD))
            add(TypeInsnNode(Opcodes.CHECKCAST, "javax/crypto/Cipher"))
            add(VarInsnNode(Opcodes.ASTORE, 2))
            
            add(VarInsnNode(Opcodes.ALOAD, 2))
            val notNullLabel = LabelNode()
            add(JumpInsnNode(Opcodes.IFNONNULL, notNullLabel))
            
            add(LdcInsnNode("DES/CBC/PKCS5Padding"))
            add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "javax/crypto/Cipher",
                "getInstance",
                "(Ljava/lang/String;)Ljavax/crypto/Cipher;",
                false
            ))
            add(VarInsnNode(Opcodes.ASTORE, 2))
            
            add(VarInsnNode(Opcodes.ALOAD, 1))
            add(VarInsnNode(Opcodes.ILOAD, 0))
            add(VarInsnNode(Opcodes.ALOAD, 2))
            add(InsnNode(Opcodes.AASTORE))
            
            add(notNullLabel)
            add(VarInsnNode(Opcodes.ALOAD, 2))
            add(InsnNode(Opcodes.ARETURN))
        }
        
        method.maxStack = 3
        method.maxLocals = 3
        
        classNode.methods.add(method)
        
        return methodName
    }
}
