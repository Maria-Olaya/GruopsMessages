package eafit.gruopChat.group.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.group.dto.*;
import eafit.gruopChat.group.exception.*;
import eafit.gruopChat.group.model.*;
import eafit.gruopChat.group.repository.*;
import eafit.gruopChat.group.service.GroupService;
import eafit.gruopChat.shared.enums.GroupRole;
import eafit.gruopChat.shared.enums.InvitationStatus;
import eafit.gruopChat.user.exception.UserNotFoundException;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;

@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final GroupInvitationRepository invitationRepository;
    private final UserRepository userRepository;

    public GroupServiceImpl(GroupRepository groupRepository,
                            GroupMemberRepository memberRepository,
                            ChannelRepository channelRepository,
                            GroupInvitationRepository invitationRepository,
                            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.channelRepository = channelRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    // ===================== GRUPOS =====================

    @Override
    public GroupResponseDTO createGroup(Long creatorUserId, GroupRequestDTO request) {
        User creator = findActiveUser(creatorUserId);

        Group group = new Group();
        group.setName(request.name());
        group.setDescription(request.description());
        group.setCreatedBy(creator);
        group.setPrivate(request.isPrivate());

        Group saved = groupRepository.save(group);

        GroupMember adminMember = new GroupMember();
        adminMember.setGroup(saved);
        adminMember.setUser(creator);
        adminMember.setRole(GroupRole.ADMIN);
        memberRepository.save(adminMember);

        return mapGroupToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponseDTO getGroupById(Long groupId) {
        return mapGroupToDTO(findGroup(groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> getGroupsByMember(Long userId) {
        return groupRepository.findGroupsByMemberUserId(userId)
                .stream().map(this::mapGroupToDTO).collect(Collectors.toList());
    }

    @Override
    public GroupResponseDTO updateGroup(Long groupId, Long requestingUserId, GroupRequestDTO request) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, requestingUserId);
        group.setName(request.name());
        group.setDescription(request.description());
        group.setPrivate(request.isPrivate());
        return mapGroupToDTO(group);
    }

    @Override
    public void deleteGroup(Long groupId, Long requestingUserId) {
        Group group = findGroup(groupId);
        if (!group.getCreatedBy().getUserId().equals(requestingUserId)) {
            throw new NotGroupAdminException();
        }
        groupRepository.delete(group);
    }

    // ===================== MIEMBROS =====================

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponseDTO> getMembers(Long groupId) {
        findGroup(groupId);
        return memberRepository.findByGroupGroupId(groupId)
                .stream().map(this::mapMemberToDTO).collect(Collectors.toList());
    }

    @Override
    public void removeMember(Long groupId, Long adminUserId, Long targetUserId) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        if (group.getCreatedBy().getUserId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot remove the group creator");
        }
        memberRepository.deleteByGroupGroupIdAndUserUserId(groupId, targetUserId);
    }

    @Override
    public void changeGroupRole(Long groupId, Long adminUserId, Long targetUserId, GroupRole newRole) {
        findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        GroupMember member = memberRepository
                .findByGroupGroupIdAndUserUserId(groupId, targetUserId)
                .orElseThrow(() -> new NotMemberException(targetUserId, groupId));
        member.setRole(newRole);
    }

    @Override
    public void leaveGroup(Long groupId, Long userId) {
        Group group = findGroup(groupId);
        if (group.getCreatedBy().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Group creator cannot leave. Transfer ownership or delete the group.");
        }
        if (!memberRepository.existsByGroupGroupIdAndUserUserId(groupId, userId)) {
            throw new NotMemberException(userId, groupId);
        }
        memberRepository.deleteByGroupGroupIdAndUserUserId(groupId, userId);
    }

    // ===================== INVITACIONES =====================

    @Override
    public InvitationResponseDTO sendInvitation(Long groupId, Long adminUserId, Long invitedUserId) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        User admin = findActiveUser(adminUserId);
        User invitedUser = findActiveUser(invitedUserId);

        if (memberRepository.existsByGroupGroupIdAndUserUserId(groupId, invitedUserId)) {
            throw new AlreadyMemberException(invitedUserId, groupId);
        }

        invitationRepository.findByGroupGroupIdAndInvitedUserUserIdAndStatus(
                groupId, invitedUserId, InvitationStatus.PENDING)
                .ifPresent(i -> { throw new AlreadyMemberException(invitedUserId, groupId); });

        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setInvitedBy(admin);
        invitation.setInvitedUser(invitedUser);
        invitation.setStatus(InvitationStatus.PENDING);

        return mapInvitationToDTO(invitationRepository.save(invitation));
    }

    @Override
    public InvitationResponseDTO respondToInvitation(Long invitationId, Long userId, boolean accept) {
        GroupInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));

        if (!invitation.getInvitedUser().getUserId().equals(userId)) {
            throw new NotGroupAdminException();
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation already responded");
        }

        invitation.setStatus(accept ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());

        if (accept) {
            GroupMember member = new GroupMember();
            member.setGroup(invitation.getGroup());
            member.setUser(invitation.getInvitedUser());
            member.setRole(GroupRole.MEMBER);
            memberRepository.save(member);
        }

        return mapInvitationToDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponseDTO> getPendingInvitations(Long userId) {
        return invitationRepository
                .findByInvitedUserUserIdAndStatus(userId, InvitationStatus.PENDING)
                .stream().map(this::mapInvitationToDTO).collect(Collectors.toList());
    }

    // ===================== CANALES =====================

    @Override
    public ChannelResponseDTO createChannel(Long groupId, Long adminUserId, ChannelRequestDTO request) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        User admin = findActiveUser(adminUserId);

        if (channelRepository.existsByGroupGroupIdAndName(groupId, request.name())) {
            throw new DuplicateChannelNameException(request.name());
        }

        Channel channel = new Channel();
        channel.setGroup(group);
        channel.setName(request.name());
        channel.setDescription(request.description());
        channel.setCreatedBy(admin);

        return mapChannelToDTO(channelRepository.save(channel));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDTO> getChannels(Long groupId) {
        findGroup(groupId);
        return channelRepository.findByGroupGroupId(groupId)
                .stream().map(this::mapChannelToDTO).collect(Collectors.toList());
    }

    @Override
    public ChannelResponseDTO updateChannel(Long channelId, Long adminUserId, ChannelRequestDTO request) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
        assertAdmin(channel.getGroup().getGroupId(), adminUserId);
        channel.setName(request.name());
        channel.setDescription(request.description());
        return mapChannelToDTO(channel);
    }

    @Override
    public void deleteChannel(Long channelId, Long adminUserId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
        assertAdmin(channel.getGroup().getGroupId(), adminUserId);
        channelRepository.delete(channel);
    }

    // ===================== HELPERS =====================

    private Group findGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.isEnabled()) throw new UserNotFoundException(userId);
        return user;
    }

    private void assertAdmin(Long groupId, Long userId) {
        GroupMember member = memberRepository
                .findByGroupGroupIdAndUserUserId(groupId, userId)
                .orElseThrow(NotGroupAdminException::new);
        if (member.getRole() != GroupRole.ADMIN) {
            throw new NotGroupAdminException();
        }
    }

    // ===================== MAPPERS =====================

    private GroupResponseDTO mapGroupToDTO(Group group) {
        int memberCount = memberRepository.findByGroupGroupId(group.getGroupId()).size();
        int channelCount = channelRepository.findByGroupGroupId(group.getGroupId()).size();
        return new GroupResponseDTO(
                group.getGroupId(), group.getName(), group.getDescription(),
                group.getCreatedBy().getUserId(), group.getCreatedBy().getName(),
                group.isPrivate(), memberCount, channelCount, group.getCreatedAt());
    }

    private GroupMemberResponseDTO mapMemberToDTO(GroupMember member) {
        return new GroupMemberResponseDTO(
                member.getUser().getUserId(), member.getUser().getName(),
                member.getUser().getEmail(), member.getRole(), member.getJoinedAt());
    }

    private ChannelResponseDTO mapChannelToDTO(Channel channel) {
        return new ChannelResponseDTO(
                channel.getChannelId(), channel.getGroup().getGroupId(),
                channel.getName(), channel.getDescription(),
                channel.getCreatedBy().getUserId(), channel.getCreatedBy().getName(),
                channel.getCreatedAt());
    }

    private InvitationResponseDTO mapInvitationToDTO(GroupInvitation inv) {
        return new InvitationResponseDTO(
                inv.getInvitationId(), inv.getGroup().getGroupId(), inv.getGroup().getName(),
                inv.getInvitedBy().getUserId(), inv.getInvitedBy().getName(),
                inv.getInvitedUser().getUserId(), inv.getInvitedUser().getName(),
                inv.getStatus(), inv.getSentAt(), inv.getRespondedAt());
    }
}