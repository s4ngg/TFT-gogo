package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientVersionPatchMappingRepository extends JpaRepository<ClientVersionPatchMapping, Long> {

    Optional<ClientVersionPatchMapping> findByClientVersion(String clientVersion);

    boolean existsByClientVersion(String clientVersion);

    List<ClientVersionPatchMapping> findAllByOrderByClientVersionAsc();
}
