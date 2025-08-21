package kr.hhplus.be.server.common.lock;

/**
 * 락 키 생성을 위한 인터페이스
 * Spring EL보다 안전하고 명확함
 */
public interface Lockable {

    /**
     * 락 키를 생성한다.
     * 형식: 서버명:기능:식별자
     * 예: "ecommerce:order:process:123"
     */
    String getLockKey();
    
    /**
     * 메서드 파라미터를 기반으로 락 키를 생성한다.
     * AOP에서 메서드 파라미터를 전달받아 정확한 키를 생성할 수 있도록 함
     * 
     * @param methodArgs 메서드 파라미터 배열
     * @return 생성된 락 키
     */
    default String getLockKey(Object[] methodArgs) {
        return getLockKey();
    }
}