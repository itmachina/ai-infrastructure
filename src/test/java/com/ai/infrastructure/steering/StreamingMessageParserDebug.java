package com.ai.infrastructure.steering;

/**
 * Simple debug test for StreamingMessageParser
 */
public class StreamingMessageParserDebug {
    public static void main(String[] args) {
        System.out.println("Starting StreamingMessageParser debug test...");
        
        try {
            // Create input queue and parser
            AsyncMessageQueue<String> inputQueue = new AsyncMessageQueue<>();
            StreamingMessageParser parser = new StreamingMessageParser(inputQueue);
            
            // Start parser
            parser.startProcessing();
            System.out.println("Parser started");
            
            // Send simple text message
            System.out.println("Sending simple text message...");
            inputQueue.enqueue("Hello, World!\n");
            
            // Try to read parsed message
            System.out.println("Trying to read parsed message...");
            QueueMessage<UserMessage> message = parser.getOutputStream().read().join();
            
            if (message.isDone()) {
                System.out.println("Received done message");
            } else {
                System.out.println("Received message: " + message.getValue().getContent());
            }
            
            // Complete queue
            inputQueue.complete();
            parser.close();
            
            System.out.println("Test completed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}