package v1.attijariconverter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import v1.attijariconverter.service.ConversionService;

@RestController
@RequestMapping("/api/admin/history")
public class AdminHistoryController {

    @Autowired
    private ConversionService conversionService;

    private boolean isAdmin(Authentication auth){
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @DeleteMapping("/self")
    public ResponseEntity<?> deleteOwn(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!isAdmin(auth)) return ResponseEntity.status(403).body("Accès refusé");
        long count = conversionService.deleteOwnHistory();
        return ResponseEntity.ok("Historique personnel supprimé: " + count + " entrées");
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!isAdmin(auth)) return ResponseEntity.status(403).body("Accès refusé");
        long count = conversionService.deleteHistoryForUser(username);
        return ResponseEntity.ok("Historique de '"+username+"' supprimé: " + count + " entrées");
    }

    @DeleteMapping("/entry/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable String id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!isAdmin(auth)) return ResponseEntity.status(403).body("Accès refusé");
        boolean ok = conversionService.deleteHistoryEntry(id);
        if(ok) return ResponseEntity.ok("Entrée supprimée: " + id);
        return ResponseEntity.status(404).body("Entrée introuvable: " + id);
    }
}
