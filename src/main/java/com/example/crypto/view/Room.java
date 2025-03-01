package com.example.crypto.view;

import org.apache.commons.lang3.tuple.Pair;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.StreamResource;
import com.example.crypto.Kafka.KafkaWriter;
import com.example.crypto.Server;
import com.example.crypto.cipher.CipherManagement;
import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.atmosphere.interceptor.AtmosphereResourceStateRecovery;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Route("room")
@Slf4j
public class Room extends VerticalLayout implements HasUrlParameter<String> {
    private final Server server;
    private final KafkaWriter kafkaWriter;
    private MessageLayotDisplay messageLayotDisplay;

    private String topicOutput;
    private volatile CipherManagement cipherEncryption;

    private long idClient;
    private long idRoom;

    private long secondClientId;
    private final Backend backend;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        String[] params = parameter.split("/");
        idClient = Long.parseLong(params[0]);
        idRoom = Long.parseLong(params[1]);
        if (server.checkNoExistClient(idClient)) {
            Notification.show("Нет пользователя");
            setEnabled(false);
        } else {
            log.info("starting a kafka stream");
            service.submit(backend::KafkaConsumerRun);
            server.insertWindow("room/" + idClient + "/" + idRoom, event.getUI());
        }
    }

    @Override
    protected void onDetach(DetachEvent event) {
        server.leaveRoom(idClient, idRoom);
        if (topicOutput != null) {
            kafkaWriter.sendMessage(new Message("disconect", null, null, 0, null).convertToBytes(), topicOutput);
        }
        //????
        server.leaveRoom(idClient, idRoom);
        backend.close();

        service.shutdown();
        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Room(KafkaWriter kafkaWriter, Server server) {
        log.info("Room view is running");
        topicOutput = null;
        cipherEncryption = null;
        this.server = server;
        this.kafkaWriter = kafkaWriter;
        new Frontend().createPage();
        this.backend = new Backend();

    }

    public class Backend {
        private static final String bootstrapServer = "localhost:9093";
        private static final String offsetReset = "earliest";
        private static final Random random = new Random();
        private static final ObjectMapper objectMapper = new ObjectMapper();
        private byte[] p;
        private byte[] privateKey;
        private byte[] publicKeyAnotherClient;
        private volatile CipherManagement CipherForDecoding;
        private volatile boolean flag = true;
        private CipherInfoMassage cipherInfoMassageAnotherClient;

        public void KafkaConsumerRun() {
            log.info("Start kafka, idClient {}", idClient);
            CipherInfoMassage cipherInfoMassageThisClient = server.getCipherInfoMessageForClient(idClient, idRoom);
            Properties properties = new Properties();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group_" + idClient + "_" + idRoom);
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
            KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<>(properties,
                    new ByteArrayDeserializer(), new ByteArrayDeserializer());
            kafkaConsumer.subscribe(Collections.singleton("input_" + idClient + "_" + idRoom));
            log.info("Kafka consumer is creating {}", idClient);
            try {
                while (flag) {
                    ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                    for (var con : consumerRecords) {
                        String message = new String(con.value());
                        if (message.contains("cipher_info")) {
                            log.info("Hand cipher_info message from kafka {}", idClient);
                            cipherInfoMassageAnotherClient = objectMapper.readValue(message, CipherInfoMassage.class);
                            secondClientId = cipherInfoMassageAnotherClient.getAnotherClientId();
                            privateKey = privateKeyGeneration();
                            p = cipherInfoMassageAnotherClient.getP();
                            topicOutput = "input_" + cipherInfoMassageAnotherClient.getAnotherClientId() + "_" + idRoom;
                            byte[] publicKey = publicKeyGeneration(p, cipherInfoMassageAnotherClient.getG(), privateKey);
                            log.info("Message in kafka {}, in topic {}, clienId {}", new KeyMassage("key_info", publicKey).convertToString(), topicOutput, idClient);
                            kafkaWriter.sendMessage(new KeyMassage("key_info", publicKey).convertToBytes(), topicOutput);
                            log.info("the message has been sent successfully with client {} id to {}", idClient, topicOutput);
                            if (publicKeyAnotherClient != null) {
                                cipherInfoMassageAnotherClient.setPublicKey(publicKeyAnotherClient);
                                CipherForDecoding = CipherFromCipherInfoMessage.createCipher(cipherInfoMassageAnotherClient, new BigInteger(privateKey), new BigInteger(p));

                                cipherInfoMassageThisClient.setPublicKey(publicKeyAnotherClient);
                                cipherEncryption = CipherFromCipherInfoMessage.createCipher(cipherInfoMassageThisClient, new BigInteger(privateKey), new BigInteger(p));
                                log.info("public key another client != null , id current client {}", idClient);
                            }

                        } else if (message.contains("key_info")) {
                            log.info("Hand key_info message from kafka {}", idClient);
                            KeyMassage keyMassage = objectMapper.readValue(message, KeyMassage.class);
                            log.info("Reading message {}, from id client{}", keyMassage.convertToString(), idClient);
                            if (cipherInfoMassageAnotherClient != null) {

                                cipherInfoMassageAnotherClient.setPublicKey(keyMassage.getPublicKey());

                                CipherForDecoding = CipherFromCipherInfoMessage.createCipher(cipherInfoMassageAnotherClient, new BigInteger(privateKey), new BigInteger(p));


                                cipherInfoMassageThisClient.setPublicKey(keyMassage.getPublicKey());

                                cipherEncryption = CipherFromCipherInfoMessage.createCipher(cipherInfoMassageThisClient, new BigInteger(privateKey), new BigInteger(p));

                                log.info(" cipherInfoMassageAnotherClient != null , id current client {}", idClient);
                            } else {
                                publicKeyAnotherClient = keyMassage.getPublicKey();
                                log.info(" cipherInfoMassageAnotherClient == null , id current client {}", idClient);
                            }
                        } else if (message.contains("delete_message")) {
                            Message messageForDelete = objectMapper.readValue(message, Message.class);
                            log.info("message to delete {}", messageForDelete.convertToString());
                            log.info("consumer get command to delete message, id client {}", idClient);
                            messageLayotDisplay.deleteMessage(messageForDelete.getIndexMessage());
                        } else if (message.contains("disconect")) {
                            cipherEncryption = null;
                            CipherForDecoding = null;
                            log.info("consumer get command to disconnect, id client {}", idClient);

                            messageLayotDisplay.clearDisplay();
                        } else {
                            log.info("consumer get message, id client {}", idClient);
                            log.info("id client {}, message before decoding {} ", idClient, new String(con.value()));
                            log.info("id client {}, message after decoding {}", idClient, new String(CipherForDecoding.TextDecoding(con.value())));

                            Message concreteMessage = ParsingMassage.parsingMessage(new String(CipherForDecoding.TextDecoding(con.value())));
//                          // Message concreteMessage = ParsingMassage.parsingMessage(new String(con.value()));


                            if (concreteMessage != null && concreteMessage.getBytes() != null) {
                                log.info("message after parsing {}, id client {}", concreteMessage.convertToString(), idClient);
                                server.saveMessage(secondClientId, idClient, concreteMessage);
                                if (concreteMessage.getTypeFormat().equals("image")) {
                                    messageLayotDisplay.addImageMessageInDisplay(concreteMessage.getBytes(), concreteMessage.getFileName(), MessageLayotDisplay.Purpose.Another);
                                } else if (concreteMessage.getTypeFormat().equals("text")) {
                                    messageLayotDisplay.addTextMessageInDisplay(new String(concreteMessage.getBytes()), MessageLayotDisplay.Purpose.Another);
                                } else {
                                    messageLayotDisplay.addFileMessageInDisplay(concreteMessage.getBytes(), concreteMessage.getFileName(), MessageLayotDisplay.Purpose.Another);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
                log.error(Arrays.deepToString(ex.getStackTrace()));
            }
            kafkaConsumer.close();

        }

        private byte[] privateKeyGeneration() {

            return new BigInteger(100, random).toByteArray();
        }

        private byte[] publicKeyGeneration(byte[] p, byte[] g, byte[] privateKey) {
            BigInteger bigIntegerP = new BigInteger(p);
            BigInteger bigIntegerG = new BigInteger(g);
            BigInteger bigIntegerKey = new BigInteger(privateKey);
            return bigIntegerG.modPow(bigIntegerKey, bigIntegerP).toByteArray();
        }

        public void close() {

            flag = false;
        }
    }

    public class MessageLayotDisplay {
        private final VerticalLayout messageLayout;
        private final KafkaWriter kafkaWriter;

        public MessageLayotDisplay(VerticalLayout messageLayout, KafkaWriter kafkaWriter) {
            this.messageLayout = messageLayout;
            this.kafkaWriter = kafkaWriter;

        }

        public enum Purpose {
            This,
            Another
        }

        public void addTextMessageInDisplay(String message, Purpose purpose) {
            log.info("Message show {}, id client {}", message, idClient);
            var UiOpt = getUI();
            if (UiOpt.isPresent()) {
                UI ui = UiOpt.get();
                ui.access(() -> {
                    Div TextDiv = new Div();
                    TextDiv.setText(message);
                    if (purpose.equals(Purpose.Another)) {
                        TextDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    } else {
                        TextDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");
                        DeleteMessageFromThisClient(messageLayout, TextDiv);
                    }

                    TextDiv.getStyle()
                            .set("border-radius", "5px")
                            .set("padding", "10px")
                            .set("border", "1px solid #ddd");
                    messageLayout.add(TextDiv);
                    messageLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void addImageMessageInDisplay(byte[] data, String fileName, Purpose purpose) {
            log.info("Image show {}", idClient);
            var UiOpt = getUI();
            if (UiOpt.isPresent()) {
                UI ui = UiOpt.get();
                ui.access(() -> {
                    Div image = new Div();
                    StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(data));
                    Image im = new Image(resource, "failed to upload image");

                    image.add(im);
                    if (purpose.equals(Purpose.Another)) {
                        image.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    } else {
                        image.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");
                        DeleteMessageFromThisClient(messageLayout, image);
                    }
                    image.getStyle()
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("width", "60%")
                            .set("flex-shrink", "0");

                    image.getStyle()
                            .set("width", "100%")
                            .set("height", "100%");
                    messageLayout.add(image);
                    messageLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");

                });
            }
        }

        public void addFileMessageInDisplay(byte[] data, String fileName, Purpose purpose) {
            log.info("File show {}", idClient);
            var UiOpt = getUI();
            if (UiOpt.isPresent()) {
                UI ui = UiOpt.get();
                ui.access(() -> {
                    Div file = new Div();
                    StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(data));
                    Anchor anchor = new Anchor(resource, "");
                    anchor.getElement().setAttribute("download", true);
                    Button button = new Button(fileName, event -> anchor.getElement().callJsFunction("click"));
                    file.add(button, anchor);
                    if (purpose.equals(Purpose.Another)) {
                        file.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    } else {
                        file.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");
                        DeleteMessageFromThisClient(messageLayout, file);
                    }
                    file.getStyle()
                            .set("display", "inline-block")
                            .set("max-width", "80%")
                            .set("overflow", "hidden")
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("flex-shrink", "0");
                    messageLayout.add(file);
                    messageLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        private void DeleteMessageFromThisClient(VerticalLayout messageLayout, Div div) {
            messageLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
            div.addClickListener(event -> {
                int index = messageLayout.indexOf(div);
                messageLayout.remove(div);
                kafkaWriter.sendMessage(new Message("delete_message", "text", null, index, null).convertToBytes(), topicOutput);
            });

        }

        private void deleteMessage(int index) {
            var UiOpt = getUI();
            if (UiOpt.isPresent()) {
                UI ui = UiOpt.get();
                ui.access(() -> {
                    Component removingComponent = messageLayout.getComponentAt(index);
                    messageLayout.remove(removingComponent);
                });
            }
        }

        private void clearDisplay() {
            var UiOpt = getUI();
            if (UiOpt.isPresent()) {
                UI ui = UiOpt.get();
                ui.access(messageLayout::removeAll);
            }
        }

    }

    public class Frontend {
        private final TextField textField;
        private final List<Pair<String, InputStream>> filesArray = new ArrayList<>();

        public Frontend() {
            textField = new TextField();
            textField.setWidth("410px");
        }

        public void createPage() {
            setAlignItems(Alignment.CENTER);
            setSizeFull();
            setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            VerticalLayout messageLayout = getMessageVerticalLayout();
            HorizontalLayout inputHorizontalLayout = getInputHorizontalLayout();
            add(messageLayout, inputHorizontalLayout);
            messageLayotDisplay = new MessageLayotDisplay(messageLayout, kafkaWriter);

        }

        private VerticalLayout getMessageVerticalLayout() {
            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.getStyle()
                    .set("max-width", "620px")
                    .set("max-height", "500px")
                    .set("border", "1px dashed #4A90E2")
                    .set("border-radius", "5px")
                    .set("padding", "10px")
                    .set("overflow-y", "auto");

            verticalLayout.setWidth("620px");
            verticalLayout.setHeight("500px");

            return verticalLayout;
        }

        private HorizontalLayout getInputHorizontalLayout() {
            HorizontalLayout horizontalLayout = getHorizontalLayout();
            horizontalLayout.setWidth("700px");
            horizontalLayout.setAlignItems(Alignment.BASELINE);
            horizontalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
            horizontalLayout.setSpacing(true);
            horizontalLayout.getStyle().set("padding-left", "90px");

            VerticalLayout verticalLayout = new VerticalLayout(horizontalLayout);
            verticalLayout.setWidth("700px");

            verticalLayout.setAlignItems(Alignment.CENTER);
            verticalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
            verticalLayout.getStyle().set("position", "relative");

            return horizontalLayout;
        }

        private HorizontalLayout getHorizontalLayout() {
            Upload upload = getButton();
            Button button = new Button("Отправить");
            button.addClickListener(e -> send(upload));
            upload.getElement().getStyle().set("position", "absolute").set("top", "600px").set("left", "470x");
            add(upload);
            return new HorizontalLayout(textField, button);

        }

        public void send(Upload upload) {
            log.info("message is sent by the client id {}", idClient);
            if (cipherEncryption != null) {
                log.info("cipherEncryption not null");
                try {
                    for (var file : filesArray) {
                        log.info("send file, client id {}", idClient);
                        byte[] fileInByte = getByteArrayFromInputStream(file.getRight());
                        String type = getTypeFormat(file.getLeft());
                        Message message = new Message("message", type, file.getLeft(), 0, fileInByte);
                        byte[] messageInByte = message.convertToBytes();
                        log.info("the beginning of file encryption, id client {}", idClient);
                        kafkaWriter.sendMessage(cipherEncryption.encrypt(messageInByte), topicOutput);
                        log.info("the file is finally encrypted, id client {}", idClient);
                        server.saveMessage(idClient, secondClientId, message);
                        if (type.equals("image")) {
                            messageLayotDisplay.addImageMessageInDisplay(fileInByte, file.getLeft(), MessageLayotDisplay.Purpose.This);
                        } else {
                            messageLayotDisplay.addFileMessageInDisplay(fileInByte, file.getLeft(), MessageLayotDisplay.Purpose.This);
                        }

                    }
                    filesArray.clear();
                    upload.clearFileList();
                    String message = textField.getValue();
                    if (!message.isEmpty()) {
                        log.info("Message from field for encryption: {}, client {}", message, idClient);
                        Message result = new Message("message", "text", "text", 0, message.getBytes());
                        log.info("Message in class Message for encryption {}", result.convertToString());
                        byte[] resultBytes = result.convertToBytes();
                        log.info("the beginning of text encryption, id client {}", idClient);
                        kafkaWriter.sendMessage(cipherEncryption.encrypt(resultBytes), topicOutput);
                        log.info("the text is finally encrypted, id client {}", idClient);
//                        // kafkaWriter.sendMessage(resultBytes, topicOutput);
                        server.saveMessage(idClient, secondClientId, result);
                        messageLayotDisplay.addTextMessageInDisplay(message, MessageLayotDisplay.Purpose.This);
                    }
                    textField.clear();

                } catch (IOException | RuntimeException ex) {
                    log.error(ex.getMessage());
                    log.error(Arrays.deepToString(ex.getStackTrace()));
                }
            } else {
                Notification.show("Не удалось отправить сообщение");
            }
        }

        private String getTypeFormat(String file) {
            int index = file.lastIndexOf('.');
            String type = file.substring(index + 1);
            if (type.equals("jpg") || type.equals("png") || type.equals("jpeg")) {
                return "image";
            }
            return "other";
        }

        private byte[] getByteArrayFromInputStream(InputStream inputStream) throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            int flag;
            byte[] temporary = new byte[1024];
            while ((flag = inputStream.read(temporary, 0, temporary.length)) > 0) {
                result.write(temporary, 0, flag);
            }
            result.flush();
            return result.toByteArray();
        }

        private Upload getButton() {
            MultiFileMemoryBuffer multiFileMemoryBuffer = new MultiFileMemoryBuffer();
            Upload buttonUpload = new Upload(multiFileMemoryBuffer);
            Button button = new Button("+");
            buttonUpload.setUploadButton(button);
            buttonUpload.setWidth("620px");
            buttonUpload.getStyle()
                    .set("padding", "0")
                    .set("margin", "0")
                    .set("border", "none");
            buttonUpload.setDropLabel(new Span(""));
            buttonUpload.setDropLabelIcon(new Span(""));
            buttonUpload.addSucceededListener(event -> {
                String file = event.getFileName();
                filesArray.add(Pair.of(file, multiFileMemoryBuffer.getInputStream(file)));
            });
            return buttonUpload;
        }
    }
}
