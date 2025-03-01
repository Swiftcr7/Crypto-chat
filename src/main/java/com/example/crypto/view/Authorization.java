package com.example.crypto.view;

import com.example.crypto.Server;
import com.example.crypto.model.ClientInfo;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Not;
import org.springframework.stereotype.Service;


import java.awt.*;

@Slf4j
@Route("")
public class Authorization extends VerticalLayout {
    public Authorization(Server server) {
        H1 header = new H1("Авторизация");
        TextField textField = new TextField("Введите имя");
        ComboBox<String> comboBox = new ComboBox<>("Алгоритм");
        comboBox.setItems("RC6", "Twofish");

        comboBox.setValue("RC6");
        ComboBox<String> paddingComboBox = new ComboBox<>("Padding");
        paddingComboBox.setItems("ANSIX923", "ISO10126", "Zeros", "PKCS7");
        paddingComboBox.setValue("ANSIX923");
        ComboBox<String> modeComboBox = new ComboBox<>("Mode");
        modeComboBox.setItems("ECB", "CBC", "CFB", "CTR", "OFB", "PCBC", "RD");
        modeComboBox.setValue("ECB");
        Button button = new Button("Создать", event -> {
            if (!textField.getValue().isEmpty()) {
                if (textField.getValue().matches("^[a-zA-Z0-9]+$")) {
                    ClientInfo clientInfo = server.saveClient(textField.getValue(), comboBox.getValue(), paddingComboBox.getValue(), modeComboBox.getValue());
                    if (clientInfo == null) {
                        Notification.show("Ошибка авторизации: не удалось создать пользователя, попробуйте еще раз");
                    } else {
                        Notification.show("Авторизация выполнена");
                        UI.getCurrent().navigate(String.valueOf(clientInfo.getId()));
                    }


                } else {
                    Notification.show("Ошибка авторизации: введите только латинские буквы");
                }
            } else {
                Notification.show("Ошибка авторизации: введите не пустое имя");
            }

        });
        button.setWidth("195px");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(header, textField, comboBox, paddingComboBox, modeComboBox, button);
    }
}
