package com.example.crypto.Repository;

import com.example.crypto.model.CipherInfo;
import com.example.crypto.model.ClientInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ClientInfoRepository extends CrudRepository<ClientInfo, Long> {
    @Transactional
    default ClientInfo addRoom(long id, long idRoom) {
        ClientInfo c = findById(id).orElse(null);
        if (c != null) {
            long[] newRoom = new long[c.getRooms().length + 1];
            System.arraycopy(c.getRooms(), 0, newRoom, 0, c.getRooms().length);
            newRoom[c.getRooms().length] = idRoom;
            c.setRooms(newRoom);
            return save(c);
        }
        return null;

    }

    @Transactional
    default ClientInfo removeRoom(long id, long idRoom) {
        ClientInfo c = findById(id).orElse(null);
        if (c != null) {
            long[] newRoom = new long[c.getRooms().length - 1];
            int index = 0;
            for (int i = 0; i < c.getRooms().length; i++) {
                if (c.getRooms()[i] != idRoom) {
                    newRoom[index++] = c.getRooms()[i];
                }
            }
            c.setRooms(newRoom);
            return save(c);
        }
        return null;

    }
}
