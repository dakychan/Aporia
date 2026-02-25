package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;




@DontObfuscate
public interface GpuFence extends AutoCloseable {
   @Override
   void close();

   boolean awaitCompletion(long var1);
}
