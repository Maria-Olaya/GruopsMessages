package eafit.gruopChat.group.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<GroupResponseDTO> createGroup(
            @RequestParam Long creatorUserId,
            @Valid @RequestBody GroupRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(creatorUserId, request));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> getMyGroups(@RequestParam Long userId) {
        return ResponseEntity.ok(groupService.getGroupsByMember(userId));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> updateGroup(
            @PathVariable Long groupId,
            @RequestParam Long requestingUserId,
            @Valid @RequestBody GroupRequestDTO request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, requestingUserId, request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @RequestParam Long requestingUserId) {
        groupService.deleteGroup(groupId, requestingUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponseDTO>> getMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getMembers(groupId));
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestParam Long adminUserId) {
        groupService.removeMember(groupId, adminUserId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/members/{targetUserId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestParam Long adminUserId,
            @RequestParam GroupRole role) {
        groupService.changeGroupRole(groupId, adminUserId, targetUserId, role);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @RequestParam Long userId) {
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/invitations")
    public ResponseEntity<InvitationResponseDTO> sendInvitation(
            @PathVariable Long groupId,
            @RequestParam Long adminUserId,
            @RequestParam Long invitedUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.sendInvitation(groupId, adminUserId, invitedUserId));
    }

    @PatchMapping("/invitations/{invitationId}")
    public ResponseEntity<InvitationResponseDTO> respondToInvitation(
            @PathVariable Long invitationId,
            @RequestParam Long userId,
            @RequestParam boolean accept) {
        return ResponseEntity.ok(groupService.respondToInvitation(invitationId, userId, accept));
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<InvitationResponseDTO>> getPendingInvitations(
            @RequestParam Long userId) {
        return ResponseEntity.ok(groupService.getPendingInvitations(userId));
    }

    @PostMapping("/{groupId}/channels")
    public ResponseEntity<ChannelResponseDTO> createChannel(
            @PathVariable Long groupId,
            @RequestParam Long adminUserId,
            @Valid @RequestBody ChannelRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createChannel(groupId, adminUserId, request));
    }

    @GetMapping("/{groupId}/channels")
    public ResponseEntity<List<ChannelResponseDTO>> getChannels(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getChannels(groupId));
    }

    @PutMapping("/channels/{channelId}")
    public ResponseEntity<ChannelResponseDTO> updateChannel(
            @PathVariable Long channelId,
            @RequestParam Long adminUserId,
            @Valid @RequestBody ChannelRequestDTO request) {
        return ResponseEntity.ok(groupService.updateChannel(channelId, adminUserId, request));
    }

    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long channelId,
            @RequestParam Long adminUserId) {
        groupService.deleteChannel(channelId, adminUserId);
        return ResponseEntity.noContent().build();
    }
}