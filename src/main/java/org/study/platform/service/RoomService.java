package org.study.platform.service;

import org.study.platform.entity.Room;
import org.study.platform.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    // 방 생성
    @Transactional
    public Room createRoom(String roomName, Long creatorId) {
        Room room = new Room(roomName, creatorId);
        return roomRepository.save(room);
    }

    // 방 삭제 (방장만 가능)
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);

        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }

        Room room = roomOpt.get();
        if (!room.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }

        roomRepository.delete(room);
    }

    // 모든 방 조회
    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    // 방 조회 (ID로)
    public Optional<Room> findById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    // 방 이름으로 검색
    public List<Room> searchByName(String roomName) {
        return roomRepository.findByRoomNameContaining(roomName);
    }

    // 특정 사용자가 생성한 방 목록
    public List<Room> findByCreatorId(Long creatorId) {
        return roomRepository.findByCreatorId(creatorId);
    }
}