package com.ecsimsw.gateway.domain;

import org.springframework.data.repository.CrudRepository;

public interface BURepo extends CrudRepository<BlockedUser, String> {
}
