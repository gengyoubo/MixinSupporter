package github.com.gengyoubo.Compatible;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;

public class CompatibleGetAnnotation {
    public static PsiAnnotation getAnnotation(PsiModifierListOwner owner, String fqn) {
        if (owner == null) return null;

        PsiModifierList list = owner.getModifierList();
        if (list == null) return null;

        for (PsiAnnotation annotation : list.getAnnotations()) {
            String qName = annotation.getQualifiedName();
            if (fqn.equals(qName)) {
                return annotation;
            }
        }
        return null;
    }
}
