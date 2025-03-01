package com.example.crypto;

import com.example.crypto.Kafka.KafkaWriter;
import com.example.crypto.Repository.CipherInfoRepository;
import com.example.crypto.Repository.ClientInfoRepository;
import com.example.crypto.Repository.MessageInfoRepository;
import com.example.crypto.Repository.RoomInfoRepository;
import com.example.crypto.cipher.algoritm.DiffieHellman;
import com.example.crypto.model.*;
import com.vaadin.flow.component.UI;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.select.Evaluator;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class Server {
    private final CipherInfoRepository cipherInfoRepository;
    private final ClientInfoRepository clientInfoRepository;
    private final MessageInfoRepository messageInfoRepository;
    private final RoomInfoRepository roomInfoRepository;
    private final KafkaWriter kafkaWriter;

    private static final Random random = new Random();
    private static final Map<String, UI> windows = new HashMap<>();
    private static final Map<Long, Pair<Long, Long>> room = new HashMap<>();

    public Server(ClientInfoRepository clientInfoRepository, CipherInfoRepository cipherInfoRepository, MessageInfoRepository messageInfoRepository, RoomInfoRepository roomInfoRepository, KafkaWriter kafkaWriter) {
        this.clientInfoRepository = clientInfoRepository;
        this.kafkaWriter = kafkaWriter;
        this.cipherInfoRepository = cipherInfoRepository;
        this.roomInfoRepository = roomInfoRepository;
        this.messageInfoRepository = messageInfoRepository;
    }

    public synchronized ClientInfo saveClient(String name, String nameAlgo, String namePadding, String nameMode) {
        CipherInfo cipherInfo = cipherInfoRepository.save(getCipher(nameAlgo, namePadding, nameMode));
        return clientInfoRepository.save(ClientInfo.builder().idCipherInfo(cipherInfo.getId())
                .rooms(new long[0]).name(name).build());

    }

    public synchronized boolean joiningRoom(long idClient, long idRoom) {
        var client = clientInfoRepository.findById(idClient);
        if (client.isPresent()) {
            if (room.containsKey(idRoom)) {
                Pair<Long, Long> roomForConnection = room.get(idRoom);
                if ((roomForConnection.getLeft() == null || roomForConnection.getRight() == null) &&
                        !((roomForConnection.getRight() != null && roomForConnection.getRight() == idClient)
                                || (roomForConnection.getLeft() != null && roomForConnection.getLeft() == idClient))) {
                    long secondClient;
                    if (roomForConnection.getLeft() == null) {
                        secondClient = roomForConnection.getRight();
                    } else {
                        secondClient = roomForConnection.getLeft();
                    }
                    room.put(idRoom, Pair.of(idClient, secondClient));
                    ClientInfo clientInfo = clientInfoRepository.addRoom(idClient, idRoom);
                    if (clientInfo == null) {
                        return false;
                    }
                    return runProcessing(idRoom, idClient, secondClient);

                }
                return false;

            } else {
                if (!roomInfoRepository.existsRoomInfoByRoomId(idRoom)) {
                    BigInteger[] df = DiffieHellman.generateParameters(300);
                    byte[] p = df[0].toByteArray();
                    byte[] g = df[1].toByteArray();
                    roomInfoRepository.save(RoomInfo.builder().roomId(idRoom).p(p).g(g).build());
                }
                room.put(idRoom, Pair.of(idClient, null));
                clientInfoRepository.addRoom(idClient, idRoom);
                return true;

            }
        }
        return false;
    }

    private CipherInfo getCipher(String nameAlgo, String namePadding, String nameMode) {
        return switch (nameAlgo) {
            case "RC6" -> CipherInfo.builder().nameAlgorithm(nameAlgo).encryptionMode(nameMode).namePadding(namePadding)
                    .sizeBlockInBits(128).sizeKeyInBits(128).initializationVector(generateVector(16)).build();
            case "Twofish" ->
                    CipherInfo.builder().nameAlgorithm(nameAlgo).encryptionMode(nameMode).namePadding(namePadding)
                            .sizeBlockInBits(128).sizeKeyInBits(128).initializationVector(generateVector(16)).build();
            default -> throw new IllegalStateException("Unexpected value: " + nameAlgo);
        };
    }

    private byte[] generateVector(int size) {
        byte[] vector = new byte[size];
        for (int i = 0; i < size; i++) {
            vector[i] = (byte) random.nextInt(128);
        }
        return vector;
    }

    public CipherInfo getCipherByClientId(long IdClient) {
        var client = clientInfoRepository.findById(IdClient);
        if (client.isPresent()) {
            ClientInfo clientInfo = client.get();
            return cipherInfoRepository.findById(clientInfo.getIdCipherInfo()).orElse(null);
        }
        return null;
    }

    private boolean runProcessing(long IdRoom, long IdFirstClient, long IdSecondClient) {
        CipherInfo FirstCipherInfo = getCipherByClientId(IdFirstClient);
        CipherInfo SecondCipherInfo = getCipherByClientId(IdSecondClient);
        String firstOutputTopic = "input_" + IdSecondClient + "_" + IdRoom;
        String SecondOutputTopic = "input_" + IdFirstClient + "_" + IdRoom;
        RoomInfo roomInfo = getRoomInfoByRoomId(IdRoom);
        if (FirstCipherInfo != null && SecondCipherInfo != null && roomInfo != null) {
            CipherInfoMassage FirstMessage = new CipherInfoMassage(FirstCipherInfo, roomInfo, IdFirstClient);
            CipherInfoMassage SecondMessage = new CipherInfoMassage(SecondCipherInfo, roomInfo, IdSecondClient);
            kafkaWriter.sendMessage(FirstMessage.convertToBytes(), firstOutputTopic);
            kafkaWriter.sendMessage(SecondMessage.convertToBytes(), SecondOutputTopic);
            return true;
        }
        return false;
    }

    public RoomInfo getRoomInfoByRoomId(long IdRoom) {
        return roomInfoRepository.getRoomInfoByRoomId(IdRoom).orElse(null);
    }

    public synchronized void leaveRoom(long idClient, long idRoom) {
        if (room.containsKey(idRoom)) {
            Pair<Long, Long> visitorRoom = room.get(idRoom);
            if ((visitorRoom.getRight() != null && visitorRoom.getRight() == idClient)) {
                room.put(idRoom, Pair.of(visitorRoom.getLeft(), null));
            } else if (visitorRoom.getLeft() != null && visitorRoom.getLeft() == idClient) {
                room.put(idRoom, Pair.of(null, visitorRoom.getRight()));
            } else {
                return;
            }
            Pair<Long, Long> newRoom = room.get(idRoom);
            if (newRoom.getLeft() == null && newRoom.getRight() == null) {
                room.remove(idRoom);
            }
            String url = "room/" + idClient + "/" + idRoom;
            UI ui = windows.get(url);
            if (ui != null) {
                ui.getPage().executeJs("window.close()");
                deleteWindow(url);
            }
            clientInfoRepository.removeRoom(idClient, idRoom);
        }
    }

    public synchronized void deleteWindow(String url) {
        windows.remove(url);
    }

    public boolean checkNoExistClient(long idClient) {
        return !clientInfoRepository.existsById(idClient);
    }

    public synchronized void saveMessage(long output, long input, Message message) {
        messageInfoRepository.save(MessageInfo.builder().to(input).from(output).message(message).build());
    }

    public CipherInfoMassage getCipherInfoMessageForClient(long idClient, long idRoom) {
        RoomInfo roomInfo = getRoomInfoByRoomId(idRoom);
        CipherInfo cipherInfo = getCipherByClientId(idClient);
        if (roomInfo != null && cipherInfo != null) {
            return new CipherInfoMassage(cipherInfo, roomInfo, idClient);
        }
        return null;
    }

    public synchronized void insertWindow(String Url, UI ui) {
        windows.put(Url, ui);
    }

    public synchronized boolean noContainsWindow(String Url) {
        return !windows.containsKey(Url);
    }

}
