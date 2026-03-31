package github.com.gengyoubo;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import github.com.gengyoubo.ASM.AsmScanner;
import github.com.gengyoubo.ASM.AsmTargetInfo;
import github.com.gengyoubo.Type.AtType;
import github.com.gengyoubo.Type.MixinType;
import github.com.gengyoubo.Type.ValueType;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MixinAnnotationCompletionContributor extends CompletionContributor {
    public MixinAnnotationCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().afterLeaf("@"),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet result
                    ) {
                        PsiElement element = parameters.getPosition();
                        for (int mixin = 0; mixin < 2; mixin++) {
                            String mixinName = MixinType.getDescriptionByMixin(mixin);
                            addTemplates(element, result, mixinName, mixin);
                        }
                    }
                }
        );
    }

    private void addTemplates(PsiElement element, CompletionResultSet result, String text, int mixin) {
        PsiClass targetClass = MixinUtils.getTargetClassFromMixin(element);
        if (targetClass == null) return;
        Set<String> seen = new HashSet<>();

        for (PsiMethod method : targetClass.getAllMethods()) {
            PsiClass declaring = method.getContainingClass();
            if (declaring != null && "java.lang.Object".equals(declaring.getQualifiedName())) {
                continue;
            }

            for (ValueType valueType : ValueType.values()) {
                if (!completionConfig.isEnabled(valueType)) continue;

                for (AtType atType : AtType.values()) {
                    collectTargets(method, result, text, atType, mixin, valueType, seen);
                }
            }
        }
    }

    private static void collectTargets(
            PsiMethod method,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            Set<String> seen
    ) {
        if (!valueType.needTarget()) {
            addSimpleCompletion(method, result, text, atType, mixin, valueType, seen);
            return;
        }

        PsiCodeBlock body = method.getBody();
        if (body != null) {
            switch (valueType) {
                case INVOKE, INVOKE_ASSIGN, INVOKE_STRING ->
                        scanInvoke(method, body, result, text, atType, mixin, valueType, seen);
                case FIELD ->
                        scanField(method, body, result, text, atType, mixin, valueType, seen);
                case NEW ->
                        scanNew(method, body, result, text, atType, mixin, valueType, seen);
            }
            return;
        }

        AsmTargetInfo asmInfo = loadAsmTargets(method);
        if (asmInfo == null) return;

        addAsmTargets(method, result, text, atType, mixin, valueType, asmInfo, seen);
    }
    private static void addAsmTargets(
            PsiMethod method,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            AsmTargetInfo info,
            Set<String> seen
    ) {
        switch (valueType) {
            case INVOKE, INVOKE_ASSIGN, INVOKE_STRING -> {
                for (String target : info.getInvokes()) {
                    addTargetCompletion(method, result, text, atType, mixin, valueType, target, target, seen, null);
                }
            }
            case FIELD -> {
                for (String target : info.getFields()) {
                    addTargetCompletion(method, result, text, atType, mixin, valueType, target, target, seen, null);
                }
            }
            case NEW -> {
                for (String target : info.getNews()) {
                    addTargetCompletion(method, result, text, atType, mixin, valueType, target, "new", seen, null);
                }
            }
        }
    }
    private static String buildCompletionKey(String text, ValueType valueType, String methodName, String target, Integer ordinal) {
        return text + "|" + valueType.name() + "|" + methodName + "|" + target + "|" + ordinal;
    }
    private static String getMethodName(PsiMethod method) {
        return method.isConstructor() ? "<init>" : method.getName();
    }

    private static String getMethodDescriptor(PsiMethod method) {
        StringBuilder desc = new StringBuilder("(");

        for (PsiParameter param : method.getParameterList().getParameters()) {
            desc.append(toDescriptor(param.getType()));
        }

        desc.append(")");
        desc.append(toDescriptor(method.getReturnType()));
        return desc.toString();
    }

    private static AsmTargetInfo loadAsmTargets(PsiMethod method) {
        PsiClass owner = method.getContainingClass();
        if (owner == null) return null;

        byte[] bytes = loadClassBytes(owner);
        if (bytes == null) return null;

        return AsmScanner.scanMethod(bytes, getMethodName(method), getMethodDescriptor(method));
    }

    private static byte[] loadClassBytes(PsiClass owner) {
        try {
            PsiFile file = owner.getContainingFile();
            if (file != null) {
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile != null && "class".equalsIgnoreCase(virtualFile.getExtension())) {
                    return virtualFile.contentsToByteArray();
                }
            }

            String qualifiedName = owner.getQualifiedName();
            if (qualifiedName == null) return null;

            String resourcePath = qualifiedName.replace('.', '/') + ".class";
            ClassLoader loader = MixinAnnotationCompletionContributor.class.getClassLoader();
            if (loader == null) return null;

            try (InputStream is = loader.getResourceAsStream(resourcePath)) {
                if (is == null) return null;
                return is.readAllBytes();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getMixinTarget(PsiMethod method) {
        PsiClass owner = method.getContainingClass();
        if (owner == null) return null;

        String ownerName = owner.getQualifiedName();
        if (ownerName == null) return null;

        ownerName = ownerName.replace('.', '/');
        return buildMethodTarget(ownerName, method);
    }

    private static String buildMethodTarget(String ownerInternalName, PsiMethod method) {
        return "L" + ownerInternalName + ";" + getMethodName(method) + getMethodDescriptor(method);
    }

    private static String getInvokeTarget(PsiMethod resolvedMethod, PsiMethodCallExpression call) {
        PsiReferenceExpression methodExpression = call.getMethodExpression();
        PsiExpression qualifier = methodExpression.getQualifierExpression();
        if (qualifier != null) {
            PsiType qualifierType = qualifier.getType();
            if (qualifierType instanceof PsiClassType classType) {
                PsiClass qualifierClass = classType.resolve();
                if (qualifierClass != null && qualifierClass.getQualifiedName() != null) {
                    return buildMethodTarget(
                            qualifierClass.getQualifiedName().replace('.', '/'),
                            resolvedMethod
                    );
                }
            }
        }

        return getMixinTarget(resolvedMethod);
    }

    private static String toDescriptor(PsiType type) {
        if (type == null) return "V";

        if (type instanceof PsiPrimitiveType) {
            return switch (type.getCanonicalText()) {
                case "byte" -> "B";
                case "char" -> "C";
                case "double" -> "D";
                case "float" -> "F";
                case "int" -> "I";
                case "long" -> "J";
                case "short" -> "S";
                case "boolean" -> "Z";
                case "void" -> "V";
                default -> "Ljava/lang/Object;";
            };
        }

        if (type instanceof PsiArrayType array) {
            return "[" + toDescriptor(array.getComponentType());
        }

        if (type instanceof PsiClassType classType) {
            PsiClass cls = classType.resolve();
            if (cls != null && cls.getQualifiedName() != null) {
                return "L" + cls.getQualifiedName().replace('.', '/') + ";";
            }
        }

        return "Ljava/lang/Object;";
    }

    private static void addSimpleCompletion(
            PsiMethod method,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            Set<String> seen
    ) {
        String name = getMethodName(method);
        String type = valueType.getDescription().toUpperCase();
        String key = buildCompletionKey(text, valueType, name, "", null);
        if (!seen.add(key)) return;

        result.addElement(
                LookupElementBuilder
                        .create(key, text + " " + type + " " + name)
                        .withPresentableText(text)
                        .withTypeText(type, true)
                        .withTailText(" " + name, true)
                        .withInsertHandler((ctx, item) ->
                                ctx.getDocument().replaceString(
                                        ctx.getStartOffset(),
                                        ctx.getTailOffset(),
                                        text + "(method = \"" + name +
                                                "\", at = @At(\"" + type + "\"))"
                                )
                        )
        );
    }

    private static void scanInvoke(
            PsiMethod method,
            PsiCodeBlock body,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            Set<String> seen
    ) {
        Map<String, Integer> ordinals = new HashMap<>();
        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression call) {
                super.visitMethodCallExpression(call);

                PsiMethod target = call.resolveMethod();
                if (target == null) return;

                String mixinTarget = getInvokeTarget(target, call);
                if (mixinTarget == null) return;
                int ordinal = ordinals.getOrDefault(mixinTarget, 0);
                ordinals.put(mixinTarget, ordinal + 1);

                addTargetCompletion(
                        method,
                        result,
                        text,
                        atType,
                        mixin,
                        valueType,
                        mixinTarget,
                        target.getName(),
                        seen,
                        ordinal
                );
            }
        });
    }

    private static void scanField(
            PsiMethod method,
            PsiCodeBlock body,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            Set<String> seen
    ) {
        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expr) {
                super.visitReferenceExpression(expr);

                PsiElement resolved = expr.resolve();
                if (!(resolved instanceof PsiField field)) return;

                String target = getFieldTarget(field);
                if (target == null) return;

                addTargetCompletion(
                        method,
                        result,
                        text,
                        atType,
                        mixin,
                        valueType,
                        target,
                        field.getName(),
                        seen,
                        null
                );
            }
        });
    }

    private static void scanNew(
            PsiMethod method,
            PsiCodeBlock body,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            Set<String> seen
    ) {
        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitNewExpression(PsiNewExpression expr) {
                super.visitNewExpression(expr);

                PsiJavaCodeReferenceElement ref = expr.getClassReference();
                if (ref == null) return;

                PsiElement resolved = ref.resolve();
                if (!(resolved instanceof PsiClass cls)) return;
                if (cls.getQualifiedName() == null) return;

                String target = "L" + cls.getQualifiedName().replace('.', '/') + ";";

                addTargetCompletion(
                        method,
                        result,
                        text,
                        atType,
                        mixin,
                        valueType,
                        target,
                        "new",
                        seen,
                        null
                );
            }
        });
    }

    private static void addTargetCompletion(
            PsiMethod method,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            String target,
            String name,
            Set<String> seen,
            Integer ordinal
    ) {
        String methodName = getMethodName(method);
        String key = buildCompletionKey(text, valueType, methodName, target, ordinal);
        if (!seen.add(key)) return;
        String type = valueType.getDescription().toUpperCase();
        String shortTarget = shortenTarget(target, name);
        String ordinalText = ordinal == null ? "" : " [" + ordinal + "]";

        result.addElement(
                LookupElementBuilder
                        .create(key, text + " " + type + " " + methodName + " " + name)
                        .withPresentableText(text)
                        .withTypeText(type, true)
                        .withTailText(" " + methodName + " -> " + shortTarget + ordinalText, true)
                        .withInsertHandler((ctx, item) ->
                                ctx.getDocument().replaceString(
                                        ctx.getStartOffset(),
                                        ctx.getTailOffset(),
                                        text + "(method = \"" + methodName +
                                                "\", at = @At(value = \"" + type +
                                                "\", target = \"" + target + "\"" +
                                                (ordinal == null ? "" : ", ordinal = " + ordinal) +
                                                "))"
                                )
                        )
        );
    }

    private static String shortenTarget(String target, String fallbackName) {
        int methodSep = target.indexOf(';');
        if (methodSep >= 0 && methodSep + 1 < target.length()) {
            String ownerDisplay = shortenOwner(target.substring(0, methodSep + 1));
            String tail = target.substring(methodSep + 1);
            int descStart = tail.indexOf('(');
            int fieldSep = tail.indexOf(':');

            if (descStart >= 0) {
                return ownerDisplay + "." + tail.substring(0, descStart) + "(...)";
            }
            if (fieldSep >= 0) {
                return ownerDisplay + "." + tail.substring(0, fieldSep);
            }
            if (!tail.isEmpty()) {
                return ownerDisplay + "." + tail;
            }
        }

        if (target.startsWith("L") && target.endsWith(";")) {
            return shortenOwner(target);
        }

        return fallbackName;
    }

    private static String shortenOwner(String ownerTarget) {
        if (!ownerTarget.startsWith("L") || !ownerTarget.endsWith(";")) {
            return ownerTarget;
        }

        String internalName = ownerTarget.substring(1, ownerTarget.length() - 1);
        int slash = internalName.lastIndexOf('/');
        return slash >= 0 ? internalName.substring(slash + 1) : internalName;
    }

    private static String getFieldTarget(PsiField field) {
        PsiClass owner = field.getContainingClass();
        if (owner == null) return null;

        String ownerName = owner.getQualifiedName();
        if (ownerName == null) return null;

        ownerName = ownerName.replace('.', '/');
        String name = field.getName();
        String desc = toDescriptor(field.getType());

        return "L" + ownerName + ";" + name + ":" + desc;
    }
}
