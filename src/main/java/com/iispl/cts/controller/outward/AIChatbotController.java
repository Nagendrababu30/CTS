package com.iispl.cts.controller.outward;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

import com.iispl.cts.service.outward.AIChatbotService;

public class AIChatbotController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox chatListbox;

    @Wire
    private Textbox messageTextbox;

    @Wire
    private Button sendButton;

    private final AIChatbotService aiChatbotService =
            new AIChatbotService();


    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        addAIMessage(
                "Hello! I am the CTS AI Assistant. "
                + "How can I help you?"
        );
    }


    @Listen("onClick=#sendButton")
    public void sendMessage() {

        String userMessage = messageTextbox.getValue();

        if (userMessage == null ||
                userMessage.trim().isEmpty()) {

            return;
        }

        userMessage = userMessage.trim();

        // Display user's question
        addUserMessage(userMessage);

        // Clear textbox
        messageTextbox.setValue("");

        // Call service
        String aiResponse =
                aiChatbotService.askAI(userMessage);

        // Display AI response
        addAIMessage(aiResponse);
    }


    private void addUserMessage(String message) {

        Listitem item = new Listitem();

        Listcell cell = new Listcell();

        Label label = new Label();

        label.setValue("You: " + message);

        label.setStyle(
                "font-size:14px;"
                + "padding:10px;"
                + "font-weight:bold;"
        );

        cell.appendChild(label);

        item.appendChild(cell);

        chatListbox.appendChild(item);
    }


    private void addAIMessage(String message) {

        Listitem item = new Listitem();

        Listcell cell = new Listcell();

        Label label = new Label();

        label.setValue("AI: " + message);

        label.setStyle(
                "font-size:14px;"
                + "padding:10px;"
        );

        cell.appendChild(label);

        item.appendChild(cell);

        chatListbox.appendChild(item);
    }
}