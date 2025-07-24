package kr.hhplus.be.server.balance.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.balance.domain.UserBalance;

/**
 * 잔액 저장소 인터페이스
 * 
 * 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용
 * - 낙관적 락 지원을 위한 메서드 제공
 * - 테스트 시 Mock으로 쉽게 대체 가능
 * 
 * 책임:
 * - 사용자 잔액 CRUD
 * - 잔액 변동 이력 저장
 * - 낙관적 락을 통한 동시성 제어
 */
public interface UserBalanceRepository {

    /**
     * 사용자 ID로 잔액 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 잔액 정보 (없으면 Empty)
     */
    Optional<UserBalance> findByUserId(Long userId);

    /**
     * 잔액 저장 (생성 또는 수정)
     * 
     * 🔒 낙관적 락:
     * - version 필드 자동 증가
     * - 동시 수정 시 충돌 감지
     * 
     * @param userBalance 저장할 잔액 정보
     * @return 저장된 잔액 정보 (version 증가됨)
     */
    UserBalance save(UserBalance userBalance);

    /**
     * ID로 잔액 조회
     * 
     * @param id 잔액 ID
     * @return 잔액 정보
     */
    Optional<UserBalance> findById(Long id);

    /**
     * 모든 잔액 조회 (관리자용)
     * 
     * @return 전체 잔액 목록
     */
    List<UserBalance> findAll();

    /**
     * 사용자 잔액 삭제
     * 
     * @param userBalance 삭제할 잔액 정보
     */
    void delete(UserBalance userBalance);
}
