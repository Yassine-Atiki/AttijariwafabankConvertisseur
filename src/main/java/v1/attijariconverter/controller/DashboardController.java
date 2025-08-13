package v1.attijariconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import v1.attijariconverter.service.ConversionService;

@Controller
public class DashboardController {

    @Autowired
    private ConversionService conversionService;

    private void populateModel(Model model) {
        model.addAttribute("conversionHistory", conversionService.getConversionHistory());
        model.addAttribute("totalConversions", conversionService.getTotalConversions());
        model.addAttribute("validConversions", conversionService.getSuccessfulConversions());
        model.addAttribute("invalidConversions", conversionService.getFailedConversions());
        // Liste détaillée des erreurs pour Rapports & Logs
        model.addAttribute("failedConversionsList", conversionService.getInvalidConversions());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            model.addAttribute("username", auth.getName());
        }
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        populateModel(model);
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardAlt(Model model) {
        populateModel(model);
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
