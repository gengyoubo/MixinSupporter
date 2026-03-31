package github.com.gengyoubo.ASM;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AsmScanner {
    public static AsmTargetInfo scanMethod(byte[] bytes, String targetMethodName, String targetMethodDesc) {
        AsmTargetInfo info = new AsmTargetInfo();
        ClassReader cr = new ClassReader(bytes);

        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions
            ) {
                if (!targetMethodName.equals(name) || !targetMethodDesc.equals(descriptor)) {
                    return null;
                }

                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String name,
                            String descriptor,
                            boolean isInterface
                    ) {
                        info.getInvokes().add("L" + owner + ";" + name + descriptor);
                    }

                    @Override
                    public void visitFieldInsn(
                            int opcode,
                            String owner,
                            String name,
                            String descriptor
                    ) {
                        info.getFields().add("L" + owner + ";" + name + ":" + descriptor);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW) {
                            info.getNews().add("L" + type + ";");
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return info;
    }
}
