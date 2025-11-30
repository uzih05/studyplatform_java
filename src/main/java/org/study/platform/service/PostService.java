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

    @Transactional
    public Post createPost(Long roomId, Long authorId, String title, String content, Post.PostType postType) {
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

    public List<Post> findByRoomId(Long roomId) {
        return postRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    public List<Post> findNoticesByRoomId(Long roomId) {
        return postRepository.findByRoomIdAndPostTypeOrderByCreatedAtDesc(roomId, Post.PostType.NOTICE);
    }

    public List<Post> findGeneralPostsByRoomId(Long roomId) {
        return postRepository.findByRoomIdAndPostTypeOrderByCreatedAtDesc(roomId, Post.PostType.GENERAL);
    }

    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }
}
