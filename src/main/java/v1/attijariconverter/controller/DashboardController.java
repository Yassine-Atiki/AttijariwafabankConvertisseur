package v1.attijariconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import v1.attijariconverter.service.ConversionService;
import v1.attijariconverter.model.ConversionHistory;

@Controller
public class DashboardController {

    @Autowired
    private ConversionService conversionService;

    private boolean isAdmin(Authentication auth){
        if(auth==null) return false;
        for(GrantedAuthority ga: auth.getAuthorities()){
            if("ROLE_ADMIN".equals(ga.getAuthority())) return true;
        }
        return false;
    }

    private void populateModel(Model model, int otherUsersPage, int historyPage) {
        model.addAttribute("conversionHistory", conversionService.getConversionHistory());
        model.addAttribute("totalConversions", conversionService.getTotalConversions());
        model.addAttribute("validConversions", conversionService.getSuccessfulConversions());
        model.addAttribute("invalidConversions", conversionService.getFailedConversions());
        model.addAttribute("failedConversionsList", conversionService.getInvalidConversions());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            model.addAttribute("username", auth.getName());
            boolean admin = isAdmin(auth);
            model.addAttribute("isAdmin", admin);

            // Pagination pour l'historique personnel (10 par page)
            Page<ConversionHistory> historyPage_obj = conversionService.getConversionHistoryPaginated(historyPage, 10);
            model.addAttribute("conversionHistoryPage", historyPage_obj);
            model.addAttribute("conversionHistoryPaginated", historyPage_obj.getContent());
            model.addAttribute("currentHistoryPage", historyPage);
            model.addAttribute("totalHistoryPages", historyPage_obj.getTotalPages());
            model.addAttribute("hasNextHistory", historyPage_obj.hasNext());
            model.addAttribute("hasPrevHistory", historyPage_obj.hasPrevious());
            model.addAttribute("totalHistoryElements", historyPage_obj.getTotalElements());

            if(admin){
                // Pagination pour les autres utilisateurs (10 par page)
                Page<ConversionHistory> otherUsersHistoryPage = conversionService.getOtherUsersHistoryPaginated(otherUsersPage, 10);
                model.addAttribute("otherUsersHistoryPage", otherUsersHistoryPage);
                model.addAttribute("otherUsersHistory", otherUsersHistoryPage.getContent());
                model.addAttribute("currentOtherUsersPage", otherUsersPage);
                model.addAttribute("totalOtherUsersPages", otherUsersHistoryPage.getTotalPages());
                model.addAttribute("hasNextOtherUsers", otherUsersHistoryPage.hasNext());
                model.addAttribute("hasPrevOtherUsers", otherUsersHistoryPage.hasPrevious());
                model.addAttribute("totalOtherUsersElements", otherUsersHistoryPage.getTotalElements());

                // Statistiques utilisateurs pour admin
                model.addAttribute("totalActiveUsers", conversionService.getTotalActiveUsers());
                model.addAttribute("totalUsersWithSuccess", conversionService.getTotalUsersWithSuccess());
                model.addAttribute("totalUsersWithErrors", conversionService.getTotalUsersWithErrors());
            }
        }
    }

    @GetMapping("/")
    public String dashboard(Model model,
                           @RequestParam(defaultValue = "0") int otherUsersPage,
                           @RequestParam(defaultValue = "0") int historyPage) {
        populateModel(model, otherUsersPage, historyPage);
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardAlt(Model model,
                              @RequestParam(defaultValue = "0") int otherUsersPage,
                              @RequestParam(defaultValue = "0") int historyPage) {
        populateModel(model, otherUsersPage, historyPage);
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
