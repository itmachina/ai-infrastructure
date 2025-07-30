package com.ai.infrastructure.agent.interaction.router;

import com.ai.infrastructure.agent.interaction.model.Message;
import com.ai.infrastructure.agent.interaction.model.ChannelType;
import com.ai.infrastructure.agent.interaction.model.MessageType;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * 消息路由器 - 根据消息内容和用户偏好选择合适的渠道
 */
public class MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);
    
    // 用户渠道偏好映射
    private final Map<String, ChannelPreference> userPreferences = new HashMap<>();
    
    // 消息类型与渠道的匹配规则
    private final Map<Pattern, ChannelType> contentBasedRouting = new HashMap<>();
    private final Map<MessageType, ChannelType> typeBasedRouting = new HashMap<>();
    
    // 渠道优先级
    private final Map<ChannelType, Integer> channelPriority = new HashMap<>();
    
    public MessageRouter() {
        initializeRoutingRules();
    }
    
    /**
     * 初始化路由规则
     */
    private void initializeRoutingRules() {
        // 内容基础路由规则
        contentBasedRouting.put(Pattern.compile("紧急|重要|报警"), ChannelType.SMS);
        contentBasedRouting.put(Pattern.compile("钉钉|dingtalk|群组"), ChannelType.DINGTALK);
        contentBasedRouting.put(Pattern.compile("微信|wechat|公众号"), ChannelType.WECHAT);
        contentBasedRouting.put(Pattern.compile("邮件|email|报告"), ChannelType.EMAIL);
        contentBasedRouting.put(Pattern.compile("web|网页|界面"), ChannelType.WEB);
        
        // 消息类型路由规则
        typeBasedRouting.put(MessageType.TEXT, ChannelType.WEB);
        typeBasedRouting.put(MessageType.IMAGE, ChannelType.WECHAT);
        typeBasedRouting.put(MessageType.FILE, ChannelType.EMAIL);
        typeBasedRouting.put(MessageType.VOICE, ChannelType.VOICE);
        typeBasedRouting.put(MessageType.VIDEO, ChannelType.WEB);
        typeBasedRouting.put(MessageType.CARD, ChannelType.DINGTALK);
        typeBasedRouting.put(MessageType.TEMPLATE, ChannelType.WECHAT);
        typeBasedRouting.put(MessageType.RICH_TEXT, ChannelType.WEB);
        
        // 渠道优先级 (数值越大优先级越高)
        channelPriority.put(ChannelType.SMS, 9);
        channelPriority.put(ChannelType.PUSH, 8);
        channelPriority.put(ChannelType.DINGTALK, 7);
        channelPriority.put(ChannelType.WECHAT, 6);
        channelPriority.put(ChannelType.EMAIL, 5);
        channelPriority.put(ChannelType.API, 4);
        channelPriority.put(ChannelType.WEB, 3);
        channelPriority.put(ChannelType.VOICE, 2);
    }
    
    /**
     * 路由消息到合适的渠道
     * @param message 消息对象
     * @param availableChannels 可用渠道列表
     * @return 目标渠道类型
     */
    public ChannelType routeMessage(Message message, List<ChannelType> availableChannels) {
        logger.debug("Routing message: {}", message);
        
        // 1. 检查用户偏好
        ChannelPreference userPref = getUserPreference(message.getUserId());
        if (userPref != null && availableChannels.contains(userPref.getPreferredChannel())) {
            logger.info("Using user preferred channel: {} for user: {}", 
                       userPref.getPreferredChannel(), message.getUserId());
            return userPref.getPreferredChannel();
        }
        
        // 2. 基于消息内容路由
        ChannelType contentBased = routeByContent(message);
        if (contentBased != null && availableChannels.contains(contentBased)) {
            logger.info("Using content based channel: {} for message: {}", 
                       contentBased, message.getContent().substring(0, Math.min(50, message.getContent().length())));
            return contentBased;
        }
        
        // 3. 基于消息类型路由
        ChannelType typeBased = routeByMessageType(message);
        if (typeBased != null && availableChannels.contains(typeBased)) {
            logger.info("Using message type based channel: {} for message type: {}", 
                       typeBased, message.getMessageType());
            return typeBased;
        }
        
        // 4. 基于优先级选择默认渠道
        ChannelType defaultChannel = selectDefaultChannel(availableChannels);
        logger.info("Using default channel: {} for message", defaultChannel);
        return defaultChannel;
    }
    
    /**
     * 根据消息内容路由
     */
    private ChannelType routeByContent(Message message) {
        String content = message.getContent().toLowerCase();
        
        for (Map.Entry<Pattern, ChannelType> entry : contentBasedRouting.entrySet()) {
            if (entry.getKey().matcher(content).find()) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 根据消息类型路由
     */
    private ChannelType routeByMessageType(Message message) {
        return typeBasedRouting.get(message.getMessageType());
    }
    
    /**
     * 选择默认渠道
     */
    private ChannelType selectDefaultChannel(List<ChannelType> availableChannels) {
        if (availableChannels.isEmpty()) {
            throw new IllegalArgumentException("No available channels");
        }
        
        ChannelType selected = null;
        int maxPriority = -1;
        
        for (ChannelType channel : availableChannels) {
            int priority = channelPriority.getOrDefault(channel, 0);
            if (priority > maxPriority) {
                maxPriority = priority;
                selected = channel;
            }
        }
        
        return selected;
    }
    
    /**
     * 设置用户渠道偏好
     * @param userId 用户ID
     * @param preferredChannel 偏好渠道
     * @param priority 优先级
     */
    public void setUserPreference(String userId, ChannelType preferredChannel, int priority) {
        userPreferences.put(userId, new ChannelPreference(preferredChannel, priority));
        logger.info("Set user preference - User: {}, Channel: {}, Priority: {}", 
                   userId, preferredChannel, priority);
    }
    
    /**
     * 获取用户偏好
     */
    private ChannelPreference getUserPreference(String userId) {
        return userPreferences.get(userId);
    }
    
    /**
     * 移除用户偏好
     */
    public void removeUserPreference(String userId) {
        userPreferences.remove(userId);
        logger.info("Removed user preference for user: {}", userId);
    }
    
    /**
     * 获取所有用户偏好
     */
    public Map<String, ChannelPreference> getAllUserPreferences() {
        return new HashMap<>(userPreferences);
    }
    
    /**
     * 清空所有用户偏好
     */
    public void clearAllUserPreferences() {
        userPreferences.clear();
        logger.info("Cleared all user preferences");
    }
    
    /**
     * 用户渠道偏好类
     */
    public static class ChannelPreference {
        private final ChannelType preferredChannel;
        private final int priority;
        
        public ChannelPreference(ChannelType preferredChannel, int priority) {
            this.preferredChannel = preferredChannel;
            this.priority = priority;
        }
        
        public ChannelType getPreferredChannel() {
            return preferredChannel;
        }
        
        public int getPriority() {
            return priority;
        }
    }
}