package com.example.crypto.Repository;

import com.example.crypto.model.RoomInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomInfoRepository extends CrudRepository<RoomInfo, Long> {
    boolean existsRoomInfoByRoomId(long roomId);

    Optional<RoomInfo> getRoomInfoByRoomId(long roomId);
}
