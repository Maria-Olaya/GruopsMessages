package eafit.gruopChat.presence.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.presence.dto.PresenceEventDTO;
import eafit.gruopChat.presence.service.PresenceService;

@RestController
public class PresenceController {

    private final PresenceService       presenceService;
    private final GroupMemberRepository memberRepository;

    public PresenceController(PresenceService presenceService,
                              GroupMemberRepository memberRepository) {
        this.presenceService  = presenceService;
        this.memberRepository = memberRepository;
    }

    // GET /api/presence/group/{groupId}
    // Devuelve el estado online/offline de todos los miembros del grupo
    // El frontend lo llama al abrir un grupo para el estado inicial
    @GetMapping("/api/presence/group/{groupId}")
    public ResponseEntity<List<PresenceEventDTO>> getGroupPresence(
            @PathVariable Long groupId) {

        List<PresenceEventDTO> presence = memberRepository
                .findByGroupGroupId(groupId)
                .stream()
                .map(m -> {
                    Long uid = m.getUser().getUserId();
                    boolean online = presenceService.isOnline(uid);
                    return new PresenceEventDTO(
                            uid,
                            m.getUser().getName(),
                            online,
                            online ? null : presenceService.getLastSeen(uid)
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(presence);
    }
}