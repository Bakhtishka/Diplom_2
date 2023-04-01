    public class UserResponse {
        Boolean success;
        String accessToken;

        public UserResponse() {
        }

        public UserResponse(Boolean success, String accessToken) {
            this.success = success;
            this.accessToken = accessToken;
        }
    }

