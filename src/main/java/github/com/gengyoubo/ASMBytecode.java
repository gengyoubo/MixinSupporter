package github.com.gengyoubo;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.InputStream;
import java.io.PrintWriter;

public class ASMBytecode {
    private static boolean key=true;
    public static void main(String[] args) throws Exception {
        getBytecode(Override.class);
    }
    public static void getBytecode(Class<?> clazz) throws Exception {
        if(key){
        String path = "/" + clazz.getName().replace('.', '/') + ".class";

        try (InputStream is = clazz.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("找不到 class 文件: " + path);
            }

            byte[] bytes = is.readAllBytes();
            ClassReader cr = new ClassReader(bytes);

            cr.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
            key=false;
        }
        }
    }
}
