package github.com.gengyoubo.Type;

public enum ValueType {
    //方法开始时执行
    HEAD(0, "head"),
    //所有 return 之前执行
    RETURN(1, "return"),
    //方法最后一个 return 前执行
    TAIL(2, "tail"),
    //在调用某个方法时注入
    INVOKE(3,"invoke"),
    //在方法调用 返回值赋值后 注入
    INVOKE_ASSIGN(4,"invoke"),
    //访问字段时注入
    FIELD(5,"field"),
    //对象创建时注入
    NEW(6,"new"),
    //调用带字符串参数的方法
    INVOKE_STRING(7,"invokeString"),
    //跳转指令处注入
    JUMP(8,"jump"),
    //常量加载时注入
    CONSTANT(9,"constant");
    private final int value;
    private final String description;

    ValueType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
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
    public static String getDescriptionByValue(int value) {
        for (ValueType type : ValueType.values()) {
            if (type.getValue() == value) {
                return type.getDescription();
            }
        }
        return null; // 或者您可以返回一个默认值或抛出异常
    }
    public boolean needTarget() {
        return switch (this) {
            case INVOKE,
                 INVOKE_ASSIGN,
                 FIELD,
                 NEW,
                 INVOKE_STRING -> true;
            default -> false;
        };
    }
}
