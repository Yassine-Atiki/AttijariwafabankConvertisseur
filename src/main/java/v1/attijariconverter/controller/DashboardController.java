package v1.attijariconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import v1.attijariconverter.service.ConversionService;

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

    private void populateModel(Model model) {
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
            if(admin){
                model.addAttribute("otherUsersHistory", conversionService.getOtherUsersHistory());
            }
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
