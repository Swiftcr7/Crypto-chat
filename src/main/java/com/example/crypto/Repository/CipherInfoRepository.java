package com.example.crypto.Repository;

import com.example.crypto.model.CipherInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CipherInfoRepository extends CrudRepository<CipherInfo, Long> {
}