package v1.attijariconverter.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import v1.attijariconverter.model.ConversionHistory;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversionHistoryRepository extends MongoRepository<ConversionHistory, String> {

    List<ConversionHistory> findByConversionDateBetween(LocalDateTime start, LocalDateTime end);

    List<ConversionHistory> findByStatusOrderByConversionDateDesc(String status);

    List<ConversionHistory> findTop10ByOrderByConversionDateDesc();
}
