package com.example.crypto.view;

import com.example.crypto.Server;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.interceptor.AtmosphereResourceStateRecovery;


@Route("")
@Slf4j
public class Client extends VerticalLayout implements HasUrlParameter<String> {
    private long IdClient;
    private TextField RoomtextField;
    private Server server;


    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        IdClient = Long.parseLong(parameter);
        if (server.checkNoExistClient(IdClient)) {
            Notification.show("Клиент не найден");
            setEnabled(false);
        }
    }

    public Client(Server server) {
        this.server = server;
        HorizontalLayout chatLayout = new HorizontalLayout();
        RoomtextField = new TextField();
        RoomtextField.setPlaceholder("Введите id комнаты");
        H3 roomHeader = new H3("Доступные комнаты");
        roomHeader.getStyle().set("color", "#333333");
        roomHeader.getStyle().set("font-size", "18px");
        Button StartButton = new Button("Начать", event -> startChat());
        RoomtextField.setWidth("500px");
        chatLayout.getStyle().set("padding", "10px");
        chatLayout.add(RoomtextField, StartButton);
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.add(chatLayout, roomHeader);
        add(mainLayout);
    }

    private void startChat() {
        String IdRoom = RoomtextField.getValue();
        if (IdRoom.isEmpty()) {
            Notification.show("id комнаты должно быть не пустое");
            return;
        }
        String url = "room/" + IdClient + "/" + IdRoom;
        if (server.joiningRoom(IdClient, Long.parseLong(IdRoom))) {
            if (server.noContainsWindow(url)) {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", url);
                infoAboutChat(IdRoom);

            } else {
                Notification.show("Невозможно подключится. Чат уже открыт");
            }

        } else {
            Notification.show("Комната занята");
        }
    }

    private void infoAboutChat(String IdRoom) {
        HorizontalLayout horizontalLayoutChatInfo = new HorizontalLayout();
        horizontalLayoutChatInfo.setWidth("650px");
        horizontalLayoutChatInfo.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        horizontalLayoutChatInfo.setWidth("25%");
        horizontalLayoutChatInfo.getStyle().set("border", "1px solid #A9A9A9");
//        horizontalLayoutChatInfo.getStyle().set("border", "2px dashed #00BFFF");
        horizontalLayoutChatInfo.getStyle().set("padding", "10px");
        horizontalLayoutChatInfo.getStyle().set("border-radius", "5px");
        horizontalLayoutChatInfo.getStyle().set("background-color", "#E6E6FA");
//        horizontalLayoutChatInfo.getStyle().set("border-radius", "5px");
        Button chatButton = getChayButton(IdRoom);
        Button buttonForLeave = getLeaveChat(IdRoom, horizontalLayoutChatInfo);

        horizontalLayoutChatInfo.add(chatButton, buttonForLeave);
        horizontalLayoutChatInfo.expand(chatButton);
        add(horizontalLayoutChatInfo);
    }

    private Button getChayButton(String IdRoom) {
        String url = "room/" + IdClient + "/" + IdRoom;
        Button result = new Button("Комната - " + IdRoom, event -> {
            if (server.noContainsWindow(url) && server.joiningRoom(IdClient, Long.parseLong(IdRoom))) {
                UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", url);
            } else {
                if (!server.noContainsWindow(url)) {
                    Notification.show("Вы уже в комнате");

                } else {
                    Notification.show("Комната занята");

                }
            }
        });
        result.setWidth("125px");
        result.getStyle().set("background-color", "white");
        result.getStyle().set("color", "black");
        return result;

    }

    private Button getLeaveChat(String IdRoom, HorizontalLayout horizontalLayout) {
        Button result = new Button("❌", event -> {
            server.leaveRoom(IdClient, Long.parseLong(IdRoom));
            removeChatInfo(horizontalLayout);
        });
        result.getStyle().set("background-color", "white");
        result.getStyle().set("color", "black");
        result.setWidth("15px");
        return result;

    }

    private void removeChatInfo(HorizontalLayout horizontalLayout) {

        remove(horizontalLayout);
    }
}