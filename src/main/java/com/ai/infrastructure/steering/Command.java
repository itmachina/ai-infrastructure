package com.ai.infrastructure.steering;

/**
 * 命令类
 */
public class Command {
    private final String mode;
    private final String value;
    private final long timestamp;
    
    public Command(String mode, String value) {
        this.mode = mode;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取命令模式
     * @return String
     */
    public String getMode() {
        return mode;
    }
    
    /**
     * 获取命令值
     * @return String
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 获取时间戳
     * @return long
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "Command{mode='" + mode + "', value='" + value + "', timestamp=" + timestamp + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Command command = (Command) obj;
        return mode.equals(command.mode) && value.equals(command.value);
    }
    
    @Override
    public int hashCode() {
        int result = mode.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}