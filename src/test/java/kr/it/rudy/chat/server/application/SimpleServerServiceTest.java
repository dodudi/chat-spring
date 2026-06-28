package kr.it.rudy.chat.server.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.server.domain.*;
import kr.it.rudy.chat.server.dto.*;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SimpleServerServiceTest {

    @Mock private ServerRepository serverRepository;
    @Mock private ServerMemberRepository serverMemberRepository;
    @Mock private ServerInviteRepository serverInviteRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SimpleServerService serverService;

    private User owner;
    private User nonOwner;
    private Server server;

    @BeforeEach
    void setUp() {
        owner = User.create("ext-owner");
        ReflectionTestUtils.setField(owner, "id", 1L);

        nonOwner = User.create("ext-other");
        ReflectionTestUtils.setField(nonOwner, "id", 2L);

        server = Server.create(owner, "테스트 서버", "설명", false);
        ReflectionTestUtils.setField(server, "id", 100L);
    }

    @Test
    void createServer_정상_생성시_ServerResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-owner")).willReturn(Optional.of(owner));
        given(serverRepository.save(any())).willReturn(server);
        given(serverMemberRepository.save(any())).willReturn(ServerMember.create(server, owner));

        // when
        ServerResponse response = serverService.createServer("ext-owner",
                new CreateServerRequest("테스트 서버", "설명", false));

        // then
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("테스트 서버");
        assertThat(response.ownerId()).isEqualTo(1L);
        then(serverMemberRepository).should().save(any());
    }

    @Test
    void findById_존재하는_서버_조회시_ServerResponse_반환() {
        // given
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));

        // when
        ServerResponse response = serverService.findById(100L);

        // then
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("테스트 서버");
    }

    @Test
    void findById_존재하지_않는_서버_조회시_AuthException_발생() {
        // given
        given(serverRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.findById(999L))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void updateServer_소유자가_수정시_ServerResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-owner")).willReturn(Optional.of(owner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));

        // when
        ServerResponse response = serverService.updateServer("ext-owner", 100L,
                new UpdateServerRequest("수정된 이름", "수정된 설명", true));

        // then
        assertThat(response.name()).isEqualTo("수정된 이름");
        assertThat(response.isPublic()).isTrue();
    }

    @Test
    void updateServer_소유자가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));

        // when & then
        assertThatThrownBy(() -> serverService.updateServer("ext-other", 100L,
                new UpdateServerRequest("수정된 이름", null, false)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_FORBIDDEN.getMessage());
    }

    @Test
    void deleteServer_소유자가_삭제시_정상_삭제() {
        // given
        given(userRepository.findByExternalId("ext-owner")).willReturn(Optional.of(owner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));

        // when
        serverService.deleteServer("ext-owner", 100L);

        // then
        then(serverRepository).should().delete(server);
    }

    @Test
    void deleteServer_소유자가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));

        // when & then
        assertThatThrownBy(() -> serverService.deleteServer("ext-other", 100L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_FORBIDDEN.getMessage());
    }

    @Test
    void joinServer_유효한_초대코드로_참여시_ServerMemberResponse_반환() {
        // given
        ServerInvite invite = ServerInvite.create(server, owner, "validcode12345", null, null);
        ServerMember member = ServerMember.create(server, nonOwner);

        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverInviteRepository.findByCode("validcode12345")).willReturn(Optional.of(invite));
        given(serverMemberRepository.existsByServerIdAndUserId(100L, 2L)).willReturn(false);
        given(serverMemberRepository.save(any())).willReturn(member);

        // when
        ServerMemberResponse response = serverService.joinServer("ext-other", "validcode12345");

        // then
        assertThat(response.externalId()).isEqualTo("ext-other");
        assertThat(invite.getUses()).isEqualTo(1);
    }

    @Test
    void joinServer_존재하지_않는_초대코드_사용시_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverInviteRepository.findByCode("invalid")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.joinServer("ext-other", "invalid"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.INVITE_NOT_FOUND.getMessage());
    }

    @Test
    void joinServer_만료된_초대코드_사용시_AuthException_발생() {
        // given
        Instant past = Instant.now().minusSeconds(3600);
        ServerInvite expiredInvite = ServerInvite.create(server, owner, "expiredcode123", null, past);

        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverInviteRepository.findByCode("expiredcode123")).willReturn(Optional.of(expiredInvite));

        // when & then
        assertThatThrownBy(() -> serverService.joinServer("ext-other", "expiredcode123"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.INVITE_EXPIRED.getMessage());
    }

    @Test
    void joinServer_소진된_초대코드_사용시_AuthException_발생() {
        // given
        ServerInvite exhaustedInvite = ServerInvite.create(server, owner, "exhausted12345", 1, null);
        exhaustedInvite.incrementUses();

        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverInviteRepository.findByCode("exhausted12345")).willReturn(Optional.of(exhaustedInvite));

        // when & then
        assertThatThrownBy(() -> serverService.joinServer("ext-other", "exhausted12345"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.INVITE_EXHAUSTED.getMessage());
    }

    @Test
    void joinServer_이미_가입한_서버에_재참여시_AuthException_발생() {
        // given
        ServerInvite invite = ServerInvite.create(server, owner, "validcode12345", null, null);

        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverInviteRepository.findByCode("validcode12345")).willReturn(Optional.of(invite));
        given(serverMemberRepository.existsByServerIdAndUserId(100L, 2L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> serverService.joinServer("ext-other", "validcode12345"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_ALREADY_JOINED.getMessage());
    }

    @Test
    void leaveServer_소유자가_아닌_멤버_탈퇴시_정상_처리() {
        // given
        ServerMember member = ServerMember.create(server, nonOwner);

        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));
        given(serverMemberRepository.findByServerIdAndUserId(100L, 2L)).willReturn(Optional.of(member));

        // when
        serverService.leaveServer("ext-other", 100L);

        // then
        then(serverMemberRepository).should().delete(member);
    }

    @Test
    void leaveServer_소유자가_탈퇴시_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-owner")).willReturn(Optional.of(owner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));

        // when & then
        assertThatThrownBy(() -> serverService.leaveServer("ext-owner", 100L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_OWNER_CANNOT_LEAVE.getMessage());
    }

    @Test
    void findMembers_서버_멤버_목록_반환() {
        // given
        ServerMember member = ServerMember.create(server, nonOwner);

        given(serverRepository.existsById(100L)).willReturn(true);
        given(serverMemberRepository.findAllByServerId(100L)).willReturn(List.of(member));

        // when
        List<ServerMemberResponse> responses = serverService.findMembers(100L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).externalId()).isEqualTo("ext-other");
    }

    @Test
    void findMembers_존재하지_않는_서버_조회시_AuthException_발생() {
        // given
        given(serverRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> serverService.findMembers(999L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_NOT_FOUND.getMessage());
    }

    @Test
    void createInvite_멤버가_초대_생성시_InviteResponse_반환() {
        // given
        ServerInvite invite = ServerInvite.create(server, owner, "newcode1234567", 10, null);
        ReflectionTestUtils.setField(invite, "id", 1L);

        given(userRepository.findByExternalId("ext-owner")).willReturn(Optional.of(owner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(100L, 1L)).willReturn(true);
        given(serverInviteRepository.save(any())).willReturn(invite);

        // when
        InviteResponse response = serverService.createInvite("ext-owner", 100L,
                new CreateInviteRequest(10, null));

        // then
        assertThat(response.code()).isEqualTo("newcode1234567");
        assertThat(response.maxUses()).isEqualTo(10);
        assertThat(response.serverId()).isEqualTo(100L);
    }

    @Test
    void createInvite_서버_멤버가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-other")).willReturn(Optional.of(nonOwner));
        given(serverRepository.findById(100L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(100L, 2L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> serverService.createInvite("ext-other", 100L,
                new CreateInviteRequest(null, null)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_MEMBER_NOT_FOUND.getMessage());
    }
}
