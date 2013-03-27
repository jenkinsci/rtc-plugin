package hudson.util;

public class SecretTestHelper {
    
    /**
     * Test helper to allow the package-protected Secret value to be configured in tests.
     */
    public static void setSecret(String value) {        
        Secret.SECRET = value;         
    }
}
