package v1.attijariconverter.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import v1.attijariconverter.model.ConversionHistory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository Spring Data Mongo pour la collection conversion_history.
 * Méthodes dérivées générées automatiquement par convention de nommage.
 */
@Repository
public interface ConversionHistoryRepository extends MongoRepository<ConversionHistory, String> {

    /** Trouve toutes les conversions entre deux instants (tous utilisateurs). */
    List<ConversionHistory> findByConversionDateBetween(LocalDateTime start, LocalDateTime end);

    /** Trouve par status global, tri décroissant. */
    List<ConversionHistory> findByStatusOrderByConversionDateDesc(String status);

    /** 10 dernières conversions globales. */
    List<ConversionHistory> findTop10ByOrderByConversionDateDesc();

    /** Conversions d'un utilisateur (tri descendant). */
    List<ConversionHistory> findByOwnerUsernameOrderByConversionDateDesc(String ownerUsername);

    /** Conversions d'un utilisateur filtrées par status. */
    List<ConversionHistory> findByOwnerUsernameAndStatusOrderByConversionDateDesc(String ownerUsername, String status);

    /** 10 dernières conversions d'un utilisateur. */
    List<ConversionHistory> findTop10ByOwnerUsernameOrderByConversionDateDesc(String ownerUsername);

    /** Conversions d'un utilisateur sur intervalle temporel. */
    List<ConversionHistory> findByOwnerUsernameAndConversionDateBetween(String ownerUsername, LocalDateTime start, LocalDateTime end);

    /** Historique de tous les autres utilisateurs (vue admin). */
    List<ConversionHistory> findByOwnerUsernameNotOrderByConversionDateDesc(String ownerUsername);

    /** Suppression massive par owner. */
    long deleteByOwnerUsername(String ownerUsername);

    /** Suppression des entrées orphelines (sans owner). */
    long deleteByOwnerUsernameIsNull();

    /** Recherche des entrées orphelines (diagnostic / migration). */
    List<ConversionHistory> findByOwnerUsernameIsNull();

    // Méthodes de pagination ajoutées
    Page<ConversionHistory> findByOwnerUsername(String ownerUsername, Pageable pageable);

    Page<ConversionHistory> findByOwnerUsernameNot(String ownerUsername, Pageable pageable);

    // Méthodes pour statistiques admin
    @Query(value = "{}", count = true)
    long countDistinctOwnerUsername();

    @Query(value = "{'status': ?0}", count = true)
    long countDistinctOwnerUsernameByStatus(String status);

    // Nouvelles méthodes pour les statistiques par utilisateur
    @Query(value = "{}", fields = "{ 'ownerUsername' : 1, '_id' : 0 }")
    List<ConversionHistory> findAllWithUsernameOnly();

    long countByOwnerUsername(String ownerUsername);

    long countByOwnerUsernameAndStatus(String ownerUsername, String status);

    List<ConversionHistory> findTop5ByOwnerUsernameOrderByConversionDateDesc(String ownerUsername);

    long countByOwnerUsernameAndConversionDateBetween(String ownerUsername, LocalDateTime start, LocalDateTime end);

    long countByOwnerUsernameAndStatusAndConversionDateBetween(String ownerUsername, String status, LocalDateTime start, LocalDateTime end);
}
