package github.com.gengyoubo;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import github.com.gengyoubo.Compatible.CompatibleGetAnnotation;

public class MixinUtils {
    public static PsiClass getTargetClassFromMixin(PsiElement element) {

        PsiClass mixinClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (mixinClass == null) return null;

        PsiAnnotation mixinAnnotation = CompatibleGetAnnotation.getAnnotation(
                mixinClass,
                "org.spongepowered.asm.mixin.Mixin"
        );
        if (mixinAnnotation == null) return null;

        PsiAnnotationMemberValue value = mixinAnnotation.findAttributeValue("value");
        if (value == null) return null;

        // @Mixin(Target.class)
        if (value instanceof PsiClassObjectAccessExpression) {
            PsiTypeElement operand =
                    ((PsiClassObjectAccessExpression) value).getOperand();

            PsiType type = operand.getType();
            if (type instanceof PsiClassType) {
                return ((PsiClassType) type).resolve();
            }
        }

        return null;
    }
}
