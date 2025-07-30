package com.ai.infrastructure.agent.interaction.manager;

import com.ai.infrastructure.agent.interaction.channel.InteractionChannel;
import com.ai.infrastructure.agent.interaction.channel.InteractionChannel.ChannelConfig;
import com.ai.infrastructure.agent.interaction.channel.InteractionChannel.ChannelStatus;
import com.ai.infrastructure.agent.interaction.factory.ChannelFactory;
import com.ai.infrastructure.agent.interaction.model.ChannelType;
import com.ai.infrastructure.agent.interaction.model.Message;
import com.ai.infrastructure.agent.interaction.model.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 渠道管理器
 */
public class ChannelManager {
    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);
    
    private final Map<String, InteractionChannel> channels = new ConcurrentHashMap<>();
    private final ChannelFactory channelFactory;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    
    public ChannelManager() {
        this.channelFactory = ChannelFactory.getInstance();
    }
    
    /**
     * 初始化渠道管理器
     */
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing ChannelManager");
            
            // 启动健康检查调度器
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::performHealthCheck, 
                                       30, 30, TimeUnit.SECONDS);
            
            logger.info("ChannelManager initialized successfully");
        }
    }
    
    /**
     * 注册渠道
     * @param channelId 渠道ID
     * @param channelType 渠道类型
     * @param config 配置参数
     * @return 是否注册成功
     */
    public boolean registerChannel(String channelId, ChannelType channelType, 
                                 Map<String, Object> config) {
        try {
            logger.info("Registering channel: {} - {}", channelId, channelType);
            
            if (channels.containsKey(channelId)) {
                logger.warn("Channel already exists: {}", channelId);
                return false;
            }
            
            // 创建渠道实例
            InteractionChannel channel = channelFactory.createChannel(channelType, config);
            
            // 验证配置
            if (!channel.validateConfig()) {
                logger.error("Invalid channel configuration: {}", channelId);
                return false;
            }
            
            // 连接渠道
            channel.connect();
            
            // 注册渠道
            channels.put(channelId, channel);
            
            logger.info("Channel registered successfully: {}", channelId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to register channel: " + channelId, e);
            return false;
        }
    }
    
    /**
     * 注销渠道
     * @param channelId 渠道ID
     * @return 是否注销成功
     */
    public boolean unregisterChannel(String channelId) {
        try {
            logger.info("Unregistering channel: {}", channelId);
            
            InteractionChannel channel = channels.remove(channelId);
            if (channel == null) {
                logger.warn("Channel not found: {}", channelId);
                return false;
            }
            
            channel.disconnect();
            
            logger.info("Channel unregistered successfully: {}", channelId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to unregister channel: " + channelId, e);
            return false;
        }
    }
    
    /**
     * 发送消息
     * @param channelId 渠道ID
     * @param message 消息内容
     * @return 发送结果
     */
    public boolean sendMessage(String channelId, Message message) {
        try {
            InteractionChannel channel = channels.get(channelId);
            if (channel == null) {
                logger.error("Channel not found: {}", channelId);
                return false;
            }
            
            if (!channel.isConnected()) {
                logger.warn("Channel not connected: {}", channelId);
                return false;
            }
            
            // 设置渠道类型
            message.setChannelType(channel.getChannelType());
            
            // 发送消息
            var result = channel.sendMessage(message);
            
            if (result.isSuccess()) {
                logger.info("Message sent successfully via channel: {}", channelId);
                return true;
            } else {
                logger.error("Failed to send message via channel: {} - {}", 
                           channelId, result.getError());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to send message via channel: " + channelId, e);
            return false;
        }
    }
    
    /**
     * 接收消息
     * @param channelId 渠道ID
     * @return 接收到的消息
     */
    public Message receiveMessage(String channelId) {
        try {
            InteractionChannel channel = channels.get(channelId);
            if (channel == null) {
                logger.error("Channel not found: {}", channelId);
                return null;
            }
            
            if (!channel.isConnected()) {
                logger.warn("Channel not connected: {}", channelId);
                return null;
            }
            
            return channel.receiveMessage();
            
        } catch (Exception e) {
            logger.error("Failed to receive message from channel: " + channelId, e);
            return null;
        }
    }
    
    /**
     * 获取渠道状态
     * @param channelId 渠道ID
     * @return 渠道状态
     */
    public ChannelStatus getChannelStatus(String channelId) {
        InteractionChannel channel = channels.get(channelId);
        if (channel == null) {
            return null;
        }
        return channel.getStatus();
    }
    
    /**
     * 获取所有渠道ID
     * @return 渠道ID列表
     */
    public List<String> getAllChannelIds() {
        return new ArrayList<>(channels.keySet());
    }
    
    /**
     * 获取指定类型的所有渠道
     * @param channelType 渠道类型
     * @return 渠道列表
     */
    public List<InteractionChannel> getChannelsByType(ChannelType channelType) {
        List<InteractionChannel> result = new ArrayList<>();
        for (InteractionChannel channel : channels.values()) {
            if (channel.getChannelType() == channelType) {
                result.add(channel);
            }
        }
        return result;
    }
    
    /**
     * 检查渠道是否存在
     * @param channelId 渠道ID
     * @return 是否存在
     */
    public boolean containsChannel(String channelId) {
        return channels.containsKey(channelId);
    }
    
    /**
     * 获取渠道数量
     * @return 渠道数量
     */
    public int getChannelCount() {
        return channels.size();
    }
    
    /**
     * 关闭所有渠道
     */
    public void shutdown() {
        logger.info("Shutting down ChannelManager");
        
        // 关闭调度器
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 断开所有渠道连接
        for (String channelId : channels.keySet()) {
            try {
                unregisterChannel(channelId);
            } catch (Exception e) {
                logger.error("Failed to shutdown channel: " + channelId, e);
            }
        }
        
        initialized.set(false);
        logger.info("ChannelManager shutdown completed");
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            logger.debug("Performing channel health check");
            
            for (Map.Entry<String, InteractionChannel> entry : channels.entrySet()) {
                String channelId = entry.getKey();
                InteractionChannel channel = entry.getValue();
                
                if (channel.isConnected()) {
                    ChannelStatus status = channel.getStatus();
                    if (!status.isConnected()) {
                        logger.warn("Channel {} appears to be disconnected", channelId);
                        try {
                            channel.disconnect();
                            channel.connect();
                            logger.info("Channel {} reconnected successfully", channelId);
                        } catch (Exception e) {
                            logger.error("Failed to reconnect channel: " + channelId, e);
                        }
                    }
                } else {
                    logger.warn("Channel {} is not connected", channelId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to perform health check", e);
        }
    }
}