package github.com.gengyoubo;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import github.com.gengyoubo.Compatible.CompatibleRepeat;
import github.com.gengyoubo.Type.AtType;
import github.com.gengyoubo.Type.MixinType;
import github.com.gengyoubo.Type.ValueType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class MixinAnnotationCompletionContributor extends CompletionContributor {
    //兼容栏:PsiClass.getAnnotation(String)
    public MixinAnnotationCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().afterLeaf("@"),
                new CompletionProvider<>() {

                    @Override
                    protected void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet result) {

                        // 添加 Mixin 注解补全
                        PsiElement element = parameters.getPosition();
                        for(int mixin=0;mixin<2;mixin++){
                            String mixinName = MixinType.getDescriptionByMixin(mixin);
                            addTemplates(element, result, mixinName,mixin);
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
            if (declaring != null &&
                    "java.lang.Object".equals(declaring.getQualifiedName())) {
                continue;
            }

            String name = method.isConstructor() ? "<init>" : method.getName();

            for (ValueType valueType : ValueType.values()) {
                String Type = valueType.getDescription().toUpperCase();

                for (AtType atType : AtType.values()) {

                    collectTargets(
                            method,
                            result,
                            text,
                            atType,
                            mixin,
                            valueType,
                            name,
                            Type
                    );
                }
            }
        }
    }
    private static void collectTargets(
            PsiMethod injectMethod,
            CompletionResultSet result,
            String text,
            AtType atType,
            int mixin,
            ValueType valueType,
            String name,
            String Type
    ) {
        PsiCodeBlock body = injectMethod.getBody();
        if (body == null) return;

        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NonNull PsiMethodCallExpression call) {
                super.visitMethodCallExpression(call);

                PsiMethod target = call.resolveMethod();
                if (target == null) return;

                String mixinTarget = getMixinTarget(target);
                if (mixinTarget == null) return;

                String invokeName = target.getName();

                // by 参数
                String by = (atType == AtType.BY) ? ", by = 0" : "";

                // 缩进补偿
                int mixinIndent = (mixin == 1) ? 14 : 0;

                String invokeText;
                String atText;
                //repeat(" ", 26 + mixinIndent)
                if (valueType == ValueType.INVOKE) {
                    invokeText =
                            ",\n " + CompatibleRepeat.repeat(" ", 26 + mixinIndent) +
                                    "target = \"" + mixinTarget + "\"," +
                                    "\n " + CompatibleRepeat.repeat(" ", 27 + mixinIndent) +
                                    "shift = At.Shift." + atType.getDescription() + by +
                                    "))";

                    atText = " "+atType.getDescription();
                } else {
                    invokeText = "))";
                    atText = "";
                    invokeName="";
                }

                result.addElement(
                        LookupElementBuilder
                                .create(text + " " + Type + " " + name + " "+ atText)
                                .withTailText(" "+invokeName, true)
                                .withInsertHandler((ctx, item) ->
                                        ctx.getDocument().replaceString(
                                                ctx.getStartOffset(),
                                                ctx.getTailOffset(),
                                                text + "(method = \"" + name + "\", " +
                                                        "at = @At(value = \"" + Type + "\"" +
                                                        invokeText
                                        )
                                )
                );
            }
        });
    }
    private static String getMixinTarget(PsiMethod method) {

        PsiClass owner = method.getContainingClass();
        if (owner == null) return null;

        String ownerName = owner.getQualifiedName();
        if (ownerName == null) return null;
        ownerName = ownerName.replace('.', '/');
        String methodName = method.isConstructor() ? "<init>" : method.getName();

        StringBuilder desc = new StringBuilder("(");

        for (PsiParameter param : method.getParameterList().getParameters()) {
            desc.append(toDescriptor(param.getType()));
        }

        desc.append(")");

        PsiType returnType = method.getReturnType();
        desc.append(toDescriptor(returnType));

        return "L" + ownerName + ";" + methodName + desc;
    }
    private static String toDescriptor(PsiType type) {
        if (type == null) return "V";

        // primitive
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

        // array
        if (type instanceof PsiArrayType array) {
            return "[" + toDescriptor(array.getComponentType());
        }

        // class
        if (type instanceof PsiClassType classType) {
            PsiClass cls = classType.resolve();
            if (cls != null && cls.getQualifiedName() != null) {
                return "L" + cls.getQualifiedName().replace('.', '/') + ";";
            }
        }

        return "Ljava/lang/Object;";
    }
}
