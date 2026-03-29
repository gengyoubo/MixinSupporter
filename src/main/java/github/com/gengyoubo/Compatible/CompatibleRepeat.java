package github.com.gengyoubo.Compatible;

public class CompatibleRepeat {
    public static String repeat(String str, int count) {
        if (str == null) {
            return null;
        }
        if (count <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}