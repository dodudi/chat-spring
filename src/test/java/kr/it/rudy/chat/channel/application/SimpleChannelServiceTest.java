package kr.it.rudy.chat.channel.application;

import kr.it.rudy.chat.channel.domain.*;
import kr.it.rudy.chat.channel.dto.*;
import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.server.domain.Server;
import kr.it.rudy.chat.server.domain.ServerMemberRepository;
import kr.it.rudy.chat.server.domain.ServerRepository;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SimpleChannelServiceTest {

    @Mock private ChannelRepository channelRepository;
    @Mock private ChannelCategoryRepository channelCategoryRepository;
    @Mock private ServerRepository serverRepository;
    @Mock private ServerMemberRepository serverMemberRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SimpleChannelService channelService;

    private User member;
    private Server server;
    private ChannelCategory category;
    private Channel channel;

    @BeforeEach
    void setUp() {
        member = User.create("ext-member");
        ReflectionTestUtils.setField(member, "id", 1L);

        server = Server.create(member, "테스트 서버", null, false);
        ReflectionTestUtils.setField(server, "id", 10L);

        category = ChannelCategory.create(server, "일반", 0);
        ReflectionTestUtils.setField(category, "id", 20L);

        channel = Channel.create(server, category, ChannelType.TEXT, "general", null, 0);
        ReflectionTestUtils.setField(channel, "id", 30L);
    }

    @Test
    void createCategory_서버_멤버가_카테고리_생성시_CategoryResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(serverRepository.findById(10L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(channelCategoryRepository.save(any())).willReturn(category);

        // when
        CategoryResponse response = channelService.createCategory("ext-member", 10L,
                new CreateCategoryRequest("일반", 0));

        // then
        assertThat(response.name()).isEqualTo("일반");
        assertThat(response.serverId()).isEqualTo(10L);
    }

    @Test
    void createCategory_서버_멤버가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(serverRepository.findById(10L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> channelService.createCategory("ext-member", 10L,
                new CreateCategoryRequest("일반", 0)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_FORBIDDEN.getMessage());
    }

    @Test
    void createChannel_카테고리_없이_채널_생성시_ChannelResponse_반환() {
        // given
        Channel noCategory = Channel.create(server, null, ChannelType.TEXT, "general", null, 0);
        ReflectionTestUtils.setField(noCategory, "id", 31L);

        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(serverRepository.findById(10L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(channelRepository.save(any())).willReturn(noCategory);

        // when
        ChannelResponse response = channelService.createChannel("ext-member", 10L,
                new CreateChannelRequest("general", ChannelType.TEXT, null, null, 0));

        // then
        assertThat(response.name()).isEqualTo("general");
        assertThat(response.type()).isEqualTo(ChannelType.TEXT);
        assertThat(response.categoryId()).isNull();
    }

    @Test
    void createChannel_카테고리와_함께_채널_생성시_ChannelResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(serverRepository.findById(10L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(channelCategoryRepository.findById(20L)).willReturn(Optional.of(category));
        given(channelRepository.save(any())).willReturn(channel);

        // when
        ChannelResponse response = channelService.createChannel("ext-member", 10L,
                new CreateChannelRequest("general", ChannelType.TEXT, null, 20L, 0));

        // then
        assertThat(response.categoryId()).isEqualTo(20L);
    }

    @Test
    void createChannel_존재하지_않는_카테고리_지정시_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(serverRepository.findById(10L)).willReturn(Optional.of(server));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);
        given(channelCategoryRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> channelService.createChannel("ext-member", 10L,
                new CreateChannelRequest("general", ChannelType.TEXT, null, 999L, 0)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    void findChannelsByServer_채널_목록_반환() {
        // given
        given(serverRepository.existsById(10L)).willReturn(true);
        given(channelRepository.findAllByServerIdOrderByPosition(10L)).willReturn(List.of(channel));

        // when
        List<ChannelResponse> responses = channelService.findChannelsByServer(10L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("general");
    }

    @Test
    void findChannelsByServer_존재하지_않는_서버_조회시_AuthException_발생() {
        // given
        given(serverRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> channelService.findChannelsByServer(999L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SERVER_NOT_FOUND.getMessage());
    }

    @Test
    void findById_존재하는_채널_조회시_ChannelResponse_반환() {
        // given
        given(channelRepository.findById(30L)).willReturn(Optional.of(channel));

        // when
        ChannelResponse response = channelService.findById(30L);

        // then
        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.name()).isEqualTo("general");
    }

    @Test
    void findById_존재하지_않는_채널_조회시_AuthException_발생() {
        // given
        given(channelRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> channelService.findById(999L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_NOT_FOUND.getMessage());
    }

    @Test
    void updateChannel_서버_멤버가_수정시_ChannelResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(channelRepository.findById(30L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);

        // when
        ChannelResponse response = channelService.updateChannel("ext-member", 30L,
                new UpdateChannelRequest("renamed", "새 설명", true, 10));

        // then
        assertThat(response.name()).isEqualTo("renamed");
        assertThat(response.isNsfw()).isTrue();
        assertThat(response.slowmodeSeconds()).isEqualTo(10);
    }

    @Test
    void updateChannel_서버_멤버가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(channelRepository.findById(30L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> channelService.updateChannel("ext-member", 30L,
                new UpdateChannelRequest("renamed", null, false, 0)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_FORBIDDEN.getMessage());
    }

    @Test
    void deleteChannel_서버_멤버가_삭제시_정상_처리() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(channelRepository.findById(30L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(true);

        // when
        channelService.deleteChannel("ext-member", 30L);

        // then
        then(channelRepository).should().delete(channel);
    }

    @Test
    void deleteChannel_서버_멤버가_아닌_경우_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-member")).willReturn(Optional.of(member));
        given(channelRepository.findById(30L)).willReturn(Optional.of(channel));
        given(serverMemberRepository.existsByServerIdAndUserId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> channelService.deleteChannel("ext-member", 30L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.CHANNEL_FORBIDDEN.getMessage());
    }
}
