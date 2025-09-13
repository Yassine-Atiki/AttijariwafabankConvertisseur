package v1.attijariconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam; // ajout
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import v1.attijariconverter.service.ConversionService;
import org.springframework.data.domain.Page; // ajout
import v1.attijariconverter.model.ConversionHistory; // ajout

@Controller
public class DashboardController {

    @Autowired
    private ConversionService conversionService;

    private static final int PAGE_SIZE = 10; // taille page par défaut

    private boolean isAdmin(Authentication auth){
        if(auth==null) return false;
        for(GrantedAuthority ga: auth.getAuthorities()){
            if("ROLE_ADMIN".equals(ga.getAuthority())) return true;
        }
        return false;
    }

    private void populateModel(Model model, int historyPage, int otherUsersPage) {
        // Historique personnel paginé
        Page<ConversionHistory> history = conversionService.getConversionHistoryPaginated(historyPage, PAGE_SIZE);
        model.addAttribute("conversionHistoryPaginated", history.getContent());
        model.addAttribute("currentHistoryPage", history.getNumber());
        model.addAttribute("totalHistoryPages", history.getTotalPages());
        model.addAttribute("totalHistoryElements", history.getTotalElements());
        model.addAttribute("hasPrevHistory", history.hasPrevious());
        model.addAttribute("hasNextHistory", history.hasNext());

        // Compteurs & listes dérivées
        model.addAttribute("totalConversions", conversionService.getTotalConversions());
        model.addAttribute("validConversions", conversionService.getSuccessfulConversions());
        model.addAttribute("invalidConversions", conversionService.getFailedConversions());
        model.addAttribute("failedConversionsList", conversionService.getInvalidConversions());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            model.addAttribute("username", auth.getName());
            boolean admin = isAdmin(auth);
            model.addAttribute("isAdmin", admin);
            if(admin){
                // Historique des autres utilisateurs paginé
                Page<ConversionHistory> others = conversionService.getOtherUsersHistoryPaginated(otherUsersPage, PAGE_SIZE);
                model.addAttribute("otherUsersHistory", others.getContent());
                model.addAttribute("currentOtherUsersPage", others.getNumber());
                model.addAttribute("totalOtherUsersPages", others.getTotalPages());
                model.addAttribute("totalOtherUsersElements", others.getTotalElements());
                model.addAttribute("hasPrevOtherUsers", others.hasPrevious());
                model.addAttribute("hasNextOtherUsers", others.hasNext());
                // Liste des utilisateurs (sélecteur admin)
                model.addAttribute("allUsers", conversionService.getAllUsers());
            }
        }
    }

    @GetMapping("/")
    public String dashboard(Model model,
                             @RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
                             @RequestParam(value = "otherUsersPage", defaultValue = "0") int otherUsersPage) {
        populateModel(model, Math.max(historyPage,0), Math.max(otherUsersPage,0));
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardAlt(Model model,
                                @RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
                                @RequestParam(value = "otherUsersPage", defaultValue = "0") int otherUsersPage) {
        populateModel(model, Math.max(historyPage,0), Math.max(otherUsersPage,0));
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
