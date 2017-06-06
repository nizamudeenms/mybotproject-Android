package com.ibm.watson_conversation;

import java.io.Serializable;

/**
 * Encapsulates a conversation message including the text and user.
 */
class ConversationMessage implements Serializable {

    private final String messageText;
    private final String user;

    /**
     * Creates a conversation message.
     * @param messageText Text inside the message.
     * @param user Creator of the message.
     */
    ConversationMessage(String messageText, String user) {
        this.messageText = messageText;
        this.user = user;
    }

    String getMessageText(){return messageText;}
    String getUser(){return user;}
}
