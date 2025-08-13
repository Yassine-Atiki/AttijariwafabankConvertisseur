package v1.attijariconverter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import v1.attijariconverter.model.ConversionHistory;
import v1.attijariconverter.service.ConversionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private ConversionService conversionService;

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            List<ConversionHistory> allHistory = conversionService.getConversionHistory();
            List<ConversionHistory> validConversions = conversionService.getValidConversions();
            List<ConversionHistory> invalidConversions = conversionService.getInvalidConversions();

            model.addAttribute("totalConversions", allHistory.size());
            model.addAttribute("validConversions", validConversions.size());
            model.addAttribute("invalidConversions", invalidConversions.size());

            // Ajouter les données pour l'historique et les rapports
            model.addAttribute("conversionHistory", allHistory);
            model.addAttribute("validFiles", validConversions);
            model.addAttribute("invalidFiles", invalidConversions);

        } catch (Exception e) {
            // En cas d'erreur, initialiser avec des valeurs par défaut
            model.addAttribute("totalConversions", 0);
            model.addAttribute("validConversions", 0);
            model.addAttribute("invalidConversions", 0);
            model.addAttribute("conversionHistory", List.of());
            model.addAttribute("validFiles", List.of());
            model.addAttribute("invalidFiles", List.of());
        }

        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardAlt(Model model) {
        return dashboard(model);
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // API pour les données de graphiques
    @GetMapping("/api/dashboard/stats/daily")
    @ResponseBody
    public Map<String, Object> getDailyStats() {
        try {
            List<ConversionHistory> allHistory = conversionService.getAllConversions();

            // Grouper par jour
            Map<String, Long> dailyTotal = allHistory.stream()
                .collect(Collectors.groupingBy(
                    h -> h.getConversionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    Collectors.counting()
                ));

            Map<String, Long> dailySuccess = allHistory.stream()
                .filter(h -> "SUCCESS".equals(h.getStatus()))
                .collect(Collectors.groupingBy(
                    h -> h.getConversionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    Collectors.counting()
                ));

            Map<String, Long> dailyError = allHistory.stream()
                .filter(h -> h.getStatus().startsWith("ERROR"))
                .collect(Collectors.groupingBy(
                    h -> h.getConversionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    Collectors.counting()
                ));

            // Créer les listes ordonnées par date
            List<String> dates = dailyTotal.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

            List<Long> totalCounts = dates.stream()
                .map(date -> dailyTotal.getOrDefault(date, 0L))
                .collect(Collectors.toList());

            List<Long> successCounts = dates.stream()
                .map(date -> dailySuccess.getOrDefault(date, 0L))
                .collect(Collectors.toList());

            List<Long> errorCounts = dates.stream()
                .map(date -> dailyError.getOrDefault(date, 0L))
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("total", totalCounts);
            result.put("success", successCounts);
            result.put("error", errorCounts);

            return result;
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("dates", Arrays.asList());
            errorResult.put("total", Arrays.asList());
            errorResult.put("success", Arrays.asList());
            errorResult.put("error", Arrays.asList());
            return errorResult;
        }
    }

    @GetMapping("/api/dashboard/stats/hourly")
    @ResponseBody
    public Map<String, Object> getHourlyStats() {
        try {
            List<ConversionHistory> todayHistory = conversionService.getTodayConversions();

            // Grouper par heure (0-23)
            Map<Integer, Long> hourlyTotal = todayHistory.stream()
                .collect(Collectors.groupingBy(
                    h -> h.getConversionDate().getHour(),
                    Collectors.counting()
                ));

            Map<Integer, Long> hourlySuccess = todayHistory.stream()
                .filter(h -> "SUCCESS".equals(h.getStatus()))
                .collect(Collectors.groupingBy(
                    h -> h.getConversionDate().getHour(),
                    Collectors.counting()
                ));

            // Créer les données pour 24 heures
            List<String> hours = new ArrayList<>();
            List<Long> totalCounts = new ArrayList<>();
            List<Long> successCounts = new ArrayList<>();

            for (int i = 0; i < 24; i++) {
                hours.add(String.format("%02d:00", i));
                totalCounts.add(hourlyTotal.getOrDefault(i, 0L));
                successCounts.add(hourlySuccess.getOrDefault(i, 0L));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("hours", hours);
            result.put("total", totalCounts);
            result.put("success", successCounts);

            return result;
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("hours", Arrays.asList());
            errorResult.put("total", Arrays.asList());
            errorResult.put("success", Arrays.asList());
            return errorResult;
        }
    }
}
