package com.example.crypto.Repository;

import com.example.crypto.model.MessageInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends CrudRepository<MessageInfo, String> {
}
