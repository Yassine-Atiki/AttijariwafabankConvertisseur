package v1.attijariconverter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import v1.attijariconverter.model.ConversionHistory;
import v1.attijariconverter.repository.ConversionHistoryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrôleur REST fournissant des statistiques journalières filtrées par utilisateur.
 * (Actuellement, l'accès est autorisé à tous selon SecurityConfig, mais la sélection se fait via SecurityContext.)
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private ConversionHistoryRepository conversionHistoryRepository;

    /**
     * DTO minimal pour retourner les stats du jour demandé.
     * total = nombre total de conversions
     * valid = nombre de conversions succès
     * error = nombre de conversions en erreur
     */
    public static class DayStats {
        private long total;
        private long valid;
        private long error;

        public DayStats(long total, long valid, long error) {
            this.total = total;
            this.valid = valid;
            this.error = error;
        }

        public long getTotal() { return total; }
        public long getValid() { return valid; }
        public long getError() { return error; }
    }

    public static class RangeStats {
        private long total;
        private long valid;
        private long error;

        public RangeStats(long total, long valid, long error) {
            this.total = total;
            this.valid = valid;
            this.error = error;
        }
        public long getTotal() { return total; }
        public long getValid() { return valid; }
        public long getError() { return error; }
    }

    /**
     * Retourne les statistiques (total / SUCCESS / ERROR) pour l'utilisateur courant sur une date donnée.
     * @param dateStr format ISO (YYYY-MM-DD)
     */
    @GetMapping("/by-date")
    public ResponseEntity<DayStats> getStatsByDate(@RequestParam("date") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";

        List<ConversionHistory> list = conversionHistoryRepository
                .findByOwnerUsernameAndConversionDateBetween(username, start, end);
        long total = list.size();
        long valid = list.stream().filter(h -> "SUCCESS".equalsIgnoreCase(h.getStatus())).count();
        long error = list.stream().filter(h -> "ERROR".equalsIgnoreCase(h.getStatus())).count();

        return ResponseEntity.ok(new DayStats(total, valid, error));
    }

    @GetMapping("/by-range")
    public ResponseEntity<RangeStats> getStatsByRange(@RequestParam("from") String fromStr,
                                                      @RequestParam("to") String toStr) {
        LocalDate from = LocalDate.parse(fromStr);
        LocalDate to = LocalDate.parse(toStr);
        if (to.isBefore(from)) {
            // Inverser si nécessaire
            LocalDate tmp = from; from = to; to = tmp;
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";

        List<ConversionHistory> list = conversionHistoryRepository
                .findByOwnerUsernameAndConversionDateBetween(username, start, endExclusive);
        long total = list.size();
        long valid = list.stream().filter(h -> "SUCCESS".equalsIgnoreCase(h.getStatus())).count();
        long error = list.stream().filter(h -> "ERROR".equalsIgnoreCase(h.getStatus())).count();

        return ResponseEntity.ok(new RangeStats(total, valid, error));
    }
}
