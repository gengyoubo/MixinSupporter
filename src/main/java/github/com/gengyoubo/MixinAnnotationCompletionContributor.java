package github.com.gengyoubo;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import github.com.gengyoubo.Type.MixinType;
import github.com.gengyoubo.Type.ValueType;
import org.jetbrains.annotations.NotNull;

public class MixinAnnotationCompletionContributor extends CompletionContributor {
    public MixinAnnotationCompletionContributor() {

        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement(),
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
                            addTemplates(element, result, mixinName);
                        }
                    }
                }
        );
    }
    private static void collectTargets(PsiMethod injectMethod, CompletionResultSet result, String text) {
        PsiCodeBlock body = injectMethod.getBody();
        if (body == null) return;

        body.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression call) {
                super.visitMethodCallExpression(call);

                PsiMethod target = call.resolveMethod();
                if (target == null) return;

                String mixinTarget = getMixinTarget(target);
                if (mixinTarget == null) return;

                // 被注入的方法名
                String injectName = injectMethod.isConstructor()
                        ? "<init>"
                        : injectMethod.getName();

                // 被调用的方法名
                String invokeName = target.getName();

                result.addElement(
                        LookupElementBuilder.create(text+" INVOKE " +injectName+" "+ invokeName)
                                .withInsertHandler((ctx, item) ->
                                        ctx.getDocument().replaceString(
                                                ctx.getStartOffset(),
                                                ctx.getTailOffset(),
                                                text+"(method = \"" + injectName + "\", " +
                                                        "at = @At(value = \"INVOKE\", target = \"" + mixinTarget + "\"))"
                                        )
                                )
                );
            }
        });
    }
    private void addTemplates(PsiElement element, CompletionResultSet result,String text) {
        PsiClass targetClass = MixinUtils.getTargetClassFromMixin(element);
        if (targetClass == null) return;
        for (PsiMethod method : targetClass.getAllMethods()) {
            // 过滤 java.lang.Object 方法
            PsiClass declaring = method.getContainingClass();
            if (declaring != null &&
                    "java.lang.Object".equals(declaring.getQualifiedName())) {
                continue;
            }
            String name = method.isConstructor() ? "<init>" : method.getName();
            for(int Value = 0; Value < 3; Value++){
                String Type= ValueType.getDescriptionByValue(Value).toUpperCase();
                //AFTER
            result.addElement(
                    LookupElementBuilder.create(text+" "+Type+" AFTER " + name)
                            .withInsertHandler((ctx, item) ->
                                    ctx.getDocument().replaceString(
                                            ctx.getStartOffset(),
                                            ctx.getTailOffset(),
                                            text + "(method = \"" + name + "\", " +
                                                    "at = @At(value = \"+Type+\", shift = At.Shift.AFTER))"
                                    )

                            )
            );
            //BEFORE
            result.addElement(
                    LookupElementBuilder.create(text+" "+Type+" BEFORE " + name)
                            .withInsertHandler((ctx, item) ->
                                    ctx.getDocument().replaceString(
                                            ctx.getStartOffset(),
                                            ctx.getTailOffset(),
                                            text + "(method = \"" + name + "\", " +
                                                    "at = @At(value = \"+Type+\", shift = At.Shift.BEFORE))"
                                    )

                            )
            );
            //BY
            result.addElement(
                    LookupElementBuilder.create(text+" "+Type+" BY " + name)
                            .withInsertHandler((ctx, item) ->
                                    ctx.getDocument().replaceString(
                                            ctx.getStartOffset(),
                                            ctx.getTailOffset(),
                                            text + "(method = \"" + name + "\", " +
                                                    "at = @At(value = \"+Type+\", shift = At.Shift.BY, by = 0))"
                                    )
                            )
            );
            }
            //INVOKE
            collectTargets(method, result, text);
        }
    }
    private static String getMixinTarget(PsiMethod method) {

        PsiClass owner = method.getContainingClass();
        if (owner == null) return null;

        String ownerName = owner.getQualifiedName().replace('.', '/');
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

        if (type.equals(PsiType.VOID)) return "V";
        if (type.equals(PsiType.INT)) return "I";
        if (type.equals(PsiType.BOOLEAN)) return "Z";
        if (type.equals(PsiType.BYTE)) return "B";
        if (type.equals(PsiType.CHAR)) return "C";
        if (type.equals(PsiType.SHORT)) return "S";
        if (type.equals(PsiType.LONG)) return "J";
        if (type.equals(PsiType.FLOAT)) return "F";
        if (type.equals(PsiType.DOUBLE)) return "D";

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
}
