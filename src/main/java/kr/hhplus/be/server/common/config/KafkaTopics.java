package kr.hhplus.be.server.common.config;

/**
 * Kafka 토픽 이름 상수 관리 클래스
 *
 * 피드백 반영: "coupon-issued"와 같은 토픽 이름은 공통된 상수로 관리
 *
 * 효과:
 * - 토픽 이름 변경 시 한 곳에서만 수정
 * - 오타 방지 및 컴파일 타임 체크
 * - IDE 자동 완성 지원
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // 인스턴스 생성 방지
    }

    /**
     * 주문 관련 토픽
     */
    public static final String ORDER_COMPLETED = "order-completed";

    /**
     * 쿠폰 관련 토픽
     */
    public static final String COUPON_ISSUE = "coupon-issue";

    /**
     * 사용자 활동 관련 토픽
     */
    public static final String USER_ACTIVITY = "user-activity";

    /**
     * 잔액 관련 토픽
     */
    public static final String BALANCE_ACTIVITY = "balance-activity";

    /**
     * 상품 관련 토픽
     */
    public static final String PRODUCT_ACTIVITY = "product-activity";

    /**
     * 일반 이벤트 토픽 (기본값)
     */
    public static final String GENERAL_EVENTS = "general-events";

    /**
     * Consumer Group 이름들
     */
    public static final class ConsumerGroups {

        private ConsumerGroups() {
            // 인스턴스 생성 방지
        }

        public static final String DATA_PLATFORM_CONSUMER_GROUP = "data-platform-consumer-group";
        public static final String COUPON_ISSUE_CONSUMER_GROUP = "coupon-issue-consumer-group";
        public static final String ORDER_ANALYTICS_GROUP = "order-analytics-group";
        public static final String NOTIFICATION_GROUP = "notification-group";
    }
}