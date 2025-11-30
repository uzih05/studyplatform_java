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

    @Transactional
    public Room createRoom(String roomName, Long creatorId) {
        Room room = new Room(roomName, creatorId);
        return roomRepository.save(room);
    }

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

    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> findById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    public List<Room> searchByName(String roomName) {
        return roomRepository.findByRoomNameContaining(roomName);
    }

    public List<Room> findByCreatorId(Long creatorId) {
        return roomRepository.findByCreatorId(creatorId);
    }
}
