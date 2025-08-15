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
}