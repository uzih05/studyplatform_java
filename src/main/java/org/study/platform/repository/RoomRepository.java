package org.study.platform.repository;

import org.study.platform.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByRoomNameContaining(String roomName);
    List<Room> findByCreatorId(Long creatorId);
}
