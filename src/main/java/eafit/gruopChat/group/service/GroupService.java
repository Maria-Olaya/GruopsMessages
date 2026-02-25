package eafit.gruopChat.group.service;

import java.util.List;

import eafit.gruopChat.group.dto.*;
import eafit.gruopChat.shared.enums.GroupRole;

public interface GroupService {

    // ===== GRUPOS =====
    GroupResponseDTO createGroup(Long creatorUserId, GroupRequestDTO request);
    GroupResponseDTO getGroupById(Long groupId);
    List<GroupResponseDTO> getGroupsByMember(Long userId);
    GroupResponseDTO updateGroup(Long groupId, Long requestingUserId, GroupRequestDTO request);
    void deleteGroup(Long groupId, Long requestingUserId);

    // ===== MIEMBROS =====
    List<GroupMemberResponseDTO> getMembers(Long groupId);
    void removeMember(Long groupId, Long adminUserId, Long targetUserId);
    void changeGroupRole(Long groupId, Long adminUserId, Long targetUserId, GroupRole newRole);
    void leaveGroup(Long groupId, Long userId);

    // ===== INVITACIONES =====
    InvitationResponseDTO sendInvitation(Long groupId, Long adminUserId, Long invitedUserId);
    InvitationResponseDTO respondToInvitation(Long invitationId, Long userId, boolean accept);
    List<InvitationResponseDTO> getPendingInvitations(Long userId);

    // ===== CANALES =====
    ChannelResponseDTO createChannel(Long groupId, Long adminUserId, ChannelRequestDTO request);
    List<ChannelResponseDTO> getChannels(Long groupId);
    ChannelResponseDTO updateChannel(Long channelId, Long adminUserId, ChannelRequestDTO request);
    void deleteChannel(Long channelId, Long adminUserId);
}