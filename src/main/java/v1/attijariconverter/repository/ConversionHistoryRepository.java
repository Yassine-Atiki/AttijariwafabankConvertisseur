package v1.attijariconverter.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import v1.attijariconverter.model.ConversionHistory;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversionHistoryRepository extends MongoRepository<ConversionHistory, String> {

    List<ConversionHistory> findByConversionDateBetween(LocalDateTime start, LocalDateTime end);

    List<ConversionHistory> findByStatusOrderByConversionDateDesc(String status);

    List<ConversionHistory> findTop10ByOrderByConversionDateDesc();

    List<ConversionHistory> findByOwnerUsernameOrderByConversionDateDesc(String ownerUsername);

    List<ConversionHistory> findByOwnerUsernameAndStatusOrderByConversionDateDesc(String ownerUsername, String status);

    List<ConversionHistory> findTop10ByOwnerUsernameOrderByConversionDateDesc(String ownerUsername);

    List<ConversionHistory> findByOwnerUsernameAndConversionDateBetween(String ownerUsername, LocalDateTime start, LocalDateTime end);

    List<ConversionHistory> findByOwnerUsernameNotOrderByConversionDateDesc(String ownerUsername);

    long deleteByOwnerUsername(String ownerUsername);

    long deleteByOwnerUsernameIsNull();

    List<ConversionHistory> findByOwnerUsernameIsNull();

    // Méthodes de pagination ajoutées
    Page<ConversionHistory> findByOwnerUsername(String ownerUsername, Pageable pageable);

    Page<ConversionHistory> findByOwnerUsernameNot(String ownerUsername, Pageable pageable);

    // Méthodes pour statistiques admin
    @Query(value = "{}", count = true)
    long countDistinctOwnerUsername();

    @Query(value = "{'status': ?0}", count = true)
    long countDistinctOwnerUsernameByStatus(String status);
}
