package org.study.platform.service;

import org.study.platform.entity.Post;
import org.study.platform.entity.Room;
import org.study.platform.repository.PostRepository;
import org.study.platform.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final RoomRepository roomRepository;

    @Autowired
    public PostService(PostRepository postRepository, RoomRepository roomRepository) {
        this.postRepository = postRepository;
        this.roomRepository = roomRepository;
    }

    // 게시글 작성
    @Transactional
    public Post createPost(Long roomId, Long authorId, String title, String content, Post.PostType postType) {
        // 공지사항은 방장만 작성 가능
        if (postType == Post.PostType.NOTICE) {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 방입니다.");
            }

            Room room = roomOpt.get();
            if (!room.getCreatorId().equals(authorId)) {
                throw new IllegalArgumentException("공지사항은 방장만 작성할 수 있습니다.");
            }
        }

        Post post = new Post(roomId, authorId, title, content, postType);
        return postRepository.save(post);
    }

    // 게시글 수정
    @Transactional
    public Post updatePost(Long postId, Long userId, String title, String content) {
        Optional<Post> postOpt = postRepository.findById(postId);

        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

        Post post = postOpt.get();
        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 수정할 수 있습니다.");
        }

        post.setTitle(title);
        post.setContent(content);
        return postRepository.save(post);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Optional<Post> postOpt = postRepository.findById(postId);

        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

        Post post = postOpt.get();
        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    // 특정 방의 모든 게시글 조회 (최신순)
    public List<Post> findByRoomId(Long roomId) {
        return postRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    // 특정 방의 공지사항만 조회
    public List<Post> findNoticesByRoomId(Long roomId) {
        return postRepository.findByRoomIdAndPostType(roomId, Post.PostType.NOTICE);
    }

    // 특정 방의 일반 게시글만 조회
    public List<Post> findGeneralPostsByRoomId(Long roomId) {
        return postRepository.findByRoomIdAndPostType(roomId, Post.PostType.GENERAL);
    }

    // 게시글 조회 (ID로)
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }
}