package com.ljj.campusrun.social;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ljj.campusrun.repository.ClubMemberRepository;
import com.ljj.campusrun.repository.ClubMessageRepository;
import com.ljj.campusrun.repository.ClubRepository;
import com.ljj.campusrun.repository.FeedPostRepository;
import com.ljj.campusrun.repository.PostCommentRepository;
import com.ljj.campusrun.repository.PostLikeRepository;
import com.ljj.campusrun.repository.PostReportRepository;
import com.ljj.campusrun.repository.UserRepository;
import org.junit.jupiter.api.Test;

class SocialServiceTest {

    @Test
    void listClubMessagesShouldRequireMembership() {
        FeedPostRepository feedPostRepository = mock(FeedPostRepository.class);
        PostCommentRepository postCommentRepository = mock(PostCommentRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        PostReportRepository postReportRepository = mock(PostReportRepository.class);
        ClubRepository clubRepository = mock(ClubRepository.class);
        ClubMessageRepository clubMessageRepository = mock(ClubMessageRepository.class);
        ClubMemberRepository clubMemberRepository = mock(ClubMemberRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SocialService socialService = new SocialService(
                feedPostRepository,
                postCommentRepository,
                postLikeRepository,
                postReportRepository,
                clubRepository,
                clubMessageRepository,
                clubMemberRepository,
                userRepository
        );

        when(clubMemberRepository.existsByClubIdAndUserIdAndActiveTrue(2L, 1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> socialService.listClubMessages(1L, 2L));
    }
}
