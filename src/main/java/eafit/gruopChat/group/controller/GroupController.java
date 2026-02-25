package eafit.gruopChat.group.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import eafit.gruopChat.group.dto.*;
import eafit.gruopChat.group.service.GroupService;
import eafit.gruopChat.shared.enums.GroupRole;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // ================= GRUPOS =================

    @PostMapping
    public ResponseEntity<GroupResponseDTO> createGroup(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GroupRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(userId, request));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> getMyGroups(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(groupService.getGroupsByMember(userId));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> updateGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GroupRequestDTO request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, userId, request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long userId) {
        groupService.deleteGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    // ================= MIEMBROS =================

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponseDTO>> getMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getMembers(groupId));
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal Long userId) {
        groupService.removeMember(groupId, userId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/members/{targetUserId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal Long userId,
            @RequestParam GroupRole role) {
        groupService.changeGroupRole(groupId, userId, targetUserId, role);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long userId) {
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    // ================= INVITACIONES =================

    @PostMapping("/{groupId}/invitations")
    public ResponseEntity<InvitationResponseDTO> sendInvitation(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long userId,
            @RequestParam Long invitedUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.sendInvitation(groupId, userId, invitedUserId));
    }

    @PatchMapping("/invitations/{invitationId}")
    public ResponseEntity<InvitationResponseDTO> respondToInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal Long userId,
            @RequestParam boolean accept) {
        return ResponseEntity.ok(groupService.respondToInvitation(invitationId, userId, accept));
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<InvitationResponseDTO>> getPendingInvitations(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(groupService.getPendingInvitations(userId));
    }

    // ================= CANALES =================

    @PostMapping("/{groupId}/channels")
    public ResponseEntity<ChannelResponseDTO> createChannel(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChannelRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createChannel(groupId, userId, request));
    }

    @GetMapping("/{groupId}/channels")
    public ResponseEntity<List<ChannelResponseDTO>> getChannels(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getChannels(groupId));
    }

    @PutMapping("/channels/{channelId}")
    public ResponseEntity<ChannelResponseDTO> updateChannel(
            @PathVariable Long channelId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChannelRequestDTO request) {
        return ResponseEntity.ok(groupService.updateChannel(channelId, userId, request));
    }

    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long channelId,
            @AuthenticationPrincipal Long userId) {
        groupService.deleteChannel(channelId, userId);
        return ResponseEntity.noContent().build();
    }
}