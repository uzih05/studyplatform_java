package org.study.platform.repository;

import org.study.platform.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // 방 이름으로 검색
    List<Room> findByRoomNameContaining(String roomName);

    // 생성자 ID로 방 목록 조회
    List<Room> findByCreatorId(Long creatorId);
}