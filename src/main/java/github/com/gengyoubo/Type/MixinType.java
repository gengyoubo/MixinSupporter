package github.com.gengyoubo.Type;

public enum MixinType {
    Inject(0,"Inject"),
    Redirect(1,"Redirect");

    private final int value;
    private final String description;
    MixinType(int value, String description) {
        this.value = value;
        this.description = description;
    }
    public int getMixin() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * 根据输入的值获取对应的文本描述
     * @param value 输入的值 (如 0, 1, 2)
     * @return 对应的描述文本，如果未找到则返回 null
     */
    public static String getDescriptionByMixin(int value) {
        for (MixinType type : MixinType.values()) {
            if (type.getMixin() == value) {
                return type.getDescription();
            }
        }
        return null; // 或者您可以返回一个默认值或抛出异常
    }
}
