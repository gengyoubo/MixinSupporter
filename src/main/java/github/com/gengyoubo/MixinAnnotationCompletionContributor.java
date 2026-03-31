package github.com.gengyoubo;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import github.com.gengyoubo.ASM.AsmTargetInfo;
import github.com.gengyoubo.Type.AtType;
import github.com.gengyoubo.Type.MixinType;
import github.com.gengyoubo.Type.ValueType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class MixinAnnotationCompletionContributor extends CompletionContributor {
    private static final Set<String> seen = new HashSet<>();
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

        for (PsiMethod method : targetClass.getAllMethods()) {
            PsiClass declaring = method.getContainingClass();
            if (declaring != null && "java.lang.Object".equals(declaring.getQualifiedName())) {
                continue;
            }

            for (ValueType valueType : ValueType.values()) {
                if (!completionConfig.isEnabled(valueType)) continue;

                for (AtType atType : AtType.values()) {
                    collectTargets(method, result, text, atType, mixin, valueType);
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
            ValueType valueType
    ) {
        if (!valueType.needTarget()) {
            addSimpleCompletion(method, result, text, atType, mixin, valueType);
            return;
        }

        PsiCodeBlock body = method.getBody();
        if (body != null) {
            switch (valueType) {
                case INVOKE, INVOKE_ASSIGN, INVOKE_STRING ->
                        scanInvoke(method, body, result, text, atType, mixin, valueType);
                case FIELD ->
                        scanField(method, body, result, text, atType, mixin, valueType);
                case NEW ->
                        scanNew(method, body, result, text, atType, mixin, valueType);
            }
            return;
        }

        AsmTargetInfo asmInfo = loadAsmTargets(method);
        if (asmInfo == null) return;

        addAsmTargets(method, result, text, atType, mixin, valueType, asmInfo);
    }
    private static void addAsmTargets(
            PsiMethod method,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            AsmTargetInfo info
    ) {
        switch (valueType) {
            case INVOKE, INVOKE_ASSIGN, INVOKE_STRING -> {
                for (String target : info.getInvokes()) {
                    addTargetCompletion(method, result, text, atType, mixin, valueType, target, target);
                }
            }
            case FIELD -> {
                for (String target : info.getFields()) {
                    addTargetCompletion(method, result, text, atType, mixin, valueType, target, target);
                }
            }
            case NEW -> {
                for (String target : info.getNews()) {
                    addTargetCompletion(method, result, text, atType, mixin, valueType, target, "new");
                }
            }
        }
    }
    private static String buildCompletionKey(String text, ValueType valueType, String methodName, String target) {
        return text + "|" + valueType.name() + "|" + methodName + "|" + target;
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

    private static String getMixinTarget(PsiMethod method) {
        PsiClass owner = method.getContainingClass();
        if (owner == null) return null;

        String ownerName = owner.getQualifiedName();
        if (ownerName == null) return null;

        ownerName = ownerName.replace('.', '/');
        return "L" + ownerName + ";" + getMethodName(method) + getMethodDescriptor(method);
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
            ValueType valueType
    ) {
        String name = getMethodName(method);
        String type = valueType.getDescription().toUpperCase();

        result.addElement(
                LookupElementBuilder
                        .create(text + " " + type + " " + name)
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
            ValueType valueType
    ) {
        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression call) {
                super.visitMethodCallExpression(call);

                PsiMethod target = call.resolveMethod();
                if (target == null) return;

                String mixinTarget = getMixinTarget(target);
                if (mixinTarget == null) return;

                addTargetCompletion(
                        method,
                        result,
                        text,
                        atType,
                        mixin,
                        valueType,
                        mixinTarget,
                        target.getName()
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
            ValueType valueType
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
                        field.getName()
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
            ValueType valueType
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
                        "new"
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
            String name
    ) {
        String methodName = getMethodName(method);
        String type = valueType.getDescription().toUpperCase();

        result.addElement(
                LookupElementBuilder
                        .create(text + " " + type + " " + methodName + " " + name)
                        .withInsertHandler((ctx, item) ->
                                ctx.getDocument().replaceString(
                                        ctx.getStartOffset(),
                                        ctx.getTailOffset(),
                                        text + "(method = \"" + methodName +
                                                "\", at = @At(value = \"" + type +
                                                "\", target = \"" + target + "\"))"
                                )
                        )
        );
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
